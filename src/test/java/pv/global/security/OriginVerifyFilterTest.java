package pv.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class OriginVerifyFilterTest {

    private static final String SECRET = "test-origin-secret";

    @Test
    @DisplayName("헤더 값이 일치하면 다음 필터로 넘긴다")
    void passesWhenHeaderMatches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OriginVerifyFilter.HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new OriginVerifyFilter(SECRET).doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("헤더가 없으면 403을 반환하고 다음 필터로 넘기지 않는다")
    void rejectsWhenHeaderMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new OriginVerifyFilter(SECRET).doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("헤더 값이 다르면 403을 반환한다")
    void rejectsWhenHeaderWrong() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OriginVerifyFilter.HEADER, "wrong");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new OriginVerifyFilter(SECRET).doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("비밀 값이 비어 있으면(로컬 개발) 검사하지 않는다")
    void skipsWhenSecretEmpty() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new OriginVerifyFilter("").doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("비밀 값 앞뒤 공백·개행은 무시한다")
    void stripsSecretWhitespace() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OriginVerifyFilter.HEADER, SECRET);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new OriginVerifyFilter("  " + SECRET + "\n").doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}