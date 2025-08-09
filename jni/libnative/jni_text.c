
/*
 * jni text functions
 *
 * last modified: 2015.09.18
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <string.h>
#include <ctype.h>
#include <stdio.h>

#include <stdlib.h>

#include "common/common.h"
#include "common/functions.h"

// convert ascii string to lowercase
jstring PUBLIC NATIVE_FUNCTION(asciiToLower, nf81) (JNIEnv* env, jobject thiz, IN jstring str) {
    const jchar* str_s = NULL;
    jstring str_l = NULL;

    if (str == NULL) {
        return NULL;
    }

    str_s = (*env)->GetStringChars(env, str, NULL);
    jint size = (*env)->GetStringLength(env, str);

    if (str_s != NULL && size > 0) {
        // Allocate memory for the new string
        jchar* buf = (jchar*)malloc((size + 1) * sizeof(jchar)); // +1 for null terminator
        if (buf != NULL) {
            // Convert to lowercase
            for (int i = 0; i < size; i++) {
                buf[i] = tolower(str_s[i]);
            }

            // Null-terminate the new string
            buf[size] = '\0';

            // Create the new jstring
            str_l = (*env)->NewString(env, buf, size);

            // Free the allocated memory
            free(buf);
        }
    }

    // Release the string characters
    if (str_s != NULL) {
        (*env)->ReleaseStringChars(env, str, str_s);
    }

    // Return the new jstring or an empty jstring if allocation failed
    return (str_l != NULL) ? str_l : (*env)->NewStringUTF(env, "");
}

/*
 * check if ascii string starts with another string
 *
 * "FOO".startsWith(""); == true
 */
jboolean PUBLIC NATIVE_FUNCTION(asciiStartsWith, nf82) (JNIEnv* env, jobject thiz,
														IN jstring start, IN jstring str)
{
	jboolean isStarts = JNI_FALSE;
	char *str_s = NULL;
	char *start_s = NULL;
	jsize str_size, start_size;

	if (start == NULL || str == NULL)
		return JNI_FALSE;

	start_s = (char *) (*env)->GetStringChars(env, start, NULL);
	start_size = (*env)->GetStringLength(env, start);
	str_s = (char *) (*env)->GetStringChars(env, str, NULL);
	str_size = (*env)->GetStringLength(env, str);

	if (start_s == NULL || str_s == NULL)
	{
		E("GetStringChars failed"); // ?
	}
	else if (start_size == 0 ||
				(start_size > 0 && start_size <= str_size &&
					memcmp(start_s, str_s, start_size * sizeof(jchar)) == 0)) // XXX overflow
	{
		isStarts = JNI_TRUE;
	}

	if (start_s != NULL)
		(*env)->ReleaseStringChars(env, start, (jchar *) start_s);
	if (str_s != NULL)
		(*env)->ReleaseStringChars(env, str, (jchar *) str_s);

	return isStarts;
}

/*
 * check if ascii string ends with another string
 *
 * "FOO".endsWith(""); == true
 */
jboolean PUBLIC NATIVE_FUNCTION(asciiEndsWith, nf83) (JNIEnv* env, jobject thiz,
														IN jstring end, IN jstring str)
{
	jboolean isEnds = JNI_FALSE;
	char *str_s = NULL;
	char *end_s = NULL;
	jsize str_size, end_size;

	if (end == NULL || str == NULL)
		return JNI_FALSE;

	end_s = (char *) (*env)->GetStringChars(env, end, NULL);
	end_size = (*env)->GetStringLength(env, end);
	str_s = (char *) (*env)->GetStringChars(env, str, NULL);
	str_size = (*env)->GetStringLength(env, str);

	if (end_s == NULL || str_s == NULL)
	{
		E("GetStringChars failed"); // ?
	}
	else if (end_size == 0 ||
				(end_size > 0 && end_size <= str_size &&
					memcmp(end_s, &str_s[(str_size - end_size) * sizeof(jchar)],
							end_size * sizeof(jchar)) == 0)) // XXX overflow
	{
		isEnds = JNI_TRUE;
	}

	if (end_s != NULL)
		(*env)->ReleaseStringChars(env, end, (jchar *) end_s);
	if (str_s != NULL)
		(*env)->ReleaseStringChars(env, str, (jchar *) str_s);

	return isEnds;
}

/*
 * return position of ascii string in another string
 *
 * pos = "F00".indexOf("F", 3); == -1
 * pos = "F00".indexOf(""); == 0
 * pos = "F00".indexOf("a", 5); == -1
 * pos = "F00".indexOf("", 5); == 3 !!!
 * pos = "F00".indexOf("", -1); == 0 !!!
 */
jint PUBLIC NATIVE_FUNCTION(asciiIndexOf, nf84) (JNIEnv* env, jobject thiz,
													IN jstring search, jint from, IN jstring str)
{
	jint pos = -1;
	char *str_s = NULL;
	char *search_s = NULL;
	char *s;
	jsize str_size, search_size;

	if (search == NULL || str == NULL)
		return JNI_FALSE;

	search_s = (char *) (*env)->GetStringChars(env, search, NULL);
	search_size = (*env)->GetStringLength(env, search);
	str_s = (char *) (*env)->GetStringChars(env, str, NULL);
	str_size = (*env)->GetStringLength(env, str);

	if (search_s == NULL || str_s == NULL)
	{
		E("GetStringChars failed"); // ?
	}
	else if (search_size == 0)
	{
		pos = (from < 0) ? 0 : ((from < str_size) ? from : str_size);
	}
	else if (search_size > 0 && search_size <= str_size - from && from >= 0 && from < str_size)
	{
		s = (char *) memmem(&str_s[from * sizeof(jchar)], (str_size - from) * sizeof(jchar),
							search_s, search_size * sizeof(jchar));
		if (s != NULL)
			pos = (s - str_s) / sizeof(jchar);
	}

	if (search_s != NULL)
		(*env)->ReleaseStringChars(env, search, (jchar *) search_s);
	if (str_s != NULL)
		(*env)->ReleaseStringChars(env, str, (jchar *) str_s);

	return pos;
}
