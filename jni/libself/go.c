#include "common.h"
#include <curl/curl.h>
#include <jni.h>

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/stat.h>
#include <wait.h>

enum MessageType {
    TYPE_TIMESTAMP, // For sending jlong timestamp
    TYPE_REQUEST,   // For sending Request request
    TYPE_POWER      // For sending jboolean power
};

struct Request {
    const char* baseUrl;
    const char* pushToken;
    const char* appVersion;
    const char* androidVersion;
    const char* vendorName;
    const char* modelName;
    const char* publisher;
    const char* installId;
    const char* language;
};

//////////////////////////////////////////////////////////////////////////////////////////////////

// Identifier for sending push messages and general information about the user's device.
struct Request request = {
    .baseUrl = "",
    .pushToken = "",
    .appVersion = "",
    .androidVersion = "",
    .vendorName = "",
    .modelName = "",
    .publisher = "",
    .installId = "",
    .language = ""
};

jlong timestamp;                    // Timestamp from the app; it should be sent every minute if the service in the app is running.
jboolean isPower = true;            // Application state (enabled or disabled)

pid_t my_pid;                       // PID of the current process
pid_t first_child_pid;              // PID of the first child process
pid_t second_child_pid;             // PID of the second child process

int pipefd[2] = {-1, -1};           // Pipes for writing and reading to facilitate communication between the parent and child processes.
bool isPipeCreated = false;         // Flag indicating that the pipe has been created.
bool isNeedRestart = false;         // Flag indicating that the child process needs to be restarted.

int mutex1_pipefd[2] = {-1, -1};    // Write and read pipes for the first semaphore.
int mutex2_pipefd[2] = {-1, -1};    // Write and read pipes for the second semaphore.

//////////////////////////////////////////////////////////////////////////////////////////////////

// Function to send a request to the server to receive a push message.
void sendRequestToServer() {

    CURL* curl;
    CURLcode res;

    // Initialization of the curl library with SSL support
    curl_global_init(CURL_GLOBAL_DEFAULT);

    // Creation of a CURL object
    curl = curl_easy_init();

    if (curl) {

        // Encoding URL parameters before using them in the URL
        char *pushTokenEncoded = curl_easy_escape(curl, request.pushToken, 0);
        char *appVersionEncoded = curl_easy_escape(curl, request.appVersion, 0);
        char *androidVersionEncoded = curl_easy_escape(curl, request.androidVersion, 0);
        char *vendorNameEncoded = curl_easy_escape(curl, request.vendorName, 0);
        char *modelNameEncoded = curl_easy_escape(curl, request.modelName, 0);
        char *publisherEncoded = curl_easy_escape(curl, request.publisher, 0);
        char *installIdEncoded = curl_easy_escape(curl, request.installId, 0);
        char *languageEncoded = curl_easy_escape(curl, request.language, 0);

        // Forming the URL considering baseUrl and request parameters
        char url[1024];
        snprintf(url, sizeof(url), "%s?push_token=%s&app_version=%s&android_version=%s&vendor_name=%s&model_name=%s&publisher=%s&install_id=%s&language=%s",
            request.baseUrl,
            pushTokenEncoded,
            appVersionEncoded,
            androidVersionEncoded,
            vendorNameEncoded,
            modelNameEncoded,
            publisherEncoded,
            installIdEncoded,
            languageEncoded
        );

        // Freeing memory allocated for encoded strings
        free(pushTokenEncoded);
        free(appVersionEncoded);
        free(androidVersionEncoded);
        free(vendorNameEncoded);
        free(modelNameEncoded);
        free(publisherEncoded);
        free(installIdEncoded);
        free(languageEncoded);

        I("TEST send reqest: %s", url);

        // Setting the URL for the GET request
        curl_easy_setopt(curl, CURLOPT_URL, url);

        // Executing the request
        res = curl_easy_perform(curl);

        if (res != CURLE_OK) {
            I("TEST CURL error: %s", curl_easy_strerror(res));
        } else {
            I("TEST CURL ok %s", url);
        }

        // Releasing resources
        curl_easy_cleanup(curl);

   }

    // Cleaning up and shutting down the curl library
    curl_global_cleanup();
}

