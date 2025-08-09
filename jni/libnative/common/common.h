
/*
 * common defines
 *
 * last modified: 2015.09.14
 *
 * note: compile all code with '-Wall -fvisibility=hidden', link with '-s -dead-strip'
 *     and add PUBLIC define to exported symbols (jni functions and etc.)
 */

#ifndef COMMON_H
#define COMMON_H

//#define DEBUG        1
#define LOG_TAG      "libnative"
#define LIBS_VERSION 20150914 // sync with LibNative.java !!! (and update on every libs edit)

#define NATIVE_FUNCTION(x,f) Java_app_common_LibNative_##f
#define SCAN_FUNCTION(x,f)   Java_app_scanner_LibScan_##f
#define PATCH_FUNCTION(x,f)  Java_app_common_LibPatch_##f
#define TYPES_FUNCTION(x,f)  Java_app_common_LibTypes_##f

/* ----------------------------------------------------------- */

#include <limits.h>

#if defined (__ANDROID_API__) || defined (_JNI_)
#include <jni.h>
#endif

#ifdef __ANDROID_API__
#include <android/log.h> // + pass -llog to linker
#else
#include <stdio.h>
#endif

#ifdef __linux__
#  define OS_LINUX

#  ifdef __ANDROID_API__
#    define OS_ANDROID
#  endif
#endif

/*
 * log messages
 * E() print always, I() and I_IF() only if DEBUG defined
 */
#ifdef OS_ANDROID
#  define  I_(fmt,...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "%s: " fmt "%s", __FUNCTION__ , __VA_ARGS__)
#  define  E_(fmt,...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s: " fmt "%s", __FUNCTION__ , __VA_ARGS__)
#else
#  define  I_(fmt,...)  printf("[" LOG_TAG "] %s: " fmt "%s\n", __FUNCTION__ , __VA_ARGS__)
#  define  E_(fmt,...)  printf("[" LOG_TAG "] %s: " fmt "%s\n", __FUNCTION__ , __VA_ARGS__)
#endif
#if DEBUG
#  define  I(x...)       I_(x,"")
#  define  I_NOT(y,x...) do { if(!(y)) I_(x,""); } while(0)
#  define  E(x...)       E_(x,"")
#else
#  define  I(...)        do {} while(0)
#  define  I_NOT(y,...)  do {} while(0)
#  define  E(x...)       E_(x,"")
#endif /* DEBUG */

#define IN
#define OUT
#define INOUT

#define PUBLIC  __attribute__ ((visibility ("default")))
#define PRIVATE __attribute__ ((visibility ("hidden")))

#define INLINE       __inline__
#define FORCE_INLINE __inline__ __attribute__((always_inline))

#define BIG_CONSTANT(x) (x##LLU)

//
#define CTASTR2(pre,post) pre ## post
#define CTASTR(pre,post) CTASTR2(pre,post)
#define STATIC_ASSERT2(cond,msg) \
	typedef struct { int CTASTR(static_assertion_failed_,msg) : !!(cond); } \
		CTASTR(static_assertion_failed_,__COUNTER__)
#define STATIC_ASSERT(cond) STATIC_ASSERT2(cond, expression_not_true)

//
#define IS_ALIGNED(p,a) (((uintptr_t) p) % (a) == 0)
#define ALIGN_UP(p,a) (((uintptr_t)(p) + (a)-1) & ~((a)-1))

#endif /* COMMON_H */
