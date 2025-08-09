
/*
 * last modified: 2013.12.20
 */

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>
#include <stdlib.h>

#include "bindata.h"
#include "common/common.h"
#include "common/endian.h"

static INLINE int bin_data_is_zip   (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_tar   (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_gzip  (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_bzip2 (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_rar   (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_7zip  (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_cab   (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_xz    (IN uint8_t *data, IN size_t size);

static INLINE int bin_data_is_elf   (IN uint8_t *data, IN size_t size);
static INLINE int bin_data_is_dex   (IN uint8_t *data, IN size_t size);

/*
 * check binary data for known formats
 *
 * return binary data format type (see bin_data_type)
 * on unknown type return 0 (BIN_DATA_UNKNOWN)
 */
static INLINE int bin_data_detect_type0 (IN uint8_t *data, IN size_t size)
{
	uint32_t first;
	int type = BIN_DATA_UNKNOWN;

	// check first bytes and call additional function for each signature
	if (size < BIN_DATA_LEN_MIN)
		return BIN_DATA_UNKNOWN;

	// first 4 bytes
	BE32PTR_TO_CPU_UA(data, first);
	switch (first)
	{
		case NATIVE_NUM32('P','K',0x03,0x04):
			type = bin_data_is_zip(data, size);
			break;
		case NATIVE_NUM32('R','a','r','!'):
			type = bin_data_is_rar(data, size);
			break;
		case NATIVE_NUM32('7','z',0xBC,0xAF):
			type = bin_data_is_7zip(data, size);
			break;
		case NATIVE_NUM32('M','S','C','F'):
			type = bin_data_is_cab(data, size);
			break;
		case NATIVE_NUM32(0xFD,'7','z','X'):
			type = bin_data_is_xz(data, size);
			break;
//		case NATIVE_NUM32(0,0,0,0):
//			type = bin_data_is_iso(data, size);
			break;

		case NATIVE_NUM32(0x7f,'E','L','F'):
			type = bin_data_is_elf(data, size);
			break;
		case NATIVE_NUM32('d','e','x',0x0A):
			type = bin_data_is_dex(data, size);
			break;
	}

	if (type != BIN_DATA_UNKNOWN)
		return type;

	// first 3 bytes
	first = first & NATIVE_NUM32(0xFF,0xFF,0xFF,0);
	switch (first)
	{
		case NATIVE_NUM32('B','Z','h',0):
			type = bin_data_is_bzip2(data, size);
			break;
	}

	if (type != BIN_DATA_UNKNOWN)
		return type;

	// first 2 bytes
	first = first & NATIVE_NUM32(0xFF,0xFF,0,0);
	switch (first)
	{
		case NATIVE_NUM32(0x1F,0x8B,0,0):
			type = bin_data_is_gzip(data, size);
			break;
	}

	if (type != BIN_DATA_UNKNOWN)
		return type;

	// other
	if (data[0] != 0)
		type = bin_data_is_tar(data, size);

	return type;
}

int bin_data_detect_type (IN uint8_t *data, IN size_t size)
{
	return bin_data_detect_type0(data, size);
}

/*
 * search in binary data first known format
 *
 * return binary data format type (see bin_data_type) and
 *     offset to format start (if offset != NULL)
 * on unknown type return 0 (BIN_DATA_UNKNOWN)
 */
int bin_data_search_type (IN uint8_t *data, IN size_t size, OUT size_t *offset)
{
	int type = BIN_DATA_UNKNOWN;
	size_t size_cur = size;
	uint8_t *data_cur = data;

	while (size_cur >= BIN_DATA_LEN_MIN &&
			(type = bin_data_detect_type0(data_cur, size_cur)) == BIN_DATA_UNKNOWN)
	{
		data_cur++;
		size_cur--;
	}

	if (type != BIN_DATA_UNKNOWN && offset != NULL)
		*offset = data_cur - data;

	return type;
}

/* ----------------------------------------------------------- */

/*
 * detect ZIP format: 'P' 'K' 03 04 + LOCAL_HEADER
 * see https://users.cs.jmu.edu/buchhofp/forensics/formats/pkzip.html
 *
 * return BIN_DATA_ZIP or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_zip (IN uint8_t *data, IN size_t size)
{
	uint32_t name_len;

	if (size < 30) // == name_ofs
		return BIN_DATA_UNKNOWN;

	// XXX check compression method ?

	// read first file name size
	BE32PTR_TO_CPU_UA(data + 26, name_len);
	if (name_len == 0)
		return BIN_DATA_UNKNOWN;

	return BIN_DATA_ZIP;
}

/*
 * detect TAR format
 * see http://www.gnu.org/software/tar/manual/html_node/Standard.html
 *
 * return BIN_DATA_TAR or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_tar (IN uint8_t *data, IN size_t size)
{
	uint i;
	uint32_t sig;

	if (size < 265) // see version offset in posix_header
		return BIN_DATA_UNKNOWN;

	// check magic 'ustar'
	BE32PTR_TO_CPU_UA(data + 257, sig);
	if (sig != NATIVE_NUM32('u','s','t','a')) return BIN_DATA_UNKNOWN;
	if (data[257 + 4] != 'r') return BIN_DATA_UNKNOWN;

	// check mode, uid, gid, size, mtime, chksum fields (octal numbers in ASCII C string)
	for (i = 100; i < 124; i++)
	{
		// XXX
		switch (i)
		{
			case 107: case 115: case 123: case 135: case 147: case 155:
				if (data[i] != 0) return BIN_DATA_UNKNOWN;
				break;
			default:
				if (data[i] == 0) return BIN_DATA_UNKNOWN;
				break;
		}
	}

	return BIN_DATA_TAR;
}

/*
 * detect GZIP format: 1F 8B + CM FLG MTIME XFLG OS
 * see http://www.onicos.com/staff/iz/formats/gzip.html
 *
 * return BIN_DATA_GZIP or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_gzip (IN uint8_t *data, IN size_t size)
{
	if (size < 10) // 2 + 1 + 1 + 4 + 1 + 1
		return BIN_DATA_UNKNOWN;

	// check reserved bits in flags
	if ((data[3] & 0xE0) != 0)
		return BIN_DATA_UNKNOWN;

	// check OS field
	if (data[9] > 0x0d && data[9] != 0xFF)
		return BIN_DATA_UNKNOWN;

	return BIN_DATA_GZIP;
}

/*
 * detect BZIP2 format: 42 5A 68 + LVL + 31 41 59 26 53 59
 * see http://en.wikipedia.org/wiki/Bzip2
 *
 * return BIN_DATA_BZIP2 or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_bzip2 (IN uint8_t *data, IN size_t size)
{
	uint16_t sig1;
	uint32_t sig2;

	if (size < 10) // 3 + 1 + 6
		return BIN_DATA_UNKNOWN;

	// check compression level
	if (data[3] < '0' || data[3] > '9')
		return BIN_DATA_UNKNOWN;

	// check 48-bit magic (pi number)
	BE16PTR_TO_CPU_UA(data + 4, sig1);
	if (sig1 != NATIVE_NUM16(0x31,0x41)) return BIN_DATA_UNKNOWN;
	BE32PTR_TO_CPU_UA(data + 4 + 2, sig2);
	if (sig2 != NATIVE_NUM32(0x59,0x26,0x53,0x59)) return BIN_DATA_UNKNOWN;

	return BIN_DATA_BZIP2;
}

/*
 * detect RAR format: 52 61 72 21 + 1A 07 00 or 52 61 72 21 + 1A 07 01 00
 * see http://www.rarlab.com/technote.htm
 *
 * return BIN_DATA_RAR4, BIN_DATA_RAR5 or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_rar (IN uint8_t *data, IN size_t size)
{
	uint16_t sig;

	if (size < 7) // 4.x sign - 7 bytes, 5.x sign - 8 bytes
		return BIN_DATA_UNKNOWN;

	// check signature first part end
	BE16PTR_TO_CPU_UA(data + 4, sig);
	if (sig != NATIVE_NUM16(0x1A,0x07))
		return BIN_DATA_UNKNOWN;

	// check signature second part
	if (data[6] == 0x00)
		return BIN_DATA_RAR4;
	else if (size > 7 && data[6] == 0x01 && data[7] == 0x00)
		return BIN_DATA_RAR5;

	return BIN_DATA_UNKNOWN;
}

/*
 * detect 7-ZIP format: 37 7A BC AF + 27 1C
 * see http://en.wikipedia.org/wiki/7z
 *
 * return BIN_DATA_7ZIP or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_7zip (IN uint8_t *data, IN size_t size)
{
	uint16_t sig;

	if (size < 6)
		return BIN_DATA_UNKNOWN;

	// check signature end
	BE16PTR_TO_CPU_UA(data + 4, sig);
	if (sig != NATIVE_NUM16(0x27,0x1C))
		return BIN_DATA_UNKNOWN;

	return BIN_DATA_7ZIP;
}

/*
 * detect CAB format: 'M' 'S' 'C' 'F' + .. + 03 01
 * see http://msdn.microsoft.com/en-us/library/bb417343.aspx#cfheader
 *
 * return BIN_DATA_CAB or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_cab (IN uint8_t *data, IN size_t size)
{
	uint16_t ver;

	if (size < 26)
		return BIN_DATA_UNKNOWN;

	// check version 1.3
	BE16PTR_TO_CPU_UA(data + 24, ver);
	if (ver != NATIVE_NUM16(3,1))
		return BIN_DATA_UNKNOWN;

	return BIN_DATA_CAB;
}

/*
 * detect XZ format: '7' 'z' 'X' 'Z' 00
 * see http://en.wikipedia.org/wiki/Xz
 *
 * return BIN_DATA_XZ or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_xz (IN uint8_t *data, IN size_t size)
{
	uint16_t sig;

	if (size < 6)
		return BIN_DATA_UNKNOWN;

	// check signature end
	BE16PTR_TO_CPU_UA(data + 4, sig);
	if (sig != NATIVE_NUM16('Z',0x00))
		return BIN_DATA_UNKNOWN;

	return BIN_DATA_XZ;
}

/*
 * detect ELF format: 7F 'E' 'L' 'F'
 * see http://en.wikipedia.org/wiki/Executable_and_Linkable_Format
 *
 * return BIN_DATA_ELF32, BIN_DATA_ELF64 or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_elf (IN uint8_t *data, IN size_t size)
{
	if (size < 6)
		return BIN_DATA_UNKNOWN;

	// check data encoding
	if (data[5] != 1 && data[5] != 2) // little-endian or big-endian
		return BIN_DATA_UNKNOWN;

	// check for architecture
	if (data[4] == 1)
		return BIN_DATA_ELF32;
	else if (data[4] == 2)
		return BIN_DATA_ELF64;

	return BIN_DATA_UNKNOWN;
}

/*
 * detect DEX (Dalvik EXecutable) format: 'd' 'e' 'x' '\n' + '0' '3' '5' '\0'
 * see http://source.android.com/devices/tech/dalvik/dex-format.html
 *
 * return BIN_DATA_DEX035 or BIN_DATA_UNKNOWN
 */
static INLINE int bin_data_is_dex (IN uint8_t *data, IN size_t size)
{
	uint32_t ver;

	if (size < 8)
		return BIN_DATA_UNKNOWN;

	// check version
	BE32PTR_TO_CPU_UA(data + 4, ver);
	if (ver == NATIVE_NUM32('0','3','5','\0'))
		return BIN_DATA_DEX035;

	return BIN_DATA_UNKNOWN;
}
