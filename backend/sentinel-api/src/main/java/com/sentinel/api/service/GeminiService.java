package com.sentinel.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sentinel.api.model.ConversationMessage;
import com.sentinel.api.model.MessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    @Value("${sentinel.gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${sentinel.gemini.model:gemini-3.7-flash}")
    private String geminiModel;

    private final ToolExecutionService toolExecutionService;

    public GeminiService(ToolExecutionService toolExecutionService) {
        this.toolExecutionService = toolExecutionService;
    }

    public boolean isConfigured() {
        return geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.startsWith("${");
    }

    public String generateResponse(Long userId, Long currentAppId, List<ConversationMessage> history, String userPrompt) {
        if (!isConfigured()) {
            return "### ⚠️ Google Gemini API Key Not Configured\n\n" +
                   "The Sentinel AI Assistant requires a **Google Gemini API Key** to interactively analyze telemetry, logs, and API health.\n\n" +
                   "**To configure:**\n" +
                   "1. Obtain an API key from Google AI Studio: [https://aistudio.google.com/](https://aistudio.google.com/)\n" +
                   "2. Set the environment variable `GEMINI_API_KEY=<your_api_key>` in your environment or `application.yml` (`sentinel.gemini.api-key`).\n" +
                   "3. Restart the Sentinel backend.\n\n" +
                   "Once configured, Sentinel AI will automatically query your live applications, metrics, endpoints, and health probes.";
        }

        try {
            return callGeminiWithTools(userId, currentAppId, history, userPrompt);
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return "### ❌ Sentinel AI Error\n\nUnable to complete request: " + e.getMessage() +
                   "\n\nPlease check your Sentinel backend logs or verify your Gemini API key and network connectivity.";
        }
    }

    private String callGeminiWithTools(Long userId, Long currentAppId, List<ConversationMessage> history, String userPrompt) throws Exception {
        String activeModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-3.7-flash";
        String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + activeModel + ":generateContent?key=" + geminiApiKey.trim();

        log.info("Preparing Gemini request using model '{}'", activeModel);

        // 1. Build Initial Request Payload
        ObjectNode root = MAPPER.createObjectNode();

        // System Instruction
        ObjectNode systemInstruction = root.putObject("systemInstruction");
        ObjectNode systemPart = systemInstruction.putArray("parts").addObject();
        systemPart.put("text", "You are the Sentinel AI Observability & API Security Assistant. " +
            "You provide factual, concise, and helpful answers about monitored applications, API endpoints, request telemetry, latency, health status, and errors. " +
            "CRITICAL PRINCIPLE: Sentinel's live database and real observation engines are the sole authoritative source of truth. You must NEVER manufacture or guess operational metrics, counts, health statuses, or error details. " +
            "Always invoke the appropriate Sentinel tools to retrieve live backend data when answering questions about applications, metrics, logs, health, or API catalogs. " +
            "Previous conversation history is strictly conversational context and must never be treated as current telemetry. " +
            "If a tool returns an empty list, zero count, or UNKNOWN status, report that honest state factually without inventing numbers or placeholder endpoints. " +
            "Format responses using clean GitHub-style Markdown.");

        // Contents (History + User Prompt)
        ArrayNode contents = root.putArray("contents");
        if (history != null && !history.isEmpty()) {
            List<ConversationMessage> pastHistory = history;
            // Exclude the current user prompt if it was already appended to history before calling generateResponse
            if (!pastHistory.isEmpty() && pastHistory.get(pastHistory.size() - 1).getSender() == MessageSender.USER
                && userPrompt.equals(pastHistory.get(pastHistory.size() - 1).getContent())) {
                pastHistory = pastHistory.subList(0, pastHistory.size() - 1);
            }

            int startIdx = Math.max(0, pastHistory.size() - 8);
            String lastRole = null;
            for (int i = startIdx; i < pastHistory.size(); i++) {
                ConversationMessage msg = pastHistory.get(i);
                String role = msg.getSender() == MessageSender.USER ? "user" : "model";
                if (role.equals(lastRole)) {
                    continue; // Skip consecutive identical roles
                }
                ObjectNode turn = contents.addObject();
                turn.put("role", role);
                turn.putArray("parts").addObject().put("text", msg.getContent());
                lastRole = role;
            }
        }
        // Current user message
        ObjectNode currentTurn = contents.addObject();
        currentTurn.put("role", "user");
        currentTurn.putArray("parts").addObject().put("text", userPrompt);

        // Tools declaration
        ArrayNode tools = root.putArray("tools");
        ObjectNode functionDeclarations = tools.addObject();
        ArrayNode declList = functionDeclarations.putArray("functionDeclarations");
        addToolDeclarations(declList);

        // 2. First Call to Gemini
        String requestJson = MAPPER.writeValueAsString(root);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(45))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();

        log.info("Sending Turn 1 request to Gemini API (model: '{}')", activeModel);
        HttpResponse<String> resp = sendWithRetry(req, "Turn 1");
        log.info("Gemini API Turn 1 responded with HTTP status {}", resp.statusCode());

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            log.warn("Gemini API Turn 1 error (HTTP {}): {}", resp.statusCode(), extractErrorMessage(resp.body()));
            return "Gemini API returned status " + resp.statusCode() + ": " + extractErrorMessage(resp.body());
        }

        JsonNode responseNode = MAPPER.readTree(resp.body());
        JsonNode candidates = responseNode.path("candidates");
        if (candidates.isEmpty()) {
            return "No response generated by Gemini.";
        }

        JsonNode firstCandidate = candidates.get(0);
        JsonNode contentParts = firstCandidate.path("content").path("parts");

        // Check if Gemini invoked a function call
        JsonNode functionCallNode = null;
        for (JsonNode part : contentParts) {
            if (part.has("functionCall")) {
                functionCallNode = part.get("functionCall");
                break;
            }
        }

        // If direct text response (no tool call needed)
        if (functionCallNode == null) {
            for (JsonNode part : contentParts) {
                if (part.has("text")) {
                    return part.get("text").asText();
                }
            }
            return "Received empty response from assistant.";
        }

        // 3. Handle Function Call
        String functionName = functionCallNode.path("name").asText();
        JsonNode argsNode = functionCallNode.path("args");
        Map<String, Object> args = new HashMap<>();
        if (argsNode.isObject()) {
            argsNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isInt() || entry.getValue().isLong()) {
                    args.put(entry.getKey(), entry.getValue().asLong());
                } else {
                    args.put(entry.getKey(), entry.getValue().asText());
                }
            });
        }

        log.info("Gemini invoked tool '{}' with args: {}", functionName, args);
        String toolResultJson = toolExecutionService.executeTool(functionName, args, userId, currentAppId);

        // 4. Second Turn to Gemini with Function Response
        ObjectNode modelTurn = contents.addObject();
        modelTurn.put("role", "model");
        modelTurn.putArray("parts").addObject().set("functionCall", functionCallNode);

        ObjectNode toolTurn = contents.addObject();
        toolTurn.put("role", "user");
        ObjectNode funcRespPart = toolTurn.putArray("parts").addObject().putObject("functionResponse");
        funcRespPart.put("name", functionName);
        funcRespPart.putObject("response").set("result", MAPPER.readTree(toolResultJson));

        // Call Gemini again for final synthesized answer
        String secondRequestJson = MAPPER.writeValueAsString(root);
        HttpRequest secondReq = HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(45))
            .POST(HttpRequest.BodyPublishers.ofString(secondRequestJson))
            .build();

        log.info("Sending Turn 2 request to Gemini API with tool result (model: '{}')", activeModel);
        HttpResponse<String> secondResp = sendWithRetry(secondReq, "Turn 2");
        log.info("Gemini API Turn 2 responded with HTTP status {}", secondResp.statusCode());

        if (secondResp.statusCode() >= 200 && secondResp.statusCode() < 300) {
            JsonNode secondRespNode = MAPPER.readTree(secondResp.body());
            JsonNode secondCandidates = secondRespNode.path("candidates");
            if (!secondCandidates.isEmpty()) {
                JsonNode parts = secondCandidates.get(0).path("content").path("parts");
                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        return part.get("text").asText();
                    }
                }
            }
        } else {
            log.warn("Gemini API Turn 2 error (HTTP {}): {}", secondResp.statusCode(), extractErrorMessage(secondResp.body()));
        }

        // Fallback: return raw tool output if second turn failed
        return "### Telemetry Tool Result (`" + functionName + "`):\n```json\n" + toolResultJson + "\n```";
    }

    private HttpResponse<String> sendWithRetry(HttpRequest req, String turnLabel) throws Exception {
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 503 || resp.statusCode() == 429) {
            log.warn("Gemini API {} received transient HTTP {}. Retrying once in 2 seconds...", turnLabel, resp.statusCode());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
            resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Gemini API {} retry returned HTTP status {}", turnLabel, resp.statusCode());
        }
        return resp;
    }

    private void addToolDeclarations(ArrayNode declList) {
        // Tool 1: list_applications
        ObjectNode t1 = declList.addObject();
        t1.put("name", "list_applications");
        t1.put("description", "List all monitored applications registered by the user in Sentinel.");
        t1.putObject("parameters").put("type", "OBJECT");

        // Tool 2: get_application_health
        ObjectNode t2 = declList.addObject();
        t2.put("name", "get_application_health");
        t2.put("description", "Get the real-time health probe status, latency, and last checked timestamp for an application.");
        ObjectNode p2 = t2.putObject("parameters");
        p2.put("type", "OBJECT");
        ObjectNode props2 = p2.putObject("properties");
        props2.putObject("applicationId").put("type", "INTEGER").put("description", "The ID of the application");
        p2.putArray("required").add("applicationId");

        // Tool 3: get_application_metrics
        ObjectNode t3 = declList.addObject();
        t3.put("name", "get_application_metrics");
        t3.put("description", "Query telemetry metrics including total requests, success/failure counts, error rates, and average latency.");
        ObjectNode p3 = t3.putObject("parameters");
        p3.put("type", "OBJECT");
        ObjectNode props3 = p3.putObject("properties");
        props3.putObject("applicationId").put("type", "INTEGER").put("description", "The ID of the application");
        props3.putObject("timeRange").put("type", "STRING").put("description", "Time window: '1h', '24h', or '7d'");
        p3.putArray("required").add("applicationId");

        // Tool 4: get_request_logs
        ObjectNode t4 = declList.addObject();
        t4.put("name", "get_request_logs");
        t4.put("description", "Retrieve recent HTTP request logs, status codes, endpoints, and error messages.");
        ObjectNode p4 = t4.putObject("parameters");
        p4.put("type", "OBJECT");
        ObjectNode props4 = p4.putObject("properties");
        props4.putObject("applicationId").put("type", "INTEGER").put("description", "Optional application ID");
        props4.putObject("limit").put("type", "INTEGER").put("description", "Number of logs to retrieve (default 20)");
        props4.putObject("minStatusCode").put("type", "INTEGER").put("description", "Filter logs with status code >= minStatusCode (e.g. 400 for errors)");

        // Tool 5: get_api_catalog
        ObjectNode t5 = declList.addObject();
        t5.put("name", "get_api_catalog");
        t5.put("description", "Get the list of discovered and documented API endpoints in the Sentinel API catalog for an application.");
        ObjectNode p5 = t5.putObject("parameters");
        p5.put("type", "OBJECT");
        ObjectNode props5 = p5.putObject("properties");
        props5.putObject("applicationId").put("type", "INTEGER").put("description", "The ID of the application");
        p5.putArray("required").add("applicationId");

        // Tool 6: get_system_overview
        ObjectNode t6 = declList.addObject();
        t6.put("name", "get_system_overview");
        t6.put("description", "Get global system overview including total applications, healthy vs degraded count, 24h traffic, error counts, and MySQL/Redis status.");
        t6.putObject("parameters").put("type", "OBJECT");
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode node = MAPPER.readTree(responseBody);
            if (node.has("error") && node.get("error").has("message")) {
                return node.get("error").get("message").asText();
            }
        } catch (Exception ignored) {}
        return responseBody;
    }
}
