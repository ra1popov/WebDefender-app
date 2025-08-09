
/*
 * jni functions to work with processes
 *
 * last modified: 2014.10.08
 */

#include <unistd.h>
#include <stdlib.h>

#include "common/common.h"
#include "common/functions.h"

/*
 * search processes names by uid and return it name
 * return NULL if names not found or on error
 */
#define PKGNAMES_MAX 33
jobjectArray PUBLIC NATIVE_FUNCTION(getNameFromUid, nf50) (JNIEnv* env, jobject thiz,
															IN jint uid)
{
	char names[PKGNAMES_MAX][ANDROID_PKGNAME_MAX];
	int count, i;
	jobjectArray result;

	count = uid2name(uid, names, PKGNAMES_MAX);
	if (count <= 0)
	{
		I("uid2name %d failed", uid);
		count = 0;
		return NULL;
	}

	result = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"),
									(*env)->NewStringUTF(env, ""));
	if (result == NULL)
	{
		E("NewObjectArray failed");
		return NULL;
	}

	for (i = 0; i < count; i++)
		(*env)->SetObjectArrayElement(env, result, i, (*env)->NewStringUTF(env, names[i]));

	return result;
}

/*
 * exit ;) TODO XXX add threads stop
 */
void PUBLIC NATIVE_FUNCTION(exit, nf51) (JNIEnv* env, jobject thiz, jint code)
{
	exit(code);
}
