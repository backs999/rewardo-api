package travel.rewardo.rewardapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class ApiClientConfig {

    @Value("${api.client.use-proxy:false}")
    private boolean useProxy;

    @Bean
    public OkHttpClient okHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false);
        
        if (useProxy) {
            builder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress("surfshark-vpn", 8888)));
        }
        
        return builder.build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // Register JSR310 module for Java 8 date/time types
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }
}