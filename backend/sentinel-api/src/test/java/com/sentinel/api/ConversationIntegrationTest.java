package com.sentinel.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.CreateApplicationRequest;
import com.sentinel.api.dto.CreateConversationRequest;
import com.sentinel.api.dto.LoginRequest;
import com.sentinel.api.dto.RegisterRequest;
import com.sentinel.api.dto.SendMessageRequest;
import com.sentinel.api.dto.UpdateConversationRequest;
import com.sentinel.api.repository.ConversationMessageRepository;
import com.sentinel.api.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String userAToken;
    private String userBToken;
    private Long userAAppId;

    @BeforeEach
    void setUp() throws Exception {
        userAToken = registerOrLogin("conv_user_a_" + System.currentTimeMillis() + "@sentinel.dev", "User A", "password123");
        userBToken = registerOrLogin("conv_user_b_" + System.currentTimeMillis() + "@sentinel.dev", "User B", "password123");

        CreateApplicationRequest appReq = new CreateApplicationRequest();
        appReq.setName("PixelVault Test App");
        appReq.setBaseUrl("https://pixelvault-clean-api.onrender.com");
        appReq.setDescription("PixelVault target application for AI testing");

        MvcResult appRes = mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(appReq)))
            .andExpect(status().isCreated())
            .andReturn();

        userAAppId = MAPPER.readTree(appRes.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void testCreateConversationAndAddMessages() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest();
        createReq.setApplicationId(userAAppId);
        createReq.setTitle("PixelVault AI Test Session");
        createReq.setInitialPrompt("Hello Sentinel AI");

        MvcResult res = mockMvc.perform(post("/api/v1/conversations")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title", is("PixelVault AI Test Session")))
            .andExpect(jsonPath("$.messages", hasSize(2))) // User + AI Assistant
            .andReturn();

        Long conversationId = MAPPER.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Send a follow-up message
        SendMessageRequest msgReq = new SendMessageRequest();
        msgReq.setContent("Can you tell me about the available image endpoints?");

        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(msgReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages", hasSize(4))); // + User + AI
    }

    @Test
    void testUserIsolationCannotAccessAnotherUsersConversation() throws Exception {
        // User A creates conversation
        CreateConversationRequest createReq = new CreateConversationRequest();
        createReq.setApplicationId(userAAppId);
        createReq.setTitle("User A Private Chat");

        MvcResult res = mockMvc.perform(post("/api/v1/conversations")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andReturn();

        Long conversationId = MAPPER.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // User B attempts to read User A's conversation -> 404
        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isNotFound());

        // User B attempts to patch User A's conversation -> 404
        UpdateConversationRequest patchReq = new UpdateConversationRequest();
        patchReq.setTitle("Hacked Title");
        mockMvc.perform(patch("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userBToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(patchReq)))
            .andExpect(status().isNotFound());

        // User B attempts to delete User A's conversation -> 404
        mockMvc.perform(delete("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userBToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void testSearchAndRenameConversation() throws Exception {
        CreateConversationRequest createReq = new CreateConversationRequest();
        createReq.setApplicationId(userAAppId);
        createReq.setTitle("UniqueAlphaTestingKeyword");

        MvcResult res = mockMvc.perform(post("/api/v1/conversations")
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(createReq)))
            .andExpect(status().isCreated())
            .andReturn();

        Long conversationId = MAPPER.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // Search conversations
        mockMvc.perform(get("/api/v1/conversations?search=UniqueAlpha")
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title", is("UniqueAlphaTestingKeyword")));

        // Rename conversation
        UpdateConversationRequest updateReq = new UpdateConversationRequest();
        updateReq.setTitle("Renamed AI Session");
        mockMvc.perform(patch("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userAToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(updateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Renamed AI Session")));

        // Delete conversation
        mockMvc.perform(delete("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isNoContent());

        // Verify deleted
        mockMvc.perform(get("/api/v1/conversations/" + conversationId)
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void testAiTestPlanDiscoveryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/applications/" + userAAppId + "/ai-test-plan")
                .header("Authorization", "Bearer " + userAToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId", is(userAAppId.intValue())))
            .andExpect(jsonPath("$.steps").isArray());
    }

    private String registerOrLogin(String email, String name, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(name, email, password);
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MAPPER.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

        return MAPPER.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }
}
