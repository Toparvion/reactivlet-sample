# ReactivLet Demo :tv:

Reactivlet (“reactive” + “servlet”) is a sample Java project showcasing some techniques and solutions enabling servlet logic to run in reactive environment with minimal to no changes. This may be helpful to facilitate the smooth transition from traditional servlet stack to reactive one.

The sample accompanies the [article](https://toparvion.pro/post/2021/reactivlet/) (in Russian only) describing the approach in detail.

## Project contents

The sample is a Gradle multi-project application consisting of three sub-projects:

* `webmvc` – a simple Spring WebMVC (servlet) application providing the only REST API method:

  * `GET /{temlpate}` – proxies the request to `template` URI of the Wiremock mock server (see below); 

* `webflux`   – a Spring WebFlux (reactive) application providing the following REST API methods:

  * `GET /reactive/{template}` proxies the `template` query to Wiremock by means of reactive WebClient;
  * `GET /sync/{template}` proxies the `template` query to Wiremock by means of servlet RestTemplate;
  * `GET /feign/{template}` proxies the `template` query to Wiremock by means of OpenFeign client;

* `shared` – an internal component (a kind of library) shared between both web applications as a dependency. Not suitable for standalone running but when included into a web application provides it with a REST API method:

  * `GET /inspect` – returns various data extracted from the request like cookies, headers, URI parameters and attributes.

  Beside this method, the `shared` component also enriches the web applications with some additional infrastructure facilities:

  * logging of arguments and the results of each `@GetMapping`-annotated method;
  * setting `rid` MDC mark in logs;
  * providing consistent access to current HTTP request by means of its `HttpRequestAccessor` class (much like `RequestContextHolder` did in Spring WebMVC).

* `wiremock` – standalone [Wiremock](http://wiremock.org/) distribution serving as a target for proxying request through the sample web application. Supports two replies (declared in `wiremock/mappings` directory):

  * `/fast` – replies immediately with body `{"responder":"wiremock","mode":"FAST"}`
  * `/slow` – replies with 2 seconds delay and body `{"responder":"wiremock","mode":"SLOW"}`


## Usage

**Prerequisite**: [Install](https://bell-sw.com/pages/downloads/#/java-17-lts%20/%20current) JDK **17** or newer version.

1. Check out the repository to a local directory:

   ```shell
   $ git clone https://github.com/Toparvion/reactivlet-sample.git
   $ cd reactivlet-sample
   ```

2. Launch Wiremock as a target for the web applications to proxy incoming requests:

   ```sh
   $ ./gradlew :wiremock
   ```

   This will stay active until you stop the mock server with `Ctrl+C`.

3. Open another terminal and start WebMVC application:

   ```sh
   $ ./gradlew :webmvc:bootRun
   ```

   This will stay active until you stop the application with `Ctrl+C`.
   By default the application listens to port `8080` but you can change it by adding `--server.port=XXXX` argument.

4. Open one more terminal and start WebFlux application:

   ```sh
   $ gradlew :webflux:bootRun
   ```

   This will stay active until you stop the application with `Ctrl+C`.
   By default the application listens to port `8081` but you can change it by adding `--server.port=XXXX` argument.

5. Keep practicing in opening new terminals and make some requests to the running applications:

   ```sh
   # webmvc application
   $ curl -X GET --location "http://localhost:8080/fast?rid=123"
   $ curl -X GET --location "http://localhost:8080/slow?rid=123"
   $ curl -X GET --location "http://localhost:8080/inspect?rid=123&sid=abc&rid=567" \
       -H "Accept: application/json" \
       -H "Cookie: jid=ABC; cookie2=val2"
   
   # webflux application
   $ curl -X GET --location "http://localhost:8081/reactive/fast?rid=123"
   $ curl -X GET --location "http://localhost:8081/sync/fast?rid=123"
   $ curl -X GET --location "http://localhost:8081/feign/fast?rid=123"
   $ curl -X GET --location "http://localhost:8081/inspect?rid=123&sid=abc&rid=567" \
       -H "Accept: application/json" \
       -H "Cookie: jid=ABC; cookie2=val2"
   ```

   Note that every request mirrors its data in logs of corresponding web application as well as Wiremock console (except of `/inspect` request). 

