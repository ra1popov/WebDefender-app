
/*
 * last modified: 2014.09.10
 */

#ifndef JFUNCTIONS_H
#define JFUNCTIONS_H

#include <stdint.h>
#include "common/common.h"

#ifdef __cplusplus
extern "C" {
#endif

int get_direct_buffer_address (JNIEnv* env, IN jobject buf, OUT void **buf_ptr);

int get_class_loader (JNIEnv* env, IN char* app_class, OUT jclass *class_loader,
						OUT jmethodID *find_method);

#ifdef __cplusplus
}
#endif

#endif /* JFUNCTIONS_H */
