
/*
 * contains common functions
 *
 * last modified: 2015.09.18
 */

#include <stdbool.h>
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>
#include <libgen.h>

#include "common.h"
#ifdef OS_LINUX
#include <netinet/in.h> // or endian.h conflict
#endif
#include "functions.h"
#include "internal.h"
#include "structs.h"

#ifdef OS_LINUX
//#include <asm/types.h>
//#include <arpa/inet.h>
#include <sys/socket.h>
#include <linux/netlink.h>
//#include <netinet/in.h>
#include <net/if.h>
//#include <linux/inet_diag.h>
#include <linux/rtnetlink.h>

#include <signal.h>
#include <pthread.h>
#endif

/*
 * check if string contains valid integer number
 */
bool is_int (IN const char *str)
{
	char *s = (char *) str;

	// skip number sign
	if (*s == '-' || *s == '+')
		s++;

	// empty string ?
	if (*s == '\0')
		return false;

	// check for non-digit chars
	while (*s && isdigit(*s))
		s++;
	if (*s)
		return false;

	return true;
}

/*
 * search first process by uid and save it name
 *
 * name - buffer [size][ANDROID_PKGNAME_MAX] for save process name
 * size - buffer size
 *     if name == NULL check only if have any process with uid (return 1 or 0)
 *
 * return number of process founded (max size)
 * on error return < 0
 */
int uid2name (IN unsigned int uid, INOUT char name[][ANDROID_PKGNAME_MAX], IN int size)
{
	char buf[PATH_MAX], *bname;
	DIR *proc_dir = NULL;
	struct dirent *dir_entry;
	struct stat dir_stat;
	FILE *file;
	unsigned int readed;
	int proc_num = 0;

	proc_dir = opendir("/proc");
	if (proc_dir == NULL)
	{
		I("opendir error %d", errno);
		return -1;
	}

	// search process
	while ((dir_entry = readdir(proc_dir)) != NULL)
	{
		if (dir_entry->d_type != DT_DIR || !is_int(dir_entry->d_name))
			continue;

		// check process uid

		SNPRINTF(buf, "/proc/%s", dir_entry->d_name);
		if (stat(buf, &dir_stat) == -1)
		{
			I("stat '%s' error %d", buf, errno);
			continue;
		}
		if (uid != dir_stat.st_uid)
			continue;

		// found process with searched uid
		if (name == NULL)
			break;
		else if (proc_num >= size)
			break;

		// read process name

		SNPRINTF(buf, "/proc/%s/cmdline", dir_entry->d_name);
		file = fopen(buf, "r");
		if (file == NULL)
		{
			// comm data max len == TASK_COMM_LEN (currently 16 chars) !
			SNPRINTF(buf, "/proc/%s/comm", dir_entry->d_name);
			file = fopen(buf, "r");
			if (file == NULL)
			{
				I("can't open cmdline");
				break; // exit while
			}
		}

		readed = fread(buf, 1, sizeof(buf) - 1, file);
		fclose(file);

		if (readed == 0)
			continue;
		buf[readed] = '\0';

		// get basename if readed full path
		if (buf[0] == '/' && (bname = basename(buf)) != NULL) {}
		else bname = buf;

		//I("proc '%s' buf '%s' bname '%s'", dir_entry->d_name, buf, bname);
		strncpy(name[proc_num], bname, ANDROID_PKGNAME_MAX);
		name[proc_num][ANDROID_PKGNAME_MAX - 1] = '\0';

		proc_num++;
	}

	closedir(proc_dir);
	return proc_num;
}

/* ----------------------------------------------------------- */

/*
 * read file data
 * return mem ptr to file data allocated with i_malloc or NULL
 *
 * work with internal functions
 */
uint8_t* read_file (const char* filename)
{
	struct stat s_buf;
	FILE *f;
	size_t tmp;
	uint8_t *buf;

	// open file
	f = i_fopen(filename, "rb");
	if (f == NULL)
		return NULL;

	if (i_fstat(i_fileno(f), &s_buf) == -1)
	{
		I("stat '%s' error %d", filename, errno);
		return NULL;
	}

	// read
	buf = i_malloc(s_buf.st_size);
	tmp = i_fread(buf, 1, s_buf.st_size, f);
	i_fclose(f);

	if (tmp != s_buf.st_size)
	{
		I("i_fread %zu (%zu) failed", tmp, (size_t) s_buf.st_size); // XXX, under MSVC use %Id
		i_free(buf);
		return NULL;
	}

	return buf;
}

