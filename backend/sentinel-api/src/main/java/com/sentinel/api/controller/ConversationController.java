package com.sentinel.api.controller;

import com.sentinel.api.dto.AiTestPlanDto;
import com.sentinel.api.dto.AiTestRunReportDto;
import com.sentinel.api.dto.ConversationDetailDto;
import com.sentinel.api.dto.ConversationDto;
import com.sentinel.api.dto.ConversationMessageDto;
import com.sentinel.api.dto.CreateConversationRequest;
import com.sentinel.api.dto.RunAiTestRequest;
import com.sentinel.api.dto.SendMessageRequest;
import com.sentinel.api.dto.UpdateConversationRequest;
import com.sentinel.api.security.UserPrincipal;
import com.sentinel.api.service.AiTestEngineService;
import com.sentinel.api.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ConversationController {

    private final ConversationService conversationService;
    private final AiTestEngineService aiTestEngineService;

    public ConversationController(
        ConversationService conversationService,
        AiTestEngineService aiTestEngineService
    ) {
        this.conversationService = conversationService;
        this.aiTestEngineService = aiTestEngineService;
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDetailDto> createConversation(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateConversationRequest request
    ) {
        ConversationDetailDto created = conversationService.createConversation(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> listConversations(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(required = false) Long applicationId,
        @RequestParam(required = false) String search
    ) {
        List<ConversationDto> list = conversationService.listConversations(principal.getId(), applicationId, search);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDetailDto> getConversation(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        ConversationDetailDto detail = conversationService.getConversation(id, principal.getId());
        return ResponseEntity.ok(detail);
    }

    @PatchMapping("/conversations/{id}")
    public ResponseEntity<ConversationDto> updateConversation(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody UpdateConversationRequest request
    ) {
        ConversationDto updated = conversationService.updateConversation(id, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/conversations/{id}")
    public ResponseEntity<ConversationDto> updateConversationPut(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody UpdateConversationRequest request
    ) {
        ConversationDto updated = conversationService.updateConversation(id, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        conversationService.deleteConversation(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ConversationDetailDto> sendMessage(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @Valid @RequestBody SendMessageRequest request
    ) {
        ConversationDetailDto detail = conversationService.sendMessage(id, principal.getId(), request);
        return ResponseEntity.ok(detail);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<ConversationMessageDto>> getMessages(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id
    ) {
        List<ConversationMessageDto> messages = conversationService.getMessages(id, principal.getId());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{id}/run-ai-test")
    public ResponseEntity<AiTestRunReportDto> runAiTestForConversation(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long id,
        @RequestBody RunAiTestRequest request
    ) {
        AiTestRunReportDto report = conversationService.runAiTestForConversation(id, principal.getId(), request);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/applications/{applicationId}/ai-test-plan")
    public ResponseEntity<AiTestPlanDto> getAiTestPlan(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        AiTestPlanDto plan = aiTestEngineService.generateTestPlan(principal.getId(), applicationId);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/applications/{applicationId}/run-ai-test")
    public ResponseEntity<AiTestRunReportDto> runDirectAiTest(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestBody RunAiTestRequest request
    ) {
        request.setApplicationId(applicationId);
        AiTestRunReportDto report = aiTestEngineService.executeAiTestRun(principal.getId(), request);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/applications/{applicationId}/bulk-api-check")
    public ResponseEntity<com.sentinel.api.dto.BulkApiCheckResponse> bulkApiCheck(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestBody com.sentinel.api.dto.BulkApiCheckRequest request
    ) {
        request.setApplicationId(applicationId);
        com.sentinel.api.dto.BulkApiCheckResponse response = aiTestEngineService.executeBulkApiCheck(principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications/{applicationId}/ai-test-session")
    public ResponseEntity<com.sentinel.api.dto.AiTestSessionDto> getAiTestSession(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        com.sentinel.api.dto.AiTestSessionDto session = aiTestEngineService.getOrCreateSession(principal.getId(), applicationId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/applications/{applicationId}/ai-test-session/input")
    public ResponseEntity<com.sentinel.api.dto.AiTestSessionDto> provideSessionInput(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId,
        @RequestBody Map<String, String> body
    ) {
        String inputKey = body.get("inputKey");
        String inputValue = body.get("inputValue");
        String fileBase64 = body.get("fileBase64");
        String fileName = body.get("fileName");
        String fileContentType = body.get("fileContentType");
        com.sentinel.api.dto.AiTestSessionDto session = aiTestEngineService.provideSessionInput(
            principal.getId(), applicationId, inputKey, inputValue, fileBase64, fileName, fileContentType
        );
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/applications/{applicationId}/ai-test-session")
    public ResponseEntity<Void> cancelAiTestSession(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable Long applicationId
    ) {
        aiTestEngineService.cancelSession(principal.getId(), applicationId);
        return ResponseEntity.noContent().build();
    }
}