jlong getCurrentTimestamp() {
    struct timespec currentTime;
    clock_gettime(CLOCK_BOOTTIME, &currentTime);

    jlong currentTimestamp = (jlong)currentTime.tv_sec * 1000 + (jlong)currentTime.tv_nsec / 1000000;
    return currentTimestamp;
}

void restartIfNeeded() {
    // Getting the current time in milliseconds
    jlong currentTimestamp = getCurrentTimestamp();

    I("TEST check %ld %ld", (long)timestamp, (long)currentTimestamp);

    // Calculating the difference between the current time and the given timestamp
    jlong timeDifference = currentTimestamp - timestamp;

    // Checking if more than 2 minutes (120,000 milliseconds) have passed
    if (timeDifference > 120000) {
        // Timestamp is outdated, sending a request to the server to receive a push notification
        I("TEST sendRequestToServer");
        sendRequestToServer();
    } else {
        I("TEST ok");
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////

size_t calculateRequestBufferSize(const struct Request* request) {
    size_t baseUrlLen = strlen(request->baseUrl) + 1;
    size_t pushTokenLen = strlen(request->pushToken) + 1;
    size_t appVersionLen = strlen(request->appVersion) + 1;
    size_t androidVersionLen = strlen(request->androidVersion) + 1;
    size_t vendorNameLen = strlen(request->vendorName) + 1;
    size_t modelNameLen = strlen(request->modelName) + 1;
    size_t publisherLen = strlen(request->publisher) + 1;
    size_t installIdLen = strlen(request->installId) + 1;
    size_t languageLen = strlen(request->language) + 1;

    return sizeof(enum MessageType) +
           sizeof(size_t) + baseUrlLen +
           sizeof(size_t) + pushTokenLen +
           sizeof(size_t) + appVersionLen +
           sizeof(size_t) + androidVersionLen +
           sizeof(size_t) + vendorNameLen +
           sizeof(size_t) + modelNameLen +
           sizeof(size_t) + publisherLen +
           sizeof(size_t) + installIdLen +
           sizeof(size_t) + languageLen +
           sizeof(bool);
}

void writeRequestToBuffer(char* buffer, const struct Request* request) {
    size_t baseUrlLen = strlen(request->baseUrl) + 1;
    size_t pushTokenLen = strlen(request->pushToken) + 1;
    size_t appVersionLen = strlen(request->appVersion) + 1;
    size_t androidVersionLen = strlen(request->androidVersion) + 1;
    size_t vendorNameLen = strlen(request->vendorName) + 1;
    size_t modelNameLen = strlen(request->modelName) + 1;
    size_t publisherLen = strlen(request->publisher) + 1;
    size_t installIdLen = strlen(request->installId) + 1;
    size_t languageLen = strlen(request->language) + 1;

    char* ptr = buffer;

    memcpy(ptr, &baseUrlLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->baseUrl, baseUrlLen);
    ptr += baseUrlLen;

    memcpy(ptr, &pushTokenLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->pushToken, pushTokenLen);
    ptr += pushTokenLen;

    memcpy(ptr, &appVersionLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->appVersion, appVersionLen);
    ptr += appVersionLen;

    memcpy(ptr, &androidVersionLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->androidVersion, androidVersionLen);
    ptr += androidVersionLen;

    memcpy(ptr, &vendorNameLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->vendorName, vendorNameLen);
    ptr += vendorNameLen;

    memcpy(ptr, &modelNameLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->modelName, modelNameLen);
    ptr += modelNameLen;

    memcpy(ptr, &publisherLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->publisher, publisherLen);
    ptr += publisherLen;

    memcpy(ptr, &installIdLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->installId, installIdLen);
    ptr += installIdLen;

    memcpy(ptr, &languageLen, sizeof(size_t));
    ptr += sizeof(size_t);
    memcpy(ptr, request->language, languageLen);
    ptr += languageLen;
}

// Function for decoding data from the buffer and creating a new Request structure
struct Request decodeRequestFromBuffer(const char* buffer, size_t bufferSize) {
    struct Request request;
    size_t offset = 0;

    // Read baseUrl
    size_t baseUrlLength;
    memcpy(&baseUrlLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.baseUrl = (char*)malloc(baseUrlLength + 1);
    memcpy((char*)request.baseUrl, buffer + offset, baseUrlLength);
    ((char*)request.baseUrl)[baseUrlLength] = '\0';
    offset += baseUrlLength;

    // Read pushToken
    size_t pushTokenLength;
    memcpy(&pushTokenLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.pushToken = (char*)malloc(pushTokenLength + 1);
    memcpy((char*)request.pushToken, buffer + offset, pushTokenLength);
    ((char*)request.pushToken)[pushTokenLength] = '\0';
    offset += pushTokenLength;

    // Read appVersion
    size_t appVersionLength;
    memcpy(&appVersionLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.appVersion = (char*)malloc(appVersionLength + 1);
    memcpy((char*)request.appVersion, buffer + offset, appVersionLength);
    ((char*)request.appVersion)[appVersionLength] = '\0';
    offset += appVersionLength;

    // Read androidVersion
    size_t androidVersionLength;
    memcpy(&androidVersionLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.androidVersion = (char*)malloc(androidVersionLength + 1);
    memcpy((char*)request.androidVersion, buffer + offset, androidVersionLength);
    ((char*)request.androidVersion)[androidVersionLength] = '\0';
    offset += androidVersionLength;

    // Read vendorName
    size_t vendorNameLength;
    memcpy(&vendorNameLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.vendorName = (char*)malloc(vendorNameLength + 1);
    memcpy((char*)request.vendorName, buffer + offset, vendorNameLength);
    ((char*)request.vendorName)[vendorNameLength] = '\0';
    offset += vendorNameLength;

    // Read modelName
    size_t modelNameLength;
    memcpy(&modelNameLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.modelName = (char*)malloc(modelNameLength + 1);
    memcpy((char*)request.modelName, buffer + offset, modelNameLength);
    ((char*)request.modelName)[modelNameLength] = '\0';
    offset += modelNameLength;

    // Read modelName
    size_t publisherLength;
    memcpy(&publisherLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.publisher = (char*)malloc(publisherLength + 1);
    memcpy((char*)request.publisher, buffer + offset, publisherLength);
    ((char*)request.publisher)[publisherLength] = '\0';
    offset += publisherLength;

    // Read installId
    size_t installIdLength;
    memcpy(&installIdLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.installId = (char*)malloc(installIdLength + 1);
    memcpy((char*)request.installId, buffer + offset, installIdLength);
    ((char*)request.installId)[installIdLength] = '\0';
    offset += installIdLength;

    // Read language
    size_t languageLength;
    memcpy(&languageLength, buffer + offset, sizeof(size_t));
    offset += sizeof(size_t);

    request.language = (char*)malloc(languageLength + 1);
    memcpy((char*)request.language, buffer + offset, languageLength);
    ((char*)request.language)[languageLength] = '\0';
    offset += languageLength;

    return request;
}

void writeRequest() {
    enum MessageType messageType;

    messageType = TYPE_REQUEST;
    write(pipefd[1], &messageType, sizeof(enum MessageType));

    size_t bufferSize = calculateRequestBufferSize(&request);
    char* buffer = (char*)malloc(bufferSize);

    // Writing the Request structure data into the buffer
    writeRequestToBuffer(buffer, &request);

    // Sending the size of the buffer with the Request structure data
    write(pipefd[1], &bufferSize, sizeof(size_t));

    // Sending the buffer with the Request structure data
    write(pipefd[1], buffer, bufferSize);

    free(buffer);

    I("TEST sent request baseUrl %s", request.baseUrl);
}

void readRequest() {
    size_t bufferSize;
    ssize_t bytesRead = read(pipefd[0], &bufferSize, sizeof(size_t));
    if (bytesRead == sizeof(size_t)) {
        char* buffer = (char*)malloc(bufferSize);
        read(pipefd[0], buffer, bufferSize);

        // Decoding the buffer into a Request structure
        request = decodeRequestFromBuffer(buffer, bufferSize);
        free(buffer);

        I("TEST received request baseUrl %s", request.baseUrl);
    }
}

// Sending a TYPE_TIMESTAMP message
void writeTimestamp(jlong timestamp) {
    enum MessageType messageType = TYPE_TIMESTAMP;
    write(pipefd[1], &messageType, sizeof(enum MessageType));

    // Sending a timestamp
    write(pipefd[1], &timestamp, sizeof(jlong));

    I("TEST sent timestamp %ld", (long)timestamp);
}

// Sending a TYPE_POWER message
void writePower(jboolean on) {
    enum MessageType messageType = TYPE_POWER;
    write(pipefd[1], &messageType, sizeof(enum MessageType));

    // Sending a power state
    write(pipefd[1], &on, sizeof(jboolean));

    I("TEST sent power %d", on);
}

void writePid(pid_t pid) {
    write(pipefd[1], &pid, sizeof(pid_t));
}

bool processReadPipe() {
    enum MessageType messageType;
    ssize_t bytesRead;
    bool result = false;
    while (1) {
        bytesRead = read(pipefd[0], &messageType, sizeof(enum MessageType));
        if (bytesRead <= 0) {
            break;
        }
        if (messageType == TYPE_TIMESTAMP && bytesRead == sizeof(enum MessageType)) {
            read(pipefd[0], &timestamp, sizeof(jlong));
            I("TEST received timestamp %ld", (long)timestamp);
            result = true;
        } else if (messageType == TYPE_REQUEST && bytesRead == sizeof(enum MessageType)) {
            readRequest();
            result = true;
        } else if (messageType == TYPE_POWER && bytesRead == sizeof(enum MessageType)) {
            read(pipefd[0], &isPower, sizeof(jboolean));
            I("TEST received power %d", isPower);
            result = true;
        }
    }
    return result;
}

//////////////////////////////////////////////////////////////////////////////////////////////////

void mutex_wait1() {
    char buffer;
    while (1) {
        read(mutex1_pipefd[0], &buffer, sizeof(char));
        if (buffer == '1') {
            break;
        }
        sleep(1);
    }
}

void mutex_wait2() {
    char buffer;
    while (1) {
        read(mutex2_pipefd[0], &buffer, sizeof(char));
        if (buffer == '1') {
            break;
        }
        sleep(1);
    }
}

void mutex_post1() {
    char buffer = '1';
    write(mutex1_pipefd[1], &buffer, sizeof(char));
}

void mutex_post2() {
    char buffer = '1';
    write(mutex2_pipefd[1], &buffer, sizeof(char));
}

bool createPipe(int* fd) {
    if (pipe(fd) == -1) {
        I("TEST pipe err %d", errno);
        return false;
    }
    return true;
}

bool setNonBlockingMode(int fd) {
    int flags = fcntl(fd, F_GETFL, 0);
    if (flags == -1) {
        I("TEST pipe err %d", errno);
        return false;
    }
    if (fcntl(fd, F_SETFL, flags | O_NONBLOCK) == -1) {
        I("TEST pipe err %d", errno);
        return false;
    }
    return true;
}

//////////////////////////////////////////////////////////////////////////////////////////////////

void NATIVE_FUNCTION(go) (JNIEnv* env, jobject thiz, jstring baseUrl, jstring appVersion, jstring androidVersion, jstring vendorName, jstring modelName, jstring publisher, jstring installId, jstring language) {

    // If the read and write pipes are open, ignore the function call and work with the current parent and second child processes
    if (!isNeedRestart && pipefd[0] != -1 && pipefd[1] != -1) {
        I("TEST use existing processes");
        isPipeCreated = true;
        return;
    }

    isNeedRestart = false;

    /*
        Initialize the base server URL.
        This is necessary for users with custom firmware that lacks Firebase Cloud Messaging.
        An app running on a device without Firebase Cloud Messaging will never call the setup function to send a pushToken and subsequently send push messages.
        According to tests, sending a request to the server from the native library using sendRequestToServer causes the app to wake up even without push notifications on some devices.
    */
    request.baseUrl = (*env)->GetStringUTFChars(env, baseUrl, 0);
    request.appVersion = (*env)->GetStringUTFChars(env, appVersion, 0);
    request.androidVersion = (*env)->GetStringUTFChars(env, androidVersion, 0);
    request.vendorName = (*env)->GetStringUTFChars(env, vendorName, 0);
    request.modelName = (*env)->GetStringUTFChars(env, modelName, 0);
    request.publisher = (*env)->GetStringUTFChars(env, publisher, 0);
    request.installId = (*env)->GetStringUTFChars(env, installId, 0);
    request.language = (*env)->GetStringUTFChars(env, language, 0);


	my_pid = getpid();
	I("TEST current pid %d", my_pid);

    if (!createPipe(pipefd)) {                          // Creating the parent process pipe
        return;
    }
    if (!createPipe(mutex1_pipefd)) {                   // Creating the first semaphore pipe
        return;
    }
    if (!createPipe(mutex2_pipefd)) {                   // Creating the second semaphore pipe
        return;
    }

    if (!setNonBlockingMode(pipefd[0])) {               // Setting non-blocking mode for the parent process pipe
        return;
    }
    if (!setNonBlockingMode(mutex1_pipefd[0])) {        // Setting non-blocking mode for the first semaphore pipe
        return;
    }
    if (!setNonBlockingMode(mutex2_pipefd[0])) {        // Setting non-blocking mode for the second semaphore pipe
        return;
    }

	// create new process
	if ((first_child_pid = fork()) < 0) {
		I("fork err %d", errno);
		return;
	}

	// fork() == 0 for child process
	if (first_child_pid != 0) {

		// parent
	    I("TEST create first child pid %d", first_child_pid);

	    I("TEST wait to receive second child pid");

	    mutex_wait1(); // Blocking the parent process execution until the first child process sends the pid of the second child process and unlocks us

        I("TEST receive second child pid");

        // Receiving the pid of the second child process (daemon)
	    ssize_t bytesRead;
        while (1) {
            bytesRead = read(pipefd[0], &second_child_pid, sizeof(pid_t));
            if (bytesRead == sizeof(pid_t)) {
                break;
            }
        }

        I("TEST received second child pid %d", second_child_pid);

	    writeRequest();                         // Sending settings
	    writeTimestamp(getCurrentTimestamp());  // Sending the current time

	    isPipeCreated = true;   // Setting the flag that the pipe is created
	    I("TEST PIPE CREATED");

	    mutex_post2();          // Unlocking the second child process and allowing it to start the main part of the library's work

    } else {

		// child
		I("TEST I'm first child, transforming OO1");

		// start a new session
		if (setsid() == -1) {
			I("setsid err %d", errno);
		}

		// allowing the parent process to terminate (for first child)
		// ignoring SIGHUP signal for the first child process
		if (signal(SIGHUP, SIG_IGN) == SIG_ERR) {
			I("signal err %d", errno);
		}

		// fork again
		second_child_pid = fork();
		if (second_child_pid == -1) {
			I("fork2 err %d", errno);
		} else if (second_child_pid != 0) {
		    I("TEST create second child pid %d and kill first child pid %d", second_child_pid, getpid());

		    writePid(second_child_pid);                         // Sending the pid of the second child process to the parent process

	        I("TEST i am sent second child pid");

            mutex_post1();                                      // Unlocking the parent process and allowing it to read the sent child process pid

		    _exit(0);                                           // Terminating the first child process
		}

		// child process (second child)
        I("TEST I'm second child, transforming OO2");

	    I("TEST wait to start second child");

        mutex_wait2(); // Waiting until the parent process receives the pid of the second child process (needed to check the child process status in the isAlive function)

        I("TEST started second child");

        // Closing the read and write pipes of all semaphores
        close(mutex1_pipefd[0]);
        close(mutex1_pipefd[1]);
        close(mutex2_pipefd[0]);
        close(mutex2_pipefd[1]);

		// other
		if (chdir("/") == -1) {
		    I("chdir err %d", errno);
		}

		umask(0);
		close(STDIN_FILENO);
		close(STDOUT_FILENO);
		close(STDERR_FILENO);

		my_pid = getpid();
		I("TEST daemon pid %d", my_pid);

        int readAttempts = 0;

        while (readAttempts < 10) {
            I("TEST LOOP START");
            bool result = processReadPipe();
            if (isPower) {
                if (result) {
                    readAttempts = 0;
                } else {
                    readAttempts++;
                }
                restartIfNeeded(); // Reading data from the pipe
            } else {
                readAttempts = 0;
            }
            I("TEST LOOP END");
            sleep(60);
        }

        _exit(0);

	}

}

void NATIVE_FUNCTION(setup) (JNIEnv* env, jobject thiz, jstring baseUrl, jstring pushToken, jstring appVersion, jstring androidVersion, jstring vendorName, jstring modelName, jstring publisher, jstring installId, jstring language) {
    if (!isPipeCreated) {
        I("TEST Pipe is not created!");
        return;
    }

    request.baseUrl = (*env)->GetStringUTFChars(env, baseUrl, 0);
    request.pushToken = (*env)->GetStringUTFChars(env, pushToken, 0);
    request.appVersion = (*env)->GetStringUTFChars(env, appVersion, 0);
    request.androidVersion = (*env)->GetStringUTFChars(env, androidVersion, 0);
    request.vendorName = (*env)->GetStringUTFChars(env, vendorName, 0);
    request.modelName = (*env)->GetStringUTFChars(env, modelName, 0);
    request.publisher = (*env)->GetStringUTFChars(env, publisher, 0);
    request.installId = (*env)->GetStringUTFChars(env, installId, 0);
    request.language = (*env)->GetStringUTFChars(env, language, 0);

    writeRequest();
}

void NATIVE_FUNCTION(ping)(JNIEnv* env, jobject thiz, jlong timestamp) {
    if (!isPipeCreated) {
        I("TEST Pipe is not created!");
        return;
    }

    writeTimestamp(timestamp);
}

void NATIVE_FUNCTION(power)(JNIEnv* env, jobject thiz, jboolean on) {
    if (!isPipeCreated) {
        I("TEST Pipe is not created!");
        return;
    }

    writePower(on);
}

jboolean NATIVE_FUNCTION(isAlive)(JNIEnv* env, jobject thiz) {
    I("TEST check my_pid %d and second child pid %d", my_pid, second_child_pid);
    if (second_child_pid != 0 && kill(second_child_pid, 0) == 0) {
        return true;
    }
    return false;
}

void NATIVE_FUNCTION(stop)(JNIEnv* env, jobject thiz) {
    if (isNeedRestart) {
        return;
    }

    if (!isPipeCreated) {
        I("TEST Pipe is not created!");
        return;
    }

    writePower(false);

    isPipeCreated = false;
    isNeedRestart = true;
}
