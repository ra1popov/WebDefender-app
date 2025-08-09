
/*
 * last modified: 2013.12.26
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include "fixeddb.h"
#include "scan.h"
#include "common/common.h"
#include "common/internal.h"
#include "common/endian.h"
#include "common/hash.h"
#include "common/bitset.h"

/*
 * load fixed length search db from file
 * return 0 on success and db ptr (call fixeddb_free on it to free memory)
 * on error return < 0
 *
 * see fixeddb.h for db formats description (disk + 'in memory')
 *
 * will not load db if program_version < db_header.program_version
 * return errors:
 *     -1 - unknown error
 *     -2 - memory allocate error
 *     -3 - can't open file
 *     -4 - header read or parse error (i.g. incorrect program version)
 *     -5 - records read or parse error
 *
 * work with internal functions
 */
int fixeddb_load (IN const char *filepath, IN uint32_t program_version, OUT void **db)
{
	FILE *f;
	int retval = -1;
	uint size, i, j;
	uint32_t records_num = 0;
	bool record_err = false;
	fixeddb_header_t db_header;
	fixeddb_hashinfo_t db_hashinfo;
	bitset_t *db_lx_filter;
	fixeddb_record_t **db_lx_map;
	fixeddb_record_t *db_records, *db_record_cur;

	*db = NULL;

	// open file
	f = i_fopen(filepath, "r+b");
	if (f == NULL)
		return -3;

	// parse db
	do
	{
		// read and check header
		retval = -4;
		if (i_fread(&db_header, 1, sizeof(fixeddb_header_t), f) != sizeof(fixeddb_header_t))
			break;

		BE32_TO_CPU_S(db_header.signature);
		BE32_TO_CPU_S(db_header.type);
		BE32_TO_CPU_S(db_header.version);
		BE32_TO_CPU_S(db_header.program_version);
		BE32_TO_CPU_S(db_header.records_num);
		BE32_TO_CPU_S(db_header.lx_hashes_num);

		if (db_header.signature != SCANDB_SIGNATURE ||
			db_header.type != FIXEDDB_SIGNATURE ||
			program_version < db_header.program_version ||
			db_header.records_num > SCANDB_RECORDS_NUM_MAX ||
			db_header.lx_hashes_num > FIXEDDB_HASHMAP_LEN)
		{
			break;
		}

		// allocate memory
		retval = -2;
		size = sizeof(fixeddb_header_t) + FIXEDDB_FILTER_SIZE +
				FIXEDDB_HASHMAP_LEN * sizeof(fixeddb_record_t*) +
				db_header.records_num * sizeof(fixeddb_record_t) +
				(db_header.lx_hashes_num + 1) * sizeof(fixeddb_record_t) + /* for empty records */
				sizeof(FIXEDDB_SIGNATURE_END);
		*db = i_malloc(size);
		if (*db == NULL)
			break;

		// fill 'in memory' db
		memset(*db, 0, size);
		*((fixeddb_header_t *) *db) = db_header;
		db_lx_filter = (bitset_t *) (((uint8_t *) *db) + sizeof(fixeddb_header_t));
		db_lx_map = (fixeddb_record_t **) (((uint8_t *) db_lx_filter) + FIXEDDB_FILTER_SIZE);
		db_records = (fixeddb_record_t *) (((uint8_t *) db_lx_map) + FIXEDDB_HASHMAP_LEN * sizeof(fixeddb_record_t*));
		db_record_cur = db_records;

		// read lx hashes + records, fill lx map pointers and bitset filter
		retval = -5;
		for (i = 0; i < db_header.lx_hashes_num; i++)
		{
			if (i_fread(&db_hashinfo, 1, sizeof(fixeddb_hashinfo_t), f) != sizeof(fixeddb_hashinfo_t))
				break;

			BE16_TO_CPU_S(db_hashinfo.lx_hash);
			BE32_TO_CPU_S(db_hashinfo.records_num);

			//if (db_hashinfo.lx_hash >= FIXEDDB_HASHMAP_LEN || db_hashinfo.lx_hash == 0 || fixed warning 28.07.2023
			if (db_hashinfo.lx_hash == 0 ||
				db_hashinfo.records_num > SCANDB_RECORDS_NUM_MAX ||
				records_num + db_hashinfo.records_num > db_header.records_num ||
				records_num + db_hashinfo.records_num <= records_num)
			{
				break;
			}

			// map + filter
			bs_set_bit(db_lx_filter, db_hashinfo.lx_hash);
			db_lx_map[db_hashinfo.lx_hash] = db_record_cur;

			// records
			size = sizeof(fixeddb_record_t) * db_hashinfo.records_num;
			if (i_fread(db_record_cur, 1, size, f) != size)
				break;

			for (j = 0; j < db_hashinfo.records_num && !record_err; j++)
			{
				BE32_TO_CPU_S(db_record_cur->first);
				BE32_TO_CPU_S(db_record_cur->m3hash);
				record_err = !(record_type_is_valid(db_record_cur->type));
				db_record_cur++;
			}
			if (record_err)
				break;

			db_record_cur++; // end, empty record after current lx hash records
			records_num += db_hashinfo.records_num;
		} // for

		if (records_num == db_header.records_num)
		{
			// all ok
			db_record_cur++; // end, empty record after all records
			*((uint32_t *) db_record_cur) = FIXEDDB_SIGNATURE_END;
			retval = 0;
		}
	}
	while (0);

	// clean
	i_fclose(f);
	if (retval != 0 && *db != NULL)
		i_free(*db);

	return retval;
}

