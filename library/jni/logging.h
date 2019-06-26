//
// Created by ideal on 4/10/2019.
//

#ifndef FREEIMAGE_LOGGING_H
#define FREEIMAGE_LOGGING_H

#include <jni.h>

#define LOGV(...) __arbor_verbose(LOG_TAG, __VA_ARGS__)
#define LOGD(...) __arbor_debug(LOG_TAG, __VA_ARGS__)
#define LOGI(...) __arbor_info(LOG_TAG, __VA_ARGS__)
#define LOGW(...) __arbor_warn(LOG_TAG, __VA_ARGS__)
#define LOGE(...) __arbor_error(LOG_TAG, __VA_ARGS__)

void initializeArbor(JNIEnv *env);

void __arbor_verbose(const char *tag, const char *fmt, ...);

void __arbor_debug(const char *tag, const char *fmt, ...);

void __arbor_info(const char *tag, const char *fmt, ...);

void __arbor_warn(const char *tag, const char *fmt, ...);

void __arbor_error(const char *tag, const char *fmt, ...);

#endif //FREEIMAGE_LOGGING_H