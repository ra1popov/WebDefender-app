
/*
 * jni functions to work with network, packets and etc.
 *
 * last modified: 2015.09.18
 */

#include <errno.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <poll.h>
#include <pthread.h>
#include <string.h>
#include <unistd.h>

#include <stdlib.h> // added 28.07.2023

#include "common/common.h"
#include "common/functions.h"
#include "common/hash.h"
#include "jfunctions.h"

#ifdef OS_LINUX
#include <linux/if.h> // for IFF_*
#include <sys/ioctl.h>
#endif

#ifndef IFF_UP
#define IFF_UP 0x1
#endif

/*
 * resolve ipv4 address to domain name
 *
 * on success return 0
 * on error return h_errno (or errno) code, e.g. 1 - HOST_NOT_FOUND, 2 - TRY_AGAIN
 */
jint PUBLIC NATIVE_FUNCTION(getnameinfo, nf21) (JNIEnv* env, jobject thiz,
												IN jstring ip, OUT jobjectArray names)
{
	int result = -1; // err
	const char *ip_str = NULL;
	jsize size;
	char host_str[NI_MAXHOST];
	char addr[INET_ADDRSTRLEN];
	struct hostent *host;

	if (ip == NULL || names == NULL)
		return -1;

	do
	{
		ip_str = (*env)->GetStringUTFChars(env, ip, NULL);
		if (ip_str == NULL)
		{
			E("GetStringUTFChars failed");
			break; // OutOfMemoryError already thrown
		}

		I("resolving %s", ip_str);

		if (inet_pton(AF_INET, ip_str, addr) != 1)
		{
			I("inet_pton failed");
			result = -2;
			break;
		}
		//inet_ntop(AF_INET, addr, host_str, sizeof(addr));
		//I("test %s", host_str);

		// resolve information about ip

		host = gethostbyaddr(addr, sizeof(struct in_addr), AF_INET);
		if (host == NULL)
		{
			I("gethostbyaddr error %d %d", h_errno, errno);
			result = (h_errno == -1 && errno != 0) ? errno : h_errno; // error code
			break;
		}

		STRNCPY(host_str, host->h_name);
		I("resolved %s", host_str);

		// save name

		size = (*env)->GetArrayLength(env, names);
		if (size < 1)
		{
			I("string array not allocated");
			result = -3;
			break;
		}

		//elem = (jstring) (*env)->GetObjectArrayElement(names, 0);
		(*env)->SetObjectArrayElement(env, names, 0, (*env)->NewStringUTF(env, host_str));

		result = 0; // success
	}
	while (0);

	if (ip_str != NULL)
		(*env)->ReleaseStringUTFChars(env, ip, ip_str);

	return result;
}

/*
 * calc checksum (RFC 791) for network packet
 * return -1 on error (XXX)
 */
jshort PUBLIC NATIVE_FUNCTION(calcCheckSum, nf22) (JNIEnv* env, jobject thiz,
													IN jint start_value, IN jbyteArray bytes,
													IN jint offset, IN jint size)
{
	jbyte *jbytes;
	uint32_t sum;
	uint16_t sum_end;

	if (bytes == NULL)
		return -1;

	jbytes = (*env)->GetByteArrayElements(env, bytes, NULL);
	if (jbytes == NULL)
	{
		E("GetByteArrayElements failed");
		return -1;
	}

	sum = net_checksum_add(start_value, (uint8_t *) jbytes, offset, size);
	sum_end = net_checksum_end(sum);
	(*env)->ReleaseByteArrayElements(env, bytes, jbytes, JNI_ABORT); // not copy back contents

	return sum_end;
}

/*
 * fill first 40 bytes of frame array with Ip + Tcp headers data
 *
 * frame array must be allocated (40 bytes at least)
 * return -1 on error or 40 + dataSize on success
 */