/*
 * unload db from memory
 * return 0 on success or < 0 on error
 *
 * work with internal functions
 */
int fixeddb_free (IN void *db)
{
	if (!fixeddb_is_valid(db))
		return -1;

	i_free(db);
	return 0;
}

/* ----------------------------------------------------------- */

/*
 * return first record with same first 4 bytes or NULL (records must be sorted by first field)
 * use BE32PTR_TO_CPU_UA to fill first because data sorted and readed in BE
 */
static INLINE fixeddb_record_t* map_search_first (fixeddb_record_t* map, uint32_t first)
{
	fixeddb_record_t* map_cur = map;

	while (map_cur->type != 0 && map_cur->first < first)
		map_cur++;

	if (map_cur->type != 0 && map_cur->first == first)
		return map_cur;

	return NULL;
}

// return first record with same m3hash or NULL (map must be sorted by first + m3hash fields)
static INLINE fixeddb_record_t* map_search_m3hash (fixeddb_record_t* map, uint8_t* data, size_t size)
{
	uint32_t m3hash, first;

	// first 4 bytes
	BE32PTR_TO_CPU_UA(data, first);
	fixeddb_record_t* map_cur = map_search_first(map, first);
	if (map_cur == NULL)
		return NULL;

	// m3 hash
	murmur_hash3_32(data, size, FIXEDDB_M3HASH_SEED, (void *) &m3hash);
	while (map_cur->type != 0 && map_cur->first == first && map_cur->m3hash < m3hash)
		map_cur++;

	if (map_cur->type != 0 && map_cur->m3hash == m3hash)
		return map_cur;

	return NULL;
}

/*
 * search fixed length data in db
 * return type of founded record (see scan.h record_type) or RECORD_CLEAN if no record
 * on error return 0 (RECORD_UNKNOWN)
 *
 * see fixeddb.h for 'in memory' db format description
 *
 * data length must be > FIXEDDB_DATA_LEN_MIN or function will return RECORD_CLEAN
 * to proper url scan, data must be in lowercase
 */
uint8_t fixeddb_scan (IN void *db, IN uint8_t *data, IN size_t size)
{
	uint16_t lx_hash;
	bitset_t *db_lx_filter;
	fixeddb_record_t **db_lx_map;
	fixeddb_record_t *record;

	if (!fixeddb_is_valid(db))
		return RECORD_UNKNOWN;

	if (size < FIXEDDB_DATA_LEN_MIN)
		return RECORD_CLEAN;

	db_lx_filter = (bitset_t *) (((uint8_t *) db) + sizeof(fixeddb_header_t));
	db_lx_map = (fixeddb_record_t **) (((uint8_t *) db_lx_filter) + FIXEDDB_FILTER_SIZE);

	lx_hash = lenxor_hash(data, size);
	if (bs_get_bit(db_lx_filter, lx_hash) != 0)
	{
		// hash ok, search map elements by hash
		record = map_search_m3hash(db_lx_map[lx_hash], data, size);
		if (record != NULL)
			return record->type;
	}

	return RECORD_CLEAN;
}
