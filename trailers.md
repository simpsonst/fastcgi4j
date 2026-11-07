# Trailers

FastCGI/1.0 does not support request or response trailers, but HTTP/1.1 does through the use of `chunked` transfer encoding, and HTTP/2 does through its framing.
In anticipation of FastCGI being extended to handle trailers, the FastCGI4j API supports trailers, allowing applications to detect when the feature is available.

A request trailer would be realised as a second `FCGI_PARAMS` stream, following the first (that contains the CGI parameters) and the `FCGI_STDIN` stream.
As a trailer-capable application doesn't know whether the server will send a request trailer, there needs to be a signal in a prior message for the request to indicate this.
Without it, the application would have to wait an uncertain period of time after the close of `FCGI_STDIN` before giving up.

Conversely, if a trailer-capable server were to send a second `FCGI_PARAMS` to a trailer-incapable application, the application has no reason to protect itself from such an event, and might break.
Therefore, there must be a mechanism for a capable server to detect an incapable application, and not send the trailer.

A response trailer would be realised as another `FCGI_PARAMS` stream, but travelling from application to server (after `FCGI_STDOUT`, but before `FCGI_END_REQUEST`).
As a trailer-incapable server would not expect this, and at best would ignore it, the application needs to know early on whether a response trailer would even be allowed.
Meanwhile, the server does not need to know beforehand whether the application will send a response trailer, as it will find out soon as it receives `FCGI_END_REQUEST`.

For both response and request, a trailer expressed in a `FCGI_PARAMS` stream is treated as a sequence of key-value pairs, with possible duplicate keys.
The keys are field names of the server protocol (e.g., HTTP), and are not translated in the way that header fields are (by upper-casing, swapping underscores for dashes, or prefixing).
The keys should be considered case-insensitive.
The order of different keys has no significance, but the ordering of two fields with the same key must be preserved.


## Detecting trailer capability in the application

The `FCGI_GET_VALUES` and `FCGI_GET_VALUES_RESULT` exchange ([FastCGI &sect;4.1](https://fastcgi-archives.github.io/FastCGI_Specification.html#S4.1)) allows the server to probe the application's capabilities by asking for arbitrary variables from the application.
Requests for variables that the application does not understand are to be ignored, so new variables can be defined with backward compatibility, provided they ask about *new* capabilities.

A variable `FCGI_FIELD_HANDLING` could be defined to ask the application what its field capabilities are.
For future expansion, the value returned should be a comma-separated list of tokens (or perhaps both tokens and `token=value` pairs), listing the application's field-related capabilities.
For the purpose of trailer interactions, only one token needs to be defined, specifically `request-trailer`.
If the application does not return this token, the server assumes that the application is incapable, and does not transmit the request trailer, nor declare that it is forthcoming.


## Declaring a request trailer

The `FCGI_BEGIN_REQUEST` record ([FastCGI &sect;5.1](https://fastcgi-archives.github.io/FastCGI_Specification.html#S5.1)) has a small number of unused flags (7 out of 8 in total), and unused flags must be clear.
A single bit `FCGI_EXPECT_TRAILER` here could indicate that the application should expect a second `FCGI_PARAMS` stream after `FCGI_STDIN` has closed.
The server *must not* set `FCGI_EXPECT_TRAILER` unless the application has responded to the capability variable `FCGI_FIELD_HANDLING` with the token `request-trailer`.
The server *must* send a second `FCGI_PARAMS` stream if it has set `FCGI_EXPECT_TRAILER`, even if the request trailer is empty or absent.

Alternatively (to avoid consuming precious unused flags), a CGI parameter (passed in the first `FCGI_PARAMS`) could be defined, namely `FCGI_FIELDS`, containing comma-separated tokens or token-value pairs.
The token `expect-trailer` fulfils the role of the flag `FCGI_EXPECT_TRAILER`, with the same constraints and obligations.


## Permitting a response trailer

Another flag in `FCGI_BEGIN_REQUEST` (say, `FCGI_SUPPLY_TRAILER`) could be set to indicate to the application that the server will leave open the possibility of sending a response trailer, and will only decide that it is not forthcoming when the `FCGI_END_REQUEST` record is received.
The application sends the trailer as an `FCGI_PARAMS` stream after closing `FCGI_STDOUT`, but before sending `FCGI_END_REQUEST`.
If it does not need to send a response trailer, it need not send the `FCGI_PARAMS` stream, as sending `FCGI_END_REQUEST` precludes any further records for the request.

Alternatively, as before, the CGI parameter `FCGI_FIELDS` could contain the token `supply-trailer` instead of allocating a flag.

## Example

A trailer-capable FastCGI server has already established that a FastCGI application is also trailer-capable, and receives an HTTP/1.1 request line and header, and decides it must go to that application.
It also works out whether a request trailer is expected (the `Trailer` header field is present), and whether the client will accept a response trailer (the `trailers` token is present in the `TE` header field).
Let's suppose that both are true.
It issues an `FCGI_BEGIN_REQUEST` with the `FCGI_EXPECT_TRAILER` and `FCGI_SUPPLY_TRAILER` flags (if flags are used), or it adds `FCGI_FIELDS` to the CGI parameters with the value `expect-trailer, supply-trailer`, and passes them in the first `FCGI_PARAMS` stream from server to application.
As it receives the request body, it transmits it to the application as the `FCGI_STDIN` stream.
It then receives the request trailer in full, and transmits it as another `FCGI_PARAMS` stream.

The application responds with an `FCGI_STDOUT` stream, which consists of the response header and the response body.
Having closed that stream, the application sends an `FCGI_PARAMS` stream with the response trailer, and then sends `FCGI_END_REQUEST`.

## Implementation

The current implementation defines bits 1 and 2 of the `FCGI_BEGIN_REQUEST` flags as `FCGI_EXPECT_TRAILER` and `FCGI_SUPPLY_TRAILER`, and would send and receive trailers if these bits were ever set.
The library also recognizes the `FCGI_FIELD_HANDLING` capability variable, and responds with `request-trailer`.
