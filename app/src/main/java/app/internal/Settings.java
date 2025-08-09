package app.internal;

import app.BuildConfig;
import app.Config;

public class Settings {

	public static final boolean DEBUG				            = Config.DEBUG;

	// see Policy
	public static final boolean DEBUG_POLICY		            = false; // dump policy info
	// see RequestHeader, TCPStateMachine
	public static final boolean DEBUG_HTTP			            = false; // debug http parsing
	// see DNSRequest, DNSResponse
	public static final boolean DEBUG_DNS			            = false; // debug dns parsing
	// see TCPStateMachine, TCPClient, TunReadThread
	public static final boolean DEBUG_NET			            = false; // debug of our network (connections) processing
	public static final boolean DEBUG_WGPROXY		            = false; // debug of our proxy working
	public static final boolean DEBUG_TCP			            = false; // debug TCPClient states
	// see FilterVpnService
	public static final boolean DEBUG_STATE			            = false; // debug network (uplink) states

	//

	public static final boolean DEBUG_WRITE			            = false; // dump logs to file
	public static final boolean DEBUG_BT			            = false; // append to log messages backtraces
	public static final boolean DEBUG_PKT_DUMP		            = false; // dump all tun traffic

	//

	public static final boolean DEBUG_ALLOW_APP		            = false; // allow internet only for single app
	public static final String	DEBUG_ALLOW_APP_NAME            = "com.android.browser";

	public static final boolean DEBUG_NO_UPDATE		            = false; // disable data sending in UpdateService (enable with PROFILING)

	public static final boolean DEBUG_PROFILE_NET	            = false; // profile network objects

	public static final boolean DEBUG_LOCAL_WGPROXY             = false; // use proxy on 192.168.1.9

	public static final boolean DEBUG_NO_SCAN		            = false; // disable databases
	public static final boolean DEBUG_DB_REPLACE	            = false; // always replace same db version on app start (DEBUG !!! true)

	public static final boolean DEBUG_YOUTUBE		            = false; // show requests to youtube
	public static final boolean DEBUG_SCANNER_URLS	            = false; // show url variants in Scanner

	public static final boolean DEBUG_DROP_WG		            = false; // drop connects from WG through WG

	// license

	public static final boolean DEBUG_NOTOKEN		            = false; // getUserToken return null

	//

	// source tags
	public static final String TAG_HASHER                       = "UM_Hasher";
	public static final String TAG_LIBPATCH                     = "UM_LibPatch";
	public static final String TAG_NETINFO                      = "UM_NetInfo";
	public static final String TAG_NETUTIL                      = "UM_NetUtil";
	public static final String TAG_UTILS                        = "UM_Utils";
	public static final String TAG_BYTEBUFFERPOOL               = "UM_ByteBufferPool";
	public static final String TAG_FILTERVPNSERVICE             = "UM_FilterVpnService";
	public static final String TAG_CLIENTEVENT                  = "UM_ClientEvent";
	public static final String TAG_DNSREQUEST                   = "UM_DNSRequest";
	public static final String TAG_DNSUTILS                     = "UM_DNSUtils";
	public static final String TAG_CHUNKEDREADER                = "UM_ChunkedReader";
	public static final String TAG_REQUESTHEADER                = "UM_RequestHeader";
	public static final String TAG_CHANNELPOOL                  = "UM_ChannelPool";
	public static final String TAG_PACKET                       = "UM_Packet";
	public static final String TAG_PACKETDEQUEPOOL              = "UM_PacketDequePool";
	public static final String TAG_PACKETPOOL                   = "UM_PacketPool";
	public static final String TAG_TCPSTATEMACHINE              = "UM_TCPStateMachine";
	public static final String TAG_UDPCLIENT                    = "UM_UDPClient";
	public static final String TAG_SCANNER                      = "UM_Scanner";
	public static final String TAG_VPNLISTENER                  = "UM_VPNListener";
	public static final String TAG_PREFERENCES                  = "UM_Preferences";
	public static final String TAG_PROXYBASE                    = "UM_ProxyBase";
	public static final String TAG_NETLINE                      = "UM_NetLine";
	public static final String TAG_USB                          = "UM_Usb";
	public static final String TAG_PROXYEVENT                   = "UM_ProxyEvent";
	public static final String TAG_DNSBUFFER                    = "UM_DNSBuffer";
	public static final String TAG_DNSRESPONSE                  = "UM_DNSResponse";
	public static final String TAG_PROXYWORKER                  = "UM_ProxyWorker";
	public static final String TAG_TCPCLIENT                    = "UM_TCPClient";
	public static final String TAG_TUNREADTHREAD                = "UM_TunReadThread";
	public static final String TAG_TUNWRITETHREAD               = "UM_TunWriteThread";
	public static final String TAG_POLICY                       = "UM_Policy";
	public static final String TAG_UPDATESERVICE                = "UM_UpdateService";
	public static final String TAG_VPNSOCKET                    = "UM_VpnSocket";
	public static final String TAG_LOG                          = "UM_LOG";
	public static final String TAG_PKTDUMP                      = "UM_PKTDUMP";

