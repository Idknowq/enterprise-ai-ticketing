package com.enterprise.ticketing.knowledge.service;

import java.util.List;

public interface EmbeddingProvider {

    String modelName();

    int dimension();

    List<float[]> embed(List<String> texts);
}
