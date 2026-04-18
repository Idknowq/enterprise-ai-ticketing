package com.enterprise.ticketing.knowledge.repository;

import com.enterprise.ticketing.knowledge.entity.CitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitationRepository extends JpaRepository<CitationEntity, Long> {
}
