package site.wendev.spring.boot.istio.server.service;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.wendev.spring.boot.istio.client.api.HelloWorldGrpc;
import site.wendev.spring.boot.istio.client.api.HelloWorldService;

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