	//

	public static final String APP_NAME                         = "WebDefender";
	public static final String APP_PACKAGE                      = BuildConfig.APPLICATION_ID;
	public static final String PUBLISHER						= BuildConfig.PUBLISHER;
	public static final String APP_STORE						= BuildConfig.APP_STORE;
	public static final String APP_SITE                         = "webdefender.app";
	public static final String APP_FILES_PREFIX                 = "webdefender";

	public static final String TUN_DEFAULT_IP                   = "10.10.10.1";
	public static final String TUN_IP		                    = "10.20.30.1";

	public static final String	DNS_FAKE_IP		                = "7.1.1.1";				// "DoD Network Information Center" ip =)
	public static final byte[]	DNS_FAKE_IP_AR	                = new byte[]{7, 1, 1, 1};	// note about signed bytes
	public static final boolean DNS_SANITIZE_IPV6               = true; // remove ipv6 addresses from dns answers
	public static final boolean DNS_USE_CACHE	                = true; // use dns cache
	public static final boolean DNS_NO_ADS_IP	                = true; // don't return IP for blocked domains (ADS, MALWARE) in DNS responses
	public static final boolean DNS_IP_BLOCK_CLEAN              = false; // clean or update list of blocked domains by time

	public static final boolean LOOPBACK_DROP_CONNECTS          = false;
	public static final byte[]	LOOPBACK_IP_AR		            = new byte[]{127, 0, 0, 1};

	public static final String	TEST_LOCAL_PROXY_IP             = "7.1.1.10"; // ip to test app->proxy->vpn bug
	public static final byte[]	TEST_LOCAL_PROXY_IP_AR          = new byte[]{7, 1, 1, 10};
	public static final String	TEST_TUN_WORK_IP	            = "7.1.1.11"; // ip to test tun work
	public static final byte[]	TEST_TUN_WORK_IP_AR             = new byte[]{7, 1, 1, 11};

	public static final String	WGPROXY_MAIN_DOMAIN             = "webdefender.app";
	public static final int		WGPROXY_PORT		            = 4666;
	public static final boolean WGPROXY_CRYPT_USE	            = true;

	public static final boolean LIC_DISABLE                     = true; // disable all check for subs

	public static final boolean TCPIP_CON_USE_NETLINK           = true; // search TCP connects info with netlink
	public static final boolean UPDATE_APPS_REHASH              = true; // rehash (and other info) all apps after WG update (DEBUG !!! false)
	public static final boolean APPS_EXCLUDE_BROWSERS           = true; // separate ad blocking for browsers and other apps (see PREF_APP_ADBLOCK)
	public static final boolean EVENTS_LOG                      = true; // disable LOG_* messages logging
	public static final boolean CLEAR_CACHES                    = true; // clear apps caches and kill apps

