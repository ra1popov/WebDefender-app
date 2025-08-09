
/*
 * common functions defines
 *
 * last modified: 2014.10.07
 */

#ifndef FUNCTIONS_H
#define FUNCTIONS_H

#include <stdint.h>
#include <sys/types.h>
#include <stddef.h>
#include <stdbool.h>

#include <stdio.h>
#include <string.h>
#include <sys/time.h>

#ifdef OS_LINUX
#include <sys/socket.h>
#include <linux/netlink.h>
#endif

#include "common.h"
#include "structs.h"

#ifdef __cplusplus
extern "C" {
#endif

#define ANDROID_PKGNAME_MAX 68 // XXX, max package name size, 48 ?

#ifdef OS_ANDROID

// for getifaddrs (source-compatible subset of the BSD struct)
#define IFADDRS_NAMESZ 32
struct ifaddrs
{
	struct ifaddrs* ifa_next;  // pointer to next struct in list, or NULL at end
	char* ifa_name;            // interface name
	unsigned int ifa_flags;    // interface flags (see SIOCGIFFLAGS: IFF_UP, ...)
	struct sockaddr* ifa_addr; // interface address

	char ifa_name_int[IFADDRS_NAMESZ]; // see IF_NAMESIZE == 16
	struct sockaddr_storage ifa_addr_int;
};

#endif

/* ----------------------------------------------------------- */

#ifndef MAX
#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define MIN(a,b) (((a) < (b)) ? (a) : (b))
#endif

// string functions for local buffers with working sizeof
#define SNPRINTF(buf,p...) \
	do { snprintf(buf, sizeof(buf), p); buf[sizeof(buf) - 1] = 0; } while (0)

#define STRNCPY(buf,str) \
	strlcpy(buf, str, sizeof(buf));

//
#define ROTL32(x,y) rotl32(x,y) // for _MSC_VER use _rotl(x,r)
#define ROTL64(x,y) rotl64(x,y)

// gcc recognises this code and generates a rotate instruction for CPUs with one
static INLINE uint32_t rotl32 (uint32_t x, int8_t r) { return (x << r) | (x >> (32 - r)); }
static INLINE uint64_t rotl64 (uint64_t x, int8_t r) { return (x << r) | (x >> (64 - r)); }

static INLINE uint64_t get_time ()
{
	struct timeval tv;
	gettimeofday(&tv, NULL);
	return (tv.tv_sec * 1000000L + tv.tv_usec); // microsecond == 1/1,000,000 of a second
}

/* ----------------------------------------------------------- */

bool is_int   (IN const char *str);
int  uid2name (IN unsigned int uid, INOUT char name[][ANDROID_PKGNAME_MAX], IN int size);

uint8_t* read_file  (IN const char* filename);
int      write_file (IN const char* filename, IN uint8_t *buf, IN size_t size);

int netlink_diag_get_tcpsock (IN uint8_t *buf, IN size_t size, INOUT int *fd,
								INOUT uint32_t *sq, INOUT void **h, INOUT int *s,
								INOUT struct inet_diag_msg **r);

int netlink_route_get_msg (IN uint8_t *buf, IN size_t size, INOUT int *fd,
							INOUT void **h, INOUT int *s,
							INOUT struct nlmsghdr **r);

#ifdef OS_ANDROID

int  getifaddrs  (struct ifaddrs** result);
void freeifaddrs (struct ifaddrs* addresses);

#endif

int signal_handler_set (IN int signal, IN void* handler);
int signal_send (IN long tid, IN int signal);

#ifdef __cplusplus
}
#endif

#endif /* FUNCTIONS_H */