/*
 * write data to file
 * return 0 on success or < 0 on error
 *
 * work with internal functions
 */
int write_file (const char* filename, uint8_t *buf, size_t size)
{
	FILE *f;
	size_t tmp;

	// open file
	f = i_fopen(filename, "w+b");
	if (f == NULL)
		return -1;

	// write
	tmp = i_fwrite(buf, 1, size, f);
	i_fclose(f);

	if (tmp != size)
	{
		I("i_fwrite %zu (%zu) failed", tmp, size); // XXX, under MSVC use %Id
		return -2;
	}

	return 0;
}

/* ----------------------------------------------------------- */

#ifdef OS_LINUX

/*
 * create NETLINK_INET_DIAG socket, send TCPDIAG_GETSOCK request and process answers
 * return > 0 on success, 0 if no more results and < 0 on error
 *
 * function return answer in r variable (information about tcp socket),
 *     one answer for each function call
 * call this function until it return 0 (or < 0) to get all answers or
 *     use close(*fd) to end netlink connection
 *
 * on first call set fd variable to 0, variables sq, h, s and r will be filled by function
 * buf recommended size >= 8192
 */
int netlink_diag_get_tcpsock (IN uint8_t *buf, IN size_t size, INOUT int *fd,
								INOUT uint32_t *sq, INOUT void **h, INOUT int *s,
								INOUT struct inet_diag_msg **r)
{
	struct sockaddr_nl nladdr;
	struct {
		struct nlmsghdr nlh;
		struct inet_diag_req r;
	} req;
	struct msghdr msg;
	struct iovec iov;

	// init and send request
	if (*fd == 0)
	{
		*sq = rand(); //0;
		*h = NULL;
		*s = 0;
		*r = NULL;

		if ((*fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_INET_DIAG)) == -1)
		{
			I("socket error %d", errno);
			return -1;
		}

		// send request
		req.nlh.nlmsg_len = sizeof(req);
		req.nlh.nlmsg_type = TCPDIAG_GETSOCK;
		// NLM_F_ROOT: return the complete table instead of a single entry
		req.nlh.nlmsg_flags = NLM_F_ROOT | NLM_F_MATCH | NLM_F_REQUEST;
		req.nlh.nlmsg_pid = 0;
		// The sequence_number is used to track our messages. Since netlink is not
		// reliable, we don't want to end up with a corrupt or incomplete old
		// message in case the system is/was out of memory.
		req.nlh.nlmsg_seq = ++(*sq);
		memset(&req.r, 0, sizeof(req.r));
		req.r.idiag_family = AF_INET;
		req.r.idiag_states = 0xfff;
		req.r.idiag_ext = 0;

		//
		iov.iov_base = &req;
		iov.iov_len = sizeof(req);

		memset(&nladdr, 0, sizeof(nladdr));
		nladdr.nl_family = AF_NETLINK;

		//
		msg = (struct msghdr) {
			.msg_name = (void*) &nladdr,
			.msg_namelen = sizeof(nladdr),
			.msg_iov = &iov,
			.msg_iovlen = 1,
		};

		if (sendmsg(*fd, &msg, 0) < 0)
		{
			I("sendmsg error %d", errno);
			close(*fd);
			return -2;
		}
	}

	// read answers
	while (1)
	{
		// get answer
		if (*h == NULL || !NLMSG_OK((struct nlmsghdr*) (*h), *s))
		{
			iov.iov_base = buf;
			iov.iov_len = size;

			memset(&nladdr, 0, sizeof(nladdr));
			nladdr.nl_family = AF_NETLINK;

			//
			msg = (struct msghdr) {
				(void*) &nladdr, sizeof(nladdr),
				&iov, 1,
				NULL, 0,
				0
			};

			//I("recvmsg");
			*s = recvmsg(*fd, &msg, 0);
			if (*s < 0)
			{
				if (errno == EINTR)
					continue;

				I("recvmsg error %d", errno);
				close(*fd);
				return -3;
			}
			else if (*s == 0)
			{
				// peer has performed an orderly shutdown, end
				close(*fd);
				return 0;
			}

			*h = buf;
			if (!NLMSG_OK((struct nlmsghdr*) (*h), *s))
			{
				// bad answer, wtf ?
				I("!NLMSG_OK");
				close(*fd);
				return -4;
			}
		}

		// parse answer
		if (((struct nlmsghdr*) (*h))->nlmsg_seq == *sq)
		{
			if (((struct nlmsghdr*) (*h))->nlmsg_type == NLMSG_DONE)
			{
				// no more data, end
				//I("NLMSG_DONE");
				close(*fd);
				return 0;
			}
			else if (((struct nlmsghdr*) (*h))->nlmsg_type == NLMSG_ERROR)
			{
				I("NLMSG_ERROR");
				close(*fd);
				return -5;
			}

			// answer data
			*r = NLMSG_DATA((struct nlmsghdr*) (*h));
			*h = (void *) NLMSG_NEXT((struct nlmsghdr*) (*h), *s);

			break;
		}

		// if sequence number incorrect parse next answer
		*h = (void *) NLMSG_NEXT((struct nlmsghdr*) (*h), *s);
	} // while

	return 1;
}

