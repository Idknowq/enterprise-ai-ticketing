package com.enterprise.ticketing.knowledge.parser;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DocumentParserRegistry {

    private final List<DocumentParser> parsers;

    public DocumentParserRegistry(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    public ParsedDocument parse(MultipartFile file) {
        return parsers.stream()
                .filter(parser -> parser.supports(file))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.KNOWLEDGE_UNSUPPORTED_FILE,
                        "Only Markdown, PDF, and TXT documents are supported"
                ))
                .parse(file);
    }
}
