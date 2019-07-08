package com.wzn.dlibtool;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;


import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;

public class DlibUtils {
    private static final String TAG = "dlib";
    private static final String fileFolder= "StandardInfo/";//文件夹名
    private static final String fileName= "StandardInfo.txt";//文件名
    private static final float distanceThreshold= 30;//瞳距阈值
    private static final float angleThreshold = 5;//roll角度阈值

    // accessed by native methods
    @SuppressWarnings("unused")
    private long mNativeFaceDetContext;
    private String mLandMarkPath = "";

    static {
        try {
            System.loadLibrary("opencv_java3");
            System.loadLibrary("native-lib");
            jniNativeClassInit();
            Log.d(TAG, "jniNativeClassInit success");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "library not found");
        }
    }

    /**
     * 加载模型文件
     *
     * @return
     */
    public boolean load() {
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/dlib.dat");
        if (f.exists()) {
            if (jniInit(Environment.getExternalStorageDirectory().getAbsolutePath() + "/dlib.dat") == 0) {
                return true;
            }
        }
        return false;
    }


    /**
     * 检测人脸及关键点
     *
     * @param bitmap 传入bitmap类型
     * @return 以RectInfo类型返回, 包含4个点坐标, 以及68个关键点坐标
     */
    @Nullable
    @WorkerThread
    public List<RectInfo> detect(@NonNull Bitmap bitmap) {
        RectInfo[] detRets = jniBitmapDet(bitmap);
        return Arrays.asList(detRets);
    }

    /**
     * 检测人脸及关键点
     *
     * @param mat 传入Mat类型
     * @return 以RectInfo类型返回, 包含4个点坐标, 以及68个关键点坐标
     */
    @Nullable
    @WorkerThread
    public List<RectInfo> detect(@NonNull Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        RectInfo[] detRets = jniBitmapDet(bitmap);
        return Arrays.asList(detRets);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        //垃圾回收前, 释放本地资源
        release();
    }

    /**
     * 释放Native的资源
     */
    public void release() {
        jniDeInit();
    }

    /**
     * 检查资源文件是否存在,不存在则从Assets中复制到本地储存
     *
     * @param context  调用该方法的Activity的context
     * @param dataPath dlib.dat存放的目录
     */
    public void checkExist(Context context, String dataPath) {
        try {
            File f = new File(dataPath + "/" + "dlib.dat");
            if (!f.exists()) {
                copyFilesAssets(context, "", "dlib.dat", dataPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从assets目录中复制整个文件夹内容
     *
     * @param context Context 使用CopyFiles类的Activity
     * @param oldPath String  原文件路径  如：/aa
     * @param newPath String  复制后路径  如：xx:/bb/cc
     */
    private void copyFilesAssets(Context context, String oldPath, String targetFileName, String newPath) {
        try {
            String fileNames[] = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {//如果是目录
                for (String fileName : fileNames) {
                    copyFilesAssets(context, oldPath + fileName, targetFileName, newPath + "/" + fileName);
                }
            } else {//如果是文件
                if (oldPath.equals(targetFileName)) {
                    InputStream is = context.getAssets().open(oldPath);
                    FileOutputStream fos = new FileOutputStream(new File(newPath));
                    byte[] buffer = new byte[1024];
                    int byteCount;
                    while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                        fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                    }
                    fos.flush();//刷新缓冲区
                    is.close();
                    fos.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算瞳距
     *
     * @param landmark 68个特征点
     * @return distance 瞳距
     */
    public float getDistance(ArrayList<android.graphics.Point> landmark){
//        if(landmark.size()!=0){
//            return (float)1;
//        }else{
//            return (float)2;
//        }
        android.graphics.Point p1 = landmark.get(42);
        android.graphics.Point p2 = landmark.get(36);
        android.graphics.Point p3 = landmark.get(45);
        android.graphics.Point p4 = landmark.get(39);

//        float leftDistance = Math.sqrt(((int)p1.x) - (int)(p2.x)*((int)(p1.x) - (int)(p2.x)) +
//                ((int)(p1.y) - (int)(p2.y))*((int)(p1.y) - (int)(p2.y)));
//        float rightDistance = Math.sqrt(((int)(p3.x) - (int)(p4.x))*((int)(p3.x) - (int)(p4.x)) +
//                ((int)(p3.y) - (int)(p4.y))*((int)(p3.y) - (int)(p4.y)));
        float leftDistance = (float)Math.sqrt(((p1.x) - (p2.x))*((p1.x) -(p2.x)) +
                ((p1.y) - (p2.y))*((p1.y) - (p2.y)));
        float rightDistance = (float)Math.sqrt(((p3.x) - (p4.x))*((p3.x) - (p4.x)) +
                ((p3.y) -(p4.y))*((p3.y) - (p4.y)));
        float distance = (leftDistance + rightDistance)/2;
        return distance;
    }

    public ArrayList<Float> getAngle(Mat img, ArrayList<android.graphics.Point> landmark) {
        MatOfPoint2f image_points = new MatOfPoint2f();
        ArrayList<Point> point2s = new ArrayList<>();
        android.graphics.Point p1 = landmark.get(30);
        point2s.add(new Point(p1.x, p1.y));
        android.graphics.Point p2 = landmark.get(8);
        point2s.add(new Point(p2.x, p2.y));
        android.graphics.Point p3 = landmark.get(36);
        point2s.add(new Point(p3.x, p3.y));
        android.graphics.Point p4 = landmark.get(45);
        point2s.add(new Point(p4.x, p4.y));
        android.graphics.Point p5 = landmark.get(48);
        point2s.add(new Point(p5.x, p5.y));
        android.graphics.Point p6 = landmark.get(54);
        point2s.add(new Point(p6.x, p6.y));
        image_points.fromList(point2s);

        MatOfPoint3f model_points = new MatOfPoint3f();
        ArrayList<Point3> point3s = new ArrayList<>();
        point3s.add(new Point3(0.0f, 0.0f, 0.0f));
        point3s.add(new Point3(0.0f, -330.0f, -65.0f));
        point3s.add(new Point3(-225.0f, 170.0f, -135.0f));
        point3s.add(new Point3(225.0f, 170.0f, -135.0f));
        point3s.add(new Point3(-150.0f, -150.0f, -125.0f));
        point3s.add(new Point3(150.0f, -150.0f, -125.0f));
        model_points.fromList(point3s);
        double focal_length = img.cols();
        Point center = new Point(img.cols() / 2, img.rows() / 2);
        MatOfDouble dist_coeffs = new MatOfDouble(Mat.zeros(4, 1, CvType.makeType(CvType.CV_64F, 1)));
        Mat camera_matrix = Mat.zeros(3, 3, CvType.CV_64F);
        camera_matrix.put(0, 0, focal_length);
        camera_matrix.put(0, 1, 0);
        camera_matrix.put(0, 2, center.x);
        camera_matrix.put(1, 0, 0);
        camera_matrix.put(1, 1, focal_length);
        camera_matrix.put(1, 2, center.y);
        camera_matrix.put(2, 0, 0);
        camera_matrix.put(2, 1, 0);
        camera_matrix.put(2, 2, 1);
        Mat rotation_vector = new Mat();
        Mat translation_vector = new Mat();
        int npoints = Math.max(model_points.checkVector(3, CvType.CV_32F), model_points.checkVector(3, CvType.CV_64F));
        npoints = Math.max(image_points.checkVector(2, CvType.CV_32F), image_points.checkVector(2, CvType.CV_64F));
        Calib3d.solvePnP(model_points, image_points, camera_matrix, dist_coeffs, rotation_vector, translation_vector);
        double[] c = new double[9];
        camera_matrix.get(0, 0, c);
        double[] d = new double[3];
        rotation_vector.get(0, 0, d);
        double[] e = new double[3];
        translation_vector.get(0, 0, e);
        Mat rotation_matrix = new Mat();
        Calib3d.Rodrigues(rotation_vector, rotation_matrix);
        return rotationMatrixToEulerAngles(rotation_matrix);
    }

    private boolean isRotationMatrix(Mat R) {
        Mat RT = new Mat();
        Mat shouldBeIdentity = new Mat();
        Core.transpose(R, RT);
        double a = R.size().width;
        double b = RT.rows();
        Core.gemm(RT, R, 1.0, Mat.zeros(R.size(), R.type()), 0.0, shouldBeIdentity);
        Mat I = Mat.eye(3, 3, shouldBeIdentity.type());
        double tmp4[] = new double[9];
        I.get(0, 0, tmp4);
        double tmp1[] = new double[9];
        shouldBeIdentity.get(0, 0, tmp1);
        double tmp2[] = new double[3];
        R.get(0, 0, tmp2);
        double tmp3[] = new double[3];
        RT.get(0, 0, tmp3);
        return Core.norm(I, shouldBeIdentity) < 1e-6;

    }

    private ArrayList<Float> rotationMatrixToEulerAngles(Mat R) {

        //        if (!isRotationMatrix(R)) {
        //            return null;
        //        }
        double[] a = new double[1];
        double[] b = new double[1];
        double[] c = new double[1];
        double[] d = new double[1];
        double[] e = new double[1];
        double[] f = new double[1];
        double[] g = new double[1];
        double[] h = new double[1];
        double[] i = new double[1];
        R.get(0, 0, a);
        R.get(0, 1, b);
        R.get(0, 2, c);
        R.get(1, 0, d);
        R.get(1, 1, e);
        R.get(1, 2, f);
        R.get(2, 0, g);
        R.get(2, 1, h);
        R.get(2, 2, i);

        float sy = (float) Math.sqrt(a[0] * a[0] + d[0] * d[0]);

        boolean singular = sy < 1e-6; // If

        double x, y, z;
        if (!singular) {
            x = Math.atan2(h[0], i[0]);
            y = Math.atan2(-g[0], sy);
            z = Math.atan2(d[0], a[0]);
        } else {
            x = Math.atan2(-f[0], e[0]);
            y = Math.atan2(-g[0], sy);
            z = 0;
        }
        ArrayList<Float> temp = new ArrayList<>();
        temp.add((float) (x / 3.1415926 * 180));
        temp.add((float) (y / 3.1415926 * 180));
        temp.add((float) (z / 3.1415926 * 180));

        return temp;
    }

    /**
     * 分析检测结果，得到数据
     *
     * @param bitmap 传入bitmap类型
     * @return 以float类型返回, 包含瞳距和头的旋转角度
     */

    public ArrayList<Float> analyseData(@NonNull Bitmap bitmap) {
        ArrayList<Float> Info = new ArrayList<>();
        float roll = 1;
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat,true);//convert original bitmap to Mat
        List<RectInfo> list = detect(bitmap);
        if (list.size() != 0) {
            ArrayList<android.graphics.Point> points = list.get(0).getFaceLandmarks();
            float distance = getDistance(points);
            ArrayList<Float> angles = getAngle(mat,points);
            if (angles.size() != 0) {
            roll = angles.get(2);
            }
            Info.add(distance);
            Info.add(roll);

        }else{
            Info.add((float) -1);
        }
        return Info;
    }

    /**
     * 比较标准图片和实时图片的数据
     *
     * @param actualTimeImg 传入bitmap类型
     * @param strFilePath 传入文件路径
     * @return 以String类型返回, 返回提示信息
     */

    public String compareImg(@NonNull Bitmap actualTimeImg, String strFilePath) {

        //从文件中读取标准图片数据
        ArrayList<String> standardList = ReadTxtFile(strFilePath + fileFolder + fileName);
        if (standardList.size() == 1) {
            return "There is no standard picture's data.";
        } else {
            String hintInfo = "";
            String[] distanceArr = standardList.get(0).split("：");
            String[] angleArr = standardList.get(1).split("：");
            float standardDistance = Float.parseFloat(distanceArr[1]);
            float standardAngle = Float.parseFloat(angleArr[1]);

            //得到实时图片分析数据
            ArrayList<Float> actualTimeInfo = analyseData(actualTimeImg);
            if (actualTimeInfo.size() == 1) {
                hintInfo = "实时照片没有检测到脸\n";
            } else {
                float distanceDiffer = actualTimeInfo.get(0) - standardDistance;
                float angleDiffer = Math.abs(actualTimeInfo.get(1) - standardAngle);
                if (distanceDiffer > distanceThreshold) {
                    hintInfo = "眼屏视距过近，请移远\n";
                }
                if (angleDiffer > angleThreshold) {
                    hintInfo = hintInfo + "头倾斜严重，请转正\n";
                }

            }
            return hintInfo;
        }
    }

    // 将Info写入到文本文件中
    public void writeTxtToFile(ArrayList<Float> Info, String filePath) {
        //生成文件夹之后，再生成文件，不然会出错
        makeFilePath(filePath + fileFolder, fileName);
        String strContent;
        String strFilePath = filePath+fileFolder+fileName;
        if(Info.size() == 1){
            strContent = "没有检测到脸\r\n";
        }else{
            strContent = "瞳距："+Info.get(0)+"\r\n"+"角度："+Info.get(1)+"\r\n";
        }

        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
//            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
//            raf.seek(file.length());
//            raf.seek(0);
//            raf.write(strContent.getBytes());
//            raf.close();
            FileWriter writer = null;
            try {
                // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件,false表示覆盖的方式写入
                writer = new FileWriter(file, false);
                writer.write(strContent);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(writer != null){
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    // 生成文件
    public File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    // 生成文件夹
    public static void makeRootDirectory(String filePath) {
        File file = null;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e+"");
        }
    }

    //按行读取text文件
    public ArrayList<String> ReadTxtFile(String strFilePath){
        ArrayList newList=new ArrayList<String>();

        //打开文件
        File file = new File(strFilePath);

        //如果文件不存在
        if (file.isFile() && file.exists()){
            try {
                InputStream instream = new FileInputStream(file);
                if (instream != null){
                    InputStreamReader inputreader = new InputStreamReader(instream);
                    BufferedReader buffreader = new BufferedReader(inputreader);
                    String line;

                    //分行读取
                    while (( line = buffreader.readLine()) != null) {
                        newList.add(line+"\n");
                    }
                    instream.close();
                }
            }
            catch (java.io.FileNotFoundException e){
                Log.d("TestFile", "The File doesn't not exist.");
            }
            catch (IOException e){
                Log.d("TestFile", e.getMessage());
            }
            return newList;
        }else{
            Log.d("TestFile", "The File doesn't not exist.");
            newList.add("The File doesn't not exist.\n");
            return newList;
        }
    }
    @Keep
    private native static void jniNativeClassInit();

    @Keep
    private synchronized native int jniInit(String mLandMarkPath);

    @Keep
    private synchronized native int jniDeInit();

    @Keep
    private synchronized native RectInfo[] jniBitmapDet(Bitmap bitmap);


}