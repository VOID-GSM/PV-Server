package pv.domain.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DiscordWebhookClient {

    private static final int MAX_LENGTH = 2000;

    private final RestClient restClient;

    public DiscordWebhookClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public void send(String webhookUrl, String content) {
        String text = content.length() > MAX_LENGTH
                ? content.substring(0, MAX_LENGTH - 3) + "..."
                : content;

        Map<String, Object> body = Map.of(
                "content", text,
                "allowed_mentions", Map.of("parse", List.of())
        );

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("디스코드 웹훅 발송 실패: {}", e.getClass().getSimpleName());
        }
    }
}