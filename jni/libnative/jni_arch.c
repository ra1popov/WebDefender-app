
/*
 * jni miniz functions
 *
 * last modified: 2015.09.18
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "common/common.h"
#include "external/miniz.h"
#include "jfunctions.h"

/*
 * get file content from zip archive with miniz and return ByteBuffer or null on error
 * free returned ByteBuffer with zipFreeData
 */
jobject PUBLIC NATIVE_FUNCTION(zipReadFile, nf01) (JNIEnv* env, jobject thiz,
													IN jstring zipfile, IN jstring filename)
{
	void *data = NULL;
	const char *zipfile_s = NULL;
	const char *filename_s = NULL;

	if (zipfile == NULL || filename == NULL)
		return NULL;

	zipfile_s = (*env)->GetStringUTFChars(env, zipfile, NULL);
	filename_s = (*env)->GetStringUTFChars(env, filename, NULL);
	if (zipfile_s == NULL || filename_s == NULL)
	{
		E("GetStringUTFChars failed"); // OutOfMemoryError already thrown
	}
	else
	{
		size_t size;
		void *buf = mz_zip_extract_archive_file_to_heap(zipfile_s, filename_s, &size, 0);
		if (buf != NULL && size > 0) // size must be > 0, ART 4.4.2 bug
			data = (*env)->NewDirectByteBuffer(env, buf, size);
	}

	if (zipfile_s != NULL)
		(*env)->ReleaseStringUTFChars(env, zipfile, zipfile_s);
	if (filename_s != NULL)
		(*env)->ReleaseStringUTFChars(env, filename, filename_s);

	if (data == NULL)
		E("extract or NewDirectByteBuffer failed");

	return data;
}

// free buffer with file data, see zipReadFile
jboolean PUBLIC NATIVE_FUNCTION(zipFreeData, nf02) (JNIEnv* env, jobject thiz,
													IN jobject data)
{
	void *data_ptr;

	if (get_direct_buffer_address(env, data, &data_ptr) == 0)
		return JNI_FALSE;

	mz_free(data_ptr);
	return JNI_TRUE;
}
