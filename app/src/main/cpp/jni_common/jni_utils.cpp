/*
 *     Author : Darren
 * Created on : 06/20 2016
 *
 * Copyright (c) 2016 Darren. All rights reserved.
 */
#include <cstring>
#include <jni_common/jni_utils.h>
#include <jni_common/jni_primitives.h>
#include <sstream>
#include <unistd.h>

namespace jniutils {

std::string convertJStrToString(JNIEnv* env, jstring lString) {
  const char* lStringTmp;
  std::string str;

  lStringTmp = env->GetStringUTFChars(lString, NULL);
  if (lStringTmp == NULL)
    return NULL;

  str = lStringTmp;

  env->ReleaseStringUTFChars(lString, lStringTmp);

  return str;
}

}
