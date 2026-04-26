package com.enterprise.ticketing.ticket.service.impl;

import com.enterprise.ticketing.auth.security.UserPrincipal;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.knowledge.domain.KnowledgeDocumentCategory;
import com.enterprise.ticketing.ticket.domain.TicketStatus;
import com.enterprise.ticketing.ticket.dto.TicketDetailResponse;
import com.enterprise.ticketing.ticket.dto.TicketListQuery;
import com.enterprise.ticketing.ticket.dto.TicketListResponse;
import com.enterprise.ticketing.ticket.entity.TicketEntity;
import com.enterprise.ticketing.ticket.repository.TicketCommentRepository;
import com.enterprise.ticketing.ticket.repository.TicketEventRepository;
import com.enterprise.ticketing.ticket.repository.TicketRepository;
import com.enterprise.ticketing.ticket.service.TicketQueryService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketQueryServiceImpl implements TicketQueryService {

    private static final List<String> ALLOWED_SORT_FIELDS = List.of("createdAt", "updatedAt", "priority", "status");

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketEventRepository ticketEventRepository;
    private final TicketAccessPolicy ticketAccessPolicy;
    private final TicketDtoMapper ticketDtoMapper;

    public TicketQueryServiceImpl(
            TicketRepository ticketRepository,
            TicketCommentRepository ticketCommentRepository,
            TicketEventRepository ticketEventRepository,
            TicketAccessPolicy ticketAccessPolicy,
            TicketDtoMapper ticketDtoMapper
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketCommentRepository = ticketCommentRepository;
        this.ticketEventRepository = ticketEventRepository;
        this.ticketAccessPolicy = ticketAccessPolicy;
        this.ticketDtoMapper = ticketDtoMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public TicketListResponse listTickets(TicketListQuery query) {
        UserPrincipal currentUser = ticketAccessPolicy.currentUser();
        Page<TicketEntity> page = ticketRepository.findAll(
                buildSpecification(query, currentUser),
                PageRequest.of(query.getPage(), query.getSize(), buildSort(query))
        );

        return new TicketListResponse(
                page.getContent().stream()
                        .map(ticketDtoMapper::toTicketSummaryResponse)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(Long ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND, "Ticket not found: " + ticketId));
        ticketAccessPolicy.checkReadAccess(ticket);

        return new TicketDetailResponse(
                ticketDtoMapper.toTicketResponse(ticket),
                ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                        .stream()
                        .map(ticketDtoMapper::toTicketCommentResponse)
                        .toList(),
                ticketEventRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                        .stream()
                        .map(ticketDtoMapper::toTicketEventResponse)
                        .toList()
        );
    }

    private Specification<TicketEntity> buildSpecification(TicketListQuery query, UserPrincipal currentUser) {
        return Specification.where(buildAccessSpecification(currentUser))
                .and(keywordSpecification(query.getKeyword()))
                .and(exactSpecification("status", query.getStatus()))
                .and(exactSpecification("priority", query.getPriority()))
                .and(exactCategorySpecification(query.getCategory()))
                .and(exactLongSpecification("requester", "id", query.getRequesterId()))
                .and(exactLongSpecification("assignee", "id", query.getAssigneeId()));
    }

    private Specification<TicketEntity> buildAccessSpecification(UserPrincipal currentUser) {
        return (root, query, criteriaBuilder) -> {
            if (ticketAccessPolicy.canManageAllTickets()) {
                return criteriaBuilder.conjunction();
            }

            List<jakarta.persistence.criteria.Predicate> readablePredicates = new ArrayList<>();
            readablePredicates.add(criteriaBuilder.equal(root.get("requester").get("id"), currentUser.getId()));
            readablePredicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), currentUser.getId()));

            if (ticketAccessPolicy.isApprover()) {
                readablePredicates.add(criteriaBuilder.equal(root.get("status"), TicketStatus.WAITING_APPROVAL));
            }

            return criteriaBuilder.or(readablePredicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<TicketEntity> keywordSpecification(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
            );
        };
    }

    private Specification<TicketEntity> exactSpecification(String field, Object value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(field), value);
    }

    private Specification<TicketEntity> exactCategorySpecification(KnowledgeDocumentCategory category) {
        return (root, query, criteriaBuilder) -> category == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("category"), category.code());
    }

    private Specification<TicketEntity> exactLongSpecification(String field, String nestedField, Long value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(field).get(nestedField), value);
    }

    private Sort buildSort(TicketListQuery query) {
        String sortField = ALLOWED_SORT_FIELDS.contains(query.getSortBy()) ? query.getSortBy() : "updatedAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(query.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, sortField);
    }
}
