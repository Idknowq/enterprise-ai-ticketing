package com.enterprise.ticketing.ticket.domain;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusTest {

    @Test
    void canTransitionToAllowsOnlyConfiguredStateMachineEdges() {
        assertAllowed(TicketStatus.OPEN, TicketStatus.AI_PROCESSING, TicketStatus.WAITING_APPROVAL,
                TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.REJECTED, TicketStatus.CLOSED);
        assertAllowed(TicketStatus.AI_PROCESSING, TicketStatus.WAITING_APPROVAL, TicketStatus.IN_PROGRESS,
                TicketStatus.RESOLVED, TicketStatus.REJECTED, TicketStatus.OPEN);
        assertAllowed(TicketStatus.WAITING_APPROVAL, TicketStatus.IN_PROGRESS, TicketStatus.REJECTED);
        assertAllowed(TicketStatus.IN_PROGRESS, TicketStatus.WAITING_APPROVAL, TicketStatus.RESOLVED,
                TicketStatus.REJECTED, TicketStatus.CLOSED);
        assertAllowed(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
        assertAllowed(TicketStatus.REJECTED, TicketStatus.CLOSED);
        assertAllowed(TicketStatus.CLOSED);
    }

    @Test
    void validateTransitionToRejectsSameNullAndIllegalTargets() {
        assertThatThrownBy(() -> TicketStatus.CLOSED.validateTransitionTo(TicketStatus.OPEN))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_CONFLICT);

        assertThat(TicketStatus.OPEN.canTransitionTo(TicketStatus.OPEN)).isFalse();
        assertThat(TicketStatus.OPEN.canTransitionTo(null)).isFalse();
    }

    @Test
    void terminalStatusesAreClosedAndRejectedOnly() {
        assertThat(TicketStatus.CLOSED.isTerminal()).isTrue();
        assertThat(TicketStatus.REJECTED.isTerminal()).isTrue();
        assertThat(TicketStatus.OPEN.isTerminal()).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.isTerminal()).isFalse();
    }

    private static void assertAllowed(TicketStatus source, TicketStatus... allowedTargets) {
        Set<TicketStatus> allowed = allowedTargets.length == 0
                ? EnumSet.noneOf(TicketStatus.class)
                : EnumSet.copyOf(Set.of(allowedTargets));

        for (TicketStatus target : TicketStatus.values()) {
            assertThat(source.canTransitionTo(target))
                    .as("%s -> %s", source, target)
                    .isEqualTo(allowed.contains(target));
        }
    }
}
