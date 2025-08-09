
#
# last modified: 2014.05.08
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := native
LOCAL_SRC_FILES := common/functions.c common/hash.c common/internal.c \
                   scan/fixeddb.c scan/scan.c scan/bindata.c scan/extdb.c \
                   external/miniz.c jfunctions.c \
                   jni_arch.c jni_file.c jni_network.c jni_process.c jni_scan.c jni_text.c jni_signal.c \
                   jni_types.cpp \
                   jni_test.c
#                   external/sha1.c

LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)
#LOCAL_CFLAGS  := -Wall -Wno-deprecated -g -O0
LOCAL_CFLAGS  := -Wall -Wno-deprecated -fvisibility=hidden -O3 -fomit-frame-pointer -ffast-math -funroll-loops -ftree-vectorize
LOCAL_CFLAGS  += -fexceptions
LOCAL_CFLAGS  += -Wno-unused
LOCAL_LDFLAGS := -s -dead-strip
LOCAL_LDLIBS  := -llog

include $(BUILD_SHARED_LIBRARY)
