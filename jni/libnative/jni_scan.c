
/*
 * jni scan functions
 *
 * last modified: 2015.09.18
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "common/common.h"
#include "scan/scan.h"
#include "scan/fixeddb.h"
#include "scan/bindata.h"
#include "jfunctions.h"

/*
 * load file from filepath as database for specific scan_type (== enum ScanType)
 * return ByteBuffer value or null
 *
 * check with dbIsLoaded if database loaded or not
 */
jobject PUBLIC SCAN_FUNCTION(loadDB, sf01) (JNIEnv* env, jobject thiz,
											IN jint scan_type, IN jstring filepath,
											IN jint program_version)
{
	void *db = NULL;
	const char *filepath_s = NULL;

	if (filepath == NULL)
		return NULL;

	do
	{
		filepath_s = (*env)->GetStringUTFChars(env, filepath, NULL);
		if (filepath_s == NULL)
		{
			E("GetStringUTFChars failed");
			break; // OutOfMemoryError already thrown
		}

		if (scan_loaddb(scan_type, filepath_s, program_version, &db) != 0)
			db = NULL; // load failed
	}
	while (0);

	if (filepath_s != NULL)
		(*env)->ReleaseStringUTFChars(env, filepath, filepath_s);

	// return ptr as ByteBuffer
	if (db == NULL)
		return NULL;
	else
		return (*env)->NewDirectByteBuffer(env, db, 1); // size must be > 0, ART 4.4.2 bug
}

// unload db from memory
jboolean PUBLIC SCAN_FUNCTION(unloadDB, sf02) (JNIEnv* env, jobject thiz,
												IN jobject db)
{
	void *db_ptr;

	if (get_direct_buffer_address(env, db, &db_ptr) == 0)
		return JNI_FALSE;

	if (scan_freedb(db_ptr) != 0)
		return JNI_FALSE;

	return JNI_TRUE;
}

// check that db loaded correctly
jboolean PUBLIC SCAN_FUNCTION(dbIsLoaded, sf03) (JNIEnv* env, jobject thiz,
													IN jobject db)
{
	void *db_ptr;

	if (db == NULL)
		return JNI_FALSE;

	db_ptr = (void *) (*env)->GetDirectBufferAddress(env, db);
	if (db_ptr == NULL)
		return JNI_FALSE;

	return JNI_TRUE;
}

// return database version
jint PUBLIC SCAN_FUNCTION(dbGetVersion, sf04) (JNIEnv* env, jobject thiz,
												IN jobject db)
{
	void *db_ptr;

	if (get_direct_buffer_address(env, db, &db_ptr) == 0)
		return JNI_FALSE;

	return scandb_get_version(db_ptr);
}

// return number of records in database
jlong PUBLIC SCAN_FUNCTION(dbGetRecordsNumber, sf05) (JNIEnv* env, jobject thiz,
														IN jobject db)
{
	void *db_ptr;

	if (get_direct_buffer_address(env, db, &db_ptr) == 0)
		return JNI_FALSE;

	return scandb_get_records_num(db_ptr);
}

// return most important record type (type1 and type2 == enum RecordType)
jshort PUBLIC SCAN_FUNCTION(recordTypeGetMajor, sf06) (JNIEnv* env, jobject thiz,
														IN jint type1, IN jint type2)
{
	return (jshort) record_type_get_major(type1, type2);
}

// check if record type is dangerous object (malware, fraud)
jboolean PUBLIC SCAN_FUNCTION(recordTypeIsDangerous, sf07) (JNIEnv* env, jobject thiz,
															IN jint type)
{
	return ((record_type_is_dangerous(type)) ? JNI_TRUE : JNI_FALSE);
}

// check if record type is type is ads
jboolean PUBLIC SCAN_FUNCTION(recordTypeIsAds, sf08) (JNIEnv* env, jobject thiz,
														IN jint type)
{
	return ((record_type_is_ads(type)) ? JNI_TRUE : JNI_FALSE);
}

// check if record type is social network
jboolean PUBLIC SCAN_FUNCTION(recordTypeIsSocial, sf09) (JNIEnv* env, jobject thiz,
															IN jint type)
{
	return ((record_type_is_social(type)) ? JNI_TRUE : JNI_FALSE);
}

