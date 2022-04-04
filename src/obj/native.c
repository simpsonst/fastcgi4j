/*
 * Copyright (c) 2022, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <s.simpson@lancaster.ac.uk>
 */

#include <string.h>
#include <errno.h>

#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "uk_ac_lancs_fastcgi_transport_fork_Descriptor.h"

#define MAX(a, b) ((a) > (b) ? (a) : (b))

#define MAX_SOCKADDR_LEN						\
  MAX(sizeof(struct sockaddr),						\
      MAX(sizeof(struct sockaddr_in),					\
	  MAX(sizeof(struct sockaddr_in6), sizeof(struct sockaddr_un))))

static void throwErrno(JNIEnv *env, int ec)
{
  jclass ioext = (*env)->FindClass(env, "java/io/IOException");
  jmethodID ctr = (*env)->GetMethodID(env, ioext, "<init>",
				      "(Ljava/lang/String;)V");
  jstring str = (*env)->NewStringUTF(env, strerror(ec));
  jthrowable exc = (jthrowable) (*env)->NewObject(env, ioext, ctr, str);
  (*env)->Throw(env, exc);
}

JNIEXPORT void JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_closeSocket
(JNIEnv *env, jclass jc, jint fd)
{
  int rc = close(fd);
  if (rc < 0) throwErrno(env, errno);
}

