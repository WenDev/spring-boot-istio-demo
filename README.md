# 使用Spring Boot+gRPC构建微服务并部署到Istio

作为`Service Mesh`和云原生技术的忠实拥护者，我却一直没有开发过Service Mesh的应用。正好最近受够了Spring Cloud的“折磨”，对Kubernetes也可以熟练使用了，而且网上几乎没有Spring Boot微服务部署到Istio的案例，我就开始考虑用Spring Boot写个微服务的Demo并且部署到Istio。项目本身不复杂，就是发送一个字符串并且返回一个字符串的最简单的Demo。

> 题外话：我本来是想用Spring MVC写的——因为周围有的同学不相信Spring MVC也可以开发微服务，但是Spring MVC的各种配置和依赖问题把我整的想吐，为了少掉几根头发，还是用了方便好用的Spring Boot。

**本项目的所有代码都上传到了GitHub，地址：**https://github.com/WenDev/spring-boot-istio-demo **如果有帮助的话不要吝啬你的Star和Fork呀，非常感谢～**

## 为什么要用Istio？

目前，对于Java技术栈来说，构建微服务的最佳选择是`Spring Boot`而Spring Boot一般搭配目前落地案例很多的微服务框架`Spring Cloud`来使用。

Spring Cloud看似很完美，但是在实际上手开发后，很容易就会发现Spring Cloud存在以下比较严重的问题：

- 服务治理相关的逻辑存在于Spring Cloud Netflix等SDK中，与业务代码紧密耦合。
- SDK对业务代码侵入太大，SDK发生升级且无法向下兼容时，业务代码必须做出改变以适配SDK的升级——即使业务逻辑并没有发生任何变化。
- 各种组件令人眼花缭乱，质量也参差不齐，学习成本太高，且组件之间代码很难完全复用，仅仅为了实现治理逻辑而学习SDK也并不是很好的选择。
- 绑定于Java技术栈，虽然可以接入其他语言但要手动实现服务治理相关的逻辑，不符合微服务“可以用多种语言进行开发”的原则。
- Spring Cloud仅仅是一个开发框架，没有实现微服务所必须的服务调度、资源分配等功能，这些需求要借助Kubernetes等平台来完成。但Spring Cloud与Kubernetes功能上有重合，且部分功能也存在冲突，二者很难完美配合。

替代Spring Cloud的选择有没有呢？有！它就是`Istio`。

Istio彻底把治理逻辑从业务代码中剥离出来，成为了独立的进程（Sidecar）。部署时两者部署在一起，在一个Pod里共同运行，业务代码完全感知不到Sidecar的存在。这就实现了治理逻辑对业务代码的零侵入——实际上不仅是代码没有侵入，在运行时两者也没有任何的耦合。这使得不同的微服务完全可以使用不同语言、不同技术栈来开发，也不用担心服务治理问题，可以说这是一种很优雅的解决方案了。

所以，“为什么要使用Istio”这个问题也就迎刃而解了——因为Istio解决了传统微服务诸如业务逻辑与服务治理逻辑耦合、不能很好地实现跨语言等痛点，而且非常容易使用。只要会用Kubernetes，学习Istio的使用一点都不困难。

## 为什么要使用gRPC作为通信框架？

在微服务架构中，服务之间的通信是一个比较大的问题，一般采用RPC或者RESTful API来实现。

Spring Boot可以使用`RestTemplate`调用远程服务，但这种方式不直观，代码也比较复杂，进行跨语言通信也是个比较大的问题；而`gRPC`相比Dubbo等常见的Java RPC框架更加轻量，使用起来也很方便，代码可读性高，并且与Istio和Kubernetes可以很好地进行整合，在Protobuf和HTTP2的加持下性能也还不错，所以这次选择了gRPC来解决Spring Boot微服务间通信的问题。并且，虽然gRPC没有服务发现、负载均衡等能力，但是Istio在这方面就非常强大，两者形成了完美的互补关系。

由于考虑到各种`grpc-spring-boot-starter`可能会对Spring Boot与Istio的整合产生不可知的副作用，所以这一次我没有用任何的`grpc-spring-boot-starter`，而是直接手写了gRPC与Spring Boot的整合。不想借助第三方框架整合gRPC和Spring Boot的可以简单参考一下我的实现。

## 编写业务代码

首先使用`Spring Initializr`建立父级项目`spring-boot-istio`，并引入`gRPC`的依赖。pom文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <modules>
        <module>spring-boot-istio-api</module>
        <module>spring-boot-istio-server</module>
        <module>spring-boot-istio-client</module>
    </modules>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.6.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>site.wendev</groupId>
    <artifactId>spring-boot-istio</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>spring-boot-istio</name>
    <description>Demo project for Spring Boot With Istio.</description>
    <packaging>pom</packaging>

    <properties>
        <java.version>1.8</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.grpc</groupId>
                <artifactId>grpc-all</artifactId>
                <version>1.28.1</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>

