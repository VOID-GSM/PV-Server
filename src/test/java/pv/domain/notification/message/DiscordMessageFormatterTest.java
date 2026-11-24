package pv.domain.notification.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiscordMessageFormatterTest {

    @Test
    void 역할과_사용자_멘션은_ID_형식으로_만든다() {
        assertThat(DiscordMessageFormatter.roleMentions(List.of("111", "222"))).isEqualTo("<@&111> <@&222>");
        assertThat(DiscordMessageFormatter.userMentions(List.of("333"))).isEqualTo("<@333>");
        assertThat(DiscordMessageFormatter.roleMentions(List.of())).isEmpty();
    }

    @Test
    void 여러_줄은_줄마다_인용한다() {
        assertThat(DiscordMessageFormatter.quote("첫 줄\n둘째 줄")).isEqualTo("> 첫 줄\n> 둘째 줄");
        assertThat(DiscordMessageFormatter.quote(" ")).isEmpty();
        assertThat(DiscordMessageFormatter.quote(null)).isEmpty();
    }

    @Test
    void 파일_참조는_첨부파일로_바꾼다() {
        String content = "안내 ![사진](file:123) 와 [문서](file:45) 참고";
        assertThat(DiscordMessageFormatter.replaceFileReferences(content))
                .isEqualTo("안내 [첨부파일] 와 [첨부파일] 참고");
    }

    @Test
    void 빈_멘션은_빼고_합친다() {
        assertThat(DiscordMessageFormatter.compose("", "제목", "> 내용")).isEqualTo("제목\n> 내용");
        assertThat(DiscordMessageFormatter.compose("<@&1>", "제목", "")).isEqualTo("<@&1>\n제목");
    }
}
