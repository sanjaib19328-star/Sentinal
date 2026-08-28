package com.sentinel.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.api.dto.AiTestRunReportDto;
import com.sentinel.api.dto.AiTestStepResultDto;
import com.sentinel.api.dto.ConversationDetailDto;
import com.sentinel.api.dto.ConversationDto;
import com.sentinel.api.dto.ConversationMessageDto;
import com.sentinel.api.dto.CreateConversationRequest;
import com.sentinel.api.dto.RunAiTestRequest;
import com.sentinel.api.dto.SendMessageRequest;
import com.sentinel.api.dto.UpdateConversationRequest;
import com.sentinel.api.exception.ResourceNotFoundException;
import com.sentinel.api.model.Application;
import com.sentinel.api.model.Conversation;
import com.sentinel.api.model.ConversationMessage;
import com.sentinel.api.model.MessageSender;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.ConversationMessageRepository;
import com.sentinel.api.repository.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    private static final ObjectMapper MAPPER = com.fasterxml.jackson.databind.json.JsonMapper.builder()
        .findAndAddModules()
        .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build();

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final AiTestEngineService aiTestEngineService;
    private final GeminiService geminiService;

    public ConversationService(
        ConversationRepository conversationRepository,
        ConversationMessageRepository messageRepository,
        ApplicationRepository applicationRepository,
        AiTestEngineService aiTestEngineService,
        GeminiService geminiService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.applicationRepository = applicationRepository;
        this.aiTestEngineService = aiTestEngineService;
        this.geminiService = geminiService;
    }

    @Transactional
    public ConversationDetailDto createConversation(Long userId, CreateConversationRequest request) {
        String appName = null;
        if (request.getApplicationId() != null) {
            Application app = applicationRepository.findByIdAndOwnerId(request.getApplicationId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found or unauthorized"));
            appName = app.getName();
        }

        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = autoGenerateTitle(request.getInitialPrompt(), appName);
        }

        Conversation conversation = new Conversation(userId, request.getApplicationId(), title, request.getMetadataJson());
        Conversation saved = conversationRepository.save(conversation);

        // If initial prompt provided, save user message and generate welcome response using Gemini
        if (request.getInitialPrompt() != null && !request.getInitialPrompt().isBlank()) {
            ConversationMessage userMsg = new ConversationMessage(saved, MessageSender.USER, request.getInitialPrompt(), null);
            messageRepository.save(userMsg);

            String initialAiReply = geminiService.generateResponse(userId, request.getApplicationId(), Collections.emptyList(), request.getInitialPrompt());
            ConversationMessage aiMsg = new ConversationMessage(saved, MessageSender.ASSISTANT, initialAiReply, null);
            messageRepository.save(aiMsg);
        }

        return getConversation(saved.getId(), userId);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(Long userId, Long applicationId, String search) {
        List<Conversation> list;
        if (search != null && !search.isBlank()) {
            list = conversationRepository.searchByUserIdAndQuery(userId, search.trim());
        } else if (applicationId != null) {
            list = conversationRepository.findByUserIdAndApplicationIdOrderByUpdatedAtDesc(userId, applicationId);
        } else {
            list = conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        }

        Map<Long, String> appNameCache = new HashMap<>();

        return list.stream().map(c -> {
            ConversationDto dto = new ConversationDto();
            dto.setId(c.getId());
            dto.setUserId(c.getUserId());
            dto.setApplicationId(c.getApplicationId());
            if (c.getApplicationId() != null) {
                dto.setApplicationName(appNameCache.computeIfAbsent(c.getApplicationId(), id ->
                    applicationRepository.findById(id).map(Application::getName).orElse(null)
                ));
            }
            dto.setTitle(c.getTitle());
            dto.setMetadataJson(c.getMetadataJson());
            dto.setCreatedAt(c.getCreatedAt());
            dto.setUpdatedAt(c.getUpdatedAt());

            List<ConversationMessage> msgs = messageRepository.findByConversationIdOrderByCreatedAtAsc(c.getId());
            dto.setMessageCount(msgs.size());
            if (!msgs.isEmpty()) {
                ConversationMessage last = msgs.get(msgs.size() - 1);
                String preview = last.getContent();
                if (preview.length() > 80) preview = preview.substring(0, 77) + "...";
                dto.setLastMessagePreview(preview);
            }
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDetailDto getConversation(Long id, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        ConversationDetailDto dto = new ConversationDetailDto();
        dto.setId(conversation.getId());
        dto.setUserId(conversation.getUserId());
        dto.setApplicationId(conversation.getApplicationId());
        dto.setTitle(conversation.getTitle());
        dto.setMetadataJson(conversation.getMetadataJson());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());

        if (conversation.getApplicationId() != null) {
            applicationRepository.findById(conversation.getApplicationId()).ifPresent(app -> {
                dto.setApplicationName(app.getName());
                dto.setApplicationBaseUrl(app.getBaseUrl());
            });
        }

        List<ConversationMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        dto.setMessages(messages.stream().map(this::mapMessageToDto).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public ConversationDto updateConversation(Long id, Long userId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            conversation.setTitle(request.getTitle().trim());
        }
        if (request.getMetadataJson() != null) {
            conversation.setMetadataJson(request.getMetadataJson());
        }

        Conversation saved = conversationRepository.save(conversation);
        ConversationDto dto = new ConversationDto();
        dto.setId(saved.getId());
        dto.setUserId(saved.getUserId());
        dto.setApplicationId(saved.getApplicationId());
        dto.setTitle(saved.getTitle());
        dto.setMetadataJson(saved.getMetadataJson());
        dto.setCreatedAt(saved.getCreatedAt());
        dto.setUpdatedAt(saved.getUpdatedAt());
        return dto;
    }

    @Transactional
    public void deleteConversation(Long id, Long userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationDetailDto sendMessage(Long conversationId, Long userId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        // Save User Message
        ConversationMessage userMsg = new ConversationMessage(
            conversation,
            MessageSender.USER,
            request.getContent(),
            sanitizeMetadata(request.getMetadataJson())
        );
        messageRepository.save(userMsg);

        String userPrompt = request.getContent().toLowerCase();
        boolean isAiTestTrigger = request.isTriggerAiTesting() ||
            userPrompt.contains("test all apis") ||
            userPrompt.contains("run tests") ||
            userPrompt.contains("test suite") ||
            userPrompt.contains("test endpoints");

        if (isAiTestTrigger && conversation.getApplicationId() != null) {
            // Autonomous AI Test Suite Execution
            RunAiTestRequest testReq = new RunAiTestRequest();
            testReq.setApplicationId(conversation.getApplicationId());
            testReq.setApiKeyId(request.getApiKeyId());
            testReq.setFileBase64(request.getFileBase64());
            testReq.setFileName(request.getFileName());
            testReq.setFileContentType(request.getFileContentType());
            testReq.setFocusPrompt(request.getContent());

            AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(userId, testReq);

            String reportJson = null;
            try {
                reportJson = MAPPER.writeValueAsString(report);
            } catch (Exception e) {
                log.warn("Failed to serialize report JSON: {}", e.getMessage());
            }

            String summaryText = formatReportMarkdown(report);
            ConversationMessage aiMsg = new ConversationMessage(
                conversation,
                MessageSender.ASSISTANT,
                summaryText,
                reportJson
            );
            messageRepository.save(aiMsg);
        } else {
            // Contextual Gemini AI Assistant Response with live tools
            List<ConversationMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
            String aiResponseText = geminiService.generateResponse(
                userId,
                conversation.getApplicationId(),
                history,
                request.getContent()
            );
            ConversationMessage aiMsg = new ConversationMessage(
                conversation,
                MessageSender.ASSISTANT,
                aiResponseText,
                null
            );
            messageRepository.save(aiMsg);
        }

        // Auto-update default title if needed
        if (conversation.getTitle().equals("New Chat") || conversation.getTitle().equals("New AI Session")) {
            conversation.setTitle(autoGenerateTitle(request.getContent(), null));
            conversationRepository.save(conversation);
        }

        return getConversation(conversationId, userId);
    }

    @Transactional
    public AiTestRunReportDto runAiTestForConversation(Long conversationId, Long userId, RunAiTestRequest request) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (request.getApplicationId() == null) {
            request.setApplicationId(conversation.getApplicationId());
        }

        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(userId, request);

        // Record User Action with details of what was sent
        StringBuilder userMsgText = new StringBuilder();
        userMsgText.append("🚀 **Run Autonomous AI Test Suite**\n");
        userMsgText.append("- **Target Application:** ").append(report.getApplicationName()).append(" (ID: ").append(report.getApplicationId()).append(")\n");
        if (request.getFileName() != null && !request.getFileName().isBlank()) {
            userMsgText.append("- **Provided Test Payload:** `").append(request.getFileName()).append("` (Multipart file attached)\n");
        }
        if (request.isApproveDestructiveOperations()) {
            userMsgText.append("- **Permissions:** State-changing/clean operations approved\n");
        }
        if (request.getInitialContext() != null && !request.getInitialContext().isEmpty()) {
            userMsgText.append("- **Provided Context:** ").append(request.getInitialContext().keySet()).append("\n");
        }

        ConversationMessage userMsg = new ConversationMessage(
            conversation,
            MessageSender.USER,
            userMsgText.toString(),
            null
        );
        messageRepository.save(userMsg);

        // Record Assistant Report
        String reportJson = null;
        try {
            reportJson = MAPPER.writeValueAsString(report);
        } catch (Exception e) {
            log.warn("Failed to serialize report JSON: {}", e.getMessage());
        }

        ConversationMessage aiMsg = new ConversationMessage(
            conversation,
            MessageSender.ASSISTANT,
            formatReportMarkdown(report),
            reportJson
        );
        messageRepository.save(aiMsg);

        return report;
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageDto> getMessages(Long conversationId, Long userId) {
        conversationRepository.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
            .stream().map(this::mapMessageToDto).collect(Collectors.toList());
    }

    private ConversationMessageDto mapMessageToDto(ConversationMessage msg) {
        return new ConversationMessageDto(
            msg.getId(),
            msg.getConversation().getId(),
            msg.getSender(),
            msg.getContent(),
            msg.getMetadataJson(),
            msg.getCreatedAt()
        );
    }

    private String autoGenerateTitle(String prompt, String appName) {
        if (prompt != null && !prompt.isBlank()) {
            String clean = prompt.trim();
            if (clean.length() > 35) clean = clean.substring(0, 32) + "...";
            return clean;
        }
        if (appName != null) {
            return appName + " Testing Session";
        }
        return "AI Assistant Session";
    }

    private String formatReportMarkdown(AiTestRunReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Autonomous AI API Test Report: ").append(report.getApplicationName()).append("\n\n");
        sb.append("**Overall Status:** `").append(report.getOverallStatus()).append("` | ");
        sb.append("**Passed:** `").append(report.getPassedSteps()).append("/").append(report.getTotalSteps()).append("` | ");
        sb.append("**Avg Latency:** `").append(String.format("%.1f", report.getAvgLatencyMs())).append("ms`\n\n");
        sb.append(report.getExecutiveSummary()).append("\n\n");

        sb.append("| # | Method | Endpoint / Resolved Path | Status | Latency | Result |\n");
        sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
        int stepNum = 1;
        for (AiTestStepResultDto step : report.getStepResults()) {
            String resText;
            if (step.isPassed()) {
                resText = "✅ PASSED";
            } else if (step.isRequiresApproval() || "REQUIRES_CONFIRMATION".equals(step.getExecutionStatus())) {
                resText = "⚠️ Confirmation Needed";
            } else if (step.isBlocked() || "BLOCKED".equals(step.getExecutionStatus()) || "SKIPPED_DUE_TO_DEPENDENCY".equals(step.getExecutionStatus())) {
                resText = "🚫 BLOCKED";
            } else {
                resText = "❌ FAILED (" + (step.getStatus() > 0 ? step.getStatus() : "Error") + ")";
            }

            String pathCol = step.getResolvedPath() != null ? step.getResolvedPath() : step.getEndpoint();
            if (step.getBlockedReason() != null && !step.getBlockedReason().isBlank()) {
                pathCol += "<br>↳ *Reason: " + step.getBlockedReason() + "*";
            }

            sb.append("| ").append(stepNum++).append(" | `").append(step.getMethod()).append("` | `")
              .append(pathCol).append("` | ")
              .append(step.getStatus() > 0 ? step.getStatus() : "-").append(" | ")
              .append(step.getLatencyMs()).append("ms | ")
              .append(resText).append(" |\n");
        }

        if (report.getRememberedContext() != null && !report.getRememberedContext().isEmpty()) {
            sb.append("\n**Propagated Context Variables:**\n");
            for (Map.Entry<String, String> e : report.getRememberedContext().entrySet()) {
                if (!e.getKey().contains("base64")) {
                    String val = e.getKey().toLowerCase().contains("token") || e.getKey().toLowerCase().contains("key")
                        ? "••••••••" : e.getValue();
                    sb.append("- `").append(e.getKey()).append("`: `").append(val).append("`\n");
                }
            }
        }

        if (report.getFailureAnalysis() != null && !report.getFailureAnalysis().isBlank()) {
            sb.append("\n**Dependency & Execution Notes:**\n").append(report.getFailureAnalysis()).append("\n");
        }

        return sb.toString();
    }

    private String sanitizeMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return null;
        return metadataJson
            .replaceAll("(?i)\"sk_[a-zA-Z0-9_]+\"", "\"sk_REDACTED\"")
            .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"REDACTED\"");
    }
}