```

然后建立公共依赖模块`spring-boot-istio-api`，pom文件如下，主要就是gRPC的一些依赖：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <artifactId>spring-boot-istio</artifactId>
        <groupId>site.wendev</groupId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>spring-boot-istio-api</artifactId>

    <dependencies>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-all</artifactId>
        </dependency>
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>javax.annotation-api</artifactId>
            <version>1.3.2</version>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.6.2</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.1</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.11.3:exe:${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.28.1:exe:${os.detected.classifier}</pluginArtifact>
                    <protocExecutable>/Users/jiangwen/tools/protoc-3.11.3/bin/protoc</protocExecutable>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

建立src/main/proto文件夹，在此文件夹下建立`hello.proto`，定义服务间的接口如下：

```protobuf
syntax = "proto3";

option java_package = "site.wendev.spring.boot.istio.api";
option java_outer_classname = "HelloWorldService";

package helloworld;

service HelloWorld {
    rpc SayHello (HelloRequest) returns (HelloResponse) {}
}

message HelloRequest {
    string name = 1;
}

message HelloResponse {
    string message = 1;
}

```

很简单，就是发送一个`name`返回一个带`name`的`message`。

然后生成服务端和客户端的代码，并且放到java文件夹下。这部分内容可以参考gRPC的官方文档。

有了API模块之后，就可以编写服务提供者（服务端）和服务消费者（客户端）了。这里我们重点看一下如何整合gRPC和Spring Boot。

### 服务端

业务代码非常简单：

```java
/**
 * 服务端业务逻辑实现
 *
 * @author 江文
 * @date 2020/4/12 2:49 下午
 */
@Slf4j
@Component
public class HelloServiceImpl extends HelloWorldGrpc.HelloWorldImplBase {
    @Override
    public void sayHello(HelloWorldService.HelloRequest request,
                         StreamObserver<HelloWorldService.HelloResponse> responseObserver) {
        // 根据请求对象建立响应对象，返回响应信息
        HelloWorldService.HelloResponse response = HelloWorldService.HelloResponse
                .newBuilder()
                .setMessage(String.format("Hello, %s. This message comes from gRPC.", request.getName()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        log.info("Client Message Received：[{}]", request.getName());
    }
}

```

光有业务代码还不行，我们还需要在应用启动时把gRPC Server也给一起启动起来。首先写一下Server端的启动、关闭等逻辑：

```java
/**
 * gRPC Server的配置——启动、关闭等
 * 需要使用<code>@Component</code>注解注册为一个Spring Bean
 *
 * @author 江文
 * @date 2020/4/12 2:56 下午
 */
@Slf4j
@Component
public class GrpcServerConfiguration {
    @Autowired
    HelloServiceImpl service;

    /** 注入配置文件中的端口信息 */
    @Value("${grpc.server-port}")
    private int port;
    private Server server;

    public void start() throws IOException {
        // 构建服务端
        log.info("Starting gRPC on port {}.", port);
        server = ServerBuilder.forPort(port).addService(service).build().start();
        log.info("gRPC server started, listening on {}.", port);

        // 添加服务端关闭的逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server.");
            GrpcServerConfiguration.this.stop();
            log.info("gRPC server shut down successfully.");
        }));
    }

    private void stop() {
        if (server != null) {
            // 关闭服务端
            server.shutdown();
        }
    }

    public void block() throws InterruptedException {
        if (server != null) {
            // 服务端启动后直到应用关闭都处于阻塞状态，方便接收请求
            server.awaitTermination();
        }
    }
}

```

定义好gRPC的启动、停止等逻辑后，就可以使用`CommandLineRunner`把它加入到Spring Boot的启动中去了：

```java
/**
 * 加入gRPC Server的启动、停止等逻辑到Spring Boot的生命周期中
 *
 * @author 江文
 * @date 2020/4/12 3:10 下午
 */
@Component
public class GrpcCommandLineRunner implements CommandLineRunner {
    @Autowired
    GrpcServerConfiguration configuration;

    @Override
    public void run(String... args) throws Exception {
        configuration.start();
        configuration.block();
    }
}

```

之所以要把gRPC的逻辑注册成Spring Bean，就是因为在这里要获取到它的实例并进行相应的操作。

这样，在启动Spring Boot时，由于CommandLineRunner的存在，gRPC服务端也就可以一同启动了。

### 客户端

业务代码同样非常简单：

```java
/**
 * 客户端业务逻辑实现
 *
 * @author 江文
 * @date 2020/4/12 3:26 下午
 */
