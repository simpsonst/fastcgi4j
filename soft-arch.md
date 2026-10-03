# FastCGI4j software architecture

The library distributes FastCGI functionality across the following Java source trees:

- `role` defines the interfaces that FastCGI applications must implement to receive requests (`Responder`, `Authorizer`, `Filter`), and the types that express those requests and accept responses (correspondingly, `ResponderSession`, `AuthorizerSession`, `FilterSession`).
  These are intentially kept basic and protocol-agnostic.
- `transport` defines means of communication between web server and FastCGI application.
  Here, the choice between server-managed and stand-alone lifecyles is made, as well as whether Unix- or Internet-domain sockets are used.
  A plug-in framework permits new, environmentally selected transports to be deployed at run time, without application changes.
- `proto` defines symbolic constants and tools for (de)serializing FastCGI records.
- `engine` defines a plug-in framework for engines that bind a transport to an application's role implementations.
- `threads` extends the engine framework to allow alternative threading strategies.
- `env` exposes environmental controls, including those mandated by FastCGI, as well as some defined by this library.
- `app` defines an application shell, binding role implementations to transports, engines and engine threading, based on command-line arguments and environmental configuration.
- `util` provides libraries not specific to FastCGI that nonetheless could be useful to implementations of transports, engines and applications.
- `augctxt` uses some of those libraries to provided augmented session contexts, including one specifically for HTTP, supporting namespaced fields according to RFC2774.

The following trees provide plug-ins for the various frameworks:

- `soft` enables soft or virtual engine threading.
- `stdeng` provides a standard engine capable of multithreaded invocation of application roles, multiple concurrent transport connections, and multiple concurrent requests per connection.
- `inet` allows for Internet-domain communication with an appplication using a stand-alone lifecycle.
- `unix` provides the same, but with Unix-domain communication.
- `inherit` supports the server-managed lifecycle, for both Unix- and Internet-domain communication.
- `iis` supports applications managed by IIS.

There are some additional non-functional source trees:

- `demos` provides some demonstration applications.
- `tests` provides some unit tests and test applications.

## Jar composition

There is a mostly one-to-one mapping between trees and their jars.
For example, `fastcgi4j_proto` contains only the compiled `proto` tree.

## Internal dependencies

`util`, `role`, `proto`, `env` and `transport` each have no compile-time dependency on other trees.
Here are the other trees' dependencies:

| tree      | `app` | `augctxt` | `engine` | `env` | `proto` | `role` | `stdeng` | `threads` | `transport` | `util` |
|-----------|-------|-----------|----------|-------|---------|--------|----------|-----------|-------------|--------|
| `app`     |       |           | yes      |       |         | yes    |          |           | yes         |        |
| `augctxt` |       |           |          |       |         | yes    |          |           |             | yes    |
| `engine`  |       |           |          |       |         | yes    |          |           | yes         |        |
| `iis`     |       |           |          |       |         |        |          |           | yes         |        |
| `inet`    |       |           |          | yes   |         |        |          |           | yes         |        |
| `inherit` |       |           |          | yes   |         |        |          |           | yes         |        |
| `soft`    |       |           | yes      |       |         |        |          | yes       |             |        |
| `stdeng`  |       |           | yes      |       | yes     | yes    |          | yes       | yes         | yes    |
| `threads` |       |           | yes      |       |         |        |          |           |             |        |
| `demos`   | yes   | yes       | yes*     |       |         | yes    |          |           | yes*        | yes    |
| `unix`    |       |           |          | yes   |         |        |          |           | yes         |        |

*Note that `demos` includes some applications that divert around the application abstraction in `app`, and so consequently need access to `engine` and `transport`.
Ordinary applications do not normally need these to compile.

[!](tree-deps.svg)
