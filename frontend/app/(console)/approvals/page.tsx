"use client";

import { useAuth } from "@/components/app-provider";
import { InlineEmpty, PageError, PageLoading } from "@/components/page-state";
import { ApprovalStatusTag, TicketStatusTag } from "@/components/status-tags";
import { formatDateTime } from "@/lib/date";
import {
  approveApproval,
  listPendingApprovals,
  rejectApproval,
} from "@/lib/services/approvals";
import type { PendingApprovalResponse } from "@/types/api";
import { App, Alert, Button, Card, Form, Input, Modal, Result, Space, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

export default function ApprovalsPage() {
  const { message } = App.useApp();
  const { user } = useAuth();
  const [items, setItems] = useState<PendingApprovalResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [decisionTarget, setDecisionTarget] = useState<{
    item: PendingApprovalResponse;
    action: "APPROVE" | "REJECT";
  } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const canAccess = Boolean(user?.roles.includes("APPROVER") || user?.roles.includes("ADMIN"));

  const loadApprovals = useCallback(async () => {
    if (!user) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const response = await listPendingApprovals();
      setItems(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "审批列表加载失败");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (user && canAccess) {
      loadApprovals();
    } else {
      setLoading(false);
    }
  }, [user, canAccess, loadApprovals]);

  async function handleDecision(values: { comment?: string }) {
    if (!decisionTarget) {
      return;
    }

    setSubmitting(true);
    try {
      if (decisionTarget.action === "APPROVE") {
        await approveApproval(decisionTarget.item.approvalId, {
          comment: values.comment,
          requestId: `approve-${decisionTarget.item.approvalId}-${Date.now()}`,
        });
      } else {
        await rejectApproval(decisionTarget.item.approvalId, {
          comment: values.comment,
          requestId: `reject-${decisionTarget.item.approvalId}-${Date.now()}`,
        });
      }
      message.success(decisionTarget.action === "APPROVE" ? "审批已通过" : "审批已驳回");
      setDecisionTarget(null);
      await loadApprovals();
    } catch (submitError) {
      message.error(submitError instanceof Error ? submitError.message : "审批提交失败");
    } finally {
      setSubmitting(false);
    }
  }

  if (!canAccess) {
    return <Result status="403" title="无审批访问权限" subTitle="当前页面仅向审批人或管理员开放。" />;
  }

  if (loading && !items.length) {
    return <PageLoading tip="正在加载审批队列..." />;
  }

  if (error && !items.length) {
    return <PageError message={error} onRetry={loadApprovals} />;
  }

  const pendingItems = items.filter((item) => item.approvalStatus === "PENDING");
  const columns: ColumnsType<PendingApprovalResponse> = [
    {
      title: "审批 ID",
      dataIndex: "approvalId",
      width: 120,
      render: (value) => <Typography.Text code>#{value}</Typography.Text>,
    },
    {
      title: "工单",
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Link href={`/tickets/${record.ticketId}`} data-testid={`approval-ticket-${record.ticketId}`}>
            {record.ticketTitle}
          </Link>
          <Typography.Text type="secondary">ticket #{record.ticketId}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "阶段",
      dataIndex: "stageDisplayName",
      width: 200,
    },
    {
      title: "工单状态",
      width: 130,
      render: (_, record) => <TicketStatusTag value={record.ticketStatus} />,
    },
    {
      title: "审批状态",
      width: 130,
      render: (_, record) => <ApprovalStatusTag value={record.approvalStatus} />,
    },
    {
      title: "申请时间",
      dataIndex: "requestedAt",
      width: 180,
      render: (value) => formatDateTime(value),
    },
    {
      title: "操作",
      width: 180,
      render: (_, record) =>
        record.approvalStatus === "PENDING" ? (
          <Space>
            <Button
              type="primary"
              data-testid={`approve-ticket-${record.ticketId}`}
              onClick={() => setDecisionTarget({ item: record, action: "APPROVE" })}
            >
              通过
            </Button>
            <Button
              danger
              data-testid={`reject-ticket-${record.ticketId}`}
              onClick={() => setDecisionTarget({ item: record, action: "REJECT" })}
            >
              驳回
            </Button>
          </Space>
        ) : (
          <Typography.Text type="secondary">已处理</Typography.Text>
        ),
    },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Alert
        type="info"
        showIcon
        message="审批页面已切换到真实接口"
        description="当前页面直接对接 /api/approvals/pending、/api/approvals/{id}/approve、/api/approvals/{id}/reject。管理员可查看全部待审批项，审批人只看分配给自己的待办。"
      />

      <Card className="page-card" title="待审批列表">
        {pendingItems.length ? (
          <Table rowKey="approvalId" columns={columns} dataSource={pendingItems} pagination={false} />
        ) : (
          <InlineEmpty description="当前没有待审批工单。请先在工单详情页运行 AI，且工单内容需命中需要审批的规则，例如权限申请、生产环境访问、只读日志权限等。" />
        )}
      </Card>

      <Modal
        title={decisionTarget?.action === "APPROVE" ? "审批通过" : "审批驳回"}
        open={Boolean(decisionTarget)}
        onCancel={() => {
          setDecisionTarget(null);
        }}
        footer={null}
        destroyOnHidden
      >
        <Form
          key={
            decisionTarget
              ? `${decisionTarget.item.approvalId}-${decisionTarget.action}`
              : "approval-form"
          }
          layout="vertical"
          onFinish={handleDecision}
        >
          <Typography.Paragraph>
            当前审批目标：{decisionTarget?.item.ticketTitle}
          </Typography.Paragraph>
          <Form.Item label="审批意见" name="comment">
            <Input.TextArea
              data-testid="approval-comment-input"
              rows={4}
              maxLength={2000}
              placeholder="填写审批说明或驳回原因"
            />
          </Form.Item>
          <Space>
            <Button data-testid="approval-cancel-button" onClick={() => setDecisionTarget(null)}>
              取消
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              data-testid="approval-submit-button"
              danger={decisionTarget?.action === "REJECT"}
              loading={submitting}
            >
              提交
            </Button>
          </Space>
        </Form>
      </Modal>
    </Space>
  );
}
