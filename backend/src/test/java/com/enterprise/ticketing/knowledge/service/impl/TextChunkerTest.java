package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    @Test
    void chunkReturnsEmptyListForBlankInput() {
        TextChunker textChunker = new TextChunker(applicationProperties(400, 80));

        assertThat(textChunker.chunk(" \n\t  ")).isEmpty();
    }

    @Test
    void chunkNormalizesWhitespaceAndKeepsOneChunkForShortText() {
        TextChunker textChunker = new TextChunker(applicationProperties(400, 80));

        List<TextChunk> chunks = textChunker.chunk("First\t\tline  with  spaces.\r\n\r\n\r\nSecond line.\n\n\nThird line.");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).content()).isEqualTo("First line with spaces.\n\nSecond line.\n\nThird line.");
        assertThat(chunks.get(0).snippet()).isEqualTo("First line with spaces.  Second line.  Third line.");
    }

    @Test
    void chunkUsesMinimumChunkSizeAndCapsOverlap() {
        TextChunker textChunker = new TextChunker(applicationProperties(50, 500));

        List<TextChunk> chunks = textChunker.chunk(repeatWord("segment", 32));

        assertThat(chunks).hasSize(2);
        assertThat(chunks).extracting(TextChunk::index).containsExactly(0, 1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.content().length()).isLessThanOrEqualTo(200));
    }

    @Test
    void chunkBuildsTrimmedSnippetForLongContent() {
        TextChunker textChunker = new TextChunker(applicationProperties(320, 40));

        List<TextChunk> chunks = textChunker.chunk(repeatWord("observability", 40));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).content().length()).isGreaterThan(240);
        assertThat(chunks.get(0).snippet()).hasSize(240).endsWith("...");
    }

    private static ApplicationProperties applicationProperties(int chunkSize, int chunkOverlap) {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getKnowledge().setChunkSize(chunkSize);
        properties.getKnowledge().setChunkOverlap(chunkOverlap);
        return properties;
    }

    private static String repeatWord(String word, int count) {
        return String.join(" ", java.util.Collections.nCopies(count, word));
    }
}
