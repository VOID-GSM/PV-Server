package pv.domain.notification.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DiscordWebhookClient {

    private static final int MAX_LENGTH = 2000;

    // 디스코드 allowed_mentions의 roles/users는 각각 최대 100개
    private static final int MAX_MENTION_IDS = 100;

    private final RestClient restClient;

    public DiscordWebhookClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // 멘션 없이 보낸다.
    public void send(String webhookUrl, String content) {
        send(webhookUrl, content, false, List.of(), List.of());
    }

    // 기본은 모든 멘션을 막고(parse: []), 서버가 허용한 대상만 명시해서 울린다.
    // 사용자가 제목/본문에 @everyone 등을 써도 허용하지 않은 멘션은 울리지 않는다.
    public void send(String webhookUrl, String content, boolean mentionEveryone,
                     Collection<String> roleIds, Collection<String> userIds) {
        String text = content.length() > MAX_LENGTH
                ? content.substring(0, MAX_LENGTH - 3) + "..."
                : content;

        Map<String, Object> body = Map.of(
                "content", text,
                "allowed_mentions", Map.of(
                        "parse", mentionEveryone ? List.of("everyone") : List.of(),
                        "roles", roleIds.stream().limit(MAX_MENTION_IDS).toList(),
                        "users", userIds.stream().limit(MAX_MENTION_IDS).toList()
                )
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