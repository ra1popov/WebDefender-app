
#
# last modified: 2014.03.04
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := bspatch
LOCAL_SRC_FILES := bspatch.c huffman.c crctable.c randtable.c decompress.c bzlib.c jni_bspatch.c

LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)
#LOCAL_CFLAGS  := -DUSE_MMAP -Wall -g -O0 -DUSE_MMAP
LOCAL_CFLAGS  := -DUSE_MMAP -Wall -fvisibility=hidden -O3 -fomit-frame-pointer -ffast-math -funroll-loops -ftree-vectorize
LOCAL_LDFLAGS := -s -dead-strip
LOCAL_LDLIBS  := -llog

include $(BUILD_SHARED_LIBRARY)
