package com.enterprise.ticketing.common.controller;

import com.enterprise.ticketing.auth.security.JwtAuthenticationFilter;
import com.enterprise.ticketing.config.ApplicationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.Mockito.when;

@WebMvcTest(
        controllers = PlatformController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.enterprise.ticketing.common.handler.GlobalExceptionHandler.class)
@ActiveProfiles({"test", "webmvc"})
class PlatformControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApplicationProperties applicationProperties;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void infoReturnsWrappedPlatformMetadata() throws Exception {
        ApplicationProperties.Modules modules = new ApplicationProperties.Modules();
        modules.setAi(false);
        modules.setWorkflow(false);

        when(applicationProperties.getApiBasePath()).thenReturn("/api");
        when(applicationProperties.getModules()).thenReturn(modules);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/platform/info"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_SUCCESS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.application").value("enterprise-ai-ticketing"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.version").value("0.1.0-SNAPSHOT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.activeProfiles[0]").value("test"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.apiBasePath").value("/api"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.modules.auth").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.modules.ai").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.modules.workflow").value(false));
    }
}
