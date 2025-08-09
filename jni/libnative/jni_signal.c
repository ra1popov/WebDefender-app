
/*
 * jni signal functions
 *
 * last modified: 2014.10.08
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <signal.h>
#include <pthread.h>
#include <errno.h>

#include "common/common.h"
#include "common/functions.h"

// set handler for signals (SIGHUP 1) and return 0 on success
jint PUBLIC NATIVE_FUNCTION(signalHandlerSet, nf61) (JNIEnv* env, jobject thiz,
														IN jint signal)
{
	return signal_handler_set(signal, NULL);
}

// send signal to our process (SIGHUP 1)
jint PUBLIC NATIVE_FUNCTION(signalSendSelf, nf62) (JNIEnv* env, jobject thiz,
													IN jint signal)
{
	return raise(signal);
}

// get current thread native id
jlong PUBLIC NATIVE_FUNCTION(threadGetSelfId, nf63) (JNIEnv* env, jobject thiz)
{
	jlong id = (jlong) pthread_self();
	//I("%lu", id);
	return id;
}

// send signal to thread (see threadGetSelfId) and return 0 on success
jint PUBLIC NATIVE_FUNCTION(threadSendSignal, nf64) (JNIEnv* env, jobject thiz,
														IN jlong id, IN jint signal)
{
	//I("%lu", id);
	return signal_send(id, signal);
}
