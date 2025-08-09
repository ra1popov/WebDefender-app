
/*
 * last modified: 2013.12.26
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdlib.h>

#include "scan.h"
#include "fixeddb.h"
#include "common/common.h"
#include "common/endian.h"
#include "extdb.h"

/*
 * return most important record type (see record_type)
 * on unknown type return 0 (RECORD_UNKNOWN)
 *
 * RECORD_WHITE > RECORD_MALWARE > RECORD_FRAUD > RECORD_CHARGEABLE >
 *     RECORD_SOCIAL_* > RECORD_ADS_OK > RECORD_ADS_TPARTY > RECORD_ADS >
 *     RECORD_TEST > RECORD_CLEAN
 */
int record_type_get_major (IN int type1, IN int type2)
{
	if ((type1 != RECORD_CLEAN && !record_type_is_valid(type1)) ||
		(type2 != RECORD_CLEAN && !record_type_is_valid(type2)))
	{
		return RECORD_UNKNOWN;
	}

	if (type1 == type2) return type1;
	//
	else if (type1 == RECORD_WHITE        || type2 == RECORD_WHITE) return RECORD_WHITE;
	else if (type1 == RECORD_MALWARE      || type2 == RECORD_MALWARE) return RECORD_MALWARE;
	else if (type1 == RECORD_FRAUD        || type2 == RECORD_FRAUD) return RECORD_FRAUD;
	else if (type1 == RECORD_CHARGEABLE   || type2 == RECORD_CHARGEABLE) return RECORD_CHARGEABLE;
	//
	else if (type1 >= RECORD_SOCIAL_OTHER && type1 <= RECORD_SOCIAL_MOIKRUG) return type1;
	else if (type2 >= RECORD_SOCIAL_OTHER && type2 <= RECORD_SOCIAL_MOIKRUG) return type2;
	//
	else if (type1 == RECORD_ADS_OK       || type2 == RECORD_ADS_OK) return RECORD_ADS_OK;
	else if (type1 == RECORD_ADS_TPARTY   || type2 == RECORD_ADS_TPARTY) return RECORD_ADS_TPARTY;
	else if (type1 == RECORD_ADS          || type2 == RECORD_ADS) return RECORD_ADS;
	//
	else if (type1 == RECORD_TEST         || type2 == RECORD_TEST) return RECORD_TEST;
	else if (type1 == RECORD_CLEAN        || type2 == RECORD_CLEAN) return RECORD_CLEAN;

	return RECORD_UNKNOWN;
}

/*
 * return record type text name
 * do not modify returned string
 */
const char* record_type_get_name (IN int type)
{
	if ((type != RECORD_CLEAN && !record_type_is_valid(type)))
		return "unknown";

	switch (type)
	{
		case RECORD_TEST:       return "test";
		case RECORD_WHITE:      return "white";

		case RECORD_CHARGEABLE: return "chargeable";
		case RECORD_FRAUD:      return "fraud";
		case RECORD_MALWARE:    return "malware";

		case RECORD_ADS:        return "ads";
		case RECORD_ADS_TPARTY: return "ads_thirdparty";
		case RECORD_ADS_OK:     return "ads_ok";

		case RECORD_SOCIAL_OTHER:    return "social_other";
		case RECORD_SOCIAL_GPLUS:    return "social_googleplus";
		case RECORD_SOCIAL_VK:       return "social_vkontakte";
		case RECORD_SOCIAL_FB:       return "social_facebook";
		case RECORD_SOCIAL_TWI:      return "social_twitter";
		case RECORD_SOCIAL_ODNKLASS: return "social_odnoklassniki";
		case RECORD_SOCIAL_MAILRU:   return "social_mailru";
		case RECORD_SOCIAL_LJ:       return "social_livejournal";
		case RECORD_SOCIAL_LINKEDIN: return "social_linkedin";
		case RECORD_SOCIAL_MOIKRUG:  return "social_moikrug";

		case RECORD_CLEAN:      return "clean";
	}

	return "unknown";
}

/* ----------------------------------------------------------- */

/*
 * load db to scan selected type
 * return 0 on success and db ptr (call scan_freedb on it to free memory)
 * on error return < 0 (see error codes in db load functions)
 *
 * will not load db if program_version < db_header.program_version
 */
int scan_loaddb (IN int scan_type, IN const char *filepath, IN uint32_t program_version,
					OUT void **db)
{
	int retval = -1;

	if (!scan_type_is_valid(scan_type))
		return -1;

	switch (scan_type)
	{
		case SCAN_DOMAIN:
			retval = fixeddb_load(filepath, program_version, db);
			break;

		case SCAN_URL:
			retval = extdb_load(filepath, program_version, db);
			break;
	}

	if (retval != 0)
		E("%d, error %d", scan_type, retval);
	return retval;
}

/*
 * unload db from memory
 * return 0 on success or < 0 on error
 */
int scan_freedb (IN void *db)
{
	int retval = -1;

	if (fixeddb_is_valid(db))
		retval = fixeddb_free(db);
	else if (extdb_is_valid(db))
		retval = extdb_free(db);

	if (retval != 0)
		E("error %d", retval);
	return retval;
}

/*
 * scan data with db
 * return number of founded record (or < 0 on error)
 *     on success scan fill types array with types of founded records and
 *     fill ids array with ids of records
 *
 * types and ids arrays must be allocated with size SCANDB_DETECTS_MAX
 *
 * also see fixeddb_scan and extdb_scan
 */
long scan_data (IN void *db, IN uint8_t *data, IN size_t size,
				OUT uint8_t *types, OUT uint32_t *ids)
{
	uint32_t tmp;
	uint8_t type;
	long count = -1;

	if (fixeddb_is_valid(db))
	{
		type = fixeddb_scan(db, data, size);
		if (type == RECORD_CLEAN)
			return 0;

		types[0] = type;
		ids[0] = 0;
		count = 1;
	}
	else if (extdb_is_valid(db))
	{
		count = extdb_scan(db, data, size, types, ids);
	}

	if (count == -1 || (count > 0 && types[0] == RECORD_UNKNOWN))
	{
		BE32PTR_TO_CPU_UA(data, tmp);
		E("error, data %x len %zu", tmp, size); // XXX, under MSVC use %Id
	}

	return count;
}
