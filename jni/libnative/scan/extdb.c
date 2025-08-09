
/*
 * last modified: 2013.12.26
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

#include "extdb.h"
#include "scan.h"
#include "common/common.h"
#include "common/internal.h"
#include "common/endian.h"
#include "common/hash.h"
#include "common/bitset.h"
#include "common/functions.h"

/*
 * load variable length search db from file
 * return 0 on success and db ptr (call extdb_free on it to free memory)
 * on error return < 0
 *
 * see extdb.h for db formats description (disk + 'in memory')
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
int extdb_load (IN const char *filepath, IN uint32_t program_version, OUT void **db)
{
	FILE *f;
	int retval = -1;
	uint size, i, j;
	uint32_t tmp32, ids_num, records_num = 0;
	uint8_t tmp8;
	bool record_err = false;
	extdb_settings_t db_settings;
	extdb_header_t db_header;
	extdb_hashinfo_t db_hashinfo;
	bitset_t *db_lx_filter;
	extdb_record_t **db_lx_map;
	extdb_record_t *db_records, *db_record_cur;
	extdb_signature_t *db_signature_cur;

	*db = NULL;
	memset(&db_settings, 0, sizeof(db_settings));

    I("TEST open file: %s", filepath);
	// open file
	f = i_fopen(filepath, "r+b");
	if (f == NULL)
		return -3;
    I("TEST opened file: %s", filepath);
	// parse db
	do
	{
		// read and check header
		retval = -4;
		if (i_fread(&db_header, 1, sizeof(extdb_header_t), f) != sizeof(extdb_header_t))
			break;

		BE32_TO_CPU_S(db_header.signature);
		BE32_TO_CPU_S(db_header.type);
		BE32_TO_CPU_S(db_header.version);
		BE32_TO_CPU_S(db_header.program_version);
		BE32_TO_CPU_S(db_header.records_num);
		BE32_TO_CPU_S(db_header.lx_hashes_num);
		BE32_TO_CPU_S(db_header.signatures_num);

		if (db_header.signature != SCANDB_SIGNATURE ||
			db_header.type != EXTDB_SIGNATURE ||
			program_version < db_header.program_version ||
			db_header.records_num > SCANDB_RECORDS_NUM_MAX ||
			db_header.lx_hashes_num > EXTDB_HASHMAP_LEN ||
			db_header.signatures_num > SCANDB_RECORDS_NUM_MAX)
		{
			break;
		}

		// allocate memory
		retval = -2;
		size = sizeof(extdb_header_t) + sizeof(extdb_settings_t) +
				EXTDB_FILTER_LEN_SIZE +
				EXTDB_HASHMAP_LEN * sizeof(extdb_record_t*) +
				db_header.records_num * sizeof(extdb_record_t) +
				(db_header.lx_hashes_num + 1) * sizeof(extdb_record_t) + /* for empty records */
				(db_header.signatures_num + 1) * sizeof(extdb_signature_t) + /* for last empty record */
				sizeof(EXTDB_SIGNATURE_END);
		*db = i_malloc(size);
		if (*db == NULL)
			break;

		// fill 'in memory' db
		memset(*db, 0, size);
		*((extdb_header_t *) *db) = db_header; // + mem for settings
		db_lx_filter = (bitset_t *) (((uint8_t *) *db) + sizeof(extdb_header_t) + sizeof(extdb_settings_t));
		db_lx_map = (extdb_record_t **) (((uint8_t *) db_lx_filter) + EXTDB_FILTER_LEN_SIZE);
		db_records = (extdb_record_t *) (((uint8_t *) db_lx_map) + EXTDB_HASHMAP_LEN * sizeof(extdb_record_t*));
		db_record_cur = db_records;

		// read lx hashes + records, fill lx map pointers and bitset filter
		retval = -5;
		for (i = 0; i < db_header.lx_hashes_num; i++)
		{
			if (i_fread(&db_hashinfo, 1, sizeof(extdb_hashinfo_t), f) != sizeof(extdb_hashinfo_t))
				break;

			BE16_TO_CPU_S(db_hashinfo.lx_hash);
			BE32_TO_CPU_S(db_hashinfo.records_num);

			if (db_hashinfo.lx_hash >= EXTDB_HASHMAP_LEN || db_hashinfo.lx_hash == 0 ||
				db_hashinfo.records_num > SCANDB_RECORDS_NUM_MAX ||
				records_num + db_hashinfo.records_num > db_header.records_num ||
				records_num + db_hashinfo.records_num <= records_num)
			{
				break;
			}

			// map + filter + max len
			db_lx_map[db_hashinfo.lx_hash] = db_record_cur;
			bs_set_bit(db_lx_filter, db_hashinfo.lx_hash);

			tmp32 = (uint8_t) (db_hashinfo.lx_hash >> 8);
			db_settings.lx_hash_lm = MAX(db_settings.lx_hash_lm, tmp32);
			if (tmp32 > EXTDB_DATA_LEN_MAX)
				break;

			// records
			size = sizeof(extdb_record_t) * db_hashinfo.records_num;
			if (i_fread(db_record_cur, 1, size, f) != size)
				break;

			for (j = 0; j < db_hashinfo.records_num && !record_err; j++)
			{
				BE32_TO_CPU_S(db_record_cur->first);
				BE32_TO_CPU_S(db_record_cur->m3hash);
				BE32_TO_CPU_S(db_record_cur->bit);
				db_record_cur++;
			}
			if (record_err) // XXX ???
				break;

			db_record_cur++; // end, empty record after current lx hash records
			records_num += db_hashinfo.records_num;
		} // for

		if (records_num == db_header.records_num)
		{
			// ok
			db_record_cur++; // end, empty record after all records

			// read signatures
			retval = -6;
			db_signature_cur = (extdb_signature_t *) db_record_cur;
			tmp32 = db_header.signatures_num * sizeof(extdb_signature_t);
			if (i_fread(db_signature_cur, 1, tmp32, f) != tmp32)
				break;

			ids_num = 0;
			for (i = 0; i < db_header.signatures_num && !record_err; i++)
			{
				BE32_TO_CPU_S(db_signature_cur->flags);
				BE32_TO_CPU_S(db_signature_cur->value);
				if (db_signature_cur->flags == EXTDB_FLAG_NONE)
				{
					tmp8 = (uint8_t) db_signature_cur->value;
					tmp32 = (uint32_t) (db_signature_cur->value >> 8);
					record_err = (tmp32 > EXTDB_ID_MAX || !(record_type_is_valid(tmp8)));
					ids_num++;
				}
				else if (db_signature_cur->value >= EXTDB_FLAGS_NUM_MAX)
				{
					record_err = true;
				}
				db_signature_cur++;
			}
			if (record_err)
				break;

			db_signature_cur++; // end, empty signature after all

			// all ok
			*((uint32_t *) db_signature_cur) = EXTDB_SIGNATURE_END;

			db_settings.lx_filter = db_lx_filter;
			db_settings.lx_map = db_lx_map;
			db_settings.signatures = (extdb_signature_t *) db_record_cur;

			*((extdb_settings_t *) (((uint8_t *) *db) + sizeof(extdb_header_t))) = db_settings;
			((extdb_header_t *) *db)->records_num = ids_num;

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
int extdb_free (IN void *db)
{
	if (!extdb_is_valid(db))
		return -1;

	i_free(db);
	return 0;
}

/* ----------------------------------------------------------- */

/*
 * return first record with same first 4 bytes or NULL (records must be sorted by first field)
 * use BE32PTR_TO_CPU_UA to fill first because data sorted and readed in BE
 */
static INLINE extdb_record_t* map_search_first (extdb_record_t* map, uint32_t first)
{
	extdb_record_t* map_cur = map;

	while (map_cur->m3hash != 0 && map_cur->first < first)
		map_cur++;

	if (map_cur->m3hash != 0 && map_cur->first == first)
		return map_cur;

	return NULL;
}

// return first record with same m3hash or NULL (map must be sorted by first + m3hash fields)
static INLINE extdb_record_t* map_search_m3hash (extdb_record_t* map, uint8_t* data, size_t size)
{
	uint32_t m3hash, first;

	// first 4 bytes
	BE32PTR_TO_CPU_UA(data, first);
	extdb_record_t* map_cur = map_search_first(map, first);
	if (map_cur == NULL)
		return NULL;

	// m3 hash
	murmur_hash3_32(data, size, EXTDB_M3HASH_SEED, (void *) &m3hash);
	while (map_cur->m3hash != 0 && map_cur->first == first && map_cur->m3hash < m3hash)
		map_cur++;

	if (map_cur->m3hash != 0 && map_cur->m3hash == m3hash)
		return map_cur;

	return NULL;
}

//
static INLINE long data_search_pos (IN extdb_record_t **lx_map, IN bitset_t *lx_filter,
									IN uint8_t *data, IN size_t size, IN size_t pos,
									INOUT bitset_t *flags)
{
	uint8_t b, pb, x, len;
	//uint16_t sum, hash, tmp; // we do not use tmp and hide it, 28.07.2023
	uint16_t sum, hash;
	size_t pos_;
	int founded = 0;
	extdb_record_t *map_cur;
//	char buf[1024];

	for (pos_ = 0, len = 1, sum = 0, pb = 0, x = 0;
			pos_ < EXTDB_DATA_LEN_MAX && pos_ < size;
			pos_++, len++, pb = b)
	{
		// prepare data
		b = data[pos_];
		x ^= b;
//		sum = sum16_add_byte(sum, b);

		// check filters
//		if (bs_get_bit(sum16_filter[pos], sum) == 0)
//			return founded;
//		tmp = (pb << 8) + b;
//		if (bs_get_bit(bytes_filter[pos], tmp) == 0)
//			return founded;

		// minimal size
		if (pos_ < EXTDB_DATA_LEN_MIN - 1) // XXX
			continue;

		// lenxor hash
		hash = (((uint8_t) len) << 8) + x;
		if (bs_get_bit(lx_filter, hash) == 0)
			continue;

		// ok, search hashes
		map_cur = map_search_m3hash(lx_map[hash], data, len);
		if (map_cur != NULL)
		{
//			memcpy(buf, data, len);
//			buf[len] = '\0';
//			I("%s", buf);
			bs_set_bit(flags, map_cur->bit);
			founded++;
		}
	} // for

	return founded;
}

//
static INLINE long data_search (IN extdb_record_t **lx_map, IN bitset_t *lx_filter,
								IN uint32_t lx_lm, IN uint8_t *data, IN size_t size,
								OUT bitset_t *flags)
{
	size_t pos, size_;
	long founded = 0;
	uint8_t *data_ = data;

	for (pos = 0, size_ = size; size_ >= EXTDB_DATA_LEN_MIN; pos++, size_--, data_++)
	{
		founded += data_search_pos(lx_map, lx_filter, data_, MIN(size_, lx_lm), pos,
									flags);
	}

	return founded;
}

// XXX
static INLINE long check_flags (IN bitset_t *flags, IN extdb_signature_t *signatures,
								OUT uint8_t *types, OUT uint32_t *ids)
{
	long detects = 0;
	extdb_signature_t *s = signatures;
	bool is_detect, is_or;

	while (!(s->flags == EXTDB_FLAG_NONE && s->value == 0))
	{
		// check flags for one sign
		is_detect = true;
		while (is_detect && s->flags != EXTDB_FLAG_NONE)
		{
			if (s->flags & EXTDB_FLAG_AND)
			{
				if (bs_get_bit(flags, s->value) == 0) is_detect = false;
				if (s->flags & EXTDB_FLAG_NOT)        is_detect = !is_detect;
				s++;
			}
			else if (s->flags & EXTDB_FLAG_OR)
			{
				is_or = false;
				do
				{
					if (bs_get_bit(flags, s->value) > 0) is_or = true;
					if (s->flags & EXTDB_FLAG_NOT)       is_or = !is_or;
					s++;
				}
				while (!is_or && (s->flags & EXTDB_FLAG_OR));
				if (!is_or) is_detect = false;

				while ((s->flags & EXTDB_FLAG_OR)) s++; // skip rest or
			}
			else
			{
				// unknown flag
				is_detect = false;
				break;
			}
		}

		// skip rest
		while (s->flags != EXTDB_FLAG_NONE) s++;

		// detect ?
		if (s->value != 0)
		{
			if (is_detect)
			{
				if (detects >= SCANDB_DETECTS_MAX) return detects;
				types[detects] = (uint8_t) s->value;
				ids[detects] =   (uint32_t) (s->value >> 8);
				detects++;
			}
			s++;
		}
	} // while

	return detects;
}

/*
 * search variable length data in db
 * return number of founded record (or < 0 on error)
 *     on success scan fill types array with types of founded records (see scan.h record_type)
 *     and fill ids array with ids of records
 *
 * types and ids arrays must be allocated with size SCANDB_DETECTS_MAX
 *
 * see extdb.h for 'in memory' db format description
 *
 * data length must be > EXTDB_DATA_LEN_MIN or function will return 0
 * to proper url scan, data must be in lowercase
 */
long extdb_scan (IN void *db, IN uint8_t *data, IN size_t size, OUT uint8_t *types,
					OUT uint32_t *ids)
{
	extdb_settings_t *db_settings;
	long founded, signatures = 0;
	bitset_t flags[EXTDB_FLAGS_SIZE];

	if (!extdb_is_valid(db))
		return -1;

	if (size < EXTDB_DATA_LEN_MIN)
		return 0;

	db_settings = (extdb_settings_t *) (((uint8_t *) db) + sizeof(extdb_header_t));

	memset(flags, 0, sizeof(flags));
	founded = data_search(db_settings->lx_map, db_settings->lx_filter,
							db_settings->lx_hash_lm, data, size, flags);

	signatures = check_flags(flags, db_settings->signatures, types, ids);
	//I("found hashes %d sigs %d", (int) founded, (int) signatures);

	return signatures;
}