@RestController
@Slf4j
public class HelloController {
    @Autowired
    GrpcClientConfiguration configuration;

    @GetMapping("/hello")
    public String hello(@RequestParam(name = "name", defaultValue = "JiangWen", required = false) String name) {
        // 构建一个请求
        HelloWorldService.HelloRequest request = HelloWorldService.HelloRequest
                .newBuilder()
                .setName(name)
                .build();

        // 使用stub发送请求至服务端
        HelloWorldService.HelloResponse response = configuration.getStub().sayHello(request);
        log.info("Server response received: [{}]", response.getMessage());
        return response.getMessage();
    }
}

```

在启动客户端时，我们需要打开gRPC的客户端，并获取到`channel`和`stub`以进行RPC通信，来看看gRPC客户端的实现逻辑：

```java
/**
 * gRPC Client的配置——启动、建立channel、获取stub、关闭等
 * 需要注册为Spring Bean
 *
 * @author 江文
 * @date 2020/4/12 3:27 下午
 */
@Slf4j
@Component
public class GrpcClientConfiguration {
    /** gRPC Server的地址 */
    @Value("${server-host}")
    private String host;

    /** gRPC Server的端口 */
    @Value("${server-port}")
    private int port;

    private ManagedChannel channel;
    private HelloWorldGrpc.HelloWorldBlockingStub stub;

    public void start() {
        // 开启channel
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        // 通过channel获取到服务端的stub
        stub = HelloWorldGrpc.newBlockingStub(channel);
        log.info("gRPC client started, server address: {}:{}", host, port);
    }

    public void shutdown() throws InterruptedException {
        // 调用shutdown方法后等待1秒关闭channel
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        log.info("gRPC client shut down successfully.");
    }

    public HelloWorldGrpc.HelloWorldBlockingStub getStub() {
        return this.stub;
    }
}

```

比服务端要简单一些。

最后，仍然需要一个CommandLineRunner把这些启动逻辑加入到Spring Boot的启动过程中：

```java
/**
 * 加入gRPC Client的启动、停止等逻辑到Spring Boot生命周期中
 *
 * @author 江文
 * @date 2020/4/12 3:36 下午
 */
@Component
@Slf4j
public class GrpcClientCommandLineRunner implements CommandLineRunner {
    @Autowired
    GrpcClientConfiguration configuration;

    @Override
    public void run(String... args) {
        // 开启gRPC客户端
        configuration.start();
        
        // 添加客户端关闭的逻辑
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                configuration.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }
}

```

## 编写Dockerfile

业务代码跑通之后，就可以制作Docker镜像，准备部署到Istio中去了。

在开始编写Dockerfile之前，先改动一下客户端的配置文件：

```yaml
server:
  port: 19090
spring:
  application:
    name: spring-boot-istio-client
server-host: ${server-host}
server-port: ${server-port}

```

接下来编写Dockerfile：

服务端：

```dockerfile
FROM openjdk:8u121-jdk
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' >/etc/timezone
ADD /target/spring-boot-istio-server-0.0.1-SNAPSHOT.jar /
ENV SERVER_PORT="18080"
ENTRYPOINT java -jar /spring-boot-istio-server-0.0.1-SNAPSHOT.jar

```

主要是规定服务端应用的端口为18080，并且在容器启动时让服务端也一起启动。

客户端：

```dockerfile
FROM openjdk:8u121-jdk
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
  && echo 'Asia/Shanghai' >/etc/timezone
ADD /target/spring-boot-istio-client-0.0.1-SNAPSHOT.jar /
ENV GRPC_SERVER_HOST="spring-boot-istio-server"
ENV GRPC_SERVER_PORT="18888"
ENTRYPOINT java -jar /spring-boot-istio-client-0.0.1-SNAPSHOT.jar \
 --server-host=$GRPC_SERVER_HOST \
 --server-port=$GRPC_SERVER_PORT
 
