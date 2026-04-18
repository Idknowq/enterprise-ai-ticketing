package com.enterprise.ticketing.knowledge.parser;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentType;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(MultipartFile file) {
        return lowerFilename(file).endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType());
    }

    @Override
    public ParsedDocument parse(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String content = textStripper.getText(document);
            return new ParsedDocument(KnowledgeDocumentType.PDF, normalize(content), "application/pdf");
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.KNOWLEDGE_DOCUMENT_PROCESSING_FAILED, "Failed to parse pdf file");
        }
    }

    private String lowerFilename(MultipartFile file) {
        return file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n").trim();
    }
}
