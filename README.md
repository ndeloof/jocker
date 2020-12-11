![ci status](https://github.com/ndeloof/jocker/workflows/Continuous%20integration/badge.svg)

# Jocker, a Java client library for Docker API

Jocker is (yet another) Docker Client library to access Docker API.

![logo](jocker.png) 

Primary goals are to "_keep it simple stupid_" and avoid too many dependencies on third-party libraries

## Limited dependencies

Most docker client libraries rely on other libs, typically Rest frameworks like [Jersey](https://jersey.github.io/) 
or a full featured HTTP client like [Apache Http Client](https://hc.apache.org/) or [Netty](http://netty.io/). 

Jocker was initialy designed in the context of [Jenkins Docker plugin](https://wiki.jenkins.io/display/JENKINS/Docker+Plugin) 
development. Third party dependencies are constrained in Jenkins by core dependencies, introducing various classpath
issues.

Also, such full-features HTTP client libraries demonstrated to have issues supporting some uncommon HTTP usage in 
docker APi, like Hijacked HTTP connection to set a bidirectional stdin/stdout multiplexed stream in interactive mode, 
or support for `/var/run/docker.sock` Unix Domain Socket.

For JSON (un)marshalling we rely on [Google Gson](https://github.com/google/gson) as a tiny (standalone), simple and 
efficient JSON library.   


## KISS (Keep It Simple Stupid)

API model is generated from the [Docker official OpenAPI specification](https://docs.docker.com/engine/api/v1.40/#).
Some pull-requests have been made to help improve this API spec and ensure we get a clean model generated.
For the few corner cases where the generated model doesn't offer a nice API, we maintain some dedicated model classes. 

Jocker implement HTTP as plain text over a `java.net.Socket`. This allows transparent support for Unix Domain Socket 
thanks to [junixsocket](https://libraries.io/github/fiken/junixsocket). Also can benefit java Channels for non-blocking 
I/Os without much efforts.  

We don't claim to offer a full featured HTTP client, just implemented what's required for a Docker API server. HTTP, 
a plain value protocol, is easy to debug and to implement, with only some limited features required by Docker API.
The HTTP client implementation is about ~100 lines of code. Doing so, we have full control on HTTP frames and headers
over transport, and typically can implement HTTP connection Hijack without any hack.

## License

Licensed under [BSD](https://opensource.org/licenses/BSD-3-Clause)
Copyright 2017 Nicolas De Loof, Docker Inc.

**tl;dr:** You're free to use this code, make any changes you need, have fun with it. Contributions are welcome if 
you do something you consider useful :P


## Future plans

- [ ] implement _all_ APIs
- [ ] a fluent client for those who prefer this programming model
- [ ] a swagger codegen template so we generate a plain json-P parser and don't need Gson (code is generated, not intended to be edited)
- [X] use java.nio Channels and jnr-unixsocket to rely on non-blocking I/O
- [ ] conquer the world


## How about Docker command line? 

Jocker is desgined as a Docker API client, which for many commands is more-or-less
equivalent with the `docker` command line verbs and flags, with a significant exception
for `docker run`. 

For demonstration purpose, Jocker do include `com.docker.jocker.cli` package which is
not intended to be used for anything but experiments and demonstration. Still you can 
read this code and understand how to fill the gap between the command line you know and
the actual API calls.

If you want to run Jocker as a "command line" demo, just:

1. build the command line archive: `mvn compile assembly:single`
1. create an alias: `alias jocker="java -jar target/jocker-0.1-SNAPSHOT-jar-with-dependencies.jar"`
1. enjoy your new docker CLI :P
```console
➜  jocker run -i --rm --name jocker alpine
echo hello $HOSTNAME
hello jocker
```



## Supported APIs :

### Missing something ?

If you miss _some_ API support in following list, please consider contributing. In most cases, this is just a
question of implementing few lines of code. API type is already generated from docker's swagger API contract, 
so you only have to implement the method invocation based on API documentation. 

```java
public SomeType apiMethod(String param) {
    StringBuilder path = new StringBuilder("/v").append(version).append("/some/api?param=").append(param);
    Response r = doGET(path.toString());
    return gson.fromJson(r.getBody(), SomeType.class);
}

```

### General purpose 

  - [x] [SystemInfo](https://docs.docker.com/engine/api/v1.40/#operation/SystemInfo)

### Containers 
  
  - [x] [ContainerList](https://docs.docker.com/engine/api/v1.40/#operation/ContainerList)
  - [x] [ContainerCreate](https://docs.docker.com/engine/api/v1.40/#operation/ContainerCreate)
  - [x] [ContainerInspect](https://docs.docker.com/engine/api/v1.40/#operation/ContainerInspect)
  - [ ] [ContainerTop](https://docs.docker.com/engine/api/v1.40/#operation/ContainerTop)
  - [x] [ContainerLogs](https://docs.docker.com/engine/api/v1.40/#operation/ContainerLogs)
  - [ ] [ContainerChanges](https://docs.docker.com/engine/api/v1.40/#operation/ContainerChanges)
  - [ ] [ContainerExport](https://docs.docker.com/engine/api/v1.40/#operation/ContainerExport)
  - [ ] [ContainerStats](https://docs.docker.com/engine/api/v1.40/#operation/ContainerStats)
  - [x] [ContainerResize](https://docs.docker.com/engine/api/v1.40/#operation/ContainerResize)
  - [x] [ContainerStart](https://docs.docker.com/engine/api/v1.40/#operation/ContainerStart)
  - [x] [ContainerStop](https://docs.docker.com/engine/api/v1.40/#operation/ContainerStop)
  - [x] [ContainerRestart](https://docs.docker.com/engine/api/v1.40/#operation/ContainerRestart)
  - [x] [ContainerKill](https://docs.docker.com/engine/api/v1.40/#operation/ContainerKill)
  - [ ] [ContainerUpdate](https://docs.docker.com/engine/api/v1.40/#operation/ContainerUpdate)
  - [x] [ContainerRename](https://docs.docker.com/engine/api/v1.40/#operation/ContainerRename)
  - [x] [ContainerPause](https://docs.docker.com/engine/api/v1.40/#operation/ContainerPause)
  - [x] [ContainerUnpause](https://docs.docker.com/engine/api/v1.40/#operation/ContainerUnpause)
  - [x] [ContainerAttach](https://docs.docker.com/engine/api/v1.40/#operation/ContainerAttach)
  - [ ] [ContainerAttachWebsocket](https://docs.docker.com/engine/api/v1.40/#operation/ContainerAttachWebsocket)
  - [x] [ContainerWait](https://docs.docker.com/engine/api/v1.40/#operation/ContainerWait)
  - [x] [ContainerDelete](https://docs.docker.com/engine/api/v1.40/#operation/ContainerDelete)
  - [x] [ContainerArchiveInfo](https://docs.docker.com/engine/api/v1.40/#operation/ContainerArchiveInfo)
  - [x] [ContainerArchive](https://docs.docker.com/engine/api/v1.40/#operation/ContainerArchive)
  - [x] [PutContainerArchive](https://docs.docker.com/engine/api/v1.40/#operation/PutContainerArchive)
  - [x] [ContainerPrune](https://docs.docker.com/engine/api/v1.40/#operation/ContainerPrune)
  - [x] [ContainerAttach](https://docs.docker.com/engine/api/v1.40/#operation/ContainerAttach)
         
### Images

  - [ ] [ImageList](https://docs.docker.com/engine/api/v1.40/#operation/ImageList)
  - [x] [ImageBuild](https://docs.docker.com/engine/api/v1.40/#operation/ImageBuild)
  - [ ] [BuildPrune](https://docs.docker.com/engine/api/v1.40/#operation/BuildPrune)
  - [x] [ImageCreate](https://docs.docker.com/engine/api/v1.40/#operation/ImageCreate) (aka "pull")
  - [x] [ImageInspect](https://docs.docker.com/engine/api/v1.40/#operation/ImageInspect)
  - [ ] [ImageHistory](https://docs.docker.com/engine/api/v1.40/#operation/ImageHistory)
  - [x] [ImagePush](https://docs.docker.com/engine/api/v1.40/#operation/ImagePush)
  - [ ] [ImageTag](https://docs.docker.com/engine/api/v1.40/#operation/ImageTag)
  - [ ] [ImageDelete](https://docs.docker.com/engine/api/v1.40/#operation/ImageDelete)
  - [ ] [ImageSearch](https://docs.docker.com/engine/api/v1.40/#operation/ImageSearch)
  - [ ] [ImagePrune](https://docs.docker.com/engine/api/v1.40/#operation/ImagePrune)
  - [ ] [ImageCommit](https://docs.docker.com/engine/api/v1.40/#operation/ImageCommit)
  - [ ] [ImageGet](https://docs.docker.com/engine/api/v1.40/#operation/ImageGet)
  - [ ] [ImageGetAll](https://docs.docker.com/engine/api/v1.40/#operation/ImageGetAll)
  - [ ] [ImageLoad](https://docs.docker.com/engine/api/v1.40/#operation/ImageLoad)

### Exec

  - [x] [ContainerExec](https://docs.docker.com/engine/api/v1.40/#operation/ContainerExec)
  - [x] [ExecStart](https://docs.docker.com/engine/api/v1.40/#operation/ExecStart)
  - [ ] [ExecResize](https://docs.docker.com/engine/api/v1.40/#operation/ExecResize)
  - [x] [ExecInspect](https://docs.docker.com/engine/api/v1.40/#operation/ExecInspect)


### Networks 

  - [x] [NetworkList](https://docs.docker.com/engine/api/v1.40/#operation/NetworkList)
  - [x] [NetworkInspect](https://docs.docker.com/engine/api/v1.40/#operation/NetworkInspect)
  - [x] [NetworkDelete](https://docs.docker.com/engine/api/v1.40/#operation/NetworkDelete)
  - [x] [NetworkCreate](https://docs.docker.com/engine/api/v1.40/#operation/NetworkCreate)
  - [x] [NetworkConnect](https://docs.docker.com/engine/api/v1.40/#operation/NetworkConnect)
  - [x] [NetworkDisconnect](https://docs.docker.com/engine/api/v1.40/#operation/NetworkDisconnect)
  - [ ] [NetworkPrune](https://docs.docker.com/engine/api/v1.40/#operation/NetworkPrune)

### Volumes 

  - [x] [VolumeList](https://docs.docker.com/engine/api/v1.40/#operation/VolumeList)
  - [x] [VolumeCreate](https://docs.docker.com/engine/api/v1.40/#operation/VolumeCreate)
  - [x] [VolumeInspect](https://docs.docker.com/engine/api/v1.40/#operation/VolumeInspect)
  - [x] [VolumeDelete](https://docs.docker.com/engine/api/v1.40/#operation/VolumeDelete)
  - [ ] [VolumePrune](https://docs.docker.com/engine/api/v1.40/#operation/VolumePrune)
  
### Docker Swarm support

There's no short term plan to implement swarm related APIs (Nodes, Services, Tasks, Secrets, Configs)
But feel free to contribute if you consider this a usefull addition.


## Debug / Reverse engineering

There's few places where the docker API is not well documented, for sample 
[ImageBuild](https://docs.docker.com/engine/api/v1.40/#operation/ImageBuild) operation documentation doesn't
tell us much about the outputstream. In such circumstances we will have to reverse-engineer the docker API. 
Two (complementary) options here :

1. read [source code](https://github.com/moby/moby/tree/master/api/server). If you're not familiar with Go 
this might be a bit challenging, but one learns a lot about the API looking at this.
1. analyze HTTP traffic produced by docker CLI. For this purpose I use `dockins/dockersock` docker image to 
expose my docker4mac socket in plain HTTP :

`docker run -it -v /var/run/docker.sock:/var/run/docker.sock -p 2375:2375 dockins/dockersock`

Then I use [Wireshark](https://www.wireshark.org/) with filter `tcp.port==2375` to capture HTTP frames sent by client 
and daemon response. 


 
