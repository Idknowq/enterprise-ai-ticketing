package com.enterprise.ticketing.ticket.repository;

import com.enterprise.ticketing.ticket.entity.TicketEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<TicketEntity, Long>, JpaSpecificationExecutor<TicketEntity> {

    @Override
    @EntityGraph(attributePaths = {"requester", "assignee"})
    java.util.Optional<TicketEntity> findById(Long id);
}
