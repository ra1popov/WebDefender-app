
/*
 * jni different native types and functions
 *
 * last modified: 2014.10.08
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "common/common.h"
#include "external/sparsehash/dense_hash_set"
#include "jfunctions.h"

#ifdef __cplusplus
extern "C" {
#endif

jobject PUBLIC TYPES_FUNCTION(intSetCreate, tf01) (JNIEnv* env, jobject thiz);
void PUBLIC TYPES_FUNCTION(intSetDelete, tf02) (JNIEnv* env, jobject thiz, IN jobject set);
jlong PUBLIC TYPES_FUNCTION(intSetSize, tf03) (JNIEnv* env, jobject thiz, IN jobject set);
jboolean PUBLIC TYPES_FUNCTION(intSetContains, tf04) (JNIEnv* env, jobject thiz, IN jobject set, IN jint value);
jboolean PUBLIC TYPES_FUNCTION(intSetAdd, tf05) (JNIEnv* env, jobject thiz, IN jobject set, IN jint value);

#ifdef __cplusplus
}
#endif

using google::dense_hash_set;

/*
 * create fast int hash_set
 * ~0.75 memory overhead, values 0 and -1 can't be stored :(
 *
 * return ByteBuffer value or null
 */
jobject PUBLIC TYPES_FUNCTION(intSetCreate, tf01) (JNIEnv* env, jobject thiz)
{
	dense_hash_set<jint> *set = new dense_hash_set<jint>;
	set->set_empty_key(0);
	set->set_deleted_key(-1);
	
	// return ptr as ByteBuffer
	return env->NewDirectByteBuffer(set, 1); // size must be > 0, ART 4.4.2 bug
}

// destroy set
void PUBLIC TYPES_FUNCTION(intSetDelete, tf02) (JNIEnv* env, jobject thiz,
												IN jobject set)
{
	dense_hash_set<jint> *set_ptr;

	if (get_direct_buffer_address(env, set, (void **) &set_ptr) == 0)
		return;

	delete set_ptr;
}

// get set values count
jlong PUBLIC TYPES_FUNCTION(intSetSize, tf03) (JNIEnv* env, jobject thiz,
												IN jobject set)
{
	dense_hash_set<jint> *set_ptr;

	if (get_direct_buffer_address(env, set, (void **) &set_ptr) == 0)
		return 0;

	return (jlong) set_ptr->size();
}

// return TRUE if set contains value
jboolean PUBLIC TYPES_FUNCTION(intSetContains, tf04) (JNIEnv* env, jobject thiz,
														IN jobject set, IN jint value)
{
	dense_hash_set<jint> *set_ptr;

	if (get_direct_buffer_address(env, set, (void **) &set_ptr) == 0)
		return JNI_FALSE;

	dense_hash_set<jint>::const_iterator it = set_ptr->find(value);
	if (it != set_ptr->end())
		return JNI_TRUE;

	return JNI_FALSE;
}

// return TRUE if set was modified by operation
jboolean PUBLIC TYPES_FUNCTION(intSetAdd, tf05) (JNIEnv* env, jobject thiz,
													IN jobject set, IN jint value)
{
	dense_hash_set<jint> *set_ptr;

	if (value == 0 || value == -1)
		return JNI_FALSE;
	else if (get_direct_buffer_address(env, set, (void **) &set_ptr) == 0)
		return JNI_FALSE;

	std::pair<dense_hash_set<jint>::const_iterator, bool> result = set_ptr->insert(value);
	if (result.second)
		return JNI_TRUE;

	return JNI_FALSE;
}
