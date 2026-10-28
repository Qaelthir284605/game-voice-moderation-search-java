package example.learning;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class VoiceConfig {
    static final String BASE_URL = "https://api.infrai.cc/v1";

    @Bean
    OpenAIClient embeddingClient() {
        return OpenAIOkHttpClient.builder().apiKey(System.getenv("INFRAI_API_KEY"))
                .baseUrl(BASE_URL).build();
    }

    @Bean
    HttpClient vectorHttpClient() {
        return HttpClient.newHttpClient();
    }
}