/*
 * create NETLINK_ROUTE socket and process messages
 * return > 0 on success, 0 if no more results and < 0 on error
 *
 * function return message header in r variable (information about netlink message),
 *     one message for each function call
 * call this function until it return 0 (or < 0) to get all messages or
 *     use close(*fd) to end netlink connection
 *
 * on first call set fd variable to 0, variables h, s and r will be filled by function
 * buf recommended size >= 8192
 *
 * netlink route messages (see rtnetlink.h):
 * 16 - RTM_NEWLINK, 17 - RTM_DELLINK, 20 - RTM_NEWADDR, 21 - RTM_DELADDR,
 * 24 - RTM_NEWROUTE, 25 - RTM_DELROUTE
 */
int netlink_route_get_msg (IN uint8_t *buf, IN size_t size, INOUT int *fd,
							INOUT void **h, INOUT int *s,
							INOUT struct nlmsghdr **r)
{
	struct sockaddr_nl nladdr;
	struct msghdr msg;
	struct iovec iov;

	// init
	if (*fd == 0)
	{
		*h = NULL;
		*s = 0;
		*r = NULL;

		if ((*fd = socket(AF_NETLINK, SOCK_RAW, NETLINK_ROUTE)) < 0)
		{
			I("socket error %d", errno);
			return -1;
		}

		// bind
		memset(&nladdr, 0, sizeof(nladdr));
		nladdr.nl_family = AF_NETLINK;
		nladdr.nl_groups = RTMGRP_LINK | RTMGRP_IPV4_IFADDR | RTMGRP_IPV4_ROUTE |
							RTMGRP_IPV6_IFADDR | RTMGRP_IPV6_ROUTE;

		if (bind(*fd, (struct sockaddr *) &nladdr, sizeof(nladdr)))
		{
			I("bind error %d", errno);
			return -2;
		}
	}

	// read messages
	while (1)
	{
		// get message
		if (*h == NULL || !NLMSG_OK((struct nlmsghdr*) (*h), *s))
		{
			iov.iov_base = buf;
			iov.iov_len = size;

			memset(&nladdr, 0, sizeof(nladdr));
			nladdr.nl_family = AF_NETLINK;

			//
			msg = (struct msghdr) {
				(void*) &nladdr, sizeof(nladdr),
				&iov, 1,
				NULL, 0,
				0
			};

			//I("recvmsg");
			*s = recvmsg(*fd, &msg, 0);
			if (*s < 0)
			{
				if (errno == EINTR) // XXX || errno == EAGAIN ?
					continue;

				I("recvmsg error %d", errno);
				close(*fd);
				return -3;
			}
			else if (*s == 0)
			{
				// peer has performed an orderly shutdown? XXX end?
				//close(*fd);
				return 0;
			}

			*h = buf;
			if (!NLMSG_OK((struct nlmsghdr*) (*h), *s))
			{
				// bad message, wtf ?
				I("!NLMSG_OK");
				close(*fd);
				return -4;
			}

			//if (msg.msg_namelen != sizeof(nladdr)) // XXX check addr len?
			//{
			//	// err
			//}
		}

		// XXX additional check?
		//int len = h->nlmsg_len;	// len of all block
		//int l = len - sizeof(*h); // len of current message
		//if ((l < 0) || (len > *s))
		//{
		//	// err
		//}

		// parse message
		if (((struct nlmsghdr*) (*h))->nlmsg_type == NLMSG_DONE)
		{
			// no more data, end
			//I("NLMSG_DONE");
			//close(*fd);
			return 0;
		}
		else if (((struct nlmsghdr*) (*h))->nlmsg_type == NLMSG_ERROR)
		{
			I("NLMSG_ERROR");
			close(*fd);
			return -5;
		}

		// message data
		//*r = NLMSG_DATA((struct nlmsghdr*) (*h));
		*r = (struct nlmsghdr*) (*h);
		*h = (void *) NLMSG_NEXT((struct nlmsghdr*) (*h), *s);

		break;
	} // while

	return 1;
}