jint PUBLIC NATIVE_FUNCTION(addIpTcpHeaderNative, nf23) (JNIEnv* env, jobject thiz,
															OUT jbyteArray frame,
															IN jbyteArray sourceIp, IN jint sourcePort,
															IN jbyteArray destIp, IN jint destPort,
															IN jint seqNo, IN jint ackNo, IN jint flags,
															IN jint windowSize, IN jint dataSize)
{
	int result = -1;
	int frame_len = 0;
	uint32_t sum;
	uint16_t sum_end;
	jbyte *jsourceIp, *jdestIp, *jframe;

	if (frame == NULL || sourceIp == NULL || destIp == NULL)
		return -1;

	if ((*env)->GetArrayLength(env, frame) < 40)
	{
		I("byte array not allocated");
		return -1;
	}

	jframe = (*env)->GetByteArrayElements(env, frame, NULL);
	jsourceIp = (*env)->GetByteArrayElements(env, sourceIp, NULL);
	jdestIp = (*env)->GetByteArrayElements(env, destIp, NULL);
	if (jframe == NULL || jsourceIp == NULL || jdestIp == NULL)
	{
		E("GetByteArrayElements failed");
		goto addIpTcpHeaderNative_exit;
	}

	memset(jframe, 0, 40);

	// fill IP header
	// type of service - routine traffic + normal
	// identification 0

	frame_len = 40 + dataSize;

	jframe[0] = 69;                    // ip version 4, header length 5
	jframe[2] = frame_len >> 8;        // packet length
	jframe[3] = frame_len;
	jframe[6] = 64;                    // fragmentation flags - df
	jframe[8] = 64;                    // ttl
	jframe[9] = 6;                     // protocol TCP
	memcpy(&jframe[12], jsourceIp, 4); // source address
	memcpy(&jframe[16], jdestIp, 4);   // destination address

	sum = net_checksum_add(0, (uint8_t *) jframe, 0, 20);
	sum_end = net_checksum_end(sum);
	jframe[10] = sum_end >> 8;         // checksum
	jframe[11] = sum_end;

	// fill TCP header
	// no options

	frame_len = 20 + dataSize;

	jframe[20] = sourcePort >> 8;      // source port
	jframe[21] = sourcePort;
	jframe[22] = destPort >> 8;        // destination port
	jframe[23] = destPort;
	jframe[24] = seqNo >> 24;          // sequence number
	jframe[25] = seqNo >> 16;
	jframe[26] = seqNo >> 8;
	jframe[27] = seqNo;
	if (ackNo == -1)
	{
		jframe[28] = 0;                // acknowledgement number
		jframe[29] = 0;
		jframe[30] = 0;
		jframe[31] = 0;
	}
	else
	{
		jframe[28] = ackNo >> 24;
		jframe[29] = ackNo >> 16;
		jframe[30] = ackNo >> 8;
		jframe[31] = ackNo;
	}
	jframe[32] = 80;                   // data offset + reserved
	jframe[33] = flags;                // flags >> 2
	jframe[34] = windowSize >> 8;      // receive window size
	jframe[35] = windowSize;

	sum = net_checksum_add(6 + frame_len, (uint8_t *) jframe, 12, 8); // prot_num + tcp_len + source + dest
	sum = net_checksum_add(sum, (uint8_t *) jframe, 20, frame_len);
	sum_end = net_checksum_end(sum);
	jframe[36] = sum_end >> 8;         // checksum
	jframe[37] = sum_end;

	result = (40 + dataSize);

addIpTcpHeaderNative_exit:

	if (jframe != NULL)
		(*env)->ReleaseByteArrayElements(env, frame, jframe, 0); // update frame array
	if (jsourceIp != NULL)
		(*env)->ReleaseByteArrayElements(env, sourceIp, jsourceIp, JNI_ABORT);
	if (jdestIp != NULL)
		(*env)->ReleaseByteArrayElements(env, destIp, jdestIp, JNI_ABORT);

	return result; // tcp frame size
}

/* ----------------------------------------------------------- */

// check that netlink work correctly
jboolean PUBLIC NATIVE_FUNCTION(netlinkIsWork, nf24) (JNIEnv* env, jobject thiz)
{
	int retval, s, fd = 0;
	uint32_t sq;
	void *h;
	uint8_t buf[512];
	struct inet_diag_msg *r;

	retval = netlink_diag_get_tcpsock(buf, sizeof(buf), &fd, &sq, &h, &s, &r);
	if (retval < 0)
		return JNI_FALSE;
	else if (retval > 0)
		close(fd);

	return JNI_TRUE;
}

