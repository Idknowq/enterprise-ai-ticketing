package com.enterprise.ticketing.knowledge.parser;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentParser {

    boolean supports(MultipartFile file);

    ParsedDocument parse(MultipartFile file);
}