#ifdef OS_ANDROID

/*
 * create a linked list of structures describing the network interface (bionic miss)
 * return > 0 on success (count of interfaces), 0 if no results and < 0 on error
 *
 * see raw/dalvik/libcore/luni/src/main/native/ifaddrs-android.h
 * XXX make single query func (see netlink_diag_get_tcpsock, netlink_route_get_msg)
 */
struct addr_req
{
	// XXX use iovec instead.
	struct nlmsghdr netlink_hdr;
	struct ifaddrmsg msg;
};
#define GETIFADDRS_BUF_SIZE 8192
STATIC_ASSERT(IFADDRS_NAMESZ >= IF_NAMESIZE);

int getifaddrs (OUT struct ifaddrs** result)
{
	int fd, fd_f, res, retval, count = 0;
	size_t len, tmp;
	uint8_t buf[GETIFADDRS_BUF_SIZE];
	struct nlmsghdr* hdr;
	struct ifaddrmsg* address;
	struct rtattr* rta;
	struct ifaddrs *ptr, *info = NULL;
	struct ifreq ifr;
	*result = NULL;

    // create a netlink socket
	if ((fd = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_ROUTE)) < 0)
	{
		I("socket error %d", errno);
		return -1;
	}

	// ask for the address information
	struct addr_req addr_request;
	memset(&addr_request, 0, sizeof(addr_request));
	addr_request.netlink_hdr.nlmsg_flags = NLM_F_REQUEST | NLM_F_MATCH;
	addr_request.netlink_hdr.nlmsg_type = RTM_GETADDR;
	addr_request.netlink_hdr.nlmsg_len = NLMSG_ALIGN(NLMSG_LENGTH(sizeof(addr_request)));
	addr_request.msg.ifa_family = AF_UNSPEC; // all families
	addr_request.msg.ifa_index = 0;          // all interfaces

	len = addr_request.netlink_hdr.nlmsg_len;
	if (len != send(fd, &addr_request, len, 0))
	{
		I("send error %d", errno);
		retval = -2;
		goto getifaddrs_error;
	}

	retval = -3;
	while ((len = recv(fd, &buf[0], sizeof(buf), 0)) > 0)
	{
		hdr = (struct nlmsghdr *) &buf[0];
		for (; NLMSG_OK(hdr, len); hdr = NLMSG_NEXT(hdr, len))
		{
			switch (hdr->nlmsg_type)
			{
			case NLMSG_DONE:
				// no more data, end
				//I("NLMSG_DONE");
				close(fd);
				return count;

			case NLMSG_ERROR:
				I("NLMSG_ERROR");
				retval = -4;
				goto getifaddrs_error;

			case RTM_NEWADDR:
				address = (struct ifaddrmsg *) NLMSG_DATA(hdr);
				rta = IFA_RTA(address);
				tmp = IFA_PAYLOAD(hdr);

				while (RTA_OK(rta, tmp))
				{
					if (rta->rta_type == IFA_LOCAL &&
						(address->ifa_family == AF_INET || address->ifa_family == AF_INET6))
					{
						ptr = (struct ifaddrs *) malloc(sizeof(struct ifaddrs));
						if (ptr == NULL)
						{
							I("malloc failed");
							retval = -5;
							goto getifaddrs_error;
						}

						if (*result == NULL)
							*result = (info = ptr);
						else
							info = (info->ifa_next = ptr);
						info->ifa_next = NULL;

						//
						info->ifa_name = if_indextoname(address->ifa_index,
														&info->ifa_name_int[0]);
						res = -1;

						if (info->ifa_name != NULL &&
							(fd_f = socket(AF_INET, SOCK_DGRAM, 0)) >= 0)
						{
							memset(&ifr, 0, sizeof(ifr));
							strcpy(ifr.ifr_name, info->ifa_name);
							res = ioctl(fd_f, SIOCGIFFLAGS, &ifr);
							if (res != -1)
								info->ifa_flags = ifr.ifr_flags;
							close(fd_f);
						}

						if (res == -1)
						{
							I("if_indextoname failed");
							retval = -6;
							goto getifaddrs_error;
						}

						//
						info->ifa_addr = (struct sockaddr *) &(info->ifa_addr_int);
						info->ifa_addr_int.ss_family = address->ifa_family;

						if (address->ifa_family == AF_INET)
							memcpy((void *) &(((struct sockaddr_in *) (info->ifa_addr))->sin_addr),
									RTA_DATA(rta), RTA_PAYLOAD(rta));
						else
							memcpy((void *) &(((struct sockaddr_in6 *) (info->ifa_addr))->sin6_addr),
									RTA_DATA(rta), RTA_PAYLOAD(rta));

						count++;
					}

					rta = RTA_NEXT(rta, tmp);
				} // while
				break;
			}
		} // for
	} // while

