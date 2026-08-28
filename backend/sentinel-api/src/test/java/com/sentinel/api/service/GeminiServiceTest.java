package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.model.Conversation;
import com.sentinel.api.model.ConversationMessage;
import com.sentinel.api.model.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GeminiServiceTest {

    @Mock
    private ToolExecutionService toolExecutionService;

    private GeminiService geminiService;
    private final ObjectMapper mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    @BeforeEach
    void setUp() {
        geminiService = new GeminiService(toolExecutionService);
        ReflectionTestUtils.setField(geminiService, "geminiApiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiService, "geminiModel", "gemini-3.6-flash");
    }

    @Test
    void testIsConfigured() {
        assertTrue(geminiService.isConfigured());

        ReflectionTestUtils.setField(geminiService, "geminiApiKey", "");
        assertFalse(geminiService.isConfigured());

        ReflectionTestUtils.setField(geminiService, "geminiApiKey", "${GEMINI_API_KEY:}");
        assertFalse(geminiService.isConfigured());
    }

    @Test
    void testToolDeclarationsContainValidSchemas() throws Exception {
        Method addToolDeclarationsMethod = GeminiService.class.getDeclaredMethod("addToolDeclarations", com.fasterxml.jackson.databind.node.ArrayNode.class);
        addToolDeclarationsMethod.setAccessible(true);

        com.fasterxml.jackson.databind.node.ArrayNode declList = mapper.createArrayNode();
        addToolDeclarationsMethod.invoke(geminiService, declList);

        assertEquals(6, declList.size());

        List<String> names = new ArrayList<>();
        for (JsonNode decl : declList) {
            names.add(decl.path("name").asText());
            assertTrue(decl.has("parameters"));
            assertEquals("OBJECT", decl.path("parameters").path("type").asText());
            assertTrue(decl.path("parameters").has("properties"), "Tool " + decl.path("name").asText() + " must have properties object");
        }

        assertTrue(names.contains("list_applications"));
        assertTrue(names.contains("get_application_health"));
        assertTrue(names.contains("get_application_metrics"));
        assertTrue(names.contains("get_request_logs"));
        assertTrue(names.contains("get_api_catalog"));
        assertTrue(names.contains("get_system_overview"));
    }

    @Test
    void testBuildContentsArrayAlternatesRolesStrictly() throws Exception {
        Method buildContentsMethod = GeminiService.class.getDeclaredMethod("buildContentsArray",
            com.fasterxml.jackson.databind.node.ArrayNode.class, List.class, String.class);
        buildContentsMethod.setAccessible(true);

        Conversation conv = new Conversation(10L, 1L, "Chat", null);
        List<ConversationMessage> history = new ArrayList<>();
        history.add(new ConversationMessage(conv, MessageSender.USER, "hello", null));
        history.add(new ConversationMessage(conv, MessageSender.ASSISTANT, "hi there", null));
        history.add(new ConversationMessage(conv, MessageSender.USER, "give list of apis", null));

        com.fasterxml.jackson.databind.node.ArrayNode contents = mapper.createArrayNode();
        // The current userPrompt is "give list of apis" which is also the last message in history
        buildContentsMethod.invoke(geminiService, contents, history, "give list of apis");

        // Verify contents structure
        assertTrue(contents.size() >= 1);
        assertEquals("user", contents.get(0).path("role").asText());
        assertEquals("user", contents.get(contents.size() - 1).path("role").asText());

        // Verify strict alternation
        String lastRole = null;
        for (JsonNode turn : contents) {
            String role = turn.path("role").asText();
            assertNotEquals(lastRole, role, "Consecutive identical roles are invalid in Gemini API");
            lastRole = role;
        }
    }

    @Test
    void testFormatGroundedFallback() {
        String sampleJson = "{\"endpoints\":[{\"normalizedPath\":\"/api/v1/users\",\"method\":\"GET\",\"documentationStatus\":\"DOCUMENTED\",\"summary\":\"List Users\",\"successRate\":100.0}]}";
        String formatted = geminiService.formatGroundedFallback("get_api_catalog", sampleJson, "test");

        assertNotNull(formatted);
        assertTrue(formatted.contains("Discovered API Catalog"));
        assertTrue(formatted.contains("/api/v1/users"));
        assertTrue(formatted.contains("GET"));
    }

    @Test
    void testParseRetryDelaySecondsFromDetailsJson() {
        String jsonWithDetails = "{\n" +
            "  \"error\": {\n" +
            "    \"code\": 429,\n" +
            "    \"message\": \"Resource has been exhausted\",\n" +
            "    \"status\": \"RESOURCE_EXHAUSTED\",\n" +
            "    \"details\": [\n" +
            "      {\n" +
            "        \"@type\": \"type.googleapis.com/google.rpc.RetryInfo\",\n" +
            "        \"retryDelay\": \"36.793638475s\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        double delay = GeminiService.parseRetryDelaySeconds(jsonWithDetails);
        assertEquals(36.793638475, delay, 0.0001);
    }

    @Test
    void testParseRetryDelaySecondsFromMessageRegex() {
        String jsonWithMessageOnly = "{\n" +
            "  \"error\": {\n" +
            "    \"code\": 429,\n" +
            "    \"message\": \"You exceeded your current quota. Please retry in 15.5s.\",\n" +
            "    \"status\": \"RESOURCE_EXHAUSTED\"\n" +
            "  }\n" +
            "}";

        double delay = GeminiService.parseRetryDelaySeconds(jsonWithMessageOnly);
        assertEquals(15.5, delay, 0.0001);
    }

    @Test
    void testParseRetryDelaySecondsInvalidOrEmpty() {
        assertEquals(-1.0, GeminiService.parseRetryDelaySeconds(null));
        assertEquals(-1.0, GeminiService.parseRetryDelaySeconds(""));
        assertEquals(-1.0, GeminiService.parseRetryDelaySeconds("{\"error\":{\"message\":\"Some other error\"}}"));
    }

    @Test
    void testQuotaFallbackWithPromptListApis() {
        String fakeCatalog = "{\"totalEndpoints\":1,\"endpoints\":[{\"normalizedPath\":\"/api/v1/data\",\"method\":\"POST\",\"documentationStatus\":\"DOCUMENTED\",\"summary\":\"Submit Data\",\"successRate\":99.5}],\"applicationId\":995}";
        org.mockito.Mockito.when(toolExecutionService.executeTool(
            org.mockito.ArgumentMatchers.eq("get_api_catalog"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(fakeCatalog);

        String result = geminiService.generateQuotaFallbackResponse(1L, null, "give list of apis", null, null);
        assertNotNull(result);
        assertTrue(result.contains("Discovered API Catalog"));
        assertTrue(result.contains("/api/v1/data"));
        assertTrue(result.contains("Submit Data"));
        assertTrue(result.contains("POST"));
    }

    @Test
    void testQuotaFallbackWithPromptHealthStatus() {
        String fakeApps = "{\"totalCount\":1,\"applications\":[{\"id\":101,\"name\":\"App-1\",\"healthStatus\":\"HEALTHY\",\"connectionMode\":\"OBSERVATION\",\"baseUrl\":\"http://localhost:8000\"}]}";
        org.mockito.Mockito.when(toolExecutionService.executeTool(
            org.mockito.ArgumentMatchers.eq("list_applications"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(fakeApps);

        String result = geminiService.generateQuotaFallbackResponse(1L, null, "show the health status of all applications", null, null);
        assertNotNull(result);
        assertTrue(result.contains("Monitored Applications"));
        assertTrue(result.contains("App-1"));
        assertTrue(result.contains("HEALTHY"));
    }

    @Test
    void testQuotaFallbackWithPromptSystemOverview() {
        String fakeOverview = "{\"totalApplications\":3,\"healthyApplications\":3,\"degradedApplications\":0,\"downApplications\":0,\"totalRequests24h\":1250,\"successRate24h\":99.8,\"avgLatencyMs24h\":42.5,\"mysqlStatus\":\"UP\",\"redisStatus\":\"UP\"}";
        org.mockito.Mockito.when(toolExecutionService.executeTool(
            org.mockito.ArgumentMatchers.eq("get_system_overview"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(fakeOverview);

        String result = geminiService.generateQuotaFallbackResponse(1L, null, "give me the system overview", null, null);
        assertNotNull(result);
        assertTrue(result.contains("Global System Overview"));
        assertTrue(result.contains("1250 requests"));
        assertTrue(result.contains("42.5ms"));
        assertTrue(result.contains("MySQL: `UP`"));
    }

    @Test
    void testQuotaFallbackWithPromptUnhealthyApplications() {
        String fakeApps = "{\"totalCount\":1,\"applications\":[{\"id\":101,\"name\":\"App-1\",\"healthStatus\":\"HEALTHY\",\"connectionMode\":\"OBSERVATION\",\"baseUrl\":\"http://localhost:8000\"}]}";
        org.mockito.Mockito.when(toolExecutionService.executeTool(
            org.mockito.ArgumentMatchers.eq("list_applications"),
            org.mockito.ArgumentMatchers.anyMap(),
            org.mockito.ArgumentMatchers.eq(1L),
            org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(fakeApps);

        String result = geminiService.generateQuotaFallbackResponse(1L, null, "are there any unhealthy applications?", null, null);
        assertNotNull(result);
        assertTrue(result.contains("All monitored applications are currently HEALTHY with no active degradation"));
    }

    @Test
    void testQuotaFallbackWithExistingToolResult() {
        String sampleJson = "{\"endpoints\":[{\"normalizedPath\":\"/api/v1/auth/login\",\"method\":\"POST\",\"documentationStatus\":\"DOCUMENTED\",\"summary\":\"Login\",\"successRate\":100.0}]}";
        String result = geminiService.generateQuotaFallbackResponse(1L, 995L, "some prompt", "get_api_catalog", sampleJson);

        assertNotNull(result);
        assertTrue(result.contains("Discovered API Catalog"));
        assertTrue(result.contains("/api/v1/auth/login"));
    }
}