static void copy_back_addr(JNIEnv *env, jintArray ulen,
			   jbyteArray ubuf,
			   const char *ptr, socklen_t addrlen)
{
  jbyte *dst = (*env)->GetByteArrayElements(env, ubuf, NULL);
  for (socklen_t i = 0; i < addrlen; i++)
    dst[i] = ptr[i];
  (*env)->ReleaseByteArrayElements(env, ubuf, dst, 0);

  jint *lb = (*env)->GetIntArrayElements(env, ulen, NULL);
  lb[0] = addrlen;
  (*env)->ReleaseIntArrayElements(env, ulen, lb, 0);
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    checkDescriptor
 * Signature: ([I[B)I
 */
JNIEXPORT jint JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_checkDescriptor
(JNIEnv *env, jclass jc, jintArray ulen, jbyteArray ubuf)
{
  int fd = 0;
  union {
    struct sockaddr addr;
    char buf[MAX_SOCKADDR_LEN];
  } u0;
  socklen_t addrlen = sizeof u0.addr;
  int rc = getsockname(fd, &u0.addr, &addrlen);
  if (rc < 0) {
    if (errno == ENOTSOCK) return -1;
    throwErrno(env, errno);
    return -1;
  }
  if (addrlen <= sizeof u0.addr) {
    copy_back_addr(env, ulen, ubuf, u0.buf, addrlen);
    return fd;
  }

  union {
    struct sockaddr addr;
    char buf[MAX_SOCKADDR_LEN];
  } u;
  rc = getsockname(fd, &u.addr, &addrlen);
  if (rc < 0) {
    throwErrno(env, errno);
    return -1;
  }
  copy_back_addr(env, ulen, ubuf, u.buf, addrlen);
  return fd;
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    getSocketAddress
 * Signature: (I[B)Ljava/net/SocketAddress;
 */
JNIEXPORT jobject JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_getSocketAddress
(JNIEnv *env, jclass jc, jint ulen, jbyteArray ubuf)
{
  union {
    struct sockaddr sa;
    struct sockaddr_in in;
    struct sockaddr_in6 in6;
    struct sockaddr_un un;
    char buf[MAX_SOCKADDR_LEN];
  } u;

  /* Copy the address into our buffer. */
  {
    jbyte *src = (*env)->GetByteArrayElements(env, ubuf, NULL);
    for (jint i = 0; i < ulen; i++)
      u.buf[i] = src[i];
    (*env)->ReleaseByteArrayElements(env, ubuf, src, 0);
  }

  /* Check to see if it's AF_INET or AF_INET6. If so, extract the port
   * number, and identify the bytes of the IP address. */
  uint16_t port;
  char *ptr;
  size_t len;
  switch (u.sa.sa_family) {
  case AF_INET:
    port = ntohs(u.in.sin_port);
    ptr = (void *) &u.in.sin_addr;
    len = 4;
    break;
    
  case AF_INET6:
    port = ntohs(u.in6.sin6_port);
    ptr = (void *) &u.in6.sin6_addr;
    len = 16;
    break;

  case AF_UNIX:
    /* We recognize this type, but do nothing at this stage. */
    break;

  default:
    /* It's not recognized, so we say nothing. */
    return NULL;
  }

  switch (u.sa.sa_family) {
  case AF_INET:
  case AF_INET6: {
    /* Convert the bytes into a Java byte array. */
    jbyteArray array = (*env)->NewByteArray(env, len);
    {
      jbyte *dst = (*env)->GetByteArrayElements(env, array, NULL);
      for (size_t i = 0; i < len; i++)
	dst[i] = ptr[i];
      (*env)->ReleaseByteArrayElements(env, array, dst, 0);
    }

    /* Create the InetAddress from the Java byte array. */
    jclass inat = (*env)->FindClass(env, "java/net/InetAddress");
    jmethodID gba = (*env)->GetMethodID(env, inat, "getByAddress",
					"([b)Ljava/net/InetAddress;");
    jobject aobj = (*env)->CallStaticObjectMethod(env, inat, gba, array);
    if ((*env)->ExceptionCheck(env)) return NULL;

    /* Combine the port with the IP address. */
    jclass insat = (*env)->FindClass(env, "java/net/InetSocketAddress");
    jmethodID ctr = (*env)->GetMethodID(env, insat, "<init>",
					"(Ljava/net/InetAddress;I)V");
    jobject insa = (*env)->NewObject(env, insat, ctr, aobj, port);
    if ((*env)->ExceptionCheck(env)) return NULL;

    return insa;
  }

  case AF_UNIX: {
    /* Convert the path into a Java string.  TODO: Perhaps this string
       should not be considered compatible with UTF-8. */
    const size_t len = strnlen(u.un.sun_path, sizeof u.un.sun_path);
    char cpy[sizeof u.un.sun_path + 1];
    strncpy(cpy, u.un.sun_path, len);
    cpy[len] = '\0';
    //fprintf(stderr, "bound path: %s\n", cpy);
    jstring path = (*env)->NewStringUTF(env, cpy);
    if ((*env)->ExceptionCheck(env)) return NULL;

    /* Create a UnixDomainSocketAddress from the path. */
    jclass insat = (*env)->FindClass(env, "java/net/UnixDomainSocketAddress");
    jmethodID ctr =
      (*env)->GetStaticMethodID(env, insat, "of",
				"(Ljava/lang/String;)"
				"Ljava/net/UnixDomainSocketAddress;");
    jobject insa = (*env)->CallStaticObjectMethod(env, insat, ctr, path);
    if ((*env)->ExceptionCheck(env)) return NULL;

    return insa;
  }

  default:
    // unreachable
    return NULL;
  }
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    acceptConnection
 * Signature: (I[I[B)I
 */
JNIEXPORT jint JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_acceptConnection
(JNIEnv *env, jclass jc, jint fd, jintArray ulen, jbyteArray ubuf)
{
  union {
    struct sockaddr addr;
    char buf[MAX_SOCKADDR_LEN];
  } u;
  socklen_t addrlen = sizeof u.buf;
  int rc = accept(fd, &u.addr, &addrlen);
  if (rc < 0) {
    throwErrno(env, errno);
    return -1;
  }

  copy_back_addr(env, ulen, ubuf, u.buf, addrlen);
  return rc;
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    getAddressSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_getAddressSize
(JNIEnv *env, jclass jc)
{
  return MAX_SOCKADDR_LEN;
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    writeSocket
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_writeSocket__II
(JNIEnv *env, jclass jc, jint fd, jint b)
{
  char nb = b;
  for ( ; ; ) {
    ssize_t rc = send(fd, &nb, 1, 0);
    if (rc < 0)
      throwErrno(env, errno);
  }
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    writeSocket
 * Signature: (I[BII)V
 */
JNIEXPORT void JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_writeSocket__I_3BII
(JNIEnv *env, jclass jc, jint fd, jbyteArray b, jint off, jint len)
{
  char buf[len];
  jbyte *src = (*env)->GetByteArrayElements(env, b, NULL);
  for (jint i = 0; i < len; i++)
    buf[i] = src[i + off];
  (*env)->ReleaseByteArrayElements(env, b, src, 0);
  char *ptr = buf;
  while (len > 0) {
    ssize_t got = send(fd, ptr, len, 0);
    if (got < 0) {
      throwErrno(env, errno);
      return;
    }
    ptr += got;
    len -= got;
  }
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    readSocket
 * Signature: (I[BII)I
 */
JNIEXPORT jint JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_readSocket__I_3BII
(JNIEnv *env, jclass jc, jint fd, jbyteArray b, jint off, jint len)
{
  char buf[len];
  ssize_t rc = recv(fd, buf, len, 0);
  if (rc < 0) {
    throwErrno(env, errno);
    return -1;
  }
  if (rc == 0) return -1;

  jbyte *dst = (*env)->GetByteArrayElements(env, b, NULL);
  for (jint i = 0; i < rc; i++)
    dst[i + off] = buf[i];
  (*env)->ReleaseByteArrayElements(env, b, dst, 0);
  return rc;
}

/*
 * Class:     uk_ac_lancs_fastcgi_transport_native_unix_Descriptor
 * Method:    readSocket
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_uk_ac_lancs_fastcgi_transport_fork_Descriptor_readSocket__I
(JNIEnv *env, jclass jc, jint fd)
{
  char b;
  ssize_t rc = recv(fd, &b, 1, 0);
  if (rc < 0) {
    throwErrno(env, errno);
    return -1;
  }
  if (rc == 0) return -1;
  return (unsigned) b;
}
