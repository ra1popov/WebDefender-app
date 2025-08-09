
/*
 * last modified: 2013.12.18
 */

#include <stdint.h>

#include "hash.h"
#include "common.h"
#include "functions.h"

/*
 * calc checksum (RFC 791) for network packet
 * call net_checksum_end on result to get real checksum
 *
 * XXX incorrect work on BE systems
 */
uint32_t net_checksum_add (IN uint32_t sum, IN uint8_t *buff, IN size_t offset,
							IN size_t size)
{
	size_t i = offset, endpos = offset + size;

	// handle complete 16-bit blocks
	for (; i + 1 < endpos; i += 2)
	{
		sum += ((buff[i] << 8) & 0xFF00) + buff[i + 1]; // handle unaligned buffer
		if (sum & 0x80000000) // if high order bit set
			sum = (sum & 0xFFFF) + (sum >> 16);
	}

	// handle incomplete 16 bit block
	if (i < endpos)
	{
		sum += ((buff[i] << 8) & 0xFF00);
		if (sum & 0x80000000)
			sum = (sum & 0xFFFF) + (sum >> 16);
	}

	return sum;
}

uint16_t net_checksum_end (IN uint32_t sum)
{
	// take only 16 bits out of the 32 bit sum and add up the carries
	while (sum >> 16)
		sum = (sum & 0xFFFF) + (sum >> 16);

	return (uint16_t) (~sum);
}

/* ----------------------------------------------------------- */

/*
 * Block read - if your platform needs to do endian-swapping or can only
 * handle aligned reads, do the conversion here
 */

FORCE_INLINE uint32_t getblock32 (const uint32_t *p, int i)
{
//	return p[i];
	uint32_t val;
	LE32PTR_TO_CPU_UA(((uint8_t *) p) + i * sizeof(uint32_t), val);
	return val;
}
/*
FORCE_INLINE uint64_t getblock64 (const uint64_t *p, int i)
{
	return p[i];
}
*/
/* Finalization mix - force all bits of a hash block to avalanche */
FORCE_INLINE uint32_t fmix32 (uint32_t h)
{
	h ^= h >> 16;
	h *= 0x85ebca6b;
	h ^= h >> 13;
	h *= 0xc2b2ae35;
	h ^= h >> 16;

	return h;
}

// XXX, fix uint32_t size to size_t
void murmur_hash3_32 (IN const uint8_t* data, IN uint32_t size, IN uint32_t seed,
						OUT uint32_t* hash)
{
	const uint32_t c1 = 0xcc9e2d51;
	const uint32_t c2 = 0x1b873593;

	const int nblocks = size / 4;
	const uint32_t* blocks = (const uint32_t *) (data + nblocks * 4);
	const uint8_t* tail = (const uint8_t*) (data + nblocks * 4);

	int i;
	uint32_t k1;
	uint32_t h1 = seed;

	/* body */

	for (i = -nblocks; i; i++)
	{
		k1 = getblock32(blocks, i);

		k1 *= c1;
		k1 = ROTL32(k1,15);
		k1 *= c2;

		h1 ^= k1;
		h1 = ROTL32(h1,13);
		h1 = h1 * 5 + 0xe6546b64;
	}

	/* tail */

	k1 = 0;
	switch (size & 3)
	{
		case 3: k1 ^= tail[2] << 16;
		case 2: k1 ^= tail[1] << 8;
		case 1: k1 ^= tail[0];
				k1 *= c1; k1 = ROTL32(k1,15); k1 *= c2; h1 ^= k1;
	};

	/* finalization */

	h1 ^= size;
	h1 = fmix32(h1);

	*hash = h1;
}
