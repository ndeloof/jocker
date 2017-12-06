# Jocker, a Java client library for Docker API

Jocker is (yet another) Docker Client library to access Docker API.

![logo](jocker.png) 

Primary goals are to "_keep it simple stupid_" and avoid dependency on third-party libraries

## Limited dependencies

Most docker client libraries rely on other libs, typically Rest frameworks like [Jersey](https://jersey.github.io/) 
or a full featured HTTP client like [Apache Http Client](https://hc.apache.org/) or [Netty](http://netty.io/). 

Jocker has been created in the context of [Jenkins Docker plugin](https://wiki.jenkins.io/display/JENKINS/Docker+Plugin) 
development. Third party dependencies are constrained in Jenkins by core dependencies, introducing various classpath
issues.

Also, such helper libraries indeed demonstrated to have issues supporting some uncommon HTTP usage in docker APi, like 
Hijacked HTTP connection to attach stdin/stdout or the `/var/run/docker.sock` Unix Domain Socket.  

For JSON (un)marshalling we rely on [Google Gson](https://github.com/google/gson) as a tiny (standalone), simple and 
efficient JSON library. 


## KISS (Keep It Simple Stupid)

API model is generated from the [Docker official swagger specification](https://docs.docker.com/engine/api/v1.32/#).
For the few corner cases where the generated model doesn't offer a nice API, we maintain some dedicated model classes. 

We directly implement HTTP over a `java.net.Socket`. This allows transparent support for Unix Domain Socket thanks to 
[junixsocket](https://libraries.io/github/fiken/junixsocket).

We don't claim to offer a full featured HTTP client, just implemented what's required for a Docker API server. HTTP, 
a plain text protocol, is easy to debug and to implement, with only some limited features required by Docker API.
The HTTP client implementation is about ~100 lines of code. Doing so we have full control on how HTTP protocol is
used, and typically can implement HTTP connection Hijack without any hack.

## License

Licensed under [BSD](https://opensource.org/licenses/BSD-3-Clause)
Copyright 2017 Nicolas De Loof, CloudBees.

**tl;dr:** You're free to use this code, make any changes you need, have fun with it. Contributions are welcome if 
you do something you consider useful :P

## Supported APIs :

### General purpose 

  - [x] [SystemInfo](https://docs.docker.com/engine/api/v1.32/#operation/SystemInfo)

### Containers 
  
  - [x] [ContainerList](https://docs.docker.com/engine/api/v1.32/#operation/ContainerList)
  - [x] [ContainerCreate](https://docs.docker.com/engine/api/v1.32/#operation/ContainerCreate)
  - [x] [ContainerInspect](https://docs.docker.com/engine/api/v1.32/#operation/ContainerInspect)
  - [ ] [ContainerTop](https://docs.docker.com/engine/api/v1.32/#operation/ContainerTop)
  - [ ] [ContainerLogs](https://docs.docker.com/engine/api/v1.32/#operation/ContainerLogs)
  - [ ] [ContainerChanges](https://docs.docker.com/engine/api/v1.32/#operation/ContainerChanges)
  - [ ] [ContainerExport](https://docs.docker.com/engine/api/v1.32/#operation/ContainerExport)
  - [ ] [ContainerStats](https://docs.docker.com/engine/api/v1.32/#operation/ContainerStats)
  - [ ] [ContainerResize](https://docs.docker.com/engine/api/v1.32/#operation/ContainerResize)
  - [ ] [ContainerStart](https://docs.docker.com/engine/api/v1.32/#operation/ContainerStart)
  - [ ] [ContainerStop](https://docs.docker.com/engine/api/v1.32/#operation/ContainerStop)
  - [ ] [ContainerRestart](https://docs.docker.com/engine/api/v1.32/#operation/ContainerRestart)
  - [ ] [ContainerKill](https://docs.docker.com/engine/api/v1.32/#operation/ContainerKill)
  - [ ] [ContainerUpdate](https://docs.docker.com/engine/api/v1.32/#operation/ContainerUpdate)
  - [ ] [ContainerRename](https://docs.docker.com/engine/api/v1.32/#operation/ContainerRename)
  - [ ] [ContainerPause](https://docs.docker.com/engine/api/v1.32/#operation/ContainerPause)
  - [ ] [ContainerUnpause](https://docs.docker.com/engine/api/v1.32/#operation/ContainerUnpause)
  - [ ] [ContainerAttach](https://docs.docker.com/engine/api/v1.32/#operation/ContainerAttach)
  - [ ] [ContainerAttachWebsocket](https://docs.docker.com/engine/api/v1.32/#operation/ContainerAttachWebsocket)
  - [ ] [ContainerWait](https://docs.docker.com/engine/api/v1.32/#operation/ContainerWait)
  - [ ] [ContainerDelete](https://docs.docker.com/engine/api/v1.32/#operation/ContainerDelete)
  - [ ] [ContainerArchiveInfo](https://docs.docker.com/engine/api/v1.32/#operation/ContainerArchiveInfo)
  - [ ] [ContainerArchive](https://docs.docker.com/engine/api/v1.32/#operation/ContainerArchive)
  - [x] [PutContainerArchive](https://docs.docker.com/engine/api/v1.32/#operation/PutContainerArchive)
  - [ ] [ContainerPrune](https://docs.docker.com/engine/api/v1.32/#operation/ContainerPrune)
  - [ ] [ContainerAttach](https://docs.docker.com/engine/api/v1.32/#operation/ContainerAttach)
         
### Images

  - [ ] [ImageList](https://docs.docker.com/engine/api/v1.32/#operation/ImageList)
  - [ ] [ImageBuild](https://docs.docker.com/engine/api/v1.32/#operation/ImageBuild)
  - [ ] [BuildPrune](https://docs.docker.com/engine/api/v1.32/#operation/BuildPrune)
  - [x] [ImageCreate](https://docs.docker.com/engine/api/v1.32/#operation/ImageCreate) (aka "pull")
  - [x] [ImageInspect](https://docs.docker.com/engine/api/v1.32/#operation/ImageInspect)
  - [ ] [ImageHistory](https://docs.docker.com/engine/api/v1.32/#operation/ImageHistory)
  - [ ] [ImagePush](https://docs.docker.com/engine/api/v1.32/#operation/ImagePush)
  - [ ] [ImageTag](https://docs.docker.com/engine/api/v1.32/#operation/ImageTag)
  - [ ] [ImageDelete](https://docs.docker.com/engine/api/v1.32/#operation/ImageDelete)
  - [ ] [ImageSearch](https://docs.docker.com/engine/api/v1.32/#operation/ImageSearch)
  - [ ] [ImagePrune](https://docs.docker.com/engine/api/v1.32/#operation/ImagePrune)
  - [ ] [ImageCommit](https://docs.docker.com/engine/api/v1.32/#operation/ImageCommit)
  - [ ] [ImageGet](https://docs.docker.com/engine/api/v1.32/#operation/ImageGet)
  - [ ] [ImageGetAll](https://docs.docker.com/engine/api/v1.32/#operation/ImageGetAll)
  - [ ] [ImageLoad](https://docs.docker.com/engine/api/v1.32/#operation/ImageLoad)

### Exec

  - [x] [ContainerExec](https://docs.docker.com/engine/api/v1.32/#operation/ContainerExec)
  - [x] [ExecStart](https://docs.docker.com/engine/api/v1.32/#operation/ExecStart)
  - [ ] [ExecResize](https://docs.docker.com/engine/api/v1.32/#operation/ExecResize)
  - [x] [ExecInspect](https://docs.docker.com/engine/api/v1.32/#operation/ExecInspect)


### Networks 

TODO

### Volumes 

TODO

### Swarm 

TODO

### Nodes 

TODO

### Services 

TODO

### Tasks 

TODO

### Secrets 

TODO

### Plugins 

TODO

### Config 

TODO

