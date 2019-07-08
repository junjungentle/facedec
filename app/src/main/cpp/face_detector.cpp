//
// Created by wzn on 2018/12/25.
//

#include "face_detector.h"


FaceDetector::FaceDetector(const std::string& landmarkmodel)
        : mLandMarkModel(landmarkmodel){
    face_detector = dlib::get_frontal_face_detector();
    if (!mLandMarkModel.empty()) {
        dlib::deserialize(mLandMarkModel) >> shape_predictor;
    }
}


int FaceDetector::Detect(const cv::Mat &image) {

    if (image.empty())
        return 0;

    if (image.channels() == 1) {
        cv::cvtColor(image, image, CV_GRAY2BGR);
    }

    dlib::cv_image<dlib::bgr_pixel> dlib_image(image);

    det_rects.clear();

    det_rects = face_detector(dlib_image);

    mFaceShapeMap.clear();
    if (det_rects.size() != 0 && mLandMarkModel.empty() == false) {
        for (unsigned long j = 0; j < det_rects.size(); ++j) {
            dlib::full_object_detection shape = shape_predictor(dlib_image,det_rects[j]);
            mFaceShapeMap[j] = shape;
        }
    }

    return det_rects.size();
}

//返回检测到的人脸矩形特征框
std::vector<dlib::rectangle> FaceDetector::getDetResultRects() {
    return det_rects;
}

//返回检测到的人脸特征点
std::unordered_map<int, dlib::full_object_detection>& FaceDetector::getFaceShapeMap() {
    return mFaceShapeMap;
}

