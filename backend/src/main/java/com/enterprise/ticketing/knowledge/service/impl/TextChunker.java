package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TextChunker {

    private final ApplicationProperties applicationProperties;

    public TextChunker(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public List<TextChunk> chunk(String rawText) {
        String text = normalize(rawText);
        if (!StringUtils.hasText(text)) {
            return List.of();
        }

        int chunkSize = Math.max(200, applicationProperties.getKnowledge().getChunkSize());
        int overlap = Math.max(0, Math.min(applicationProperties.getKnowledge().getChunkOverlap(), chunkSize / 2));
        List<TextChunk> chunks = new ArrayList<>();

        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int tentativeEnd = Math.min(start + chunkSize, text.length());
            int end = adjustEnd(text, start, tentativeEnd, chunkSize);
            String content = text.substring(start, end).trim();
            if (StringUtils.hasText(content)) {
                chunks.add(new TextChunk(index++, content, snippet(content)));
            }
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }

        return chunks;
    }

    private String normalize(String rawText) {
        String normalized = rawText == null ? "" : rawText.replace("\r\n", "\n");
        normalized = normalized.replaceAll("[\\t\\x0B\\f]+", " ");
        normalized = normalized.replaceAll("(?m)[ ]{2,}", " ");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private int adjustEnd(String text, int start, int tentativeEnd, int chunkSize) {
        if (tentativeEnd >= text.length()) {
            return text.length();
        }

        int windowStart = Math.max(start + (int) (chunkSize * 0.7), start + 1);
        for (int i = tentativeEnd; i >= windowStart; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return tentativeEnd;
    }

    private String snippet(String content) {
        String normalized = content.replace('\n', ' ').trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 237) + "...";
    }
}
