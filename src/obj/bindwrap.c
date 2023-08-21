// -*- c-basic-offset: 2; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2023, Lancaster University
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
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <assert.h>
#include <signal.h>
#include <stdbool.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/wait.h>
#include <sys/stat.h>
#include <sys/signalfd.h>
#include <netinet/in.h>
#include <netdb.h>

#include <unistd.h>

#define UNIX_BIND_VAR "FASTCGI4J_UNIX_BIND"
#define INET_BIND_VAR "FASTCGI4J_INET_BIND"
#define UNIX_PEER_VAR "FASTCGI4J_WEB_SERVER_ADDRS"

static int match_var(const char *vname, const char *envp)
{
  while (*vname == *envp && *vname != '\0' && *envp != '\0' && *envp != '=') {
    vname++;
    envp++;
  }
  return *envp == '=';
}

/* Copy at most 'lim' bytes from the null-terminated string at 'src'
   to 'dest', padding with null bytes.  Return zero if 'dest' is now
   null-terminated, and non-zero otherwise (implying truncation). */
static int trunc_strncpy(char *dest, const char *src, size_t lim)
{
#if __GNUC__ >= 8
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wstringop-truncation"
#endif  
  /* GCC warns about this (-Wstringop-truncation), because we don't
     ensure there's a null byte in dest.  However, you'd only do that
     if you didn't care about losing data to truncation.  We do, so we
     test to see if it has occurred.  If it has, it's up to the caller
     to then decide to abort the process. */
  strncpy(dest, src, lim);
#if __GNUC__ >= 8
#pragma GCC diagnostic pop
#endif
  return dest[lim - 1] != '\0';
}

