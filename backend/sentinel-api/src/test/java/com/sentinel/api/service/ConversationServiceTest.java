package com.sentinel.api.service;

import com.sentinel.api.dto.ConversationDetailDto;
import com.sentinel.api.dto.SendMessageRequest;
import com.sentinel.api.model.Conversation;
import com.sentinel.api.model.ConversationMessage;
import com.sentinel.api.model.MessageSender;
import com.sentinel.api.repository.ApplicationRepository;
import com.sentinel.api.repository.ConversationMessageRepository;
import com.sentinel.api.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private ConversationRepository conversationRepository;
    private ConversationMessageRepository messageRepository;
    private ApplicationRepository applicationRepository;
    private AiTestEngineService aiTestEngineService;
    private GeminiService geminiService;
    private ImageProcessingService imageProcessingService;
    private ConversationService conversationService;

    // Distinct 2x2 test PNG A
    private static final String IMAGE_A_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFElEQVR42mNk+M9QzwAEjAwMDAwAFAAC/0wJv8gAAAAASUVORK5CYII=";
    // Distinct 3x3 test PNG B
    private static final String IMAGE_B_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAMAAAADCAYAAABWKLW/AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAB9JREFUeNpi+P//PwMTAwMDEwMDEwMjAwMzAxMDIwMAgwCqFAb7QdE0nAAAAABJRU5ErkJggg==";
    // Distinct 4x4 test PNG C
    private static final String IMAGE_C_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAYAAACp8Z5+AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAABtJREFUeNpi/P//PwMEYGJgYGBiIAkwMAAAZgAE/0Vz4gAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ConversationMessageRepository.class);
        applicationRepository = mock(ApplicationRepository.class);
        aiTestEngineService = mock(AiTestEngineService.class);
        geminiService = mock(GeminiService.class);
        imageProcessingService = new ImageProcessingService();

        conversationService = new ConversationService(
            conversationRepository,
            messageRepository,
            applicationRepository,
            aiTestEngineService,
            geminiService,
            imageProcessingService
        );
    }

    private Conversation setupMockConversation(Long convId, Long userId, Long appId) {
        Conversation conv = new Conversation(userId, appId, "Test Chat", null);
        conv.setId(convId);
        when(conversationRepository.findByIdAndUserId(convId, userId)).thenReturn(Optional.of(conv));
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));
        return conv;
    }

    @Test
    void testTextOnlyMessage() {
        Long convId = 1L;
        Long userId = 100L;
        setupMockConversation(convId, userId, 10L);

        when(geminiService.generateResponse(anyLong(), any(), anyList(), anyString(), isNull(), isNull()))
            .thenReturn("Text response from assistant");

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Hello Sentinel");

        ConversationDetailDto detail = conversationService.sendMessage(convId, userId, req);

        // Verify Gemini received null image data
        verify(geminiService).generateResponse(
            eq(userId),
            eq(10L),
            anyList(),
            eq("Hello Sentinel"),
            isNull(),
            isNull()
        );

        // Verify user message stored without file metadata
        ArgumentCaptor<ConversationMessage> msgCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(messageRepository, atLeastOnce()).save(msgCaptor.capture());

        ConversationMessage savedUserMsg = msgCaptor.getAllValues().get(0);
        assertEquals("Hello Sentinel", savedUserMsg.getContent());
        assertNull(savedUserMsg.getMetadataJson());
    }

    @Test
    void testImageOnlyMessageUsesDefaultPromptAndForwardsImage() {
        Long convId = 2L;
        Long userId = 100L;
        setupMockConversation(convId, userId, 10L);

        when(geminiService.generateResponse(anyLong(), any(), anyList(), anyString(), anyString(), anyString()))
            .thenReturn("I analyzed the image");

        SendMessageRequest req = new SendMessageRequest();
        req.setFileBase64(IMAGE_A_BASE64);
        req.setFileName("sample_a.png");
        req.setFileContentType("image/png");

        ConversationDetailDto detail = conversationService.sendMessage(convId, userId, req);

        // Verify Gemini received default prompt + exact uploaded Base64
        verify(geminiService).generateResponse(
            eq(userId),
            eq(10L),
            anyList(),
            eq("Please analyze this image."),
            eq(IMAGE_A_BASE64),
            eq("image/png")
        );

        // Verify saved message has lightweight metadata
        ArgumentCaptor<ConversationMessage> msgCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(messageRepository, atLeastOnce()).save(msgCaptor.capture());

        ConversationMessage savedUserMsg = msgCaptor.getAllValues().get(0);
        assertEquals("Please analyze this image.", savedUserMsg.getContent());
        assertNotNull(savedUserMsg.getMetadataJson());
        assertTrue(savedUserMsg.getMetadataJson().contains("\"hasImage\":true"));
        assertTrue(savedUserMsg.getMetadataJson().contains("sample_a.png"));
        assertFalse(savedUserMsg.getMetadataJson().contains(IMAGE_A_BASE64), "Full Base64 must not be stored in MySQL");
    }

    @Test
    void testTextPlusImageMessage() {
        Long convId = 3L;
        Long userId = 100L;
        setupMockConversation(convId, userId, 10L);

        when(geminiService.generateResponse(anyLong(), any(), anyList(), anyString(), anyString(), anyString()))
            .thenReturn("Architecture diagram verified");

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Check this topology");
        req.setFileBase64(IMAGE_A_BASE64);
        req.setFileName("topology.png");
        req.setFileContentType("image/png");

        conversationService.sendMessage(convId, userId, req);

        verify(geminiService).generateResponse(
            eq(userId),
            eq(10L),
            anyList(),
            eq("Check this topology"),
            eq(IMAGE_A_BASE64),
            eq("image/png")
        );
    }

    @Test
    void testConsecutiveMessagesWithDifferentImagesDoNotReusePreviousImages() {
        Long convId = 4L;
        Long userId = 100L;
        setupMockConversation(convId, userId, 10L);

        // Turn 1: Upload Image A
        SendMessageRequest req1 = new SendMessageRequest();
        req1.setContent("Inspect Image A");
        req1.setFileBase64(IMAGE_A_BASE64);
        req1.setFileName("image_a.png");
        req1.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, req1);

        verify(geminiService).generateResponse(
            eq(userId), eq(10L), anyList(), eq("Inspect Image A"), eq(IMAGE_A_BASE64), eq("image/png")
        );

        // Turn 2: Upload Image B (Must receive B, NOT A)
        SendMessageRequest req2 = new SendMessageRequest();
        req2.setContent("Inspect Image B");
        req2.setFileBase64(IMAGE_B_BASE64);
        req2.setFileName("image_b.png");
        req2.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, req2);

        verify(geminiService).generateResponse(
            eq(userId), eq(10L), anyList(), eq("Inspect Image B"), eq(IMAGE_B_BASE64), eq("image/png")
        );

        // Turn 3: Upload Image C (Must receive C, NOT B, NOT A)
        SendMessageRequest req3 = new SendMessageRequest();
        req3.setContent("Inspect Image C");
        req3.setFileBase64(IMAGE_C_BASE64);
        req3.setFileName("image_c.png");
        req3.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, req3);

        verify(geminiService).generateResponse(
            eq(userId), eq(10L), anyList(), eq("Inspect Image C"), eq(IMAGE_C_BASE64), eq("image/png")
        );

        // Turn 4: Text only (Must NOT receive Image C, B, or A)
        SendMessageRequest req4 = new SendMessageRequest();
        req4.setContent("What was the difference?");
        conversationService.sendMessage(convId, userId, req4);

        verify(geminiService).generateResponse(
            eq(userId), eq(10L), anyList(), eq("What was the difference?"), isNull(), isNull()
        );
    }

    @Test
    void testThreeDistinctImagesProcessedSeparately() {
        Long convId = 15L;
        Long userId = 100L;
        setupMockConversation(convId, userId, 10L);

        // Separate Test 1 -> Image A
        SendMessageRequest reqA = new SendMessageRequest();
        reqA.setContent("Analyze Image A");
        reqA.setFileBase64(IMAGE_A_BASE64);
        reqA.setFileName("a.png");
        reqA.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, reqA);

        verify(geminiService).generateResponse(eq(userId), eq(10L), anyList(), eq("Analyze Image A"), eq(IMAGE_A_BASE64), eq("image/png"));

        // Separate Test 2 -> Image B
        SendMessageRequest reqB = new SendMessageRequest();
        reqB.setContent("Analyze Image B");
        reqB.setFileBase64(IMAGE_B_BASE64);
        reqB.setFileName("b.png");
        reqB.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, reqB);

        verify(geminiService).generateResponse(eq(userId), eq(10L), anyList(), eq("Analyze Image B"), eq(IMAGE_B_BASE64), eq("image/png"));

        // Separate Test 3 -> Image C
        SendMessageRequest reqC = new SendMessageRequest();
        reqC.setContent("Analyze Image C");
        reqC.setFileBase64(IMAGE_C_BASE64);
        reqC.setFileName("c.png");
        reqC.setFileContentType("image/png");
        conversationService.sendMessage(convId, userId, reqC);

        verify(geminiService).generateResponse(eq(userId), eq(10L), anyList(), eq("Analyze Image C"), eq(IMAGE_C_BASE64), eq("image/png"));
    }

    @Test
    void testEmptyMessageAndNoImageThrowsValidationException() {
        Long convId = 5L;
        Long userId = 100L;
        setupMockConversation(convId, userId, null);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("   "); // Blank
        req.setFileBase64(null);

        assertThrows(IllegalArgumentException.class, () -> conversationService.sendMessage(convId, userId, req));
    }

    @Test
    void testInvalidMimeTypeRejected() {
        Long convId = 6L;
        Long userId = 100L;
        setupMockConversation(convId, userId, null);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Check this document");
        req.setFileBase64(IMAGE_A_BASE64);
        req.setFileContentType("application/pdf"); // Invalid

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> conversationService.sendMessage(convId, userId, req));
        assertTrue(ex.getMessage().contains("Unsupported image type"));
    }

    @Test
    void testMalformedBase64Rejected() {
        Long convId = 7L;
        Long userId = 100L;
        setupMockConversation(convId, userId, null);

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("Corrupted payload");
        req.setFileBase64("NOT_VALID_BASE_64_&&#%$@!");
        req.setFileContentType("image/png");

        assertThrows(IllegalArgumentException.class, () -> conversationService.sendMessage(convId, userId, req));
    }

    @Test
    void testOversizedImageRejected() {
        // Create > 10MB dummy byte array
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        String largeBase64 = java.util.Base64.getEncoder().encodeToString(largeBytes);

        assertThrows(IllegalArgumentException.class, () ->
            imageProcessingService.validateAndDecodeImage(largeBase64, "image/png")
        );
    }

    @Test
    void testTemporaryFileLifecycleAutoDeleted() throws Exception {
        byte[] sampleBytes = java.util.Base64.getDecoder().decode(IMAGE_A_BASE64);
        AtomicReference<java.nio.file.Path> capturedTempPath = new AtomicReference<>();

        String result = imageProcessingService.withTempImageFile(sampleBytes, "image/png", tempPath -> {
            capturedTempPath.set(tempPath);
            assertTrue(Files.exists(tempPath), "Temp file must exist during consumer execution");
            assertEquals(sampleBytes.length, Files.size(tempPath));
            return "SUCCESS";
        });

        assertEquals("SUCCESS", result);
        assertNotNull(capturedTempPath.get());
        assertFalse(Files.exists(capturedTempPath.get()), "Temp file must be automatically deleted after execution");
    }
}
