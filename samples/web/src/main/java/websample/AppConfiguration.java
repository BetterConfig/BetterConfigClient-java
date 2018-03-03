package websample;

import com.betterconfig.BetterConfigClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {

    @Bean
    public BetterConfigClient betterConfigClient() {
        return BetterConfigClient
                .newBuilder()
                .build("samples/01");
    }
}
