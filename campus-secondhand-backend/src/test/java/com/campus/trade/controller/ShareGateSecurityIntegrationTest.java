package com.campus.trade.controller;

import com.campus.trade.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "app.share-gate.passphrase=嗯对",
        "app.share-gate.verify-window-seconds=60",
        "app.share-gate.verify-max-requests=8",
        "app.share-gate.fail-max-attempts=4",
        "app.share-gate.fail-ban-seconds=1800"
})
class ShareGateSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("分享接口在未验证门禁时返回 403")
    void shouldRejectSharesApiWithoutVerifiedShareGate() throws Exception {
        mockMvc.perform(get("/api/shares"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Share gate verify required"));
    }

    @Test
    @DisplayName("短中文分享口令验证成功后通过安全 Cookie 建立门禁会话")
    void shouldEstablishShareGateSessionWithShortChinesePassphrase() throws Exception {
        mockMvc.perform(post("/api/auth/share-gate/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passphrase\":\"嗯对\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true))
                .andExpect(cookie().exists("campus_share_gate"));
    }

    @Test
    @DisplayName("分享口令前后空白会被规范化后再校验")
    void shouldTrimShareGatePassphraseBeforeComparison() throws Exception {
        mockMvc.perform(post("/api/auth/share-gate/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passphrase\":\"  嗯对  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @DisplayName("分享门禁状态接口可识别已验证 Cookie")
    void shouldReportVerifiedStatusFromCookie() throws Exception {
        MvcResult verifyResult = mockMvc.perform(post("/api/auth/share-gate/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passphrase\":\"嗯对\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie gateCookie = verifyResult.getResponse().getCookie("campus_share_gate");

        mockMvc.perform(get("/api/auth/share-gate/status")
                        .cookie(gateCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @DisplayName("伪造 X-Forwarded-For 不应帮助绕过门禁")
    void shouldNotTrustSpoofedForwardedIpForShareGate() throws Exception {
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/share-gate/verify")
                            .header("X-Forwarded-For", "20.20.20." + i)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"passphrase\":\"wrong-pass\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/share-gate/verify")
                        .header("X-Forwarded-For", "30.30.30.30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passphrase\":\"wrong-pass\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("尝试次数过多，请稍后再试"));
    }

    @Test
    @DisplayName("旧的分享 JWT 请求头不再被接受")
    void shouldRejectLegacyShareGateHeaderToken() throws Exception {
        String token = jwtService.generateShareGateToken(1800);
        mockMvc.perform(get("/api/shares")
                        .header("X-Share-Gate-Token", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Share gate verify required"));
    }
}
