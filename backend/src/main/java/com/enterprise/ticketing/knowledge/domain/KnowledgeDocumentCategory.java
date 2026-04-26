package com.enterprise.ticketing.knowledge.domain;

import java.util.Arrays;
import java.util.Optional;

public enum KnowledgeDocumentCategory {
    REMOTE_ACCESS("远程访问 / VPN", "VPN 证书失效、远程办公连接失败、客户端配置"),
    IDENTITY_ACCOUNT("账号与身份", "账号开通、账号锁定、离职账号禁用、AD/LDAP 问题"),
    PASSWORD_MFA("密码与 MFA", "密码重置、MFA 绑定、验证码异常、SSO 登录失败"),
    ACCESS_REQUEST("权限申请", "系统权限申请、权限变更、临时授权、审批流程"),
    EMAIL_COLLABORATION("邮件与协作", "邮箱异常、邮件组、日历、Teams/Slack/飞书协作"),
    DEVICE_HARDWARE("终端与硬件", "电脑、显示器、打印机、外设、资产更换"),
    OPERATING_SYSTEM("操作系统", "Windows/macOS 系统故障、补丁、启动异常"),
    SOFTWARE_APPLICATION("软件与应用", "办公软件、业务应用安装、客户端异常"),
    NETWORK_CONNECTIVITY("网络连接", "Wi-Fi、有线网络、DNS、代理、内网访问异常"),
    SECURITY_INCIDENT("安全事件", "钓鱼邮件、恶意软件、账号异常登录、安全上报"),
    DATA_BACKUP_RECOVERY("数据备份与恢复", "文件恢复、备份策略、误删数据恢复"),
    CLOUD_INFRASTRUCTURE("云与基础设施", "云资源、服务器、容器、存储、基础设施运维"),
    DATABASE_DATA_PLATFORM("数据库与数据平台", "数据库连接、数据权限、数据任务、BI 平台"),
    DEV_ENGINEERING("开发与工程工具", "Git、CI/CD、制品库、开发环境、测试环境"),
    ITSM_PROCESS("ITSM 流程", "工单流转、SLA、升级路径、服务目录"),
    ASSET_PROCUREMENT("资产与采购", "设备领用、采购申请、资产归还、库存流程"),
    CHANGE_RELEASE("变更与发布", "变更申请、发布窗口、回滚 SOP、维护公告"),
    POLICY_COMPLIANCE("政策与合规", "信息安全制度、审计要求、合规流程"),
    GENERAL_FAQ("通用 FAQ", "通用服务台问答、常见问题合集"),
    OTHER("其他", "暂时无法归类但仍需入库的文档，需后续治理");

    private final String displayName;
    private final String description;

    KnowledgeDocumentCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String code() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static KnowledgeDocumentCategory fromCode(String raw) {
        return find(raw).orElseThrow(() -> new IllegalArgumentException("Unsupported knowledge document category: " + raw));
    }

    public static boolean isKnown(String raw) {
        return find(raw).isPresent();
    }

    public static KnowledgeDocumentCategory[] options() {
        return values();
    }

    public static Optional<KnowledgeDocumentCategory> find(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(category -> category.name().equals(raw.trim()))
                .findFirst();
    }
}
