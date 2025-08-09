
/*
 * different OS structures defines, for use if no required headers
 * (i.g. linux kernel headers in android ndk)
 *
 * last modified: 2013.12.23
 */

#ifndef STRUCTS_H
#define STRUCTS_H

#include <stdint.h>
#include "common.h"
#include "endian.h"

#ifdef __cplusplus
extern "C" {
#endif

/* netlink defines */

// Just some random number
#define TCPDIAG_GETSOCK 18
#define DCCPDIAG_GETSOCK 19

#define INET_DIAG_GETSOCK_MAX 24

// Socket identity
struct inet_diag_sockid
{
	__be16  idiag_sport;
	__be16  idiag_dport;
	__be32  idiag_src[4];
	__be32  idiag_dst[4];
	__u32   idiag_if;
	__u32   idiag_cookie[2];
#define INET_DIAG_NOCOOKIE (~0U)
};

// Request structure
struct inet_diag_req
{
	__u8    idiag_family;           // Family of addresses
	__u8    idiag_src_len;
	__u8    idiag_dst_len;
	__u8    idiag_ext;              // Query extended information

	struct inet_diag_sockid id;

	__u32   idiag_states;           // States to dump
	__u32   idiag_dbs;              // Tables to dump (NI)
};

// Base info structure. It contains socket identity (addrs/ports/cookie)
// and, alas, the information shown by netstat.
struct inet_diag_msg
{
	__u8    idiag_family;
	__u8    idiag_state;
	__u8    idiag_timer;
	__u8    idiag_retrans;

	struct inet_diag_sockid id;

	__u32   idiag_expires;
	__u32   idiag_rqueue;
	__u32   idiag_wqueue;
	__u32   idiag_uid;
	__u32   idiag_inode;
};

#ifdef __cplusplus
}
#endif

#endif /* STRUCTS_H */