/*
 * scan data with db
 * return int array with types of founded record (see scan.h record_type)
 *     + same number of records ids (any record id may be == 0)
 * on error return NULL
 *
 * use RecordType.fromValue to convert type values
 *
 * to proper url scan, it must be in lowercase
 */
jintArray PUBLIC SCAN_FUNCTION(dbScanData, sf10) (JNIEnv* env, jobject thiz,
													IN jobject db, IN jbyteArray bytes,
													IN jlong size, IN jint dummy)
{
	void *db_ptr;
	jbyte *jbytes;
	long i, j, founded;
	uint8_t verdicts[SCANDB_DETECTS_MAX];
	uint32_t ids[SCANDB_DETECTS_MAX];
	jintArray result;
	jint *jresult;

	if (get_direct_buffer_address(env, db, &db_ptr) == 0)
		return JNI_FALSE;

	jbytes = (*env)->GetByteArrayElements(env, bytes, NULL);
	if (jbytes == NULL)
	{
		E("GetByteArrayElements failed");
		return NULL;
	}

	// scan
	founded = scan_data(db_ptr, (uint8_t *) jbytes, size, verdicts, ids);
	(*env)->ReleaseByteArrayElements(env, bytes, jbytes, JNI_ABORT); // not copy back contents
	if (founded < 0)
		return NULL;

	// result
	result = (*env)->NewIntArray(env, founded * 2);
	if (result == NULL)
	{
		E("NewIntArray failed");
		return NULL; // out of memory error thrown
	}

	jresult = (*env)->GetIntArrayElements(env, result, NULL);
	if (jresult == NULL)
	{
		E("GetIntArrayElements failed");
		return NULL;
	}

	for (i = 0, j = 0; i < founded; i++, j++)
		jresult[j] = verdicts[i];
	for (i = 0; i < founded; i++, j++)
		jresult[j] = ids[i];

	(*env)->ReleaseIntArrayElements(env, result, jresult, 0);

	return result;
}

/* ----------------------------------------------------------- */

/*
 * check data format and return type of founded format or BIN_UNKNOWN
 *
 * use BinaryType.fromValue to convert return value
 */
jshort PUBLIC SCAN_FUNCTION(binaryDataDetectType, sf11) (JNIEnv* env, jobject thiz,
															IN jbyteArray bytes,
															IN jlong size, IN jint dummy)
{
	jbyte *jbytes;
	uint8_t retval;

	if (bytes == NULL)
		return BIN_DATA_UNKNOWN;

	jbytes = (*env)->GetByteArrayElements(env, bytes, NULL);
	if (jbytes == NULL)
	{
		E("GetByteArrayElements failed");
		return BIN_DATA_UNKNOWN;
	}

	retval = bin_data_detect_type((uint8_t *) jbytes, size);
	(*env)->ReleaseByteArrayElements(env, bytes, jbytes, JNI_ABORT); // not copy back contents

	return retval;
}

/*
 * search data for first known format and return type of founded format or BIN_UNKNOWN
 *
 * use BinaryType.fromValue to convert return value
 */
jshort PUBLIC SCAN_FUNCTION(binaryDataSearchType, sf12) (JNIEnv* env, jobject thiz,
															IN jbyteArray bytes,
															IN jlong size, IN jint dummy)
{
	jbyte *jbytes;
	uint8_t retval;

	if (bytes == NULL)
		return BIN_DATA_UNKNOWN;

	jbytes = (*env)->GetByteArrayElements(env, bytes, NULL);
	if (jbytes == NULL)
	{
		E("GetByteArrayElements failed");
		return BIN_DATA_UNKNOWN;
	}

	retval = bin_data_search_type((uint8_t *) jbytes, size, NULL);
	(*env)->ReleaseByteArrayElements(env, bytes, jbytes, JNI_ABORT); // not copy back contents

	return retval;
}

// check if binary format type is archive (type == enum BinaryType)
jboolean PUBLIC SCAN_FUNCTION(binaryTypeIsArchive, sf13) (JNIEnv* env, jobject thiz,
															IN jint type)
{
	return ((bin_data_type_is_archive(type)) ? JNI_TRUE : JNI_FALSE);
}

// check if binary format type is OS executable
jboolean PUBLIC SCAN_FUNCTION(binaryTypeIsExecutable, sf14) (JNIEnv* env, jobject thiz,
																IN jint type)
{
	return ((bin_data_type_is_executable(type)) ? JNI_TRUE : JNI_FALSE);
}
