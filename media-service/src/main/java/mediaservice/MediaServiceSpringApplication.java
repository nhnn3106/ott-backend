package mediaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import mediaservice.configs.MediaCompressionProperties;
import mediaservice.configs.MediaDeleteProperties;
import mediaservice.configs.MediaUploadProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableRabbit
@EnableJpaRepositories(basePackages = "mediaservice.repositories")
@EnableConfigurationProperties({
    MediaCompressionProperties.class,
    MediaDeleteProperties.class,
    MediaUploadProperties.class
})
//@EnableRedisRepositories(basePackages = "mediaservice.repositories")
public class MediaServiceSpringApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(MediaServiceSpringApplication.class, args);
    }
}
