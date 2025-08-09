
/*
 * jni functions to work with files and disk
 *
 * last modified: 2015.09.18
 */

#include <stdbool.h>
#include <stdio.h>
#include <errno.h>
#include <fcntl.h>

#include <unistd.h> // added 28.07.2023

#include "common/common.h"

/*
 * set file fd to blocking/non-blocking state
 */
void PUBLIC NATIVE_FUNCTION(fileSetBlocking, nf10) (JNIEnv* env, jobject thiz,
													IN jint fd, IN jboolean enable)
{
	int flags;

	flags = fcntl(fd, F_GETFL);
	if (enable == JNI_TRUE)
	{
		// clear O_NONBLOCK and add O_DIRECT O_NOATIME
		flags &= (~O_NONBLOCK);
		flags &= (O_DIRECT | O_NOATIME);
	}
	else
	{
		flags &= (~(O_DIRECT | O_NOATIME));
		flags &= (O_NONBLOCK);
	}

	if (fcntl(fd, F_SETFL, flags) == -1)
		E("fcntl error %d", errno);
}

/*
 * close file fd, returns zero on success
 */
jint PUBLIC NATIVE_FUNCTION(fileClose, nf11) (JNIEnv* env, jobject thiz,
												IN jint fd)
{
	return close(fd);
}
