
#
#include $(all-subdir-makefiles)

LOCAL_PATH_ROOT := $(call my-dir)

include $(LOCAL_PATH_ROOT)/bspatch/Android.mk
include $(LOCAL_PATH_ROOT)/libnative/Android.mk

ifneq ($(FLAVOR),fdroid)
    include $(LOCAL_PATH_ROOT)/libself/Android.mk
endif
