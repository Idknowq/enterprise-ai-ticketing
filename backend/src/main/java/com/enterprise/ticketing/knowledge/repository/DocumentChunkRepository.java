package com.enterprise.ticketing.knowledge.repository;

import com.enterprise.ticketing.knowledge.entity.DocumentChunkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunkEntity, Long> {

    Optional<DocumentChunkEntity> findByChunkId(String chunkId);
}
