
/*
 * common definitions for scanner and databases
 *
 * last modified: 2013.12.20
 */

#ifndef SCAN_H
#define SCAN_H

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include <common/common.h>

#ifdef __cplusplus
extern "C" {
#endif

#define SCANDB_SIGNATURE        0x57474442 // 'WGDB'
#define SCANDB_RECORDS_NUM_MAX  0x200000

#define SCANDB_DETECTS_MAX      64

// db common header
// include it in all db headers at start (see fixeddb.h fixeddb_header_t)
#define SCANDB_HEADER_SIZE    (4+4+4+4+4)
#define SCANDB_HEADER   \
	uint32_t signature; \
	uint32_t type;      \
	uint32_t version;   \
	uint32_t program_version; \
	uint32_t records_num;

#pragma pack(1)
struct scandb_header_t_ {
	SCANDB_HEADER
};
#pragma pack()
typedef struct scandb_header_t_ scandb_header_t;
STATIC_ASSERT(sizeof(scandb_header_t) == SCANDB_HEADER_SIZE);

/*
 * common record type for all records
 * use values > RECORD_UNKNOWN and < RECORD_LAST
 */
enum record_type_
{
	RECORD_UNKNOWN      = 0,

	RECORD_TEST         = 1,
	RECORD_WHITE        = 2,

	RECORD_CHARGEABLE   = 3,
	RECORD_FRAUD        = 4,
	RECORD_MALWARE      = 5,

	RECORD_ADS          = 6,     // invasive ads
	RECORD_ADS_TPARTY   = 7,     // ads on external sites
	RECORD_ADS_OK       = 8,     // normal ads

	RECORD_SOCIAL_OTHER    = 20, // for undefined social network
	RECORD_SOCIAL_GPLUS    = 21,
	RECORD_SOCIAL_VK       = 22,
	RECORD_SOCIAL_FB       = 23,
	RECORD_SOCIAL_TWI      = 24,
	RECORD_SOCIAL_ODNKLASS = 25,
	RECORD_SOCIAL_MAILRU   = 26,
	RECORD_SOCIAL_LJ       = 27,
	RECORD_SOCIAL_LINKEDIN = 28,
	RECORD_SOCIAL_MOIKRUG  = 29,

	RECORD_LAST,
	RECORD_CLEAN        = 255
};
typedef enum record_type_ record_type;

// scan types
enum scan_type_
{
	SCAN_UNKNOWN = 0,

	SCAN_DOMAIN  = 1,
	SCAN_URL     = 2,
	SCAN_HTML    = 3,

	SCAN_LAST
};
typedef enum scan_type_ scan_type;

/* ----------------------------------------------------------- */

// check if type value valid for record (RECORD_CLEAN is not valid type for record)
static INLINE bool record_type_is_valid (IN int type) {
	I_NOT((type > RECORD_UNKNOWN && type < RECORD_LAST), "invalid record type %d", type);
	return (type > RECORD_UNKNOWN && type < RECORD_LAST);
}

// return true if record type is dangerous object (malware, fraud)
static INLINE bool record_type_is_dangerous (IN int type) {
	return (type == RECORD_FRAUD || type == RECORD_MALWARE);
}

// return true if record type is ads
static INLINE bool record_type_is_ads (IN int type) {
	return (type >= RECORD_ADS && type <= RECORD_ADS_OK);
}

// return true if record type is social network
static INLINE bool record_type_is_social (IN int type) {
	return (type >= RECORD_SOCIAL_OTHER && type <= RECORD_SOCIAL_MOIKRUG);
}

// check scan type
static INLINE bool scan_type_is_valid (IN int type) {
	I_NOT((type > SCAN_UNKNOWN && type < SCAN_LAST), "invalid scan type %d", type);
	return (type > SCAN_UNKNOWN && type < SCAN_LAST);
}

// return db version
static INLINE uint32_t scandb_get_version (IN void *db) {
	return ((scandb_header_t *) db)->version;
}

// return number of records in db
static INLINE uint32_t scandb_get_records_num (IN void *db) {
	return ((scandb_header_t *) db)->records_num;
}

/* ----------------------------------------------------------- */

int         record_type_get_major (IN int type1, IN int type2);
const char* record_type_get_name  (IN int type);

int     scan_loaddb (IN int scan_type, IN const char *filepath, IN uint32_t program_version,
						OUT void **db);
int     scan_freedb (IN void *db);
long    scan_data   (IN void *db, IN uint8_t *data, IN size_t size, OUT uint8_t *types,
						OUT uint32_t *ids);

#ifdef __cplusplus
}
#endif

#endif /* SCAN_H */