/*
 * search process uid for connection by source port and
 *     destination address + port
 *
 * return uid or -1 if not found (-2 on error)
 *
 * rIp == byte[4] array with IPv4 address in BE format
 */
jint PUBLIC NATIVE_FUNCTION(netlinkFindUid, nf25) (JNIEnv* env, jobject thiz,
													IN jint lPort,
													IN jbyteArray rIp, IN jint rPort)
{
	int uid, retval, s, fd = 0;
	uint32_t sq, addr;
	void *h;
	uint8_t buf[8192];
	struct inet_diag_msg *r;
	jbyte *jrip;

	if (rIp == NULL)
		return -2;

	if ((*env)->GetArrayLength(env, rIp) != 4)
	{
		E("GetArrayLength failed");
		return -2;
	}

	jrip = (*env)->GetByteArrayElements(env, rIp, NULL);
	if (jrip == NULL)
	{
		E("GetByteArrayElements failed");
		return -2;
	}

	// search
	uid = -1;
	addr = *((uint32_t*) jrip);
//	E("| %d -> %x:%d", lPort, addr, rPort);

	while (uid == -1 &&
			(retval = netlink_diag_get_tcpsock(buf, sizeof(buf), &fd, &sq, &h, &s, &r)) > 0)
	{
//		E("%x:%d -> %x:%d", *((uint32_t*) &(r->id.idiag_src[0])),
//			ntohs(r->id.idiag_sport), *((uint32_t*) &(r->id.idiag_dst[0])),
//			ntohs(r->id.idiag_dport));

		// XXX add ipv6 support, r->idiag_family == AF_INET6
		if (ntohs(r->id.idiag_sport) == lPort && ntohs(r->id.idiag_dport) == rPort)
		{
			if (*((uint32_t*) &(r->id.idiag_dst[0])) == addr ||
				*((uint32_t*) &(r->id.idiag_dst[3])) == addr)
			{
				uid = r->idiag_uid;
			}
		}
	}

	if (retval < 0)
		{ /*E("netlink error %d", retval);*/ uid = -2; }
	else if (retval > 0)
		close(fd);

	(*env)->ReleaseByteArrayElements(env, rIp, jrip, JNI_ABORT); // not copy back contents

	return uid;
}

/*
 * start thread to watch network changes and call java callback (type (I)V) with message id
 *
 * ids: 16 NEWLINK, 17 DELLINK, 20 NEWADDR, 21 DELADDR, 24 NEWROUTE, 25 DELROUTE
 * return -1 on error
 *
 * XXX crash by now
 */
static void* netlinkWatchNet_thread (void* arg);

struct thread_params_t_
{
	JavaVM *jvm;
	jclass classLoader;
	jmethodID findMethod;
	jstring className;
	jstring funcName;
};
typedef struct thread_params_t_ thread_params_t;

jint PUBLIC NATIVE_FUNCTION(netlinkWatchNet, nf26) (JNIEnv* env, jobject thiz,
													IN jstring className, IN jstring funcName)
{
	//char *str;
	//jint size;
	int retval;
	pthread_t thread;
	thread_params_t* params;

	params = malloc(sizeof(thread_params_t));
	if (params == NULL)
		return -1;

	if (get_class_loader(env, "app/App", &(params->classLoader),
							&(params->findMethod)) == 0)
	{
		return -1;
	}

	//
	//str = (char *) (*env)->GetStringUTFChars(env, className, NULL);
	//size = (*env)->GetStringUTFLength(env, className);
	//(*env)->ReleaseStringUTFChars(env, className, str);
	params->className = (*env)->NewGlobalRef(env, className);
	params->funcName = (*env)->NewGlobalRef(env, funcName);

	//
	retval = (*env)->GetJavaVM(env, &(params->jvm));
	if (retval != 0)
	{
		E("GetJavaVM failed");
		goto netlinkWatchNet_error;
	}

	//
	retval = pthread_create(&thread, NULL, netlinkWatchNet_thread, (void *) params);
	if (retval)
	{
		I("pthread_create error %d", retval);
		goto netlinkWatchNet_error;
	}

	return 0;

netlinkWatchNet_error:

	free(params);

	return -1;
}

