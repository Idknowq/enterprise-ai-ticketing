package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ai.dto.AiCitation;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedStructuredLlmProviderTest {

    private final RuleBasedStructuredLlmProvider provider = new RuleBasedStructuredLlmProvider();

    @Test
    void classifyVpnTicketIntoStructuredCategory() {
        StructuredLlmResponse<AiClassificationOutput> response = provider.classify(
                new AiClassificationInput(
                        "VPN 连接失败",
                        "我在家连接公司 VPN 失败，客户端提示证书失效。",
                        null
                )
        );

        assertThat(response.output().category()).isEqualTo("VPN_ISSUE");
        assertThat(response.output().priority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(response.output().confidence()).isGreaterThan(0.9d);
    }

    @Test
    void extractAccessRequestFieldsFromPermissionTicket() {
        StructuredLlmResponse<AiExtractionOutput> response = provider.extract(
                new AiExtractionInput(
                        "申请生产日志只读权限",
                        "需要开通 production log read only access 以排查线上问题。",
                        "ACCESS_REQUEST"
                )
        );

        assertThat(response.output().extractedFields())
                .containsEntry("issueType", "ACCESS_REQUEST")
                .containsEntry("environment", "PRODUCTION")
                .containsEntry("accessLevel", "READ_ONLY")
                .containsEntry("resourceType", "LOGS");
    }

    @Test
    void resolutionMarksApprovalWhenCategoryNeedsIt() {
        StructuredLlmResponse<AiResolutionOutput> response = provider.resolve(
                new AiResolutionInput(
                        "申请生产日志只读权限",
                        "需要开通生产环境日志只读权限，用于排查线上问题。",
                        new AiClassificationOutput("ACCESS_REQUEST", TicketPriority.HIGH, 0.92d),
                        java.util.Map.of(
                                "environment", "PRODUCTION",
                                "accessLevel", "READ_ONLY",
                                "resourceType", "LOGS"
                        ),
                        List.of(new AiCitation(
                                "HEURISTIC_POLICY",
                                null,
                                null,
                                "Production Log Access Approval Policy",
                                "Production access must be approved before entitlement changes.",
                                0.95d,
                                "kb://access/prod-log-approval"
                        ))
                )
        );

        assertThat(response.output().requiresApproval()).isTrue();
        assertThat(response.output().needsHumanHandoff()).isFalse();
        assertThat(response.output().suggestedActions()).isNotEmpty();
        assertThat(response.output().draftReply()).contains("approval");
    }
}
