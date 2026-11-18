package pv.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "origin.verify-secret=test-origin-secret")
@AutoConfigureMockMvc
class OriginVerifyFilterIntegrationTest {

    private static final String PROTECTED_PATH = "/home/todos";

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("헤더 없는 요청은 Spring Security보다 먼저 403으로 막힌다")
    void rejectsBeforeSecurity() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("헤더가 맞으면 Spring Security로 넘어가 인증 실패(401)가 된다")
    void passesToSecurityWhenHeaderMatches() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(OriginVerifyFilter.HEADER, "test-origin-secret"))
                .andExpect(status().isUnauthorized());
    }
}