static void* netlinkWatchNet_thread (void* arg)
{
	int retval, s, fd = 0;
	void *h;
	uint8_t buf[8192];
	struct pollfd pollfd;
	struct nlmsghdr *r;
	thread_params_t* params = (thread_params_t*) arg;

	char* funcname = NULL;
	JNIEnv *env = NULL;
	jclass clazz = NULL;
	jmethodID method_id = NULL;

	I("started");

	//
	if ((*(params->jvm))->AttachCurrentThread(params->jvm, &env, NULL) != JNI_OK)
	{
		E("AttachCurrentThread failed");
		env = NULL;
		goto netlinkWatchNet_thread_exit;
	}

	clazz = (*env)->CallObjectMethod(env, params->classLoader, params->findMethod,
										params->className);
	if (clazz == NULL)
	{
		E("FindClass failed");
		goto netlinkWatchNet_thread_exit;
	}

	funcname = (char *) (*env)->GetStringUTFChars(env, params->funcName, NULL);
	if (funcname == NULL)
	{
		E("GetStringUTFChars failed");
		goto netlinkWatchNet_thread_exit;
	}

	method_id = (*env)->GetStaticMethodID(env, clazz, funcname, "(I)V");
	if (method_id == NULL)
	{
		E("GetStaticMethodID failed");
		goto netlinkWatchNet_thread_exit;
	}

	(*env)->ReleaseStringUTFChars(env, params->funcName, funcname);
	funcname = NULL;

	(*env)->DeleteGlobalRef(env, params->classLoader);
	params->classLoader = NULL;
	(*env)->DeleteGlobalRef(env, params->className);
	params->className = NULL;
	(*env)->DeleteGlobalRef(env, params->funcName);
	params->funcName = NULL;

	//
	while (1)
	{
		while ((retval = netlink_route_get_msg(buf, sizeof(buf), &fd, &h, &s, &r)) > 0)
		{
			//goto netlinkWatchNet_thread_exit;
			//E("msg %d", r->nlmsg_type);

			(*env)->CallStaticVoidMethod(env, clazz, method_id, r->nlmsg_type);
			if ((*env)->ExceptionCheck(env))
			{
				E("CallStaticVoidMethod failed");
				goto netlinkWatchNet_thread_exit; // Oups
			}
		}

		if (retval < 0)
		{
			I("netlink error %d %d", retval, fd);
			if (fd == 0) // bad, error on netlink socket create, ending thread
				break;

			// socket already closed trying to recreate
			fd = 0;
			continue;
		}

		// read all messages, waiting
		pollfd.fd = fd;
		pollfd.events = POLLIN;
		pollfd.revents = 0;
		while ((retval = poll(&pollfd, 1, -1)) == 0) {}
		if (retval < 0) // poll error, wtf?
			I("poll error %d", retval);
	}

netlinkWatchNet_thread_exit:

	//sleep(3);
	I("exiting");

	if (fd != 0)
		close(fd);

	if (env != NULL)
	{
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionClear(env);

		//
		if (funcname != NULL)
			(*env)->ReleaseStringUTFChars(env, params->funcName, funcname);
		if (clazz != NULL)
			(*env)->DeleteLocalRef(env, clazz);

		//
		if (params->className != NULL)
			(*env)->DeleteGlobalRef(env, params->className);
		if (params->funcName != NULL)
			(*env)->DeleteGlobalRef(env, params->funcName);
		if (params->classLoader != NULL)
			(*env)->DeleteGlobalRef(env, params->classLoader);

		(*(params->jvm))->DetachCurrentThread(params->jvm);
	}
	free(params);

	pthread_exit(NULL);
	return NULL;
}

/*
 * convert buffer with ip to string
 * return "null" on error
 *
 * rIp == byte[4] array with IPv4 address in BE format
 */
