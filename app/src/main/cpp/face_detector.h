//
// Created by wzn on 2018/12/25.
//

#ifndef FACEDETECTION_FACE_DETECTOR_H
#define FACEDETECTION_FACE_DETECTOR_H


#include <vector>
#include <opencv2/opencv.hpp>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing/shape_predictor.h>
#include <dlib/opencv/cv_image.h>
#include <unordered_map>



//定义人脸检测类
class FaceDetector {
private:

    dlib::frontal_face_detector face_detector;
    std::vector<dlib::rectangle> det_rects;

    dlib::shape_predictor shape_predictor;
    std::unordered_map<int, dlib::full_object_detection> mFaceShapeMap;
    std::string mLandMarkModel;

public:

    FaceDetector(const std::string& landmarkmodel);

    //实现人脸检测和特征点检测算法
    int Detect(const cv::Mat &image);

    //返回检测结果
    std::vector<dlib::rectangle> getDetResultRects();
    std::unordered_map<int, dlib::full_object_detection>& getFaceShapeMap();

};


#endif //FACEDETECTION_FACE_DETECTOR_H
