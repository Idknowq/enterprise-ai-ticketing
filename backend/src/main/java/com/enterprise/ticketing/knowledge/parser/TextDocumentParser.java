package com.enterprise.ticketing.knowledge.parser;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TextDocumentParser implements DocumentParser {

    @Override
    public boolean supports(MultipartFile file) {
        String filename = lowerFilename(file);
        return filename.endsWith(".txt") || "text/plain".equalsIgnoreCase(file.getContentType());
    }

    @Override
    public ParsedDocument parse(MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return new ParsedDocument(KnowledgeDocumentType.TXT, normalize(content), "text/plain");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_PROCESSING_FAILED, "Failed to parse text file");
        }
    }

    private String lowerFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n").trim();
    }
}
