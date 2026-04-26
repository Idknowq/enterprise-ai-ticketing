package com.enterprise.ticketing.ai.provider;
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
            output = new AiClassificationOutput("VPN_ISSUE", TicketPriority.MEDIUM, 0.58d);
        } else if (containsAny(text, "password", "reset password", "forgot password", "密码")) {
            output = new AiClassificationOutput("PASSWORD_RESET", TicketPriority.MEDIUM, 0.56d);
        } else if (containsAny(text, "permission", "access", "权限", "只读", "read only", "生产环境", "prod")) {
            TicketPriority priority = containsAny(text, "prod", "production", "线上", "紧急") ? TicketPriority.HIGH : TicketPriority.MEDIUM;
            output = new AiClassificationOutput("ACCESS_REQUEST", priority, 0.6d);
        } else if (containsAny(text, "license", "授权", "office", "outlook", "teams")) {
            output = new AiClassificationOutput("SOFTWARE_LICENSE", TicketPriority.MEDIUM, 0.46d);
        } else if (containsAny(text, "laptop", "device", "设备", "电脑", "hardware")) {
            output = new AiClassificationOutput("DEVICE_SUPPORT", TicketPriority.MEDIUM, 0.44d);
        } else if (containsAny(text, "develop", "build", "deploy", "开发环境", "jenkins", "ide")) {
            output = new AiClassificationOutput("DEV_ENV_ISSUE", TicketPriority.HIGH, 0.42d);
        } else {
            output = new AiClassificationOutput("GENERAL_IT_SUPPORT", TicketPriority.MEDIUM, 0.18d);
        }
        return new StructuredLlmResponse<>(output, providerType(), defaultModelName(), estimateTokens(text), 32, false, null);
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
        return new StructuredLlmResponse<>(new AiExtractionOutput(fields), providerType(), defaultModelName(), estimateTokens(text), 48, false, null);
    }

    @Override
    public StructuredLlmResponse<AiResolutionOutput> resolve(AiResolutionInput input) {
        String text = normalize(input.title()) + " " + normalize(input.description());
        boolean requiresApproval = "ACCESS_REQUEST".equals(input.classification().category())
                || containsAny(text, "permission", "access", "权限", "prod", "production", "管理员");
        boolean keywordMatched = input.classification().confidence() >= 0.4d;
        boolean needsHumanHandoff = !keywordMatched
                || input.classification().confidence() < 0.65d
                || input.citations() == null
                || input.citations().isEmpty()
                || containsAny(text, "outage", "宕机", "全员", "p1", "紧急");

        List<String> actions = buildSuggestedActions(input.classification().category(), input.extractedFields(), keywordMatched);
        String draftReply = buildDraftReply(input.classification().category(), requiresApproval, needsHumanHandoff, keywordMatched, actions);

        AiResolutionOutput output = new AiResolutionOutput(requiresApproval, needsHumanHandoff, draftReply, actions);
        return new StructuredLlmResponse<>(
                output,
                providerType(),
                defaultModelName(),
                estimateTokens(text) + (input.citations() == null ? 0 : input.citations().size() * 24),
                96,
                false,
                null
        );
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
            fields.put("issueType", "CERTIFICATE_RELATED");
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
            boolean keywordMatched
    ) {
        List<String> actions = new ArrayList<>();
        if (!keywordMatched) {
            actions.add("Check whether the issue is caused by a temporary network or input problem.");
            actions.add("Capture the exact error message or screenshot and provide it to the support team.");
            actions.add("Contact a support agent for manual triage.");
        } else if ("VPN_ISSUE".equals(category)) {
            actions.add("Check network connectivity and confirm the VPN client can reach the service endpoint.");
            actions.add("Verify account status and any recent credential or certificate changes.");
            actions.add("Provide the exact VPN error message or screenshot to the support team.");
        } else if ("ACCESS_REQUEST".equals(category)) {
            actions.add("Confirm the requested resource scope and the business reason for access.");
            actions.add("Prepare approval context before any access change is made.");
            actions.add("Ask the support team to continue manual review of the request.");
        } else if ("PASSWORD_RESET".equals(category)) {
            actions.add("Verify identity through the standard support process.");
            actions.add("Try the normal password reset path and confirm whether MFA also needs attention.");
            actions.add("Contact support if login still fails after reset.");
        } else {
            actions.add("Check connectivity, account status, and the exact reproduction steps.");
            actions.add("Capture the error details and submit them for support review.");
            actions.add("Contact a support agent for manual assistance.");
        }

        if (extractedFields != null && extractedFields.containsKey("errorCode")) {
            actions.add("Search monitoring and ticket history for error code " + extractedFields.get("errorCode") + ".");
        }
        return actions.stream().distinct().limit(5).toList();
    }

    private String buildDraftReply(
            String category,
            boolean requiresApproval,
            boolean needsHumanHandoff,
            boolean keywordMatched,
            List<String> actions
    ) {
        StringBuilder builder = new StringBuilder("Fallback analysis matched the ticket to category ").append(category).append(". ");
        if (!keywordMatched) {
            builder.append("The keyword signal is weak, so this should be treated as a preliminary hint only. ");
        }
        if (requiresApproval) {
            builder.append("Any access or permission change should be reviewed before execution. ");
        } else {
            builder.append("The following steps are generic troubleshooting guidance. ");
        }
        builder.append("Suggested actions: ").append(String.join(" ", actions.stream().limit(3).toList()));
        if (needsHumanHandoff) {
            builder.append(" Manual support follow-up is recommended.");
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
