package com.enterprise.ticketing.ticket.repository;

import com.enterprise.ticketing.ticket.entity.TicketCommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, Long> {

    List<TicketCommentEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
