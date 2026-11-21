package pv.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("헬스체크는 인증 없이 호출되고 DB를 포함한 상태(UP)만 응답한다")
    void healthIsPublicAndShowsStatusOnly() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @DisplayName("헬스체크 외 Actuator 엔드포인트는 외부에 열리지 않는다")
    void otherEndpointsAreNotPublic() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/actuator"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그인하지 않으면 health 외 Actuator 경로는 401이다")
    void otherEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("로그인해도 health 외 Actuator 엔드포인트는 노출되지 않는다(404)")
    void otherEndpointsAreNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isNotFound());
    }
}