# Purpose

This library enables Java programs to serve FastCGI requests.
It features:

- support for responders, filters and authorizers;

- multithreading, optionally using [virtual threads](https://openjdk.org/jeps/425);

- concurrent requests per connection;

- support for both stand-alone and server-managed life cycles;

- Unix-domain and Internet-domain communication with web server;

- [RFC2774](https://www.rfc-editor.org/info/rfc2774/) namespaced fields;

- MIME form parsing;

- negotiation of content encodings;

- path-based instance selection and internal navigation.

There's also support for an experimental extension to FastCGI allowing [request and response trailers](trailers.md), but you probably won't find a web server that supports it.

So far, the code is *hardly tested*, so good luck, and please let me know how you get on!

# Prerequisites

Java release 21 or later is required.
Release 19 and 20 might work with preview language features enabled.

[Jardeps](https://github.com/simpsonst/jardeps) is required to build the Java components using the supplied GNU makefile.
To build and install Jardeps in `/usr/local`:

```
cd /tmp
git clone https://github.com/simpsonst/jardeps.git
cd jardeps
make
sudo make install
```

A few C/Bash programs are included, and [Binodeps](https://github.com/simpsonst/binodeps) is required to build them using the supplied GNU makefile.
To build and install Binodeps in `/usr/local`:

```
cd /tmp
git clone https://github.com/simpsonst/binodeps.git
cd binodeps
make
sudo make install
```

It is also possible to disable the C/Bash components by building and installing FastCGI4j with `ENABLE_C=no`, as shown below.

# Installation

A GNU `Makefile` is provided.
You can override default configuration by creating `config.mk` adjacent to `Makefile`, or by creating `fastcgi4j-env.mk` anywhere in GNU Make's search path (as set by `-I`), and placing overrides in either of those files.
The main variables you might want to override are:

* `PREFIX` &ndash; the installation directory, defaulting to `/usr/local`

* `JAVACFLAGS` &ndash; Append additional Java compilation switches to this variable.

* `CFLAGS` &ndash; Append additional C compilation switches to this variable.

* `LDFLAGS` &ndash; Append additional C linking switches to this variable.

[Virtual threads](https://openjdk.org/jeps/425) are a preview feature for JDK19 and JDK20, so if you're using those versions, you should set this in your `config.mk` or `fastcgi4j-env.mk`:

```
JAVACFLAGS += --enable-preview
```

The C programs have few dependencies, except for POSIX.1-2008, so you'll probably be fine on a recent Linux platform.
For GCC, you might want to set:

```
CFLAGS += -O2 -std=gnu11
```

You can disable the compilation and installation of the C/Bash components, and eliminate the dependency on Binodeps, by setting:

```
ENABLE_C=no
```

Build and install FastCGI4j to a privileged location (such as `/usr/local`) using:

```
make && sudo make install
```

For an unprivileged location (such as `~/.local`), just use:

```
make && make install
```

Files installed include:

```
$(PREFIX)/bin/fastcgi4j
$(PREFIX)/libexec/fastcgi4j/bindwrap
$(PREFIX)/libexec/fastcgi4j/testwrap
$(PREFIX)/share/java/fastcgi4j_app.jar
$(PREFIX)/share/java/fastcgi4j_augctxt.jar
$(PREFIX)/share/java/fastcgi4j_demos.jar
$(PREFIX)/share/java/fastcgi4j_engine.jar
$(PREFIX)/share/java/fastcgi4j_env.jar
$(PREFIX)/share/java/fastcgi4j_iis.jar
$(PREFIX)/share/java/fastcgi4j_inet.jar
$(PREFIX)/share/java/fastcgi4j_inherit.jar
$(PREFIX)/share/java/fastcgi4j_proto.jar
$(PREFIX)/share/java/fastcgi4j_role.jar
$(PREFIX)/share/java/fastcgi4j_soft.jar
$(PREFIX)/share/java/fastcgi4j_stdeng.jar
$(PREFIX)/share/java/fastcgi4j_threads.jar
$(PREFIX)/share/java/fastcgi4j_transport.jar
$(PREFIX)/share/java/fastcgi4j_unix.jar
$(PREFIX)/share/java/fastcgi4j_util.jar
```

# Writing a FastCGI application

Refer to the [Java API documentation](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/) for details.

Define the application's behaviour by implementing at least one of the three roles [`Responder`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/Responder), [`Filter`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/Filter) or [`Authorizer`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/Authorizer).
This is your *role implementation*.
Some example implementations can be found in `src/java/demos/`.

Then you have three choices:

1. Create an instance of your role implementation, and use it to build an [`EngineTemplate`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/engine/EngineTemplate).
  Then supply the template with one or more [`Transport`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/transport/Transport)s to get an [`EngineCluster`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/engine/EngineCluster), which repeatedly processes requests by submitted them to your role implementation.

1. Extend [`FastCGIApplication`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/app/FastCGIApplication), and create an instance of your role implementation within its `init` method.

1. Make your role implementation extend [`FastCGIApplication`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j-1/uk/ac/lancs/fastcgi/app/FastCGIApplication).
  This is the **recommended** option, especially to quickly get a test program running.

`uk.ac.lancs.fastcgi.app.FastCGIApplication` runs as a Java application, recognizing a number of command-line switches to control non-functional behaviour, including the name of your extension of it (as for options 2 and 3).
It can also pass additional arguments to your application.
You can invoke `java` either directly (providing the necessary class path to the FastCGI4j jars), or use the Bash script `fastcgi4j` which sets that up for you.
(See [*Invocation*](invoke.md).)
Using `FastCGIApplication` is compatible with both stand-alone and server-managed life cycles, but you can still consider option 1 if you need something more sophisticated.



# Demonstration

Create a script that will be executed as a FastCGI process:

```
#!/bin/bash

exec /usr/local/bin/fastcgi4j --demo MD5SumWrappedApp
```

The responder within it simply displays its environment, and an MD5 hash of the request body.
Oooh!

If your script is available through (say) `http://localhost/foo`, point your browser at it, and the response should include a line:

```
Digest: d41d8cd98f00b204e9800998ecf8427e
```

That's the MD5 sum for an empty file, since a `GET` was issued by the browser with a (necessarily) empty request body.
If you want to test sending a non-empty request body, you could use `curl`:

```
curl -T somefile.txt -X POST "http://localhost/foo"
```

Confirm that the correct hash was calculated by hashing the file yourself:

```
md5sum somefile.txt
```


# How it works

A stand-alone Internet-domain implementation in Java is fairly trivial.
Unix-domain and server-managed implementations are more problematic.
The first requires Unix-domain sockets in Java, and the second requires a means to get a server socket from file descriptor 0, which is how a socket is provided to a server-managed process.

JDK 16 introduced [`UnixDomainSocketAddress`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/net/UnixDomainSocketAddress.html), and presumably some Unix-domain socket support appeared at the same time.
In OpenJDK 18, this is (still?) limited to a [`ServerSocketChannel`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/nio/channels/ServerSocketChannel.html);
you can't have a Unix-domain [`ServerSocket`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/net/ServerSocket.html), only the channel.

As far back as JDK 1.5, [`System.inheritedChannel`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/lang/System.html#inheritedChannel()) could provide access (on Linux, at least) to file descriptor 0, and generate a `ServerSocketChannel` from it.
Hence, both Unix-domain and server-managed implementations (and the combination of both) are possible natively.

See also the [software architecture](soft-arch.md).
