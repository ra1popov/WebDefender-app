
/*
 * common definitions for binary data format detect and description
 *
 * last modified: 2013.12.20
 */

#ifndef BINDATA_H
#define BINDATA_H

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include <common/common.h>

#ifdef __cplusplus
extern "C" {
#endif

#define BIN_DATA_LEN_MIN  4   // == sizeof(uint32_t), minimal size, see bin_data_detect_type0
#define BIN_DATA_LEN_ALL  265 // == recommended size to detect any format, see bin_data_is_*

/*
 * common type for description binary data format
 * use values >= BIN_DATA_UNKNOWN and < BIN_DATA_LAST
 */
enum bin_data_type_
{
	BIN_DATA_UNKNOWN = 0,

	BIN_DATA_ZIP     = 1,
	BIN_DATA_TAR     = 2,
	BIN_DATA_GZIP    = 3,
	BIN_DATA_BZIP2   = 4,
	BIN_DATA_RAR4    = 5,
	BIN_DATA_RAR5    = 6,
	BIN_DATA_7ZIP    = 7,
	BIN_DATA_CAB     = 8,
	BIN_DATA_XZ      = 9,

	BIN_DATA_ELF32   = 20,
	BIN_DATA_ELF64   = 21,
	BIN_DATA_DEX035  = 22,

	BIN_DATA_LAST
};
typedef enum bin_data_type_ bin_data_type;

/* ----------------------------------------------------------- */

// check if type value valid bindata_type
static INLINE bool bin_data_type_is_valid (IN int type) {
	I_NOT((type >= BIN_DATA_UNKNOWN && type < BIN_DATA_LAST), "invalid bin_data type %d", type);
	return (type >= BIN_DATA_UNKNOWN && type < BIN_DATA_LAST);
}

// return true if binary data type is archive
static INLINE bool bin_data_type_is_archive (IN int type) {
	return (type >= BIN_DATA_ZIP && type <= BIN_DATA_XZ);
}

// return true if binary data type is OS executable
static INLINE bool bin_data_type_is_executable (IN int type) {
	return (type >= BIN_DATA_ELF32 && type <= BIN_DATA_DEX035);
}

/* ----------------------------------------------------------- */

int bin_data_detect_type (IN uint8_t *data, IN size_t size);
int bin_data_search_type (IN uint8_t *data, IN size_t size, OUT size_t *offset);

#ifdef __cplusplus
}
#endif

#endif /* BINDATA_H */
