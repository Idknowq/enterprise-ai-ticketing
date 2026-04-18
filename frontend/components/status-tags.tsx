"use client";

import type {
  ApprovalStatus,
  DocumentIndexStatus,
  KnowledgeAccessLevel,
  Role,
  TicketPriority,
  TicketStatus,
} from "@/types/api";
import { Tag } from "antd";

export const ticketStatusOptions: TicketStatus[] = [
  "OPEN",
  "AI_PROCESSING",
  "WAITING_APPROVAL",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
  "REJECTED",
];

export const priorityOptions: TicketPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];
export const accessLevelOptions: KnowledgeAccessLevel[] = [
  "PUBLIC",
  "INTERNAL",
  "RESTRICTED",
  "CONFIDENTIAL",
];
export const documentIndexStatusOptions: DocumentIndexStatus[] = ["PENDING", "INDEXED", "FAILED"];

const ticketStatusMeta: Record<TicketStatus, { color: string; label: string }> = {
  OPEN: { color: "default", label: "待处理" },
  AI_PROCESSING: { color: "processing", label: "AI 处理中" },
  WAITING_APPROVAL: { color: "warning", label: "待审批" },
  IN_PROGRESS: { color: "blue", label: "处理中" },
  RESOLVED: { color: "success", label: "已解决" },
  CLOSED: { color: "default", label: "已关闭" },
  REJECTED: { color: "error", label: "已驳回" },
};

const priorityMeta: Record<TicketPriority, { color: string; label: string }> = {
  LOW: { color: "default", label: "低" },
  MEDIUM: { color: "blue", label: "中" },
  HIGH: { color: "orange", label: "高" },
  URGENT: { color: "red", label: "紧急" },
};

const approvalMeta: Record<ApprovalStatus, { color: string; label: string }> = {
  PENDING: { color: "warning", label: "待审批" },
  APPROVED: { color: "success", label: "已通过" },
  REJECTED: { color: "error", label: "已驳回" },
};

const accessMeta: Record<KnowledgeAccessLevel, { color: string; label: string }> = {
  PUBLIC: { color: "default", label: "公开" },
  INTERNAL: { color: "blue", label: "内部" },
  RESTRICTED: { color: "orange", label: "受限" },
  CONFIDENTIAL: { color: "red", label: "机密" },
};

const docIndexMeta: Record<DocumentIndexStatus, { color: string; label: string }> = {
  PENDING: { color: "processing", label: "待索引" },
  INDEXED: { color: "success", label: "已索引" },
  FAILED: { color: "error", label: "索引失败" },
};

const roleMeta: Record<Role, { color: string; label: string }> = {
  EMPLOYEE: { color: "default", label: "员工" },
  SUPPORT_AGENT: { color: "blue", label: "一线支持" },
  APPROVER: { color: "gold", label: "审批人" },
  ADMIN: { color: "red", label: "管理员" },
};

export function TicketStatusTag({ value }: { value: TicketStatus }) {
  const meta = ticketStatusMeta[value];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function PriorityTag({ value }: { value?: TicketPriority | null }) {
  if (!value) {
    return <Tag>未设置</Tag>;
  }

  const meta = priorityMeta[value];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function ApprovalStatusTag({ value }: { value: ApprovalStatus }) {
  const meta = approvalMeta[value];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function AccessLevelTag({ value }: { value: KnowledgeAccessLevel }) {
  const meta = accessMeta[value];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function DocumentIndexStatusTag({ value }: { value: DocumentIndexStatus }) {
  const meta = docIndexMeta[value];
  return <Tag color={meta.color}>{meta.label}</Tag>;
}

export function RoleTags({ roles }: { roles: Role[] }) {
  return (
    <>
      {roles.map((role) => {
        const meta = roleMeta[role];
        return (
          <Tag key={role} color={meta.color}>
            {meta.label}
          </Tag>
        );
      })}
    </>
  );
}

