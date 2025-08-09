
#ifndef COMMON_H
#define	COMMON_H

//#define DEBUG 1

#include <stdbool.h>
#include <jni.h>

#if DEBUG
#include <android/log.h>
#  define  LOG_TAG    "libself"
#  define  I_(fmt,...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s: " fmt "%s", __FUNCTION__ , __VA_ARGS__)
#  define  E_(fmt,...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s: " fmt "%s", __FUNCTION__ , __VA_ARGS__)
#  define  I(x...) I_(x,"")
#  define  E(x...) E_(x,"")
#else
#  define  I(...)  do {} while (0)
#  define  E(...)  do {} while (0)
#endif

#define IN
#define OUT
#define INOUT

#define NATIVE_FUNCTION(x) Java_app_common_LibSelf_##x

#define ANDROID_PKGNAME_MAX  48 // max package name size

#endif	/* COMMON_H */
