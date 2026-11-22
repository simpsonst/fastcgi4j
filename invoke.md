# Invocation

FastCGI supports both server-managed and stand-alone life cycles:

* In the stand-alone life cycle, the web server is configured to point at the agreed address of the application, but does not start or stop it.
  The server and application could run on separate machines.

* In the server-managed life cycle, the web server is configured to run the application, typically on demand, possibly running multiple instances of the application, and killing idle instances.
  The server might create the socket that the application will listen on, which it inherits.

Both modes require configuration of the web server.
The section ["Servers that support FastCGI"](https://fastcgi-archives.github.io/#servers-that-support-fastcgi)  from the FastCGI.com Archives provides links on how to configure your server.

In either mode, you'll need to put together a command that runs your application.
In server-managed mode, you give that command to the server to run.
In stand-alone mode, you use a different mechanism to run it, such as running it manually youself.

## The `fastcgi4j` script

The `fastcgi4j` Bash script executes a Java program, having first set up the classpath to include the library.
The syntax is:

```
fastcgi4j [options] [handler|--] [args...]
```

### Software selection options

These options select which application is invoked:

* `--jar FILE` &mdash; Add `FILE` to the classpath.

* `-L DIR` or `-LDIR` &mdash; Add `DIR` to jar search path

* `+L` &mdash; Reset jar search path.

* `-l LIB` or `-lLIB` &mdash; Search for `LIB.jar` in search path, and add to classpath.

* `+l` &mdash; Reset classpath.

* `--demo` &mdash; Include the demonstration jar.

* `handler` &mdash; Instantiate this Java class as a `FastCGIApplication`.

* `--seek` &mdash; Seek `FastCGIApplication` service instead of taking first application argument (`handler`) as class name.
  Also enabled if the first unrecognized argument cannot be a class name.

* `--` &mdash; As `--seek`, but remaining arguments are passed to application.

### JVM options

These options modify the JVM:

* `-JARG` &mdash; Add `ARG` to JVM arguments.

* `-eaARG` &mdash; Add `-eaARG` (enable assertions) to JVM arguments.
  `ARG` may be empty.

* `--jvm PROG` &mdash; Use `PROG` as java command.
  Default is just `java`.

### Capability options

The application's FastCGI capabilities are expressed by a stack of [`Properties`](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/util/Properties.html) objects, the base of which is system properties.
These options modify that stack:

* `-f FILE` &mdash; Load properties in `FILE`, and push onto stack.
  The previous top of the stack becomes the new object's default.

* `+f` &mdash; Push empty properties onto stack.
  The previous top of the stack becomes the new object's default.

* `-c NUM` &mdash; Set maximum number of concurrent connections.
  This is the property `uk.ac.lancs.fastcgi.nconn`.

* `+c` &mdash; Set unlimited concurrent connections (the default).

* `-s NUM` &mdash; Set maximum number of concurrent sessions.
  This is the property `uk.ac.lancs.fastcgi.nsess`

* `+s` &mdash; Set unlimited concurrent sessions (the default).

* `-p NUM` &mdash; Set maximum number of concurrent sessions per connection.
  This is the property `uk.ac.lancs.fastcgi.nspc`.

* `+p` &mdash; Set unlimited concurrent sessions per connection (the default).

* `-b NUMx` &mdash; Set output buffer capacity to `NUM` bytes (with `kKmMgG` multiplier as `x`).
  This is the property `uk.ac.lancs.fastcgi.buffer`.

* `+b` &mdash; Set default output buffer capacity.

### Directory options

This option controls the working directory, as well as influencing other switches:

* `-C DIR` or `-CDIR` &mdash; Change to directory `DIR`.
  This influences subsequent `--jar`, `-L` and `-f` options, as well as setting the working directory for the JVM.

### Diagnostic options

This option is for diagnostic purposes:

* `-n` &mdash; Dry run.
  Print command and environment, then exit.

### Stand-alone life cycle

The following options are for stand-alone mode only:

* `--bind HOST:PORT` &mdash; Run stand-alone, listening to an Internet-domain socket bound to `HOST:PORT`.

* `--bind PATH` &mdash; Run stand-alone, bound to Unix-domain rendezvous point `PATH`, which must be absolute.

* `--peer PEER` &mdash; Add `PEER` to set of permitted peers in stand-alone mode.
  For Internet-domain, `PEER` is a hostname or IP address.
  For Unix-domain, `PEER` is `USER`, `@GROUP`, or `USER@GROUP`.

In a stand-alone life cycle, starting and stopping of the application is not under the control of the HTTP server.
The application must bind to an agreed address to receive FastCGI connections on, and then the HTTP server acts as a kind of reverse proxy onto it.

You can use the `fastcgi4j` wrapper to run a stand-alone application.
Use (say) `--bind /path/to/sock` to use a Unix-domain socket and bind it to the specified rendezvous point, or `--bind localhost:8888` to use an Internet-domain socket, and bind it to the specified local address.
You can specify both using two `--bind` switches, but you cannot specify more than one of each kind.

Especially for Internet-domain sockets, you might want to use one or more `--peer` switches to specify HTTP server hosts that may connect to the socket.

For the Unix domain, specify users (e.g., `joe`), groups (e.g., `@wheel`), or users calling with a specific effective group (e.g., `joe@users`).
If you specify a plain user, make sure it exists, or add a trailing `@`;
if not, it will be treated as an authorized client hostname for the Internet-domain socket.
When peers are specified, the rendezvous point will permit connection from all users, as the application (through its library) is expected to check authorization.
Beware of using a path in `/tmp/`, e.g., `--bind /tmp/my.sock`, as Apache is often run with a private `/tmp/`, and can't see the real directory where your socket is.

You can use `--wrap` to spoof a server-managed life cycle.
The socket is created before the Java application starts, and is passed in as file descriptor 0, as it would be if invoked by the server.
The main purpose of this is to detect that the application is already running by the bind address already being in use, but without starting a fresh JVM, making it more suitable for running as a cronjob.
The wrapper process awaits the termination of the JVM, and removes any Unix-domain rendezvous point on exit.
As `--wrap` uses channel inheritance to inject the socket into the application process, it can only support one socket, so it cannot be used with both Internet- and Unix-domain sockets simultaneously.


### Server-managed life cycle

With a server-managed life cycle, the HTTP server invokes the application on demand, possibly several times concurrently.
Do not use `--bind` or `--peer` with a server-managed life cycle, as these are for stand-alone mode only.
The information they provide is passed through the inherited environment instead.



## Apache server-managed configuration

[`mod_fcgid`](https://httpd.apache.org/mod_fcgid/) seems to be the favoured mechanism.
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


## Apache stand-alone configuration

These examples assume that you've started your application with `--bind localhost:9000`, i.e., you're using an Internet-domain socket.
As such, they refer to this address using `fcgi://localhost:9000`.
Alternatively, if you're using a Unix-domain socket, e.g., with `--bind /path/to.sock`, replace `fcgi://localhost:9000` with `unix:///path/to.sock|fcgi://localhost`.

If you have privileged access to the Apache configuration, [`ProxyPass`](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxypass) can be used to pass a whole subtree of virtual paths to your application.
This matches paths such as `/foo`, `/foo/bar`, and so on, but not `/foobar`:

```
ProxyPass "/foo" "fcgi://localhost:9000" enablereuse=on
```

It can also be used in a [`<Location>`](https://httpd.apache.org/docs/2.4/mod/core.html#location) context:

```
<Location "/foo">
  ProxyPass "fcgi://localhost:9000" enablereuse=on
</Location>
```

The following directives should also be investigated as providing more flexibility:

- [`ProxyPassMatch`](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxypassmatch)
- [`<ProxySet>`](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxyset)
- [`<Proxy>`](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxy)
- [`<ProxyMatch>`](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxymatch)
- [`<LocationMatch>`](https://httpd.apache.org/docs/2.4/mod/core.html#locationmatch)

If you only have access to `.htaccess`, [`mod_rewrite`](https://httpd.apache.org/docs/2.4/mod/mod_rewrite.html) is sometimes available:

```
RewriteEngine On
RewriteCond %{REQUEST_URI} ^/some/path(/.*)?$
RewriteRule ^.* "fcgi://localhost:9000/" [P,NE]
```


## NGINX stand-alone configurations

This example assumes you're starting your application with `--bind localhost:9000`.
A `location` directive selects the path you bind to.
Within it, set `fastcgi_pass` to point at your application's address:

```
location ~ ^/foo(/.*)?$ {
  fastcgi_pass localhost:9000;
}
```

This will match `/foo`, `/foo/`, `/foo/bar`, etc, but not `/football`.

So far, this will only get you HTTP headers as CGI parameters (e.g., `HTTP_USER_AGENT`, etc).
You will likely want additional parameters to be passed, especially `PATH_INFO` and `SCRIPT_NAME`.
Based on information from [NGINX documentation](https://www.nginx.com/resources/wiki/start/topics/examples/phpfcgi/#fastcgi-params) and [StackOverflow answer](https://stackoverflow.com/questions/20848899/nginx-phpfpm-path-info-always-empty#answer-49246280), you need include some settings from `/etc/nginx/fastcgi.conf`, and customize a few values:

```
location ~ ^/foo(/.*)?$ {
  fastcgi_pass localhost:9000;

  include fastcgi.conf;
  fastcgi_param SCRIPT_NAME       "/foo";
  fastcgi_split_path_info         ^(/foo)(/.*)$;
  fastcgi_param PATH_INFO         $fastcgi_path_info;
  fastcgi_param PATH_TRANSLATED   $document_root$fastcgi_path_info;
}
```
