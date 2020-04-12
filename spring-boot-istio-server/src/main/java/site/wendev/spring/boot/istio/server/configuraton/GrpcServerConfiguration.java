package site.wendev.spring.boot.istio.server.configuraton;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.wendev.spring.boot.istio.server.service.HelloServiceImpl;

import java.io.IOException;

/**
 * @author 江文
 * @date 2020/4/12 2:56 下午
 */
@Slf4j
@Component
public class GrpcServerConfiguration {
    @Autowired
    HelloServiceImpl service;

    @Value("${grpc.server-port}")
    private int port;
    private Server server;

    public void start() throws IOException {
        log.info("Starting gRPC on port {}.", port);
        server = ServerBuilder.forPort(port).addService(service).build().start();
        log.info("gRPC server started, listening on {}.", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down gRPC server.");
            GrpcServerConfiguration.this.stop();
            log.info("gRPC server shut down successfully.");
        }));
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void block() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
