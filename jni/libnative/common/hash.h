
/*
 * last modified: 2013.12.16
 */

#ifndef HASH_H
#define HASH_H

#include <stdint.h>
#include "common.h"

#ifdef __cplusplus
extern "C" {
#endif

// we do not use it and hide it so that there are no warnings 28.07.2023
//static INLINE uint16_t sum16_add_byte (uint16_t sum16, uint8_t b)
//{
//	sum16 += b;
//	if (sum16 >= 0x10000)
//		sum16 -= 0x10000;
//	return sum16;
//}

static INLINE uint16_t lenxor_hash (uint8_t *data, size_t size)
{
	size_t i;
	uint8_t bx = 0;
	for (i = 0; i < size; i++)
		bx ^= data[i];
	//return ((size > 0xFF) ? 0xFF00 + bx : (((uint8_t) size) << 8) + bx);
	return ((((uint8_t) size) << 8) + bx);
}

/* ----------------------------------------------------------- */

uint32_t net_checksum_add (IN uint32_t sum, IN uint8_t *buff, IN size_t offset,
							IN size_t size);
uint16_t net_checksum_end (IN uint32_t sum);

void murmur_hash3_32 (IN const uint8_t* data, IN uint32_t size, IN uint32_t seed,
						OUT uint32_t* hash);

#ifdef __cplusplus
}
#endif

#endif /* HASH_H */
