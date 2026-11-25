package pv.domain.notification.message;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 디스코드 웹훅 메시지 형식을 한 곳에서 만든다. (멘션 / 인용 / 파일 참조 치환)
public final class DiscordMessageFormatter {

    public static final String EVERYONE = "@everyone";

    // 본문의 ![이름](file:123), [이름](file:123) 형태 파일 참조
    private static final Pattern FILE_REF = Pattern.compile("!?\\[[^\\]]*\\]\\(file:\\d{1,18}\\)");
    private static final String FILE_PLACEHOLDER = "[첨부파일]";

    private DiscordMessageFormatter() {
    }

    // 역할 멘션: <@&역할ID>
    public static String roleMentions(Collection<String> roleIds) {
        return roleIds.stream()
                .map(id -> "<@&" + id + ">")
                .collect(Collectors.joining(" "));
    }

    // 사용자 멘션: <@사용자ID>
    public static String userMentions(Collection<String> userIds) {
        return userIds.stream()
                .map(id -> "<@" + id + ">")
                .collect(Collectors.joining(" "));
    }

    // 여러 줄도 모두 인용으로 보이게 줄마다 '> '를 붙인다.
    public static String quote(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.lines()
                .map(line -> "> " + line)
                .collect(Collectors.joining("\n"));
    }

    public static String replaceFileReferences(String text) {
        if (text == null) {
            return "";
        }
        return FILE_REF.matcher(text).replaceAll(FILE_PLACEHOLDER);
    }

    // 멘션 줄 / 제목 줄 / 인용 줄을 순서대로 합친다. 빈 줄은 뺀다.
    public static String compose(String mentions, String headline, String quoted) {
        return Stream.of(mentions, headline, quoted)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("\n"));
    }
}
