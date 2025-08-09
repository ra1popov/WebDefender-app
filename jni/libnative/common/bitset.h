
/*
 * last modified: 2013.12.03
 */

#ifndef BITSET_H
#define BITSET_H

#include <stdint.h>
#include "common.h"
#include "endian.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef uint8_t bitset_t;
#define BITSET_ELEM_SIZE  (sizeof(bitset_t) * 8)
#define BITSET_SIZE(N)    ((N - 1) / BITSET_ELEM_SIZE + 1)

static INLINE unsigned int _bindex  (int b) { return b / BITSET_ELEM_SIZE; }
static INLINE unsigned int _boffset (int b) { return b % BITSET_ELEM_SIZE; }

#ifdef __LITTLE_ENDIAN
static INLINE void     bs_set_bit   (bitset_t* bs, int b) { bs[_bindex(b)] |= ((1 << (BITSET_ELEM_SIZE - 1)) >> (_boffset(b))); }
static INLINE void     bs_clear_bit (bitset_t* bs, int b) { bs[_bindex(b)] &= ~((1 << (BITSET_ELEM_SIZE - 1)) >> (_boffset(b))); }
static INLINE bitset_t bs_get_bit   (bitset_t* bs, int b) { return (bs[_bindex(b)] & ((1 << (BITSET_ELEM_SIZE - 1)) >> (_boffset(b)))); }
#else
static INLINE void     bs_set_bit   (bitset_t* bs, int b) { bs[_bindex(b)] |= (1 << (_boffset(b))); }
static INLINE void     bs_clear_bit (bitset_t* bs, int b) { bs[_bindex(b)] &= ~(1 << (_boffset(b))); }
static INLINE bitset_t bs_get_bit   (bitset_t* bs, int b) { return (bs[_bindex(b)] & (1 << (_boffset(b)))); }
#endif

//
static INLINE bitset_t bs_get_elem (bitset_t* bs, int b) { return bs[_bindex(b)]; }

#ifdef __cplusplus
}
#endif

#endif /* BITSET_H */
