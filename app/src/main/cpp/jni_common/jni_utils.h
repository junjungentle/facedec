#ifndef JNI_UTILS_H
#define JNI_UTILS_H
/*
 *     Author : Darren
 * Created on : 06/20 2016
 *
 * Copyright (c) 2016 Darren. All rights reserved.
 */
#include <jni.h>
#include <string>

namespace jniutils {


std::string convertJStrToString(JNIEnv* env, jstring lString);


}  // end jniutils

#endif /* JNI_UTILS_H */
