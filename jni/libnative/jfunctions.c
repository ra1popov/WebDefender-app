
/*
 * jni helper functions
 *
 * last modified: 2014.09.10
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "common/common.h"
#include "jfunctions.h"

/*
 * get DirectBuffer native address
 *
 * return 0 on error or save address in buf_ptr
 */
int get_direct_buffer_address (JNIEnv* env, IN jobject buf, OUT void **buf_ptr)
{
	if (buf == NULL)
	{
		I("buf == NULL");
		return 0;
	}

	*buf_ptr = (void *) (*env)->GetDirectBufferAddress(env, buf);
	if (*buf_ptr == NULL)
	{
		E("GetDirectBufferAddress failed");
		return 0;
	}

	return 1;
}

/*
 * get global references to ClassLoader findClass method from app class
 * call this method only from JNI
 *
 * return 0 on error or save class and method global refs in class_loader and find_method
 *
 * return usage:
 * jstring jstr = (*env)->NewStringUTF(env, "java/lang/String");
 * (*env)->CallObjectMethod(env, class_loader, find_method, jstr)
 * (*env)->DeleteLocalRef(env, jstr);
 */
int get_class_loader (JNIEnv* env, IN char* app_class, OUT jclass *class_loader,
						OUT jmethodID *find_method)
{
	jclass appClass = NULL, classClass = NULL, classLoaderClass = NULL, classLoader = NULL;
	jmethodID getClassLoaderMethod = NULL, findClassMethod = NULL;
	int retval = 0;

	do
	{
		appClass = (*env)->FindClass(env, app_class);
		if (appClass == NULL) break;
		classClass = (*env)->GetObjectClass(env, appClass);
		if (classClass == NULL) break;
		classLoaderClass = (*env)->FindClass(env, "java/lang/ClassLoader");
		if (classLoaderClass == NULL) break;
		getClassLoaderMethod =
			(*env)->GetMethodID(env, classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
		if (getClassLoaderMethod == NULL) break;

		classLoader = (*env)->CallObjectMethod(env, appClass, getClassLoaderMethod);
		if (classLoader == NULL) break;
		findClassMethod =
			(*env)->GetMethodID(env, classLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
		if (findClassMethod == NULL) break;

		retval = 1;
		*class_loader = (*env)->NewGlobalRef(env, classLoader);
		*find_method = findClassMethod;
	}
	while (0);

	if (appClass != NULL)
		(*env)->DeleteLocalRef(env, appClass);
	if (classClass != NULL)
		(*env)->DeleteLocalRef(env, classClass);
	if (classLoaderClass != NULL)
		(*env)->DeleteLocalRef(env, classLoaderClass);
	if (classLoader != NULL)
		(*env)->DeleteLocalRef(env, classLoader);

	if (retval == 0)
		E("failed");

	return retval;
}
