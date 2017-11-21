# Jocker, a Java client library for Docker API

Jocker is (yet another) Docker Client library to access Docker API. 

Primary goals are to "_keep it simple stupid_" and avoid dependency on third-party libraries

## Limited dependencies

Most docker client libraries rely on other libs, typically Rest frameworks like [Jersey](https://jersey.github.io/) 
or a full featured HTTP client like [Apache Http Client](https://hc.apache.org/) or [Netty](http://netty.io/). 

Jocker has been created in the context of [Jenkins Docker plugin](https://wiki.jenkins.io/display/JENKINS/Docker+Plugin) 
development. Third party dependencies are constrained in Jenkins by core dependencies, introducing various classpath
issues.

Also, such helper libraries indeed demonstrated to have issues supporting some uncommon HTTP usage in docker APi, like 
Hijacked HTTP connection to attach stdin/stdout or the `/var/run/docker.sock` Unix Domain Socket.  

We directly implement HTTP over a `java.net.Socket`. As a plain text protocol with a limited use-case this in not such 
a big challenge (~100 lines of code). This allows transparent support for Unix Domain Socket thanks to 
[junixsocket](https://libraries.io/github/fiken/junixsocket).

Supported APIs :

  - [x] [SystemInfo](https://docs.docker.com/engine/api/v1.32/#operation/SystemInfo)
  - [x] [ContainerList](https://docs.docker.com/engine/api/v1.32/#operation/ContainerList)
  - [x] [ContainerInspect](https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect)
 