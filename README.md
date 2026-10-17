# Purpose

This library permits Java programs to serve FastCGI requests.
It features:

- multithreading, optionally using [virtual threads](https://openjdk.org/jeps/425);

- concurrent requests per connection;

- support for both stand-alone and server-managed lifecyles;

- Unix-domain and Internet-domain communication with web server;

- [RFC2774](https://www.rfc-editor.org/info/rfc2774/) namespaced fields;

- MIME form parsing;

- negotiation of content and transfer encodings;

- path-based instance selection and local navigation.

There's also support for an experimental extension to FastCGI allowing request and response trailers, but you probably won't find a web server that supports it.

So far, the code is *hardly tested*, so good luck, and please let me know how you get on!

# Installation

[Jardeps](https://github.com/simpsonst/jardeps) is required to build using the supplied GNU makefile.
To build and install in `/usr/local`:

```
make
sudo make install
```

The makefile attempts to load local configuration from `config.mk` adjacent to it, and then from `fastcgi4j-env.mk` in GNU Make's search path (as set by `-I`).
The main variables you might want to override are:

* `PREFIX` &ndash; the installation directory, defaulting to `/usr/local`

* `ENABLE_SOFT` &ndash; If set to `true`, `t`, `y`, `yes` (the default), `on` or `1`, enable compilation of code depending on virtual threads.
  Set to something else, such as `no`, to disable use of virtual threads.

* `JAVACFLAGS` &ndash; Append additional compilation switches to this variable.

[Virtual threads](https://openjdk.org/jeps/425) are a preview feature for JDK19 and JDK20, so if you're using those versions, and want to exploit virtual threads, you should set this in your `config.mk` or `fastcgi4j-env.mk`:

```
JAVACFLAGS += --enable-preview
```

If you're using JDK18 or earlier, or don't want virtual threads:

```
ENABLE_SOFT=no
```




# Writing a FastCGI application

Refer to the [Java API documentation](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/) for details.

Define the application's behaviour by implementing at least one of the three roles [`Responder`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/Responder), [`Filter`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/Filter) or [`Authorizer`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/Authorizer).
This is your *role implementation*.
Some example implementations can be found in `src/java/demos/`.

Then you have three choices:

1. Create an instance of your role implementation, and supply it to an [`Engine`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/engine/Engine).
  Then get the engine to repeatedly process requests.

1. Create an instance of your role implementation, extend [`FastCGIApplication`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/app/FastCGIApplication), and specify the role instance when invoked.
  Run your extension of `FastCGIApplication` as a Java application, and it will .

1. Make your role implementation extend [`FastCGIApplication`](https://www.lancaster.ac.uk/~simpsons/javadoc/fastcgi4j/fastcgi4j/uk/ac/lancs/fastcgi/app/FastCGIApplication), and run as a Java application.
  This is the **recommended** option, especially to quickly get a test program running.

`FastCGIApplication` runs as a Java application, recognizing a number of command-line switches to control non-functional behaviour.
It can also pass additional arguments to your application.
You can invoke it either directly or with the Bash script `fastcgi4j`.
(See [*Invocation*](invoke.md).)
It's compatible with both stand-alone and server-managed lifecycles, but you can still consider the other options if you need something more sophisticated.



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
