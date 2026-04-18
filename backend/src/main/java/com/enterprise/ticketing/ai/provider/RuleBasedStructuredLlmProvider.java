package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.ai.dto.AiCitation;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RuleBasedStructuredLlmProvider implements StructuredLlmProvider {

    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b([A-Z]{2,}[\\-_]?\\d{2,}|ERR\\d{2,})\\b");

    @Override
    public String providerType() {
        return "rule-based";
    }

    @Override
    public String defaultModelName() {
        return "mvp-rule-based";
    }

    @Override
    public StructuredLlmResponse<AiClassificationOutput> classify(AiClassificationInput input) {
        String text = normalize(input.title()) + " " + normalize(input.description());
        AiClassificationOutput output;
        if (containsAny(text, "vpn", "证书", "certificate", "remote access")) {
            output = new AiClassificationOutput("VPN_ISSUE", TicketPriority.MEDIUM, 0.94d);
        } else if (containsAny(text, "password", "reset password", "forgot password", "密码")) {
            output = new AiClassificationOutput("PASSWORD_RESET", TicketPriority.MEDIUM, 0.95d);
        } else if (containsAny(text, "permission", "access", "权限", "只读", "read only", "生产环境", "prod")) {
            TicketPriority priority = containsAny(text, "prod", "production", "线上", "紧急") ? TicketPriority.HIGH : TicketPriority.MEDIUM;
            output = new AiClassificationOutput("ACCESS_REQUEST", priority, 0.93d);
        } else if (containsAny(text, "license", "授权", "office", "outlook", "teams")) {
            output = new AiClassificationOutput("SOFTWARE_LICENSE", TicketPriority.MEDIUM, 0.86d);
        } else if (containsAny(text, "laptop", "device", "设备", "电脑", "hardware")) {
            output = new AiClassificationOutput("DEVICE_SUPPORT", TicketPriority.MEDIUM, 0.83d);
        } else if (containsAny(text, "develop", "build", "deploy", "开发环境", "jenkins", "ide")) {
            output = new AiClassificationOutput("DEV_ENV_ISSUE", TicketPriority.HIGH, 0.82d);
        } else {
            output = new AiClassificationOutput("GENERAL_IT_SUPPORT", TicketPriority.MEDIUM, 0.68d);
        }
        return new StructuredLlmResponse<>(output, defaultModelName(), estimateTokens(text), 32);
    }

    @Override
    public StructuredLlmResponse<AiExtractionOutput> extract(AiExtractionInput input) {
        String text = normalize(input.title()) + " " + normalize(input.description());
        Map<String, String> fields = new LinkedHashMap<>();
        populateSystemField(text, fields);
        populateIssueType(input.category(), text, fields);
        populateEnvironmentField(text, fields);
        populateAccessFields(text, fields);
        populateErrorCodeField(text, fields);
        return new StructuredLlmResponse<>(new AiExtractionOutput(fields), defaultModelName(), estimateTokens(text), 48);
    }

    @Override
    public StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input) {
        String text = normalize(input.title()) + " " + normalize(input.description());
        boolean requiresApproval = "ACCESS_REQUEST".equals(input.classification().category())
                || containsAny(text, "permission", "access", "权限", "prod", "production", "管理员");
        boolean needsHumanHandoff = input.classification().confidence() < 0.75d
                || input.citations() == null
                || input.citations().isEmpty()
                || containsAny(text, "outage", "宕机", "全员", "p1", "紧急");

        List<String> actions = buildSuggestedActions(input.classification().category(), input.extractedFields(), input.citations());
        String draftReply = buildDraftReply(input.classification().category(), requiresApproval, needsHumanHandoff, actions, input.citations());

        AiResolutionOutput output = new AiResolutionOutput(requiresApproval, needsHumanHandoff, draftReply, actions);
        return new StructuredLlmResponse<>(output, defaultModelName(), estimateTokens(text) + (input.citations() == null ? 0 : input.citations().size() * 24), 96);
    }

    private void populateSystemField(String text, Map<String, String> fields) {
        if (containsAny(text, "vpn")) {
            fields.put("system", "VPN");
        } else if (containsAny(text, "outlook", "exchange", "mail")) {
            fields.put("system", "EMAIL");
        } else if (containsAny(text, "office", "word", "excel", "ppt")) {
            fields.put("system", "OFFICE_SUITE");
        } else if (containsAny(text, "jenkins", "gitlab", "ide", "开发环境")) {
            fields.put("system", "DEV_ENV");
        }
    }

    private void populateIssueType(String category, String text, Map<String, String> fields) {
        if ("VPN_ISSUE".equals(category) && containsAny(text, "证书", "certificate")) {
            fields.put("issueType", "CERTIFICATE_EXPIRED");
            return;
        }
        if ("PASSWORD_RESET".equals(category)) {
            fields.put("issueType", "PASSWORD_RESET");
            return;
        }
        if ("ACCESS_REQUEST".equals(category)) {
            fields.put("issueType", "ACCESS_REQUEST");
            return;
        }
        if (containsAny(text, "license", "授权")) {
            fields.put("issueType", "LICENSE_ISSUE");
            return;
        }
        if (containsAny(text, "error", "异常", "失败")) {
            fields.put("issueType", "RUNTIME_ERROR");
        }
    }

    private void populateEnvironmentField(String text, Map<String, String> fields) {
        if (containsAny(text, "prod", "production", "线上", "生产")) {
            fields.put("environment", "PRODUCTION");
        } else if (containsAny(text, "staging", "预发")) {
            fields.put("environment", "STAGING");
        } else if (containsAny(text, "dev", "development", "开发")) {
            fields.put("environment", "DEVELOPMENT");
        }
    }

    private void populateAccessFields(String text, Map<String, String> fields) {
        if (containsAny(text, "只读", "read only")) {
            fields.put("accessLevel", "READ_ONLY");
        } else if (containsAny(text, "admin", "管理员")) {
            fields.put("accessLevel", "ADMIN");
        }
        if (containsAny(text, "log", "日志")) {
            fields.put("resourceType", "LOGS");
        }
    }

    private void populateErrorCodeField(String text, Map<String, String> fields) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(text.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            fields.put("errorCode", matcher.group(1));
        }
    }

    private List<String> buildSuggestedActions(
            String category,
            Map<String, String> extractedFields,
            List<AiCitation> citations
    ) {
        List<String> actions = new ArrayList<>();
        switch (category) {
            case "VPN_ISSUE" -> {
                actions.add("Verify whether the local VPN certificate or token has expired.");
                actions.add("Guide the requester to re-import or renew the VPN certificate.");
                actions.add("Collect client error screenshots if the issue persists after renewal.");
            }
            case "ACCESS_REQUEST" -> {
                actions.add("Confirm the requested resource scope, access level, and business reason.");
                actions.add("Route the request into approval before any entitlement change.");
                actions.add("After approval, ask the owning team to grant least-privilege access.");
            }
            case "PASSWORD_RESET" -> {
                actions.add("Verify requester identity through the standard support checklist.");
                actions.add("Trigger the password reset SOP and ask the user to rotate the temporary credential.");
                actions.add("Check whether MFA resynchronization is also required.");
            }
            default -> {
                actions.add("Validate the affected system, environment, and reproducible symptom.");
                actions.add("Follow the closest SOP or troubleshooting article from retrieval citations.");
                actions.add("Escalate to a support agent if the recommended steps do not resolve the issue.");
            }
        }

        if (citations != null && !citations.isEmpty()) {
            actions.add("Reference knowledge article: " + citations.get(0).title() + ".");
        }
        if (extractedFields.containsKey("errorCode")) {
            actions.add("Search monitoring and ticket history for error code " + extractedFields.get("errorCode") + ".");
        }
        return actions.stream().distinct().limit(5).toList();
    }

    private String buildDraftReply(
            String category,
            boolean requiresApproval,
            boolean needsHumanHandoff,
            List<String> actions,
            List<AiCitation> citations
    ) {
        StringBuilder builder = new StringBuilder("AI triage suggests category ").append(category).append(". ");
        if (requiresApproval) {
            builder.append("This request should go through approval before execution. ");
        } else {
            builder.append("Recommended next steps have been prepared for the support handler. ");
        }
        if (citations != null && !citations.isEmpty()) {
            builder.append("Most relevant reference: ").append(citations.get(0).title()).append(". ");
        }
        builder.append("Suggested actions: ").append(String.join(" ", actions.stream().limit(3).toList()));
        if (needsHumanHandoff) {
            builder.append(" Human follow-up is recommended because confidence or impact risk is not low enough.");
        }
        return builder.toString().trim();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String input) {
        return StringUtils.hasText(input) ? input.trim().toLowerCase(Locale.ROOT) : "";
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return Math.max(16, (text.length() / 4) + 1);
    }
}
