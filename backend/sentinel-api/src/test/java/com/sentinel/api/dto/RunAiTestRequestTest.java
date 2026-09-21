package com.sentinel.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunAiTestRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testDeserializationWithCamelCase() throws Exception {
        String json = """
            {
                "applicationId": 13,
                "apiKeyId": 9,
                "approveDestructiveOperations": true,
                "fileBase64": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
                "fileName": "photo.png",
                "fileContentType": "image/png"
            }
            """;

        RunAiTestRequest request = mapper.readValue(json, RunAiTestRequest.class);

        assertEquals(13L, request.getApplicationId());
        assertEquals(9L, request.getApiKeyId());
        assertEquals(true, request.isApproveDestructiveOperations());
        assertEquals("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==", request.getFileBase64());
        assertEquals("photo.png", request.getFileName());
        assertEquals("image/png", request.getFileContentType());
    }

    @Test
    void testDeserializationWithSnakeCaseAliases() throws Exception {
        String json = """
            {
                "application_id": 13,
                "api_key_id": 9,
                "approve_destructive_operations": true,
                "file_base64": "dGVzdC1iYXNlNjQ=",
                "file_name": "sample.jpg",
                "file_content_type": "image/jpeg"
            }
            """;

        RunAiTestRequest request = mapper.readValue(json, RunAiTestRequest.class);

        assertEquals("dGVzdC1iYXNlNjQ=", request.getFileBase64());
        assertEquals("sample.jpg", request.getFileName());
        assertEquals("image/jpeg", request.getFileContentType());
    }

    @Test
    void testDeserializationWithFileAlias() throws Exception {
        String json = """
            {
                "file": "dGVzdC1maWxl",
                "filename": "document.png",
                "contentType": "image/png"
            }
            """;

        RunAiTestRequest request = mapper.readValue(json, RunAiTestRequest.class);

        assertEquals("dGVzdC1maWxl", request.getFileBase64());
        assertEquals("document.png", request.getFileName());
        assertEquals("image/png", request.getFileContentType());
    }

    @Test
    void testFallbackToInitialContext() throws Exception {
        String json = """
            {
                "applicationId": 15,
                "initialContext": {
                    "file_base64": "Y29udGV4dC1iYXNlNjQ=",
                    "file_name": "context_img.png",
                    "file_content_type": "image/png"
                }
            }
            """;

        RunAiTestRequest request = mapper.readValue(json, RunAiTestRequest.class);

        assertEquals("Y29udGV4dC1iYXNlNjQ=", request.getFileBase64());
        assertEquals("context_img.png", request.getFileName());
        assertEquals("image/png", request.getFileContentType());
    }
}
