package com.enterprise.ticketing.ticket.repository;

import com.enterprise.ticketing.ticket.entity.TicketEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEventEntity, Long> {

    List<TicketEventEntity> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
