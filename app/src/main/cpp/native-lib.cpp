/*
 *  Created on: Oct 20, 2015
 *      Author: Tzutalin
 *
 *  Copyright (c) 2015 Tzutalin. All rights reserved.
 */
// Modified by wzn on Dec 25, 2018

#include <jni.h>
#include <android/bitmap.h>
#include <jni_common/jni_bitmap2mat.h>
#include <jni_common/jni_primitives.h>
#include <face_detector.h>
#include <jni_common/jni_utils.h>

using namespace cv;

//native方法实现

JNI_RectInfo *g_pJNI_RectInfo;

JavaVM *g_javaVM = NULL;

//该函数在加载本地库时被调用
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    g_javaVM = vm;
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    //初始化g_pJNI_RectInfo
    g_pJNI_RectInfo = new JNI_RectInfo(env);

    return JNI_VERSION_1_6;
}

//该函数用于执行清理操作
void JNI_OnUnload(JavaVM *vm, void *reserved) {

    g_javaVM = NULL;

    delete g_pJNI_RectInfo;
}


namespace {

#define JAVA_NULL 0
    using DetPtr = FaceDetector *;

    //用于存放人脸检测类对象的指针，关联Java层对象与C++底层对象（相互呼应）
    class JNI_DlibUtils {
    public:
        JNI_DlibUtils(JNIEnv *env) {
            jclass clazz = env->FindClass(CLASSNAME_DLIB_UTILS);
            mNativeContext = env->GetFieldID(clazz, "mNativeFaceDetContext", "J");
            env->DeleteLocalRef(clazz);
        }

        DetPtr getDetectorPtrFromJava(JNIEnv *env, jobject thiz) {
            DetPtr const p = (DetPtr) env->GetLongField(thiz, mNativeContext);
            return p;
        }

        void setDetectorPtrToJava(JNIEnv *env, jobject thiz, jlong ptr) {
            env->SetLongField(thiz, mNativeContext, ptr);
        }

        jfieldID mNativeContext;
    };

    // Protect getting/setting and creating/deleting pointer between java/native
    std::mutex gLock;

    std::shared_ptr<JNI_DlibUtils> getJNI_DlibUtils(JNIEnv *env) {
        static std::once_flag sOnceInitflag;
        static std::shared_ptr<JNI_DlibUtils> sJNI_DlibUtils;
        std::call_once(sOnceInitflag, [env]() {
            sJNI_DlibUtils = std::make_shared<JNI_DlibUtils>(env);
        });
        return sJNI_DlibUtils;
    }

    //从java对象获取它持有的c++对象指针
    DetPtr const getDetPtr(JNIEnv *env, jobject thiz) {
        std::lock_guard<std::mutex> lock(gLock);
        return getJNI_DlibUtils(env)->getDetectorPtrFromJava(env, thiz);
    }

    // The function to set a pointer to java and delete it if newPtr is empty
    //C++对象new以后，将指针转成long型返回给java对象持有
    void setDetPtr(JNIEnv *env, jobject thiz, DetPtr newPtr) {
        std::lock_guard<std::mutex> lock(gLock);
        DetPtr oldPtr = getJNI_DlibUtils(env)->getDetectorPtrFromJava(env, thiz);
        if (oldPtr != JAVA_NULL) {
            delete oldPtr;
        }

        getJNI_DlibUtils(env)->setDetectorPtrToJava(env, thiz, (jlong) newPtr);
    }

}  // end unnamespace

#ifdef __cplusplus
extern "C" {
#endif

#define DLIB_FACE_JNI_METHOD(METHOD_NAME) Java_com_wzn_dlibtool_DlibUtils_##METHOD_NAME

void JNIEXPORT
DLIB_FACE_JNI_METHOD(jniNativeClassInit)(JNIEnv *env, jclass _this) {}

//生成需要返回的结果数组
jobjectArray getRecResult(JNIEnv *env, DetPtr faceDetector, const int &size) {
    //根据检测到的人脸数创建相应的jobjectArray
    jobjectArray jDetRetArray = JNI_RectInfo::createJObjectArray(env, size);
    for (int i = 0; i < size; i++) {
        //对检测到的每一个人脸创建对应的实例对象，然后插入数组
        jobject jDetRet = JNI_RectInfo::createJObject(env);
        env->SetObjectArrayElement(jDetRetArray, i, jDetRet);
        dlib::rectangle rect = faceDetector->getDetResultRects()[i];

        //将人脸矩形框的值赋给对应的jobject实例对象
        g_pJNI_RectInfo->setRect(env, jDetRet, rect.left(), rect.top(),
                                     rect.right(), rect.bottom());
        g_pJNI_RectInfo->setLabel(env, jDetRet, "face");
        std::unordered_map<int, dlib::full_object_detection>& faceShapeMap =
                faceDetector->getFaceShapeMap();
        if (faceShapeMap.find(i) != faceShapeMap.end()) {
            dlib::full_object_detection shape = faceShapeMap[i];
            for (unsigned long j = 0; j < shape.num_parts(); j++) {
                int x = shape.part(j).x();
                int y = shape.part(j).y();
                // Call addLandmark
                g_pJNI_RectInfo->addLandmark(env, jDetRet, x, y);
            }
        }
    }
    return jDetRetArray;
}

JNIEXPORT jobjectArray JNICALL
DLIB_FACE_JNI_METHOD(jniBitmapDet)(JNIEnv *env, jobject thiz, jobject bitmap) {
    cv::Mat rgbaMat;
    cv::Mat bgrMat;
    jniutils::ConvertBitmapToRGBAMat(env, bitmap, rgbaMat, true);
    cv::cvtColor(rgbaMat, bgrMat, cv::COLOR_RGBA2BGR);
    //获取人脸检测类指针
    DetPtr mDetPtr = getDetPtr(env, thiz);
    //调用人脸检测算法，返回检测到的人脸数
    jint size = mDetPtr->Detect(bgrMat);
    //返回检测结果
    return getRecResult(env, mDetPtr, size);
}

jint JNIEXPORT JNICALL
DLIB_FACE_JNI_METHOD(jniInit)(JNIEnv *env, jobject thiz, jstring jLandmarkPath) {

    std::string landmarkPath = jniutils::convertJStrToString(env, jLandmarkPath);
    DetPtr mDetPtr = new FaceDetector(landmarkPath);
   //设置人脸检测类指针
    setDetPtr(env, thiz, mDetPtr);
    return JNI_OK;
}


jint JNIEXPORT JNICALL
DLIB_FACE_JNI_METHOD(jniDeInit)(JNIEnv *env, jobject thiz) {
    //指针置0
    setDetPtr(env, thiz, JAVA_NULL);
    return JNI_OK;
}

#ifdef __cplusplus
}
#endif