int main(int argc, char *const *argv)
{
  const char *baddr_un = getenv(UNIX_BIND_VAR);
  const char *baddr_in = getenv(INET_BIND_VAR);

  int sock = -1;
  if (baddr_un != NULL) {
    /* Create a UNIX-domain socket. */
    sock = socket(PF_UNIX, SOCK_STREAM, 0);
    if (sock < 0) {
      int ec = errno;
      fprintf(stderr, "%s: PF_UNIX socket: %s\n", argv[0], strerror(ec));
      return EXIT_FAILURE;
    }

    /* Bind the socket to the provided path. */
    union {
      struct sockaddr plain;
      struct sockaddr_un un;
    } addr;
    addr.un.sun_family = AF_UNIX;
    if (trunc_strncpy(addr.un.sun_path, baddr_un, sizeof addr.un.sun_path)) {
      fprintf(stderr, "%s: path too long: %s\n", argv[0], baddr_un);
      return EXIT_FAILURE;
    }
    assert(addr.un.sun_path[sizeof addr.un.sun_path - 1] == '\0');
    if (bind(sock, &addr.plain, sizeof addr) < 0) {
      int ec = errno;
      fprintf(stderr, "%s: (bind) %s: %s\n", argv[0], strerror(ec), baddr_un);
      return EXIT_FAILURE;
    }
    const char *peers = getenv(UNIX_PEER_VAR);
    if (peers != NULL) {
      /* We allow anyone to contact us, knowing that the application
         will identify and validate the peer. */
      chmod(addr.un.sun_path,
            S_IRUSR | S_IWUSR | S_IXUSR |
            S_IRGRP | S_IWGRP | S_IXGRP |
            S_IROTH | S_IWOTH | S_IXOTH);
    }
  } else if (baddr_in != NULL) {
    /* Split the address into host:port. */
    char buf[256];
    if (trunc_strncpy(buf, baddr_in, sizeof buf)) {
      fprintf(stderr, "%s: address too long: %s\n", argv[0], baddr_in);
      return EXIT_FAILURE;
    }
    char *col = strrchr(buf, ':');
    char *brac = strrchr(buf, ']');
    const char *node, *service;
    if (col == NULL || (brac != NULL && brac > col)) {
      if (brac != NULL && buf[0] == '[') {
        /* This is a v6 address with no port. */
        fprintf(stderr, "%s: port required: %s\n", argv[0], baddr_in);
        return EXIT_FAILURE;
      }
      node = NULL;
      service = buf;
    } else {
      *col = '\0';
      node = buf;
      service = col + 1;
    }

    /* Do name resolution on the node and service. */
    struct addrinfo *res;
    static const struct addrinfo hints = {
      .ai_family = AF_UNSPEC,
      .ai_socktype = SOCK_STREAM,
      .ai_flags = AI_PASSIVE | AI_V4MAPPED | AI_ADDRCONFIG,
    };
    int hrc = getaddrinfo(node, service, &hints, &res);
    if (hrc != 0) {
      fprintf(stderr, "%s: %s: %s\n", argv[0],
              gai_strerror(hrc), baddr_in);
      return EXIT_FAILURE;
    }
    if (res == NULL) {
      fprintf(stderr, "%s: unknown address: %s\n", argv[0], baddr_in);
      return EXIT_FAILURE;
    }

    /* Try to create a suitable socket from the results. */
    int mom = 0;
    sa_family_t fam = 0;
    for (struct addrinfo *ptr = res; ptr != NULL; ptr = ptr->ai_next) {
      fam = ptr->ai_family;
      sock = socket(ptr->ai_family, SOCK_STREAM, 0);
      if (sock < 0) {
        mom = 1;
        continue;
      }
      if (bind(sock, ptr->ai_addr, sizeof *ptr->ai_addr) < 0) {
        int ec = errno;
        close(sock), sock = -1;
        errno = ec;
        mom = 2;
        continue;
      }
      break;
    }
    if (sock < 0) {
      int ec = errno;
      assert(mom != 0);
      switch (mom) {
      case 1:
        fprintf(stderr, "%s: %s socket: %s\n", argv[0],
                fam == PF_INET ? "PF_INET" : "PF_INET6",
                strerror(ec));
        break;
      case 2:
        fprintf(stderr, "%s: bind (%s): %s\n", argv[0], strerror(ec), baddr_in);
        break;
      }
      return EXIT_FAILURE;
    }
  } else {
    fprintf(stderr, "%s: must specify FASTCGI4J_UNIX_BIND"
            " or FASTCGI4J_INET_BIND", argv[0]);
    return EXIT_FAILURE;
  }

  /* Get the socket ready to listen for connections. */
  if (listen(sock, 5) < 0) {
    int ec = errno;
    fprintf(stderr, "%s: PF_UNIX socket: %s\n", argv[0], strerror(ec));
    return EXIT_FAILURE;      
  }

  /* Make the socket FD 0, which is where a FastCGI process expects to
     find it.  Close the original socket. */
  if (sock != 0) {
    if (dup2(sock, 0) < 0) {
      int ec = errno;
      fprintf(stderr, "%s: dup2(sock, 0): %s\n", argv[0], strerror(ec));
      return EXIT_FAILURE;
    }
    close(sock);
    sock = 0;
  }

  if (baddr_un != NULL) {
    /* We have to fork and wait for the child to terminate, then we
       can remove the rendezvous point. */

    /* Block signals that we must pass on to the child, or that we
       must handle ourselves. */
    sigset_t interest;
    sigemptyset(&interest);
    sigaddset(&interest, SIGINT);
    sigaddset(&interest, SIGTERM);
    sigaddset(&interest, SIGHUP);
    sigaddset(&interest, SIGCHLD);
    if (sigprocmask(SIG_BLOCK, &interest, NULL) < 0) {
      int ec = errno;
      fprintf(stderr, "%s: can't block signals: %s\n", argv[0], strerror(ec));
      return EXIT_FAILURE;
    }

    int sigfd = signalfd(-1, &interest, SFD_CLOEXEC);
    if (sigfd < 0) {
      int ec = errno;
      fprintf(stderr, "%s: can't create signal fd: %s\n", argv[0], strerror(ec));
      return EXIT_FAILURE;
    }

    /* Fork into parent and child. */
    pid_t chid = fork();
    if (chid < 0) {
      int ec = errno;
      fprintf(stderr, "%s: can't fork: %s\n", argv[0], strerror(ec));
      return EXIT_FAILURE;
    }

    if (chid != 0) {
      /* This is the parent.  Close our copy of the socket. */
      close(sock);
      struct signalfd_siginfo ent;
      int rc;
      do {
        rc = read(sigfd, &ent, sizeof ent);
        if (rc < 0) {
          int ec = errno;
          fprintf(stderr, "%s: can't read signal: %s\n", argv[0], strerror(ec));
          kill(chid, SIGTERM);
          return EXIT_FAILURE;
        }
        assert(rc == sizeof ent);
        switch (ent.ssi_signo) {
          /* Pass these on to the child. */
        case SIGTERM:
          kill(chid, ent.ssi_signo);
          break;

          /* We assume these have also been sent to the child. */
        case SIGINT:
        case SIGHUP:
          break;

        case SIGCHLD:
          do {
            int stat;
            pid_t rc = waitpid(chid, &stat, WNOHANG);
            if (rc < 0) {
              int ec = errno;
              if (ec == EINTR) continue;
              fprintf(stderr, "%s: waitpid: %s\n", argv[0], strerror(ec));
              return EXIT_FAILURE;
            }
            assert(rc == chid);
            remove(baddr_un);
            return WEXITSTATUS(stat);
          } while (true);
        }
      } while (true);
    }
  }

  /* Set up the environment.  Copy everything except the variables we
     use. */
  extern char **environ;
  size_t len = 0;
  for (char *const *ptr = environ; *ptr != NULL; ptr++, len++)
    ;
  char *envp[len + 1];
  size_t idx = 0;
  for (char *const *ptr = environ; *ptr != NULL; ptr++) {
    if (match_var(UNIX_BIND_VAR, *ptr)) continue;
    if (match_var(INET_BIND_VAR, *ptr)) continue;
    envp[idx++] = *ptr;
  }
  envp[idx] = NULL;

  /* Execute the main process, inheriting FD 0. */
  int rc = execve(argv[1], argv + 1, envp);
  if (rc < 0) {
    int ec = errno;
    fprintf(stderr, "%s: could not exec command: %s:", argv[0], strerror(ec));
    for (int i = 1; i < argc; i++)
      fprintf(stderr, " %s", argv[i]);
    fprintf(stderr, "\n");
    return EXIT_FAILURE;
  }

  /* This point is unreachable. */
  abort();
}
