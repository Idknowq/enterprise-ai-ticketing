package com.enterprise.ticketing.ai.repository;

import com.enterprise.ticketing.ai.entity.AiRunEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRunRepository extends JpaRepository<AiRunEntity, Long> {

    List<AiRunEntity> findByTicketIdOrderByCreatedAtDescIdDesc(Long ticketId);
}
