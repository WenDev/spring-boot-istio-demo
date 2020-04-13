package site.wendev.spring.boot.istio.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import site.wendev.spring.boot.istio.client.api.HelloWorldService;
import site.wendev.spring.boot.istio.client.configuration.GrpcClientConfiguration;

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
