package site.wendev.spring.boot.istio.client.configuration;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.wendev.spring.boot.istio.client.api.HelloWorldGrpc;

import java.util.concurrent.TimeUnit;

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