getifaddrs_error:

	// we only get here on error
	if (retval == -3)
		I("recv error %d", errno);

	close(fd);
	freeifaddrs(*result);
	*result = NULL;

	return retval;
}

void freeifaddrs (IN struct ifaddrs* addresses)
{
	struct ifaddrs* next;

	while (addresses != NULL)
	{
		next = addresses->ifa_next;
		free(addresses);
		addresses = next;
	}
}

#endif

#else

int netlink_diag_get_tcpsock (IN uint8_t *buf, IN size_t size, INOUT int *fd,
								INOUT uint32_t *sq, INOUT void **h, INOUT int *s,
								INOUT struct inet_diag_msg **r);
{
	return -1;
}

int netlink_route_get_msg (IN uint8_t *buf, IN size_t size, INOUT int *fd,
							INOUT void **h, INOUT int *s,
							INOUT struct nlmsghdr **r)
{
	return -1;
}

int getifaddrs (OUT struct ifaddrs** result)
{
	return -1;
}

void freeifaddrs (IN struct ifaddrs* addresses)
{
	return;
}

#endif

/* ----------------------------------------------------------- */

#ifdef OS_LINUX

// set signal handler, return -1 on error (if handler == NULL set default empty handler)
static void signals_handler(const int signum, siginfo_t *const info, void *const context);

int signal_handler_set (IN int signal, IN void* handler)
{
	struct sigaction sa, sa_old;

	memset(&sa, 0, sizeof(sa));
	sigemptyset(&sa.sa_mask);
	//sa.sa_sigaction = (handler == NULL) ? signals_handler : handler; // fixed 28.07.2023
	sa.sa_sigaction = (handler == NULL) ? (void (*)(const int, siginfo_t *const, void *const))signals_handler : (void (*)(const int, siginfo_t *const, void *const))handler;
	sa.sa_flags = SA_SIGINFO;

	if (sigaction(signal, &sa, &sa_old) != 0)
	{
		E("error %d", errno);
		return -1;
	}

	return 0;
}

static void signals_handler(const int signum, siginfo_t *const info, void *const context)
{
	E("sig %d", signum);
}

// send signal to thread, return 0 on success or < 0 on error
int signal_send (IN long tid, IN int signal)
{
	int err = 0;

	// pthread_tryjoin_np on it; if alive it returns EBUSY

	err = pthread_kill((pthread_t) tid, signal); // XXX can CRASH!
	if (err != 0)
		E("error %d", err);

	return err;
}

#else

int signal_handler_set (IN int signal, IN void* handler)
{
	return -1;
}

int signal_send (IN long tid, IN int signal)
{
	return -1;
}

#endif
