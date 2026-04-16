package mediaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import mediaservice.configs.MediaCompressionProperties;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "mediaservice.repositories")
@EnableConfigurationProperties(MediaCompressionProperties.class)
//@EnableRedisRepositories(basePackages = "mediaservice.repositories")
public class MediaServiceSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceSpringApplication.class, args);
    }

}