jstring PUBLIC NATIVE_FUNCTION(ipToString, nf27) (JNIEnv* env, jobject thiz,
													IN jbyteArray ip, IN jint port)
{
	char buf[64] = {'n', 'u', 'l', 'l', '\0'};
	jbyte *jip = NULL;

	if (ip == NULL || (*env)->GetArrayLength(env, ip) != 4)
	{
		if (ip != NULL)
			E("GetArrayLength failed");
		goto ipToString_exit;
	}

	jip = (*env)->GetByteArrayElements(env, ip, NULL);
	if (jip == NULL)
	{
		E("GetByteArrayElements failed");
		goto ipToString_exit;
	}

	if (port != 0)
	{
		sprintf(buf, "%d.%d.%d.%d:%d", (uint8_t) jip[0], (uint8_t) jip[1],
				(uint8_t) jip[2], (uint8_t) jip[3], (uint) port);
	}
	else
	{
		sprintf(buf, "%d.%d.%d.%d", (uint8_t) jip[0], (uint8_t) jip[1],
				(uint8_t) jip[2], (uint8_t) jip[3]);
	}

ipToString_exit:

	if (jip != NULL)
		(*env)->ReleaseByteArrayElements(env, ip, jip, JNI_ABORT); // not copy back contents

	return (*env)->NewStringUTF(env, buf);
}

//
void PUBLIC NATIVE_FUNCTION(socketPrintInfo, nf28) (JNIEnv* env, jobject thiz,
													IN jint fd)
{
	/*
	info->tcpi_last_ack_recv = jiffies_to_msecs(now - tp->rcv_tstamp);
	jiffies_to_msecs Convert jiffies to milliseconds
	rcv_tstamp; //timestamp of last received ACK (for keepalives)
	*/

	struct tcp_info ti;
	socklen_t ti_sz = sizeof(ti);

	if (getsockopt(fd, IPPROTO_TCP, TCP_INFO, (void *) &ti, &ti_sz) != 0)
	{
		I("getsockopt error %d", errno);
		return;
	}

	E("TCP_INFO1 %i %i %i %i %i %i %i %i", ti.tcpi_state, ti.tcpi_ca_state, ti.tcpi_retransmits,
		ti.tcpi_probes, ti.tcpi_backoff, ti.tcpi_options, ti.tcpi_snd_wscale, ti.tcpi_rcv_wscale);
	E("TCP_INFO2 %u %u %u %u", ti.tcpi_rto, ti.tcpi_ato, ti.tcpi_snd_mss, ti.tcpi_rcv_mss);
	E("TCP_INFO3 %u %u %u %u %u", ti.tcpi_unacked, ti.tcpi_sacked, ti.tcpi_lost, ti.tcpi_retrans,
		ti.tcpi_fackets);
	E("TCP_INFO4 %u %u %u %u", ti.tcpi_last_data_sent, ti.tcpi_last_ack_sent,
		ti.tcpi_last_data_recv, ti.tcpi_last_ack_recv);
	E("TCP_INFO5 %u %u %u %u %u %u %u %u", ti.tcpi_pmtu, ti.tcpi_rcv_ssthresh, ti.tcpi_rtt,
		ti.tcpi_rttvar, ti.tcpi_snd_ssthresh, ti.tcpi_snd_cwnd, ti.tcpi_advmss, ti.tcpi_reordering);

	//return ti.tcpi_last_ack_recv;
}

/*
 * set socket keepalive params
 * idle - connecttion idle time (sec) before starts KA
 * count - number of KA probes before dropping the connection
 * interval - time (sec) between individual KA probes
 */
