package com.supera.desafio.access;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supera.desafio.core.domain.module.SystemModule;
import com.supera.desafio.core.repository.SystemModuleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccessRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemModuleRepository moduleRepository;

    private String bearerToken;

    @BeforeEach
    void setup() throws Exception {
        bearerToken = authenticate();
    }

    @Test
    void shouldCreateListAndCancelRequest() throws Exception {
        Long moduleId = moduleRepository.findByCode("PORTAL")
                .map(SystemModule::getId)
                .orElseThrow();
        String payload = objectMapper.writeValueAsString(new CreatePayload(List.of(moduleId), "Solicito acesso urgente para atividades.", true));

        MvcResult createResult = mockMvc.perform(post("/api/access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocol").exists())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long requestId = created.get("id").asLong();

        mockMvc.perform(get("/api/access-requests")
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(requestId));

        mockMvc.perform(get("/api/access-requests/" + requestId)
                        .header("Authorization", bearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history[0]").exists());

        String cancelPayload = objectMapper.writeValueAsString(new CancelPayload("Cancelamento de teste"));
        mockMvc.perform(post("/api/access-requests/" + requestId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearerToken)
                        .content(cancelPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    private String authenticate() throws Exception {
        String loginPayload = objectMapper.writeValueAsString(new LoginPayload("carla.ti@corp.com", "Senha123"));
        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(login.getResponse().getContentAsString());
        return "Bearer " + node.get("accessToken").asText();
    }

    private record LoginPayload(String email, String password) {
    }

    private record CreatePayload(List<Long> moduleIds, String justification, boolean urgent) {
    }

    private record CancelPayload(String reason) {
    }
}
