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

* `-eaARG` &ndash; Add `-eaARG` to JVM arguments.
  `ARG` may be empty.

* `--jvm PROG` &ndash; Use `PROG` as java command.
  Default is just `java`.

* `--seek` &ndash; Seek `FastCGIApplication` service instead of taking first application argument (handler) as class name.
  Also enabled if the first unrecognized argument cannot be a class name.

* `--` &ndash; As `--seek`, but remaining arguments are passed to application.

* `--jar FILE` &ndash; Add `FILE` to the classpath.

* `--bind HOST:PORT` &ndash; Run stand-alone, bound to `HOST:PORT`.

* `--bind PATH` &ndash; Run stand-alone, bound to Unix-domain rendezvous point `PATH`, which must be absolute.

* `--peer PEER` &ndash; Add `PEER` to set of permitted peers in stand-alone mode.
  For Internet-domain, `PEER` is a hostname or IP address.
  For Unix-domain, `PEER` is `USER`, `@GROUP`, or `USER@GROUP`.

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

[`mod_fcgid`](https://httpd.apache.org/mod_fcgid/) seems to be the favoured mechanism.
This defines the handler `fcgid-script`, and enabling the module `fcgid` also defines that it is used with the `.fcgi` suffix.
You'll need the option `ExecCGI` enabled, and the script itself must be executable.

Your script should be something like this:

```
#!/bin/bash

exec /usr/local/bin/fastcgi4j -L/usr/local/share/java -lmyapp \
  -J--enable-preview org.example.MyApp --database=/some/config
```

(`-J--enable-preview` should be redundant for JDK21+ if you want to make use of virtual threads.)

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

You can use the `fastcgi4j` wrapper to run a stand-alone application.
Use (say) `--bind /path/to/sock` to bind to a Unix-domain socket, or `--bind localhost:8888` to bind to an Internet-domain socket.
In the latter case, you might want to use one or more `--peer` switches to specify HTTPd server hosts that may connect to the socket.
Also, beware of using a path in `/tmp/`, e.g., `--bind /tmp/my.sock`, as Apache is often run with a private `/tmp/`, and can't see the real directory where your socket is.

Here's some documentation for web servers that can talk to a stand-alone FastCGI application:

* [Apache Module `mod_proxy_fcgi`](https://httpd.apache.org/docs/2.4/mod/mod_proxy_fcgi.html)

* [Apache Module `mod_authnz_fcgi`](https://httpd.apache.org/docs/2.4/mod/mod_authnz_fcgi.html)

* [NGINX `fastcgi_pass` directive](http://nginx.org/en/docs/http/ngx_http_fastcgi_module.html#fastcgi_pass)


### Apache stand-alone configuration examples

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


### NGINX stand-alone configurations

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