```

可以看到这里添加了启动参数，配合前面的配置，当这个镜像部署到Kubernetes集群时，就可以在Kubernetes的配合之下通过服务名找到服务端了。

同时，服务端和客户端的pom文件中添加：

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <executable>true</executable>
                </configuration>
            </plugin>
            <plugin>
                <groupId>com.spotify</groupId>
                <artifactId>dockerfile-maven-plugin</artifactId>
                <version>1.4.13</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.activation</groupId>
                        <artifactId>activation</artifactId>
                        <version>1.1</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <id>default</id>
                        <goals>
                            <goal>build</goal>
                            <goal>push</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <repository>wendev-docker.pkg.coding.net/develop/docker/${project.artifactId}
                    </repository>
                    <tag>${project.version}</tag>
                    <buildArgs>
                        <JAR_FILE>${project.build.finalName}.jar</JAR_FILE>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

这样执行`mvn clean package`时就可以同时把docker镜像构建出来了。

## 编写部署文件

有了镜像之后，就可以写部署文件了：

服务端：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-istio-server
spec:
  type: ClusterIP
  ports:
    - name: http
      port: 18080
      targetPort: 18080
    - name: grpc
      port: 18888
      targetPort: 18888
  selector:
    app: spring-boot-istio-server
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-istio-server
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-boot-istio-server
  template:
    metadata:
      labels:
        app: spring-boot-istio-server
    spec:
      containers:
        - name: spring-boot-istio-server
          image: wendev-docker.pkg.coding.net/develop/docker/spring-boot-istio-server:0.0.1-SNAPSHOT
          imagePullPolicy: Always
          tty: true
          ports:
            - name: http
              protocol: TCP
              containerPort: 18080
            - name: grpc
              protocol: TCP
              containerPort: 18888

```

主要是暴露服务端的端口：18080和gRPC Server的端口18888，以便可以从Pod外部访问服务端。

客户端：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-istio-client
spec:
  type: ClusterIP
  ports:
    - name: http
      port: 19090
      targetPort: 19090
  selector:
    app: spring-boot-istio-client
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-istio-client
spec:
  replicas: 1
  selector:
    matchLabels:
      app: spring-boot-istio-client
  template:
    metadata:
      labels:
        app: spring-boot-istio-client
    spec:
      containers:
        - name: spring-boot-istio-client
          image: wendev-docker.pkg.coding.net/develop/docker/spring-boot-istio-client:0.0.1-SNAPSHOT
          imagePullPolicy: Always
          tty: true
          ports:
            - name: http
              protocol: TCP
              containerPort: 19090

```

主要是暴露客户端的端口19090，以便访问客户端并调用服务端。

如果想先试试把它们部署到k8s可不可以正常访问，可以这样配置Ingress：

```yaml
apiVersion: networking.k8s.io/v1beta1
kind: Ingress
metadata:
  name: nginx-web
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/use-reges: "true"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "600"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
    - host: dev.wendev.site
      http:
        paths:
          - path: /
            backend:
              serviceName: spring-boot-istio-client
              servicePort: 19090
```

Istio的网关配置文件与k8s不大一样：

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: Gateway
metadata:
  name: spring-boot-istio-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
    - port:
        number: 80
        name: http
        protocol: HTTP
      hosts:
        - "*"
---
apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: spring-boot-istio
spec:
  hosts:
    - "*"
  gateways:
    - spring-boot-istio-gateway
  http:
    - match:
        - uri:
            exact: /hello
      route:
        - destination:
            host: spring-boot-istio-client
            port:
              number: 19090

```

主要就是暴露`/hello`这个路径，并且指定对应的服务和端口。

## 部署应用到Istio

首先搭建k8s集群并且安装istio。我使用的k8s版本是`1.16.0`，Istio版本是最新的`1.6.0-alpha.1`，使用`istioctl`命令安装Istio。建议跑通官方的`bookinfo`示例之后再来部署本项目。

注：以下命令都是在开启了自动注入Sidecar的前提下运行的

我是在虚拟机中运行的k8s，所以`istio-ingressgateway`没有外部ip：

```bash
$ kubectl get svc istio-ingressgateway -n istio-system
NAME                   TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)                                                                                                                                      AGE
istio-ingressgateway   NodePort   10.97.158.232   <none>        15020:30388/TCP,80:31690/TCP,443:31493/TCP,15029:32182/TCP,15030:31724/TCP,15031:30887/TCP,15032:30369/TCP,31400:31122/TCP,15443:31545/TCP   26h
```

所以，需要设置IP和端口，以NodePort的方式访问gateway：

```bash
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
export SECURE_INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}')
export INGRESS_HOST=127.0.0.1
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT
```

这样就可以了。

接下来部署服务：

```bash
$ kubectl apply -f spring-boot-istio-server.yml
$ kubectl apply -f spring-boot-istio-client.yml
$ kubectl apply -f istio-gateway.yml
```

必须要等到两个pod全部变为Running而且Ready变为2/2才算部署完成。

接下来就可以通过

```bash
curl -s http://${GATEWAY_URL}/hello
```

访问到服务了。如果成功返回了`Hello, JiangWen. This message comes from gRPC.`的结果，没有出错则说明部署完成。

