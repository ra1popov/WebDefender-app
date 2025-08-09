
/*
 * common definitions for fixed length search db
 *
 * last modified: 2013.12.26
 *
 * database format:
 *     header:
 *         db signature            (BE32, == SCANDB_SIGNATURE)
 *         db type                 (BE32, == FIXEDDB_SIGNATURE)
 *         db version              (BE32)
 *         minimal program version (BE32)
 *         number of records       (BE32, <= SCANDB_RECORDS_NUM_MAX)
 *         number of lx hashes     (BE32, <= FIXEDDB_HASHMAP_LEN)
 *     for each lx hash used:
 *         lx hash header:
 *             lx hash             (BE16, < FIXEDDB_HASHMAP_LEN)
 *             aligment            (16)
 *             number of records   (BE32, <= SCANDB_RECORDS_NUM_MAX)
 *         lx hash record:
 *             first 4 bytes       (BE32)
 *             murmur3 hash32      (BE32)
 *             type                (8, > RECORD_UNKNOWN && < RECORD_LAST)
 *             aligment            (24)
 *         ... next records
 *
 * records for each lx hash sorted by first 4 bytes + murmur3 hash
 * (sort by first, if equals by m3hash)
 *
 * database 'in memory' format:
 *     header                                    (CPU, all fields converted to native)
 *     lx hashes prefilter bitset                (BE, size == FIXEDDB_FILTER_SIZE)
 *     pointers to first record (or null) for each lx hash (CPU, size == FIXEDDB_HASHMAP_LEN * sizeof(void*))
 *     lx hash records (for each lx hash used):
 *         lx hash record                        (CPU, all fields converted to native)
 *         ... next records
 *         empty record                          (type == 0, after all records for same hash)
 *     empty record                              (type == 0, after all records)
 *     db end signature                          (CPU, == SCANDB_SIGNATURE_END)
 */

#ifndef FIXEDDB_H
#define FIXEDDB_H

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

#define FIXEDDB_SIGNATURE     0x46495844 // 'FIXD'
#define FIXEDDB_SIGNATURE_END 0x46495845 // 'FIXE'

#define FIXEDDB_DATA_LEN_MIN  4          // == sizeof(uint32_t), see map_search_m3hash
#define FIXEDDB_HASHMAP_LEN   0x10000    // == max lenxor hash FFFFh
#define FIXEDDB_FILTER_SIZE   (BITSET_SIZE(FIXEDDB_HASHMAP_LEN))

#define FIXEDDB_M3HASH_SEED   0x12345678

// record info
#pragma pack(1)
struct fixeddb_record_t_
{
	uint32_t first;
	uint32_t m3hash;
	uint8_t  type;
	uint8_t  aligment[3];
};
#pragma pack()
typedef struct fixeddb_record_t_ fixeddb_record_t;
STATIC_ASSERT(sizeof(fixeddb_record_t) == 4+4+1+3);

// fixed db header
#pragma pack(1)
struct fixeddb_header_t_
{
	SCANDB_HEADER
	uint32_t lx_hashes_num;
};
#pragma pack()
typedef struct fixeddb_header_t_ fixeddb_header_t;
STATIC_ASSERT(sizeof(fixeddb_header_t) == SCANDB_HEADER_SIZE+4);

// fixed db records header
#pragma pack(1)
struct fixeddb_hashinfo_t_
{
	uint16_t lx_hash;
	uint8_t  aligment[2];
	uint32_t records_num;
};
#pragma pack()
typedef struct fixeddb_hashinfo_t_ fixeddb_hashinfo_t;
STATIC_ASSERT(sizeof(fixeddb_hashinfo_t) == 2+2+4);

/* ----------------------------------------------------------- */

static INLINE bool fixeddb_is_valid (IN void *db) {
	fixeddb_header_t *header = (fixeddb_header_t *) db;
	return (header != NULL &&
			header->signature == SCANDB_SIGNATURE && header->type == FIXEDDB_SIGNATURE);
}

/* ----------------------------------------------------------- */

int     fixeddb_load (IN const char *filepath, IN uint32_t program_version, OUT void **db);
int     fixeddb_free (IN void *db);
uint8_t fixeddb_scan (IN void *db, IN uint8_t *data, IN size_t size);

#ifdef __cplusplus
}
#endif

#endif /* FIXEDDB_H */