jboolean PUBLIC NATIVE_FUNCTION(socketSetKAS, nf29) (JNIEnv* env, jobject thiz,
														IN jint fd, IN jint idle,
														IN jint count, IN jint interval)
{
	int r1, r2, r3;

	r1 = setsockopt(fd, IPPROTO_TCP, TCP_KEEPIDLE, &idle, sizeof(idle));
	if (r1 != 0)
		I("setsockopt1 error %d", errno);

	r2 = setsockopt(fd, IPPROTO_TCP, TCP_KEEPCNT, &count, sizeof(count));
	if (r2 != 0)
		I("setsockopt2 error %d", errno);

	r3 = setsockopt(fd, IPPROTO_TCP, TCP_KEEPINTVL, &interval, sizeof(interval));
	if (r3 != 0)
		I("setsockopt3 error %d", errno);

	if (r1 != 0 || r2 != 0 || r3 != 0)
	{
		//E("failed");
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

// enable/disable socket keepalive
jboolean PUBLIC NATIVE_FUNCTION(socketEnableKA, nf30) (JNIEnv* env, jobject thiz,
														IN jint fd, IN jboolean enable)
{
	int opt = (enable == JNI_TRUE) ? 1 : 0;

	if (setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, (void *) &opt, sizeof(opt)) != 0)
	{
		I("setsockopt error %d", errno);
		return JNI_FALSE;
	}

	return JNI_TRUE;
}

// check if socket have unsended data
jboolean PUBLIC NATIVE_FUNCTION(socketHaveDataToSend, nf31) (JNIEnv* env, jobject thiz,
																IN jint fd)
{
	int value;
	if (ioctl(fd, SIOCOUTQ, &value) == -1) // SIOCINQ for read buffer
	{
		I("ioctl error %d", errno);
		return JNI_FALSE; // XXX return false?
	}

	return ((value == 0) ? JNI_FALSE : JNI_TRUE);
}

// try to send data through socket with flush (return false if no data)
jboolean PUBLIC NATIVE_FUNCTION(socketSendDataForce, nf32) (JNIEnv* env, jobject thiz,
																IN jint fd, IN jbyteArray data)
{
	jbyte *jdata = NULL;
	jsize len;
	ssize_t s = 0;
	int new = 1, old = -1, err = 1;
	socklen_t sz = sizeof(old);

	if (data == NULL)
		return JNI_FALSE;

	if ((len = (*env)->GetArrayLength(env, data)) < 0) // XXX can return < 0 if too many elements???
	{
		E("GetArrayLength failed");
		return JNI_FALSE;
	}
	if (len == 0)
		return JNI_FALSE;

	jdata = (*env)->GetByteArrayElements(env, data, NULL);
	if (jdata == NULL)
	{
		E("GetByteArrayElements failed");
		return JNI_FALSE;
	}

	//
	if (getsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (void *) &old, &sz) == -1)
		goto socketSendDataForce_exit;

	if (old == 0)
	{
		// enable TCP_NODELAY
		if (setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (void *) &new, sizeof(int)) == -1)
			goto socketSendDataForce_exit;
	}

	// send data
	if ((s = send(fd, (void *) jdata, len, 0)) == -1)
		I("send error %d", errno);

socketSendDataForce_exit:

	if (old == 0)
	{
		if (setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (void *) &old, sizeof(int)) != -1)
			err = 0; // reset error flag
	}

	if (err)
		I("setsockopt (getsockopt) failed");

	(*env)->ReleaseByteArrayElements(env, data, jdata, JNI_ABORT); // not copy back contents

	return ((s <= 0 || s != len) ? JNI_FALSE : JNI_TRUE);
}

// return names of all or active (up == true) network interfaces (or null)
jobjectArray PUBLIC NATIVE_FUNCTION(getIfNames, nf33) (JNIEnv* env, jobject thiz,
														IN jboolean up)
{
	int i, count;
	struct ifaddrs *list, *info;
	jobjectArray result;

	count = getifaddrs(&list);
	if (count <= 0)
	{
		if (count < 0)
			I("getifaddrs error %d", count);
		return NULL;
	}

	count = 0;
	for (i = 0, info = list; info != NULL; info = info->ifa_next)
	{
		if (up && !(info->ifa_flags & IFF_UP))
			continue;
		count++;
	}

	result = (*env)->NewObjectArray(env, count, (*env)->FindClass(env, "java/lang/String"),
									(*env)->NewStringUTF(env, ""));
	if (result == NULL)
	{
		E("NewObjectArray failed");
		return NULL;
	}

	for (i = 0, info = list; info != NULL; info = info->ifa_next)
	{
		if (up && !(info->ifa_flags & IFF_UP))
			continue;
		(*env)->SetObjectArrayElement(env, result, i, (*env)->NewStringUTF(env, info->ifa_name));
		i++;
	}
	freeifaddrs(list);

	return result;
}
