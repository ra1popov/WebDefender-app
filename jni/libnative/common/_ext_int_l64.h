
/*
 * include _ext_endian_l.h or _ext_endian_b.h
 */

/*
 * asm-generic/int-l64.h
 *
 * Integer declarations for architectures which use "long"
 * for 64-bit types.
 */

#ifndef _EXT_INT_H
#define _EXT_INT_H

////#include <asm/bitsperlong.h>
////
////#ifndef __ASSEMBLY__
/*
 * __xx is ok: it doesn't pollute the POSIX namespace. Use these in the
 * header files exported to user space
 */

typedef __signed__ char __s8;
typedef unsigned char __u8;

typedef __signed__ short __s16;
typedef unsigned short __u16;

typedef __signed__ int __s32;
typedef unsigned int __u32;

typedef __signed__ long __s64;
typedef unsigned long __u64;

////#endif /* __ASSEMBLY__ */

#endif /* _EXT_INT_H */
