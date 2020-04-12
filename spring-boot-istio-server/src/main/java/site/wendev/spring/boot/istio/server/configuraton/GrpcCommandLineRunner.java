package site.wendev.spring.boot.istio.server.configuraton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
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
