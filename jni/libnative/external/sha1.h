
#ifndef SHA1_H
#define SHA1_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

#define SHA1_HASH_BITS  160                  // size of a SHA-1 hash in bits
#define SHA1_HASH_BYTES (SHA1_HASH_BITS/8)   // size of a SHA-1 hash in bytes
#define SHA1_BLOCK_BITS 512                  // size of a SHA-1 input block in bits
#define SHA1_BLOCK_BYTES (SHA1_BLOCK_BITS/8) // size of a SHA-1 input block in bytes

// SHA-1 context
typedef struct {
	uint32_t h[5];
	uint64_t length;
} sha1_ctx_t;

// initializes a SHA-1 context
void sha1_init(sha1_ctx_t *state);

// processes one input block (SHA1_BLOCK_BYTES size) and updates the hash context accordingly
void sha1_nextBlock (sha1_ctx_t *state, const void* block);

// processes N input blocks (each SHA1_BLOCK_BYTES size) and updates the hash context accordingly
void sha1_nextBlocks (sha1_ctx_t *state, const void* blocks, uint16_t count);

// processes the given block and finalizes the context
void sha1_lastBlock (sha1_ctx_t *state, const void* block, uint16_t length);

// processes the given blocks and finalizes the context
void sha1_lastBlocks (sha1_ctx_t *state, const void* blocks, uint32_t length);

// convert a state variable into an actual hash value
void sha1_ctx2hash (void *dest, sha1_ctx_t *state);

// hashing a message which in located entirely in RAM
void sha1(void *dest, const void* msg, uint32_t length);

#ifdef __cplusplus
}
#endif

#endif /* SHA1_H */
