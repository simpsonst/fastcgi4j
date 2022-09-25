# Purpose

This is library permits Java programs to serve FastCGI requests.

So far, the code is *hardly tested*, so good luck!

# Writing a FastCGI application

See the (Java API documentation)[https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/].

Define the application's behaviour by implementing `Responder`, `Filter` or `Authorizer`.
Then you have two choices:

* Your application should then create an (`Engine`)[https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/engine/Engine], configure it with the behaviour object, and then get the engine to repeatedly process requests.

* Extend (`FastCGIApplication`)[https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/app/FastCGIApplication], and add the behaviour object to it.
  This will recognize a number of command-line switches to control non-functional behaviour.
  Invocation with the `fastcgi4j` script will also set some of these switches.

# Invocation

The `fastcgi4j` Bash script executes a Java program, having first set up the classpath to include the library.
The syntax is:

```
fastcgi4j [options] [handler|--] [args...]
```

Options:

* `-n` &ndash; Dry run.
  Print command and environment, then exit.

* `-C DIR` or `-CDIR` &ndash; Change to directory `DIR`.

* `-L DIR` or `-LDIR` &ndash; Add `DIR` to jar search path

* `+L` &ndash; Reset jar search path.

* `-l LIB` or `-lLIB` &ndash; Search for `LIB.jar` in search path, and add to classpath.

* `+l` &ndash; Reset classpath.

* `-JARG` &ndash; Add `ARG` to JVM arguments.

* `--jvm PROG` &ndash; Use PROG as java command.
  Default is just `java`.

* `--seek` &ndash; Seek `FastCGIApplication` service instead of taking first application argument (handler) as class name.
  Also enabled if the first unrecognized argument cannot be a class name.

* `--` &ndash; As `--seek`, but remaining arguments are passed to application.

* `--jar FILE` &ndash; Add `FILE` to the classpath.

* `--bind HOST:PORT` &ndash; Run stand-alone, bound to `HOST:PORT`.

* `--bind PATH` &ndash; Run stand-alone, bound to Unix-domain rendezvous point `PATH`, which must be absolute.

* `--peer HOST` &ndash; Add `HOST` to set of permitted peers in stand-alone mode.

* `-f FILE` &ndash; Load properties in `FILE`, and push onto stack.

* `+f` &ndash; Push empty properties onto stack.

* `-c NUM` &ndash; Set maximum number of concurrent connections.

* `+c` &ndash; Set unlimited concurrent connections (the default).

* `-s NUM` &ndash; Set maximum number of concurrent sessions.

* `+s` &ndash; Set unlimited concurrent sessions (the default).

* `-p NUM` &ndash; Set maximum number of concurrent sessions per connection.

* `+p` &ndash; Set unlimited concurrent sessions per connection (the default).

* `-b NUMx` &ndash; Set output buffer capacity to `NUM` bytes (with `kKmMgG` multiplier as `x`).

* `+b` &ndash; Set default output buffer capacity.

* `--demo` &ndash; Include the demonstration jar.

## Server-managed life cycle

With a server-managed life cycle, the HTTPd server invokes the application on demand, possibly several times concurrently.

### Apache configuration

`mod_fcgid` seems to be the favoured mechanism.
This defines the handler `fcgid-script`, and enabling the module `fcgid` also defines that it is used with the `.fcgi` suffix.
You'll need the option `ExecCGI` enabled, and the script itself must be executable.

Your script should be something like this:

```
#!/bin/bash

exec /usr/local/bin/fastcgi4j -L/usr/local/share/java -lmyapp \
  org.example.MyApp --database=/some/config
```

You might want to mark a suffix-less script for FastCGI, or pass additional specific configuration to it, through `<Directory>`, `<Location>` or `<Files>` directives:

```
<Files "foo">
  SetHandler fcgid-script
  SetEnv MYAPP_XSLT_PFX "/myapp/templates/"
  SetEnv MYAPP_CONFIG "/etc/myapp.conf
</Files>
```

## Stand-alone life cycle

In a stand-alone life cycle, starting and stopping of the application is not under the control of the HTTPd server.
The application must bind to an agreed address to receive FastCGI connections on, and then the HTTPd server acts as a kind of reverse proxy onto it.

To do.

# Demonstration

Create a script that will be executed as a FastCGI process:

```
#!/bin/bash

exec /usr/local/bin/fastcgi4j --demo MD5SumWrappedApp
```

The responder within it simply displays its environment, and an MD5 hash of the request body.
Oooh!

# How it works

A stand-alone Internet-domain implementation in Java is fairly trivial.
Unix-domain and server-managed implementations are more problematic.
The first requires Unix-domain sockets in Java, and the second requires a means to get a server socket from file descriptor 0, which is how a socket is provided to a server-managed process.

JDK 16 introduced [`UnixDomainSocketAddress`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/UnixDomainSocketAddress.html), and presumably some Unix-domain socket support appeared at the same time.
In OpenJDK 18, this is (still?) limited to a `ServerSocketChannel`;
you can't have a Unix-domain `ServerSocket`, only the channel.
As far back as JDK 1.5, [`System.inheritedChannel`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#inheritedChannel()) could provide access (on Linux, at least) to file descriptor 0, and generate a `ServerSocketChannel` from it.
Hence, both Unix-domain and server-managed implementations (and the combination of both) are possible.

# To do

A FastCGI application in Java should be able to take advantage of [virtual threads](https://openjdk.org/jeps/425), which are a preview feature in JDK 19.