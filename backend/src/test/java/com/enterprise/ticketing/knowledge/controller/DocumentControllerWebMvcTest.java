package com.enterprise.ticketing.knowledge.controller;

import com.enterprise.ticketing.auth.security.JwtAuthenticationFilter;
import com.enterprise.ticketing.knowledge.domain.DocumentIndexStatus;
import com.enterprise.ticketing.knowledge.domain.KnowledgeAccessLevel;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;
import com.enterprise.ticketing.knowledge.dto.DocumentListQuery;
import com.enterprise.ticketing.knowledge.dto.DocumentListResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentMetadataResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentResponse;
import com.enterprise.ticketing.knowledge.dto.DocumentUploadRequest;
import com.enterprise.ticketing.knowledge.service.DocumentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(
        controllers = DocumentController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.enterprise.ticketing.common.handler.GlobalExceptionHandler.class)
class DocumentControllerWebMvcTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-04-19T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void listDocumentsBindsFiltersAndReturnsWrappedPage() throws Exception {
        when(documentService.listDocuments(any(DocumentListQuery.class))).thenReturn(new DocumentListResponse(
                List.of(documentResponse(701L, "VPN Handbook")),
                2,
                10,
                21,
                3
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/documents")
                        .param("keyword", "vpn")
                        .param("category", "VPN")
                        .param("department", "IT")
                        .param("accessLevel", "INTERNAL")
                        .param("indexStatus", "INDEXED")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(701))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("VPN Handbook"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(21));

        ArgumentCaptor<DocumentListQuery> queryCaptor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).listDocuments(queryCaptor.capture());
        DocumentListQuery query = queryCaptor.getValue();
        assertEquals("vpn", query.getKeyword());
        assertEquals("VPN", query.getCategory());
        assertEquals("IT", query.getDepartment());
        assertEquals(KnowledgeAccessLevel.INTERNAL, query.getAccessLevel());
        assertEquals(DocumentIndexStatus.INDEXED, query.getIndexStatus());
        assertEquals(2, query.getPage());
        assertEquals(10, query.getSize());
    }

    @Test
    void uploadDocumentBindsMultipartFormAndReturnsWrappedDocument() throws Exception {
        when(documentService.uploadDocument(any(DocumentUploadRequest.class)))
                .thenReturn(documentResponse(801L, "VPN Runbook"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vpn-runbook.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "VPN troubleshooting steps".getBytes()
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "VPN Runbook")
                        .param("category", "VPN")
                        .param("department", "IT")
                        .param("accessLevel", "INTERNAL")
                        .param("version", "v1.0")
                        .param("updatedAt", "2026-04-19T12:00:00Z"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(801))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("VPN Runbook"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.indexStatus").value("INDEXED"));

        ArgumentCaptor<DocumentUploadRequest> requestCaptor = ArgumentCaptor.forClass(DocumentUploadRequest.class);
        verify(documentService).uploadDocument(requestCaptor.capture());
        DocumentUploadRequest request = requestCaptor.getValue();
        assertEquals("vpn-runbook.txt", request.getFile().getOriginalFilename());
        assertEquals("VPN Runbook", request.getTitle());
        assertEquals("VPN", request.getCategory());
        assertEquals("IT", request.getDepartment());
        assertEquals(KnowledgeAccessLevel.INTERNAL, request.getAccessLevel());
        assertEquals("v1.0", request.getVersion());
        assertEquals(UPDATED_AT, request.getUpdatedAt());
    }

    @Test
    void uploadDocumentReturnsValidationErrorsForMissingRequiredFields() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                        .param("title", "Incomplete document"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.file").value("file is required"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.category").value("category is required"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessLevel").value("accessLevel is required"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.version").value("version is required"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.updatedAt").value("updatedAt is required"));
    }

    @Test
    void listDocumentsReturnsValidationErrorForInvalidPagination() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/documents")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON_VALIDATION_ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value("page must be >= 0"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value("size must be <= 100"));
    }

    private static DocumentResponse documentResponse(Long id, String title) {
        return new DocumentResponse(
                id,
                title,
                title.toLowerCase().replace(" ", "-") + ".txt",
                KnowledgeDocumentType.TXT,
                DocumentIndexStatus.INDEXED,
                3,
                "hashing-v1",
                new DocumentMetadataResponse(id, title, "VPN", "IT", KnowledgeAccessLevel.INTERNAL, "v1.0", UPDATED_AT),
                UPDATED_AT,
                UPDATED_AT
        );
    }
}
