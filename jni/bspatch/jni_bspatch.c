
/*
 * jni bspatch functions
 *
 * last modified: 2014.10.08
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "../libnative/common/common.h"

extern int main(int argc,char * argv[]);

// patch file with bspatch, see bspatch.c main
jboolean PUBLIC PATCH_FUNCTION(bspatchFile, pf01) (JNIEnv* env, jobject thiz,
													IN jstring oldfile, IN jstring newfile,
													IN jstring patchfile)
{
	jboolean result = JNI_FALSE;
	char * argv[4];
	const char *oldfile_s = NULL;
	const char *newfile_s = NULL;
	const char *patchfile_s = NULL;

	oldfile_s = (*env)->GetStringUTFChars(env, oldfile, NULL);
	newfile_s = (*env)->GetStringUTFChars(env, newfile, NULL);
	patchfile_s = (*env)->GetStringUTFChars(env, patchfile, NULL);
	if (oldfile_s == NULL || newfile_s == NULL || patchfile_s == NULL)
	{
		E("GetStringUTFChars failed"); // OutOfMemoryError already thrown
	}
	else
	{
		argv[0] = "bspatch";
		argv[1] = (char *) oldfile_s;
		argv[2] = (char *) newfile_s;
		argv[3] = (char *) patchfile_s;
		if (main(4, argv) == 0)
			result = JNI_TRUE;
	}

	if (oldfile_s != NULL)
		(*env)->ReleaseStringUTFChars(env, oldfile, oldfile_s);
	if (newfile_s != NULL)
		(*env)->ReleaseStringUTFChars(env, newfile, newfile_s);
	if (patchfile_s != NULL)
		(*env)->ReleaseStringUTFChars(env, patchfile, patchfile_s);

	return result;
}
