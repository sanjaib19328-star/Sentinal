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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final ObjectMapper MAPPER = com.fasterxml.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    @Value("${sentinel.gemini.api-key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    @Value("${sentinel.gemini.model:${GEMINI_MODEL:gemini-3.6-flash}}")
    private String geminiModel;

    private final ToolExecutionService toolExecutionService;

    public GeminiService(ToolExecutionService toolExecutionService) {
        this.toolExecutionService = toolExecutionService;
    }

    @jakarta.annotation.PostConstruct
    public void logStartupDiagnostics() {
        boolean configured = isConfigured();
        String primary = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-3.6-flash";
        log.info("Sentinel Gemini AI Diagnostics -> API key: {}, Primary model: '{}'",
            configured ? "CONFIGURED" : "NOT CONFIGURED", primary);
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
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("Gemini request timed out: {}", e.getMessage());
            return "### ⏳ Sentinel AI Timeout\n\nSentinel AI is taking longer than expected to process your query with upstream Gemini. Please try again in a moment.";
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return "### ❌ Sentinel AI Error\n\nUnable to complete request: " + e.getMessage() +
                   "\n\nPlease check your Sentinel backend logs or verify your Gemini API key and network connectivity.";
        }
    }

    private String callGeminiWithTools(Long userId, Long currentAppId, List<ConversationMessage> history, String userPrompt) throws Exception {
        String activeModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-3.6-flash";
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

        // Contents (Sanitized History + User Prompt)
        ArrayNode contents = root.putArray("contents");
        buildContentsArray(contents, history, userPrompt);

        // Tools declaration
        ArrayNode tools = root.putArray("tools");
        ObjectNode functionDeclarations = tools.addObject();
        ArrayNode declList = functionDeclarations.putArray("functionDeclarations");
        addToolDeclarations(declList);

        String lastFunctionName = null;
        String lastToolResultJson = null;

        for (int turn = 1; turn <= 4; turn++) {
            String turnLabel = "Turn " + turn;
            log.info("Sending {} request to Gemini API (model: '{}')", turnLabel, activeModel);
            HttpResponse<String> resp = executeGeminiCall(root, turnLabel);
            log.info("Gemini API {} responded with HTTP status {}", turnLabel, resp.statusCode());

            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                int status = resp.statusCode();
                String errorMsg = extractErrorMessage(resp.body());
                log.warn("Gemini API {} error (HTTP {}): {}", turnLabel, status, errorMsg);

                if (status == 429) {
                    return generateQuotaFallbackResponse(userId, currentAppId, userPrompt, lastFunctionName, lastToolResultJson);
                } else if (status == 503) {
                    if (lastFunctionName != null && lastToolResultJson != null) {
                        return formatGroundedFallback(lastFunctionName, lastToolResultJson, "Gemini service temporarily unavailable (HTTP 503)");
                    }
                    return "### ⏳ Upstream Service Unavailable (HTTP 503)\n\n" +
                           "Gemini AI is temporarily unavailable. Please try again in a few moments.";
                } else if (status == 404) {
                    return "### ⚠️ Gemini Model Unavailable (HTTP 404)\n\n" +
                           "The configured model `" + activeModel + "` was not found or is unavailable. " +
                           "Please verify the model name in your environment or `application.yml` (`sentinel.gemini.model`).";
                } else if (status == 401 || status == 403) {
                    return "### 🔒 Gemini API Authentication Failed (HTTP " + status + ")\n\n" +
                           "The Gemini API key could not be authenticated. " +
                           "Please verify your `GEMINI_API_KEY` configuration in AI Studio and restart Sentinel.";
                } else {
                    if (lastFunctionName != null && lastToolResultJson != null) {
                        return formatGroundedFallback(lastFunctionName, lastToolResultJson, "Gemini API returned status " + status);
                    }
                    return "### ❌ Gemini API Error (HTTP " + status + ")\n\n" + errorMsg;
                }
            }

            JsonNode responseNode = MAPPER.readTree(resp.body());
            JsonNode candidates = responseNode.path("candidates");
            if (candidates.isEmpty()) {
                if (lastFunctionName != null && lastToolResultJson != null) {
                    return formatGroundedFallback(lastFunctionName, lastToolResultJson, "No response candidates returned");
                }
                return "No response generated by Gemini.";
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode contentParts = firstCandidate.path("content").path("parts");

            // Check for function calls in candidate parts
            List<JsonNode> functionCalls = new java.util.ArrayList<>();
            for (JsonNode part : contentParts) {
                if (part.has("functionCall")) {
                    functionCalls.add(part.get("functionCall"));
                }
            }

            // If direct text response (no tool call requested this turn)
            if (functionCalls.isEmpty()) {
                StringBuilder synthesized = new StringBuilder();
                for (JsonNode part : contentParts) {
                    if (part.has("text") && !part.path("thought").asBoolean(false)) {
                        synthesized.append(part.get("text").asText());
                    }
                }
                if (synthesized.length() > 0) {
                    return synthesized.toString().trim();
                }
                for (JsonNode part : contentParts) {
                    if (part.has("text")) {
                        synthesized.append(part.get("text").asText());
                    }
                }
                if (synthesized.length() > 0) {
                    return synthesized.toString().trim();
                }
                if (lastFunctionName != null && lastToolResultJson != null) {
                    return formatGroundedFallback(lastFunctionName, lastToolResultJson, "Response completed");
                }
                return "Received empty response from assistant.";
            }

            // Append model turn (with exact candidate parts)
            ObjectNode modelTurn = contents.addObject();
            modelTurn.put("role", "model");
            modelTurn.set("parts", contentParts);

            // Append function response turn with role "function"
            ObjectNode toolTurn = contents.addObject();
            toolTurn.put("role", "function");
            ArrayNode funcRespParts = toolTurn.putArray("parts");

            for (JsonNode fcNode : functionCalls) {
                String functionName = fcNode.path("name").asText();
                JsonNode argsNode = fcNode.path("args");
                Map<String, Object> args = new HashMap<>();
                if (argsNode.isObject()) {
                    argsNode.fields().forEachRemaining(entry -> {
                        if (entry.getValue().isInt() || entry.getValue().isLong()) {
                            args.put(entry.getKey(), entry.getValue().asLong());
                        } else if (entry.getValue().isBoolean()) {
                            args.put(entry.getKey(), entry.getValue().asBoolean());
                        } else if (entry.getValue().isDouble()) {
                            args.put(entry.getKey(), entry.getValue().asDouble());
                        } else {
                            args.put(entry.getKey(), entry.getValue().asText());
                        }
                    });
                }

                log.info("Gemini invoked tool '{}' with args: {}", functionName, args);
                String toolResultJson = toolExecutionService.executeTool(functionName, args, userId, currentAppId);
                lastFunctionName = functionName;
                lastToolResultJson = toolResultJson;

                ObjectNode funcRespPart = funcRespParts.addObject().putObject("functionResponse");
                funcRespPart.put("name", functionName);
                if (fcNode.has("id")) {
                    funcRespPart.put("id", fcNode.get("id").asText());
                }

                ObjectNode respObj = funcRespPart.putObject("response");
                respObj.put("name", functionName);
                try {
                    JsonNode parsedJson = MAPPER.readTree(toolResultJson);
                    respObj.set("content", parsedJson);
                } catch (Exception parseEx) {
                    respObj.put("content", toolResultJson);
                }
            }
        }

        if (lastFunctionName != null && lastToolResultJson != null) {
            return formatGroundedFallback(lastFunctionName, lastToolResultJson, "Reasoning turns completed");
        }
        return "Assistant completed reasoning turns.";
    }

    public String generateQuotaFallbackResponse(Long userId, Long currentAppId, String userPrompt, String lastFunctionName, String lastToolResultJson) {
        if (lastFunctionName != null && lastToolResultJson != null) {
            return formatGroundedFallback(lastFunctionName, lastToolResultJson, "Gemini quota exceeded (HTTP 429)");
        }

        String promptLower = userPrompt != null ? userPrompt.toLowerCase().trim() : "";

        // 1. API Catalog & Endpoints
        if (promptLower.contains("api") || promptLower.contains("endpoint") || promptLower.contains("catalog")
            || promptLower.contains("routes") || promptLower.contains("methods") || promptLower.contains("url")) {
            String json;
            if (currentAppId != null) {
                json = toolExecutionService.executeTool("get_api_catalog", Map.of("applicationId", currentAppId), userId, currentAppId);
            } else {
                json = toolExecutionService.executeTool("get_api_catalog", Collections.emptyMap(), userId, null);
            }
            return formatApiCatalogMarkdown(json, "get_api_catalog");
        }

        // 2. Health status / Unhealthy apps
        if (promptLower.contains("health") || promptLower.contains("status") || promptLower.contains("healthy")
            || promptLower.contains("unhealthy") || promptLower.contains("degraded") || promptLower.contains("down") || promptLower.contains("probe")) {
            if (currentAppId != null) {
                String json = toolExecutionService.executeTool("get_application_health", Map.of("applicationId", currentAppId), userId, currentAppId);
                return formatHealthMarkdown(json, "get_application_health");
            } else {
                String json = toolExecutionService.executeTool("list_applications", Collections.emptyMap(), userId, null);
                return formatApplicationListMarkdown(json, "list_applications", promptLower.contains("unhealthy"));
            }
        }

        // 3. System overview & Metrics
        if (promptLower.contains("system") || promptLower.contains("overview") || promptLower.contains("dashboard")
            || promptLower.contains("summary") || promptLower.contains("traffic") || promptLower.contains("redis") || promptLower.contains("mysql")) {
            String json = toolExecutionService.executeTool("get_system_overview", Collections.emptyMap(), userId, null);
            return formatSystemOverviewMarkdown(json, "get_system_overview");
        }

        if (promptLower.contains("metric") || promptLower.contains("latency") || promptLower.contains("rate") || promptLower.contains("count")) {
            if (currentAppId != null) {
                String json = toolExecutionService.executeTool("get_application_metrics", Map.of("applicationId", currentAppId, "timeRange", "24h"), userId, currentAppId);
                return formatMetricsMarkdown(json, "get_application_metrics");
            } else {
                String json = toolExecutionService.executeTool("get_system_overview", Collections.emptyMap(), userId, null);
                return formatSystemOverviewMarkdown(json, "get_system_overview");
            }
        }

        // 4. Request Logs
        if (promptLower.contains("log") || promptLower.contains("request") || promptLower.contains("trace") || promptLower.contains("error")) {
            String json = toolExecutionService.executeTool("get_request_logs", Map.of("limit", 20), userId, currentAppId);
            return formatRequestLogsMarkdown(json, "get_request_logs");
        }

        // 5. Default fallback -> List Applications
        String json = toolExecutionService.executeTool("list_applications", Collections.emptyMap(), userId, null);
        return formatApplicationListMarkdown(json, "list_applications", false);
    }

    private void buildContentsArray(ArrayNode contents, List<ConversationMessage> history, String userPrompt) {
        if (history != null && !history.isEmpty()) {
            List<ConversationMessage> pastHistory = new java.util.ArrayList<>(history);
            // Exclude current prompt if it was already stored at the end of past history
            if (!pastHistory.isEmpty() && pastHistory.get(pastHistory.size() - 1).getSender() == MessageSender.USER
                && userPrompt.equals(pastHistory.get(pastHistory.size() - 1).getContent())) {
                pastHistory.remove(pastHistory.size() - 1);
            }

            int startIdx = Math.max(0, pastHistory.size() - 6);
            String expectedNextRole = "user";

            for (int i = startIdx; i < pastHistory.size(); i++) {
                ConversationMessage msg = pastHistory.get(i);
                String role = msg.getSender() == MessageSender.USER ? "user" : "model";

                if (!role.equals(expectedNextRole)) {
                    continue; // Skip out-of-order turns to ensure strict alternation starting with user
                }

                ObjectNode turn = contents.addObject();
                turn.put("role", role);
                turn.putArray("parts").addObject().put("text", msg.getContent());

                expectedNextRole = "user".equals(role) ? "model" : "user";
            }

            // If the last added turn was a user turn, remove it or ensure current prompt follows a model turn
            if ("model".equals(expectedNextRole) && contents.size() > 0) {
                contents.remove(contents.size() - 1);
            }
        }

        // Add current user prompt
        ObjectNode currentTurn = contents.addObject();
        currentTurn.put("role", "user");
        currentTurn.putArray("parts").addObject().put("text", userPrompt);
    }

    private HttpResponse<String> executeGeminiCall(ObjectNode root, String turnLabel) throws Exception {
        String requestJson = MAPPER.writeValueAsString(root);
        String primaryModel = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel.trim() : "gemini-3.6-flash";
        String endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + primaryModel + ":generateContent";

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", geminiApiKey.trim())
            .timeout(Duration.ofSeconds(45))
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();

        log.info("Sending {} request to Gemini API (model: '{}')", turnLabel, primaryModel);
        HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

        // Single controlled retry on transient 503 or very short 429
        if (resp.statusCode() == 503) {
            log.warn("Gemini model '{}' {} received HTTP 503. Retrying once after 1.5s...", primaryModel, turnLabel);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {}
            resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Gemini model '{}' {} retry responded with HTTP {}", primaryModel, turnLabel, resp.statusCode());
        } else if (resp.statusCode() == 429) {
            double delaySeconds = parseRetryDelaySeconds(resp.body());
            if (delaySeconds > 0 && delaySeconds <= 2.0) {
                long waitMillis = (long) (delaySeconds * 1000);
                log.info("Gemini model '{}' {} received HTTP 429 with short delay ({}s). Waiting {}ms to retry...",
                    primaryModel, turnLabel, delaySeconds, waitMillis);
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException ignored) {}
                resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                log.info("Gemini model '{}' {} retry responded with HTTP {}", primaryModel, turnLabel, resp.statusCode());
            } else {
                log.warn("Gemini API returned HTTP 429; quota/rate limit detected (delay: {}s). Using grounded Sentinel fallback.",
                    delaySeconds > 0 ? String.format("%.1f", delaySeconds) : "unspecified");
            }
        }

        return resp;
    }

    public static double parseRetryDelaySeconds(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return -1.0;
        }
        try {
            JsonNode node = MAPPER.readTree(responseBody);
            // 1. Check details array for RetryInfo
            JsonNode details = node.path("error").path("details");
            if (details.isArray()) {
                for (JsonNode detail : details) {
                    if (detail.has("retryDelay")) {
                        String delayStr = detail.get("retryDelay").asText();
                        return parseSecondsString(delayStr);
                    }
                }
            }
            // 2. Check error message regex e.g. "Please retry in 36.793638475s."
            String msg = node.path("error").path("message").asText("");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("retry in ([0-9]+(?:\\.[0-9]+)?)s", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(msg);
            if (m.find()) {
                return Double.parseDouble(m.group(1));
            }
        } catch (Exception ignored) {}
        return -1.0;
    }

    private static double parseSecondsString(String str) {
        if (str == null) return -1.0;
        String clean = str.trim().replaceAll("(?i)s$", "");
        try {
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return -1.0;
        }
    }

    public String formatGroundedFallback(String toolName, String jsonResult, String reason) {
        if ("get_api_catalog".equals(toolName)) {
            return formatApiCatalogMarkdown(jsonResult, toolName);
        } else if ("list_applications".equals(toolName)) {
            return formatApplicationListMarkdown(jsonResult, toolName, false);
        } else if ("get_system_overview".equals(toolName)) {
            return formatSystemOverviewMarkdown(jsonResult, toolName);
        } else if ("get_application_health".equals(toolName)) {
            return formatHealthMarkdown(jsonResult, toolName);
        } else if ("get_application_metrics".equals(toolName)) {
            return formatMetricsMarkdown(jsonResult, toolName);
        } else if ("get_request_logs".equals(toolName)) {
            return formatRequestLogsMarkdown(jsonResult, toolName);
        }
        return getFallbackBanner(toolName) +
               "```json\n" + jsonResult + "\n```";
    }

    private String getFallbackBanner(String toolName) {
        return "### ⚡ Sentinel Live Telemetry (`" + toolName + "`)\n\n" +
               "> **Notice:** Upstream Gemini AI quota is temporarily rate-limited. Showing live, authoritative telemetry directly from the Sentinel database.\n\n";
    }

    public String formatApiCatalogMarkdown(String jsonResult, String toolName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            if (root.has("applications")) {
                int totalEndpoints = root.path("totalEndpoints").asInt(0);
                JsonNode apps = root.path("applications");
                sb.append("### 📚 Discovered API Catalog (All Applications)\n\n");
                sb.append("**Total Applications:** `").append(root.path("totalApplications").asInt(apps.size())).append("` | ");
                sb.append("**Total Endpoints:** `").append(totalEndpoints).append("`\n\n");

                if (totalEndpoints == 0) {
                    sb.append("*No API endpoints are currently cataloged across your applications.*\n");
                    return sb.toString();
                }

                for (JsonNode app : apps) {
                    sb.append("#### 🔹 Application: **").append(app.path("applicationName").asText()).append("** (ID: `")
                      .append(app.path("applicationId").asText()).append("`)\n");
                    if (app.hasNonNull("baseUrl") && !app.path("baseUrl").asText().isBlank()) {
                        sb.append("- **Base URL:** `").append(app.path("baseUrl").asText()).append("`\n");
                    }
                    JsonNode endpoints = app.path("endpoints");
                    appendEndpointsTable(sb, endpoints);
                    sb.append("\n");
                }
            } else if (root.has("endpoints")) {
                JsonNode endpoints = root.path("endpoints");
                int total = root.path("totalEndpoints").asInt(endpoints.size());
                sb.append("### 📚 Discovered API Catalog (App ID: `").append(root.path("applicationId").asText()).append("`)\n\n");
                sb.append("**Total Documented Endpoints:** `").append(total).append("`\n\n");

                if (endpoints.isEmpty()) {
                    sb.append("*No API endpoints are currently cataloged for this application.*\n");
                } else {
                    appendEndpointsTable(sb, endpoints);
                }
            } else {
                sb.append("```json\n").append(jsonResult).append("\n```");
            }
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    private void appendEndpointsTable(StringBuilder sb, JsonNode endpoints) {
        if (endpoints.isEmpty()) {
            sb.append("*No endpoints recorded.*\n");
            return;
        }
        sb.append("| Method | Endpoint Path | Status | Summary | Success Rate |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
        for (JsonNode ep : endpoints) {
            String method = ep.path("method").asText("GET");
            String path = ep.path("normalizedPath").asText("/");
            String status = ep.path("documentationStatus").asText("DOCUMENTED");
            String summary = ep.path("summary").asText("-");
            double successRate = ep.path("successRate").asDouble(100.0);
            sb.append("| `").append(method).append("` | `").append(path).append("` | `")
              .append(status).append("` | ").append(summary.isBlank() ? "-" : summary).append(" | `")
              .append(String.format("%.1f", successRate)).append("%` |\n");
        }
    }

    public String formatApplicationListMarkdown(String jsonResult, String toolName, boolean filterUnhealthyOnly) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            JsonNode apps = root.path("applications");
            sb.append("### 🖥️ Monitored Applications\n\n");
            sb.append("**Total Registered:** `").append(root.path("totalCount").asInt(apps.size())).append("`\n\n");

            if (apps.isEmpty()) {
                sb.append("*No applications currently registered in Sentinel.*\n");
                return sb.toString();
            }

            sb.append("| ID | Name | Health Status | Connection Mode | Base URL |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
            int displayed = 0;
            for (JsonNode app : apps) {
                String health = app.path("healthStatus").asText("UNKNOWN");
                if (filterUnhealthyOnly && "HEALTHY".equalsIgnoreCase(health)) {
                    continue;
                }
                displayed++;
                sb.append("| `").append(app.path("id").asText()).append("` | **")
                  .append(app.path("name").asText()).append("** | `")
                  .append(health).append("` | `")
                  .append(app.path("connectionMode").asText("OBSERVATION")).append("` | `")
                  .append(app.path("baseUrl").asText("-")).append("` |\n");
            }
            if (filterUnhealthyOnly && displayed == 0) {
                sb.append("\n🎉 **All monitored applications are currently HEALTHY with no active degradation!**\n");
            }
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    public String formatSystemOverviewMarkdown(String jsonResult, String toolName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            sb.append("### 🌐 Global System Overview\n\n");
            sb.append("- **Total Applications:** `").append(root.path("totalApplications").asInt(0)).append("`\n");
            sb.append("- **Healthy Applications:** `").append(root.path("healthyApplications").asInt(0)).append("`\n");
            sb.append("- **Degraded Applications:** `").append(root.path("degradedApplications").asInt(0)).append("`\n");
            sb.append("- **Down Applications:** `").append(root.path("downApplications").asInt(0)).append("`\n");
            sb.append("- **24h Traffic:** `").append(root.path("totalRequests24h").asLong(0)).append(" requests`\n");
            sb.append("- **24h Success Rate:** `").append(String.format("%.1f", root.path("successRate24h").asDouble(100.0))).append("%`\n");
            sb.append("- **24h Avg Latency:** `").append(String.format("%.1f", root.path("avgLatencyMs24h").asDouble(0.0))).append("ms`\n");
            sb.append("- **Infrastructure Status:** MySQL: `").append(root.path("mysqlStatus").asText("UP"))
              .append("` | Redis: `").append(root.path("redisStatus").asText("UP")).append("`\n");
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    public String formatHealthMarkdown(String jsonResult, String toolName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            sb.append("### 🩺 Application Health Status\n\n");
            sb.append("- **Application ID:** `").append(root.path("applicationId").asText()).append("`\n");
            sb.append("- **Health Status:** `").append(root.path("healthStatus").asText("UNKNOWN")).append("`\n");
            sb.append("- **Connection Mode:** `").append(root.path("connectionMode").asText("OBSERVATION")).append("`\n");
            sb.append("- **Last Checked:** `").append(root.path("lastSeenAt").asText("N/A")).append("`\n");
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    public String formatMetricsMarkdown(String jsonResult, String toolName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            sb.append("### 📊 Telemetry Metrics\n\n");
            sb.append("- **Application ID:** `").append(root.path("applicationId").asText()).append("`\n");
            sb.append("- **Time Range:** `").append(root.path("timeRange").asText("24h")).append("`\n");
            sb.append("- **Total Requests:** `").append(root.path("totalRequests").asLong(0)).append("`\n");
            sb.append("- **Successful Requests:** `").append(root.path("successfulRequests").asLong(0)).append("`\n");
            sb.append("- **Failed Requests:** `").append(root.path("failedRequests").asLong(0)).append("`\n");
            sb.append("- **Average Latency:** `").append(String.format("%.1f", root.path("avgLatencyMs").asDouble(0.0))).append("ms`\n");
            sb.append("- **Success Rate:** `").append(String.format("%.1f", root.path("successRate").asDouble(100.0))).append("%`\n");
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    public String formatRequestLogsMarkdown(String jsonResult, String toolName) {
        StringBuilder sb = new StringBuilder();
        sb.append(getFallbackBanner(toolName));
        try {
            JsonNode root = MAPPER.readTree(jsonResult);
            JsonNode logs = root.path("logs");
            sb.append("### 📋 Recent Request Telemetry Logs\n\n");
            sb.append("**Returned Logs:** `").append(root.path("returnedCount").asInt(logs.size())).append("`\n\n");

            if (logs.isEmpty()) {
                sb.append("*No recent request logs recorded in database.*\n");
                return sb.toString();
            }

            sb.append("| Method | Path | Status | Latency | Client IP | Timestamp |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
            for (JsonNode l : logs) {
                sb.append("| `").append(l.path("method").asText()).append("` | `")
                  .append(l.path("normalizedPath").asText("/")).append("` | `")
                  .append(l.path("statusCode").asInt()).append("` | `")
                  .append(l.path("latencyMs").asLong()).append("ms` | `")
                  .append(l.path("clientIp").asText("-")).append("` | `")
                  .append(l.path("timestamp").asText("-")).append("` |\n");
            }
        } catch (Exception e) {
            sb.append("```json\n").append(jsonResult).append("\n```");
        }
        return sb.toString();
    }

    private void addToolDeclarations(ArrayNode declList) {
        // Tool 1: list_applications
        ObjectNode t1 = declList.addObject();
        t1.put("name", "list_applications");
        t1.put("description", "List all monitored applications registered by the user in Sentinel.");
        ObjectNode p1 = t1.putObject("parameters");
        p1.put("type", "OBJECT");
        p1.putObject("properties");

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
        t5.put("description", "Get the list of discovered and documented API endpoints in the Sentinel API catalog for an application or across all applications.");
        ObjectNode p5 = t5.putObject("parameters");
        p5.put("type", "OBJECT");
        ObjectNode props5 = p5.putObject("properties");
        props5.putObject("applicationId").put("type", "INTEGER").put("description", "Optional application ID. If omitted, retrieves endpoints across all user applications.");

        // Tool 6: get_system_overview
        ObjectNode t6 = declList.addObject();
        t6.put("name", "get_system_overview");
        t6.put("description", "Get global system overview including total applications, healthy vs degraded count, 24h traffic, error counts, and MySQL/Redis status.");
        ObjectNode p6 = t6.putObject("parameters");
        p6.put("type", "OBJECT");
        p6.putObject("properties");
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
