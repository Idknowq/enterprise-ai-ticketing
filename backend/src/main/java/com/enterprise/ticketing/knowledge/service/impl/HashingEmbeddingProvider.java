package com.enterprise.ticketing.knowledge.service.impl;

import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.knowledge.service.EmbeddingProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class HashingEmbeddingProvider implements EmbeddingProvider {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]+|[\\p{L}\\p{N}_-]+");

    private final ApplicationProperties applicationProperties;

    public HashingEmbeddingProvider(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String modelName() {
        return applicationProperties.getKnowledge().getEmbedding().getModel();
    }

    @Override
    public int dimension() {
        return applicationProperties.getKnowledge().getEmbeddingDimension();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embedSingle(text));
        }
        return vectors;
    }

    private float[] embedSingle(String text) {
        int dimension = dimension();
        float[] vector = new float[dimension];

        Matcher matcher = TOKEN_PATTERN.matcher(text == null ? "" : text.toLowerCase(Locale.ROOT));
        int tokenCount = 0;
        while (matcher.find()) {
            String token = matcher.group();
            int index = Math.floorMod(token.hashCode(), dimension);
            int sign = (Integer.rotateLeft(token.hashCode(), 13) & 1) == 0 ? 1 : -1;
            vector[index] += sign;
            tokenCount++;
        }

        if (tokenCount == 0) {
            return vector;
        }

        float norm = 0F;
        for (float value : vector) {
            norm += value * value;
        }
        norm = (float) Math.sqrt(norm);
        if (norm == 0F) {
            return vector;
        }

        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
        return vector;
    }
}
