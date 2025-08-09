
/*
 * common definitions for variable length search db
 *
 * last modified: 2013.12.26
 *
 * database format:
 *     header:
 *         db signature            (BE32, == SCANDB_SIGNATURE)
 *         db type                 (BE32, == EXTDB_SIGNATURE)
 *         db version              (BE32)
 *         minimal program version (BE32)
 *         number of records       (BE32, <= SCANDB_RECORDS_NUM_MAX)
 *         number of lx hashes     (BE32, <= EXTDB_HASHMAP_LEN)
 *     ++here sum, bytes prefilters++
 *     for each lx hash used:
 *         lx hash header:
 *             lx hash             (BE16, < EXTDB_HASHMAP_LEN)
 *             aligment            (16)
 *             number of records   (BE32, <= SCANDB_RECORDS_NUM_MAX)
 *         lx hash record:
 *             first 4 bytes       (BE32)
 *             murmur3 hash32      (BE32)
 *             ++here flags++
 *         ... next records
 *
 * records for each lx hash sorted by first 4 bytes + murmur3 hash
 * (sort by first, if equals by m3hash)
 *
 * database 'in memory' format:
 *     header                                    (CPU, all fields converted to native)
 *     lx hashes prefilter bitset                (BE, size == EXTDB_FILTER_SIZE)
 *     ++here sum, bytes prefilters++
 *     pointers to first record (or null) for each lx hash (CPU, size == EXTDB_HASHMAP_LEN * sizeof(void*))
 *     lx hash records (for each lx hash used):
 *         lx hash record                        (CPU, all fields converted to native)
 *         ... next records
 *         empty record                          (type == 0, after all records for same hash)
 *     empty record                              (type == 0, after all records)
 *     db end signature                          (CPU, == SCANDB_SIGNATURE_END)
 */

#ifndef EXTDB_H
#define EXTDB_H

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include "scan.h"
#include "common/common.h"
#include "common/bitset.h"

#ifdef __cplusplus
extern "C" {
#endif

#define EXTDB_SIGNATURE     0x45585444 // 'EXTD'
#define EXTDB_SIGNATURE_END 0x45585445 // 'EXTE'

#define EXTDB_DATA_LEN_MIN     4          // == sizeof(uint32_t), see map_search_m3hash
#define EXTDB_DATA_LEN_MAX     64         // for internal usage (data max len for search hash)
#define EXTDB_HASHMAP_LEN      0x4100     // == max lenxor hash 40FFh
#define EXTDB_FILTER_LEN_SIZE  (BITSET_SIZE(EXTDB_HASHMAP_LEN))

#define EXTDB_ID_MAX           0xFFFFFF

#define EXTDB_FLAGS_NUM_MAX    0x20000
#define EXTDB_FLAGS_SIZE       (BITSET_SIZE(EXTDB_FLAGS_NUM_MAX))

#define EXTDB_FILTER_SUM_LEN    0x10000     // == max sum16 FFFFh
#define EXTDB_FILTER_SUM_SIZE   (BITSET_SIZE(EXTDB_FILTER_SUM_LEN))
#define EXTDB_FILTER_BYTE_LEN   0x10000     // == max bytes FFFFh
#define EXTDB_FILTER_BYTE_SIZE  (BITSET_SIZE(EXTDB_FILTER_BYTE_LEN))

#define EXTDB_M3HASH_SEED   0x23456789

// record info
#pragma pack(1)
struct extdb_record_t_
{
	uint32_t first;
	uint32_t m3hash;
/*
	union {
	uint32_t *flags;
	uint32_t flags_num;   // save flags number on disk
	uint8_t  aligment[8]; // == MAX(sizeof(uint32_t*)) for 32/64 bits
	};
*/
	uint32_t bit;
};
#pragma pack()
typedef struct extdb_record_t_ extdb_record_t;
STATIC_ASSERT(sizeof(extdb_record_t) == 4+4+4);

// extended db header
#pragma pack(1)
struct extdb_header_t_
{
	SCANDB_HEADER
	uint32_t lx_hashes_num;
	uint32_t signatures_num; // count of all flags checks
};
#pragma pack()
typedef struct extdb_header_t_ extdb_header_t;
STATIC_ASSERT(sizeof(extdb_header_t) == SCANDB_HEADER_SIZE+4+4);

// extended db records header
#pragma pack(1)
struct extdb_hashinfo_t_
{
	uint16_t lx_hash;
	uint8_t  aligment[2];
	uint32_t records_num;
};
#pragma pack()
typedef struct extdb_hashinfo_t_ extdb_hashinfo_t;
STATIC_ASSERT(sizeof(extdb_hashinfo_t) == 2+2+4);

// flags for db signatures
enum extdb_flag_t_
{
	EXTDB_FLAG_NONE = 0, // for last signature with verdict
	EXTDB_FLAG_AND  = 1, // and
	EXTDB_FLAG_OR   = 2, // or
	EXTDB_FLAG_NOT  = 4, // not

	EXTDB_FLAG_LAST
};
typedef enum extdb_flag_t_ extdb_flags_t;

// db signature
#pragma pack(1)
struct extdb_signature_t_
{
	uint32_t flags; // if == EXTDB_FLAG_NONE then value contains (id << 8 + type)
	uint32_t value;
};
#pragma pack()
typedef struct extdb_signature_t_ extdb_signature_t;
STATIC_ASSERT(sizeof(extdb_signature_t) == 4+4);

// setting for loaded in memory extended db
#pragma pack(1)
struct extdb_settings_t_
{
	uint32_t lx_hash_lm; // max data len for lenxor hash
	bitset_t *lx_filter;
	extdb_record_t **lx_map;
	extdb_signature_t *signatures;
};
#pragma pack()
typedef struct extdb_settings_t_ extdb_settings_t;
//STATIC_ASSERT(sizeof(extdb_settings_t) == 4);

/* ----------------------------------------------------------- */

static INLINE bool extdb_is_valid (IN void *db) {
	extdb_header_t *header = (extdb_header_t *) db;
	return (header != NULL &&
			header->signature == SCANDB_SIGNATURE && header->type == EXTDB_SIGNATURE);
}

static INLINE bool extdb_flag_is_valid (IN uint32_t flag) {
	return (flag >= EXTDB_FLAG_NONE && flag < EXTDB_FLAG_LAST);
}

/* ----------------------------------------------------------- */

int  extdb_load (IN const char *filepath, IN uint32_t program_version, OUT void **db);
int  extdb_free (IN void *db);
long extdb_scan (IN void *db, IN uint8_t *data, IN size_t size, OUT uint8_t *types,
					OUT uint32_t *ids);

#ifdef __cplusplus
}
#endif

#endif /* EXTDB_H */