	public static final long	SETTINGS_RELOAD_INTERVAL        = 5 * 60 * 60 * 1000;	// 05:00 every day reload settings
	public static final long	CLEAR_CACHES_INTERVAL           = 15 * 60 * 1000; 		// clear apps caches only if WG disabled > 15 min (not used)

	//

	// gui settings
	public static final String PREF_ACTIVE			   			= "pref_active";
	public static final String PREF_BLOCK_MALICIOUS    			= "pref_block_malicious";
	public static final String PREF_BLOCK_APKS		   			= "pref_block_apks";
	public static final String PREF_USE_COMPRESSION    			= "pref_use_compression";
	public static final String PREF_PROXY_COUNTRY	   			= "pref_proxy_country";
	public static final String PREF_ANONYMIZE		   			= "pref_anonymize";
	public static final String PREF_ANONYMIZE_ONLY_BRW 			= "pref_anonymize_only_browsers";
	public static final String PREF_BLOCK_APPS_DATA    			= "pref_block_apps_data";
	public static final String PREF_BLOCK_SYSTEM_APPS_DATA		= "pref_block_system_apps_data";
	public static final String PREF_EXCLUDE_SYSTEM_APPS_DATA	= "pref_exclude_system_apps_data";
	public static final String PREF_SOCIAL_VK		   			= "pref_social_vk";
	public static final String PREF_SOCIAL_FB		   			= "pref_social_fb";
	public static final String PREF_SOCIAL_TWITTER	   			= "pref_social_twitter";
	public static final String PREF_SOCIAL_OK		   			= "pref_social_ok";
	public static final String PREF_SOCIAL_MAILRU	   			= "pref_social_mailru";
	public static final String PREF_SOCIAL_GPLUS	   			= "pref_social_gplus";
	public static final String PREF_SOCIAL_LINKEDIN    			= "pref_social_linkedin";
	public static final String PREF_SOCIAL_MOIKRUG	   			= "pref_social_moikrug";
	public static final String PREF_SOCIAL_OTHER	   			= "pref_social_other";

	public static final String PREF_CHANGE_USERAGENT   			= "pref_change_useragent";
	public static final String PREF_DESKTOP_USERAGENT  			= "pref_set_desktop_useragent";
	public static final String PREF_BLOCK_TP_CONTENT   			= "pref_block_thirdparty_content";

	// internal values
	public static final String PREF_DISABLE_TIME	   			= "pref_disable_time";
	public static final String PREF_USER_TOKEN		   			= "pref_user_token";
	public static final String PREF_USER_TOKEN_TIME    			= "pref_user_token_time";
	public static final String PREF_APP_ADBLOCK		   			= "invalid_option";
	public static final String PREF_APP_PROXY		   			= "pref_app_proxy";
	public static final String PREF_UPDATE_URL		   			= "update_url";
	public static final String PREF_UPDATE_RETRY_TIME  			= "update_retry_time";
	public static final String PREF_UPDATE_TIME		   			= "update_time";
	public static final String PREF_LAST_UPDATE_TIME   			= "last_update_time";
	public static final String PREF_UPDATE_ERROR_COUNT 			= "update_error_count";
	public static final String PREF_LAST_FASTUPDATE_TIME 		= "last_fastupdate_time";
	public static final String PREF_FASTUPDATE_ERROR_COUNT 		= "fastupdate_error_count";

	public static final String PREF_STATS			   			= "pref_stats";
	public static final String PREF_CLEARCACHES_TIME   			= "pref_cc_time";
	public static final String PREF_CLEARCACHES_NEED   			= "pref_cc_need";
	public static final String PREF_DNS_SERVERS		   			= "dns_servers";
	public static final String PREF_PROXY_DEL_TRY	   			= "proxy_delete_try";
	public static final String PREF_BASES_VERSION	   			= "bases_version";

	public static final String PREF_STATLOG_ENABLED 			= "pref_statlog_enabled";

	// manifest values
	public static final String MANIFEST_UPDATE_URL	  		 	= "update_url";

}
