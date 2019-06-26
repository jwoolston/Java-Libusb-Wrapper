#include <stdio.h>
#include "logging.h"
#include "stdbool.h"

#ifdef __ANDROID__
#include "android/log.h"
#endif

JavaVM *javaVM;
jobject arborClass;
jobject branchClass;
jobject objectClass;
jmethodID arborTag;
jmethodID branchVerbose;
jmethodID branchDebug;
jmethodID branchInfo;
jmethodID branchWarning;
jmethodID branchError;
jmethodID branchWtf;

#define LOG_TAG "logging-native"

void initializeArbor(JNIEnv *env) {
    (*env)->GetJavaVM(env, &javaVM);
    jclass localClass = (*env)->FindClass(env, "com/toxicbakery/logging/Arbor");
    arborClass = (jclass) (*env)->NewGlobalRef(env, localClass);
    localClass = (*env)->FindClass(env, "com/toxicbakery/logging/Branch");
    branchClass = (jclass) (*env)->NewGlobalRef(env, localClass);
    localClass = (*env)->FindClass(env, "java/lang/Object");
    objectClass = (*env)->NewGlobalRef(env, localClass);
    arborTag = (*env)->GetStaticMethodID(env, arborClass, "tag", "(Ljava/lang/String;)Lcom/toxicbakery/logging/Branch;");
    branchVerbose = (*env)->GetMethodID(env, branchClass, "v", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    branchDebug = (*env)->GetMethodID(env, branchClass, "d", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    branchInfo = (*env)->GetMethodID(env, branchClass, "i", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    branchWarning = (*env)->GetMethodID(env, branchClass, "w", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    branchError = (*env)->GetMethodID(env, branchClass, "e", "(Ljava/lang/String;[Ljava/lang/Object;)V");
    branchWtf = (*env)->GetMethodID(env, branchClass, "wtf", "(Ljava/lang/String;[Ljava/lang/Object;)V");
}

void arborLog(jmethodID method, const char *tag, const char *fmt, va_list args) {
    JNIEnv *jniEnv;
    // double check it's all ok
    int getEnvStat = (*javaVM)->GetEnv(javaVM, (void **) &jniEnv, JNI_VERSION_1_6);
    bool didAttach = false;
    if (getEnvStat == JNI_EDETACHED) {
        if ((*javaVM)->AttachCurrentThread(javaVM, &jniEnv, NULL) == 0) {
            didAttach = true;
        }
    }

    jstring tagString = (*jniEnv)->NewStringUTF(jniEnv, tag);
    jobject branch = (*jniEnv)->CallStaticObjectMethod(jniEnv, arborClass, arborTag, tagString);
    if ((*jniEnv)->ExceptionOccurred(jniEnv)) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Native Log", "Native logging failed. Tree: %p", branch);
#else
        fprintf(stderr, "Native logging failed. Tree: %p", branch);
#endif
        return;
    }

    const char message[500];
    vsnprintf(&message, 500, fmt, args);
    jstring messageString = (*jniEnv)->NewStringUTF(jniEnv, message);

    jobjectArray array;
    array = (*jniEnv)->NewObjectArray(jniEnv, 0, objectClass, NULL);

    (*jniEnv)->CallVoidMethod(jniEnv, branch, method, messageString, array);

    if (didAttach) {
        (*javaVM)->DetachCurrentThread(javaVM);
    }
}

void __arbor_verbose(const char *tag, const char *fmt, ...) {
    va_list localArgs;
    va_start(localArgs, fmt);
    arborLog(branchVerbose, tag, fmt, localArgs);
    va_end(localArgs);
}

void __arbor_debug(const char *tag, const char *fmt, ...) {
    va_list localArgs;
    va_start(localArgs, fmt);
    arborLog(branchDebug, tag, fmt, localArgs);
    va_end(localArgs);
}

void __arbor_info(const char *tag, const char *fmt, ...) {
    va_list localArgs;
    va_start(localArgs, fmt);
    arborLog(branchInfo, tag, fmt, localArgs);
    va_end(localArgs);
}

void __arbor_warn(const char *tag, const char *fmt, ...) {
    va_list localArgs;
    va_start(localArgs, fmt);
    arborLog(branchWarning, tag, fmt, localArgs);
    va_end(localArgs);
}

void __arbor_error(const char *tag, const char *fmt, ...) {
    va_list localArgs;
    va_start(localArgs, fmt);
    arborLog(branchError, tag, fmt, localArgs);
    va_end(localArgs);
}