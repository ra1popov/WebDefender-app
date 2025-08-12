
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := self
LOCAL_SRC_FILES := go.c

LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)

LOCAL_CFLAGS := -Wall -I$(LOCAL_PATH)/../jni-external/libcurl-android/jni/curl/include
LOCAL_LDLIBS := -llog

# Specify the dependency on the static curl library.
LOCAL_STATIC_LIBRARIES := curl

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

# The libcurl module
LOCAL_MODULE    := curl
LOCAL_SRC_FILES := $(LOCAL_PATH)/../jni-external/libcurl-android/libs/$(TARGET_ARCH_ABI)/libcurl.a

include $(PREBUILT_STATIC_LIBRARY)
