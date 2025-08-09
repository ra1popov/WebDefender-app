
/*
 * last modified: 2013.12.26
 */

#ifndef ENDIAN_H
#define ENDIAN_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

#include "_ext_endian_l.h"
//#include "_ext_endian_b.h"

// ptr2num
#define BE32PTR_TO_CPU(PTR, NUM) NUM = __be32_to_cpup((__be32 *) PTR)
#define BE16PTR_TO_CPU(PTR, NUM) NUM = __be16_to_cpup((__be16 *) PTR)

// num2num
#define CPU_TO_BE32(CPU, NUM) NUM = __cpu_to_be32(CPU)
#define CPU_TO_BE16(CPU, NUM) NUM = __cpu_to_be16(CPU)

#define BE32_TO_CPU(NUM, CPU) CPU = __be32_to_cpu(NUM)
#define BE16_TO_CPU(NUM, CPU) CPU = __be16_to_cpu(NUM)

// num2num 'in place'
#define CPU_TO_BE32_S(NUM) CPU_TO_BE32(NUM, NUM)
#define CPU_TO_BE16_S(NUM) CPU_TO_BE16(NUM, NUM)

#define BE32_TO_CPU_S(NUM) BE32_TO_CPU(NUM, NUM)
#define BE16_TO_CPU_S(NUM) BE16_TO_CPU(NUM, NUM)

// ptr2num + increase ptr
#define BE32PTR_TO_CPU_INC(PTR, NUM) \
	do { NUM = __be32_to_cpup((__be32 *) PTR); PTR = (__typeof__(PTR)) (((__be32 *) PTR) + 1); } while(0)

#define BE16PTR_TO_CPU_INC(PTR, NUM) \
	do { NUM = __be16_to_cpup((__be16 *) PTR); PTR = (__typeof__(PTR)) (((__be16 *) PTR) + 1); } while(0)

// num2ptr + increase ptr
#define CPU_TO_BE32PTR_INC(NUM, PTR) \
	do { *((__be32 *) PTR) = __cpu_to_be32(NUM); PTR = (__typeof__(PTR)) (((__be32 *) PTR) + 1); } while(0)

#define CPU_TO_BE16PTR_INC(NUM, PTR) \
	do { *((__be16 *) PTR) = __cpu_to_be16(NUM); PTR = (__typeof__(PTR)) (((__be16 *) PTR) + 1); } while(0)


// for unaligned access
#ifdef __LITTLE_ENDIAN
	# define BE32PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint32_t) (((uint8_t*) PTR)[0] << 24) | (uint32_t) (((uint8_t*) PTR)[1] << 16) | \
				(uint32_t) (((uint8_t*) PTR)[2] << 8) | (uint32_t) (((uint8_t*) PTR)[3]))

	# define BE16PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint16_t) (((uint8_t*) PTR)[0] << 8) | (uint32_t) (((uint8_t*) PTR)[1]))

	# define LE32PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint32_t) (((uint8_t*) PTR)[3] << 24) | (uint32_t) (((uint8_t*) PTR)[2] << 16) | \
				(uint32_t) (((uint8_t*) PTR)[1] << 8) | (uint32_t) (((uint8_t*) PTR)[0]))
#else
	# define BE32PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint32_t) ((uint8_t*) (PTR)[3] << 24) | (uint32_t) (((uint8_t*) PTR)[2] << 16) | \
				(uint32_t) (((uint8_t*) PTR)[1] << 8) | (uint32_t) (((uint8_t*) PTR)[0]))

	# define BE16PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint16_t) (((uint8_t*) PTR)[1] << 8) | (uint32_t) (((uint8_t*) PTR)[0]))

	# define LE32PTR_TO_CPU_UA(PTR, NUM) \
		NUM = ((uint32_t) (((uint8_t*) PTR)[0] << 24) | (uint32_t) (((uint8_t*) PTR)[1] << 16) | \
				(uint32_t) (((uint8_t*) PTR)[2] << 8) | (uint32_t) (((uint8_t*) PTR)[3]))
#endif

// define numbers
#ifdef __LITTLE_ENDIAN
	# define NATIVE_NUM32(B0, B1, B2, B3) \
		((uint32_t) (((uint8_t) B0) << 24) | (uint32_t) (((uint8_t) B1) << 16) | \
			(uint32_t) (((uint8_t) B2) << 8) | (uint32_t) ((uint8_t) B3))

	# define NATIVE_NUM16(B0, B1) \
		((uint16_t) (((uint8_t) B0) << 8) | (uint16_t) ((uint8_t) B1))

	# define NATIVE_NUM8(B0) \
		((uint8_t) B0)
#else
	# define NATIVE_NUM32(B0, B1, B2, B3) \
		((uint32_t) (((uint8_t) B3) << 24) | (uint32_t) (((uint8_t) B2) << 16) | \
			(uint32_t) (((uint8_t) B1) << 8) | (uint32_t) ((uint8_t) B0))

	# define NATIVE_NUM16(B0, B1) \
		((uint16_t) (((uint8_t) B1) << 8) | (uint16_t) ((uint8_t) B0))

	# define NATIVE_NUM8(B0) \
		((uint8_t) B0)
#endif

#ifdef __cplusplus
}
#endif

#endif /* ENDIAN_H */
