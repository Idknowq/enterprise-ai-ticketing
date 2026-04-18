"use client";

import { useAuth } from "@/components/app-provider";
import { InlineEmpty, PageError, PageLoading } from "@/components/page-state";
import { PriorityTag, TicketStatusTag, ticketStatusOptions } from "@/components/status-tags";
import { formatDateTime, formatRelative } from "@/lib/date";
import { listTicketApprovals } from "@/lib/services/approvals";
import { listTicketAiRuns, runTicketAi } from "@/lib/services/ai";
import { searchKnowledge } from "@/lib/services/documents";
import {
  appendTicketComment,
  assignTicket,
  getTicketDetail,
  updateTicketStatus,
} from "@/lib/services/tickets";
import type {
  AiWorkflowRunResponse,
  RetrievalSearchResponse,
  ApprovalStageResponse,
  TicketDetailResponse,
  TicketStatus,
} from "@/types/api";
import { ArrowLeftOutlined, RobotOutlined, SearchOutlined } from "@ant-design/icons";
import {
  Alert,
  App,
  Button,
  Card,
  Descriptions,
  Divider,
  Form,
  Input,
  List,
  Select,
  Space,
  Table,
  Tabs,
  Timeline,
  Typography,
} from "antd";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

function sortRuns(runs: AiWorkflowRunResponse[]) {
  return [...runs].sort(
    (left, right) => new Date(right.startedAt).getTime() - new Date(left.startedAt).getTime(),
  );
}

export default function TicketDetailPage() {
  const { message } = App.useApp();
  const { user } = useAuth();
  const params = useParams<{ id: string }>();
  const ticketId = Number(params.id);
  const [commentForm] = Form.useForm<{ content: string }>();
  const [statusForm] = Form.useForm<{ status: TicketStatus; reason?: string }>();
  const [assignForm] = Form.useForm<{ assigneeId: number; note?: string }>();
  const [detail, setDetail] = useState<TicketDetailResponse | null>(null);
  const [aiRuns, setAiRuns] = useState<AiWorkflowRunResponse[]>([]);
  const [approvalStages, setApprovalStages] = useState<ApprovalStageResponse[]>([]);
  const [retrieval, setRetrieval] = useState<RetrievalSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [aiError, setAiError] = useState<string | null>(null);
  const [commentLoading, setCommentLoading] = useState(false);
  const [statusLoading, setStatusLoading] = useState(false);
  const [assignLoading, setAssignLoading] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [retrievalLoading, setRetrievalLoading] = useState(false);

  const canManageTicket = Boolean(
    user?.roles.includes("SUPPORT_AGENT") || user?.roles.includes("ADMIN"),
  );

  async function loadPage() {
    setLoading(true);
    setError(null);
    setAiError(null);
    try {
      const detailResponse = await getTicketDetail(ticketId);
      setDetail(detailResponse);

      try {
        const aiResponse = await listTicketAiRuns(ticketId);
        setAiRuns(sortRuns(aiResponse));
      } catch (aiLoadError) {
        setAiRuns([]);
        setAiError(aiLoadError instanceof Error ? aiLoadError.message : "AI 运行记录加载失败");
      }

      try {
        const approvalResponse = await listTicketApprovals(ticketId);
        setApprovalStages(approvalResponse);
      } catch {
        setApprovalStages([]);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "工单详情加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!Number.isNaN(ticketId)) {
      loadPage();
    }
  }, [ticketId]);

  useEffect(() => {
    if (detail) {
      statusForm.setFieldsValue({ status: detail.ticket.status });
    }
  }, [detail, statusForm]);

  async function handleComment(values: { content: string }) {
    setCommentLoading(true);
    try {
      await appendTicketComment(ticketId, values);
      message.success("评论已添加");
      commentForm.resetFields();
      await loadPage();
    } catch (submitError) {
      message.error(submitError instanceof Error ? submitError.message : "评论提交失败");
    } finally {
      setCommentLoading(false);
    }
  }

  async function handleStatus(values: { status: TicketStatus; reason?: string }) {
    setStatusLoading(true);
    try {
      await updateTicketStatus(ticketId, values);
      message.success("状态已更新");
      statusForm.resetFields();
      await loadPage();
    } catch (submitError) {
      message.error(submitError instanceof Error ? submitError.message : "状态更新失败");
    } finally {
      setStatusLoading(false);
    }
  }

  async function handleAssign(values: { assigneeId: number; note?: string }) {
    setAssignLoading(true);
    try {
      await assignTicket(ticketId, {
        assigneeId: Number(values.assigneeId),
        note: values.note,
      });
      message.success("工单已指派");
      assignForm.resetFields();
      await loadPage();
    } catch (submitError) {
      message.error(submitError instanceof Error ? submitError.message : "指派失败");
    } finally {
      setAssignLoading(false);
    }
  }

  async function handleRunAi() {
    setAiLoading(true);
    try {
      await runTicketAi(ticketId);
      message.success("AI 分析已完成");
      await loadPage();
    } catch (runError) {
      message.error(runError instanceof Error ? runError.message : "AI 分析失败");
    } finally {
      setAiLoading(false);
    }
  }

  async function handleRetrieval() {
    setRetrievalLoading(true);
    try {
      const result = await searchKnowledge({
        ticketId,
        limit: 5,
        saveCitations: false,
      });
      setRetrieval(result);
      message.success("检索完成");
    } catch (searchError) {
      message.error(searchError instanceof Error ? searchError.message : "证据检索失败");
    } finally {
      setRetrievalLoading(false);
    }
  }

  if (loading && !detail) {
    return <PageLoading tip="正在加载工单详情..." />;
  }

  if (error && !detail) {
    return <PageError message={error} onRetry={loadPage} />;
  }

  if (!detail) {
    return <PageError message="工单不存在或当前用户无权限访问。" onRetry={loadPage} />;
  }

  const latestRun = sortRuns(aiRuns)[0];
  const aiDecision = latestRun?.finalDecision || null;

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card className="page-card">
        <Space style={{ width: "100%", justifyContent: "space-between" }} align="start">
          <Space direction="vertical" size={6}>
            <Link href="/tickets">
              <Button icon={<ArrowLeftOutlined />} type="link">
                返回工单列表
              </Button>
            </Link>
            <Space align="center" wrap>
              <Typography.Title level={3} style={{ margin: 0 }}>
                {detail.ticket.title}
              </Typography.Title>
              <Typography.Text code>#{detail.ticket.id}</Typography.Text>
              <TicketStatusTag value={detail.ticket.status} />
              <PriorityTag value={detail.ticket.priority} />
            </Space>
            <Typography.Paragraph style={{ marginBottom: 0 }}>
              {detail.ticket.description}
            </Typography.Paragraph>
          </Space>
          <Space>
            <Button icon={<SearchOutlined />} loading={retrievalLoading} onClick={handleRetrieval}>
              证据检索
            </Button>
            <Button
              type="primary"
              icon={<RobotOutlined />}
              loading={aiLoading}
              onClick={handleRunAi}
            >
              运行 AI 分析
            </Button>
          </Space>
        </Space>
      </Card>

      <div className="detail-grid">
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Card className="page-card" title="基础信息">
            <Descriptions bordered column={2}>
              <Descriptions.Item label="分类">{detail.ticket.category || "未分类"}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <TicketStatusTag value={detail.ticket.status} />
              </Descriptions.Item>
              <Descriptions.Item label="提交人">
                {detail.ticket.requester?.displayName || "-"}
              </Descriptions.Item>
              <Descriptions.Item label="处理人">
                {detail.ticket.assignee?.displayName || "未分配"}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">{formatDateTime(detail.ticket.createdAt)}</Descriptions.Item>
              <Descriptions.Item label="更新时间">{formatDateTime(detail.ticket.updatedAt)}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card className="page-card">
            <Tabs
              items={[
                {
                  key: "comments",
                  label: `评论 (${detail.comments.length})`,
                  children: (
                    <Space direction="vertical" size="large" style={{ width: "100%" }}>
                      <List
                        dataSource={detail.comments}
                        locale={{ emptyText: <InlineEmpty description="暂无评论" /> }}
                        renderItem={(comment) => (
                          <List.Item>
                            <List.Item.Meta
                              title={
                                <Space wrap>
                                  <Typography.Text strong>{comment.author?.displayName || "未知用户"}</Typography.Text>
                                  <Typography.Text type="secondary">
                                    {formatDateTime(comment.createdAt)}
                                  </Typography.Text>
                                </Space>
                              }
                              description={comment.content}
                            />
                          </List.Item>
                        )}
                      />

                      <Divider style={{ margin: 0 }} />

                      <Form form={commentForm} layout="vertical" onFinish={handleComment}>
                        <Form.Item
                          label="补充评论"
                          name="content"
                          rules={[{ required: true, message: "请输入评论内容" }]}
                        >
                          <Input.TextArea
                            rows={4}
                            maxLength={5000}
                            placeholder="补充现象、回复用户、记录处理动作等"
                          />
                        </Form.Item>
                        <Button type="primary" htmlType="submit" loading={commentLoading}>
                          提交评论
                        </Button>
                      </Form>
                    </Space>
                  ),
                },
                {
                  key: "timeline",
                  label: `时间线 (${detail.timeline.length})`,
                  children: (
                    <Timeline
                      items={detail.timeline.map((event) => ({
                        color: event.eventType.includes("REJECTED") ? "red" : "blue",
                        children: (
                          <Space direction="vertical" size={2}>
                            <Space wrap>
                              <Typography.Text strong>{event.summary}</Typography.Text>
                              <Typography.Text type="secondary">{event.eventType}</Typography.Text>
                            </Space>
                            <Typography.Text type="secondary">
                              {formatDateTime(event.createdAt)} · {event.operator?.displayName || "系统"}
                            </Typography.Text>
                          </Space>
                        ),
                      }))}
                    />
                  ),
                },
                {
                  key: "ai",
                  label: `AI 建议 (${aiRuns.length})`,
                  children: (
                    <Space direction="vertical" size="large" style={{ width: "100%" }}>
                      {aiError ? <Alert type="warning" message={aiError} showIcon /> : null}
                      {aiDecision ? (
                        <>
                          <Descriptions bordered column={2}>
                            <Descriptions.Item label="Workflow ID">{aiDecision.workflowId}</Descriptions.Item>
                            <Descriptions.Item label="生成时间">
                              {formatDateTime(aiDecision.generatedAt)}
                            </Descriptions.Item>
                            <Descriptions.Item label="分类">{aiDecision.category}</Descriptions.Item>
                            <Descriptions.Item label="优先级">
                              <PriorityTag value={aiDecision.priority} />
                            </Descriptions.Item>
                            <Descriptions.Item label="审批判断">
                              {aiDecision.requiresApproval ? "需要审批" : "无需审批"}
                            </Descriptions.Item>
                            <Descriptions.Item label="人工接管">
                              {aiDecision.needsHumanHandoff ? "需要人工接管" : "可继续自动建议"}
                            </Descriptions.Item>
                          </Descriptions>

                          <div>
                            <Typography.Title level={5}>回复草稿</Typography.Title>
                            <Typography.Paragraph>{aiDecision.draftReply}</Typography.Paragraph>
                          </div>

                          <div>
                            <Typography.Title level={5}>建议动作</Typography.Title>
                            <List
                              dataSource={aiDecision.suggestedActions}
                              renderItem={(item) => <List.Item>{item}</List.Item>}
                            />
                          </div>

                          <div>
                            <Typography.Title level={5}>结构化字段</Typography.Title>
                            {Object.keys(aiDecision.extractedFields).length ? (
                              <Descriptions bordered column={2}>
                                {Object.entries(aiDecision.extractedFields).map(([key, value]) => (
                                  <Descriptions.Item key={key} label={key}>
                                    {value}
                                  </Descriptions.Item>
                                ))}
                              </Descriptions>
                            ) : (
                              <InlineEmpty description="暂无抽取字段" />
                            )}
                          </div>

                          <div>
                            <Typography.Title level={5}>节点执行明细</Typography.Title>
                            <Table
                              rowKey="id"
                              pagination={false}
                              dataSource={latestRun?.nodes || []}
                              columns={[
                                { title: "节点", dataIndex: "nodeName" },
                                { title: "状态", dataIndex: "status" },
                                { title: "模型", dataIndex: "modelName" },
                                { title: "耗时(ms)", dataIndex: "latencyMs" },
                                { title: "摘要", dataIndex: "resultSummary" },
                              ]}
                            />
                          </div>
                        </>
                      ) : (
                        <InlineEmpty
                          description="暂无 AI 运行记录"
                          action={
                            <Button type="primary" icon={<RobotOutlined />} onClick={handleRunAi}>
                              立即运行 AI
                            </Button>
                          }
                        />
                      )}
                    </Space>
                  ),
                },
                {
                  key: "approvals",
                  label: `审批记录 (${approvalStages.length})`,
                  children: approvalStages.length ? (
                    <Timeline
                      items={approvalStages.map((stage) => ({
                        color:
                          stage.status === "APPROVED"
                            ? "green"
                            : stage.status === "REJECTED"
                              ? "red"
                              : "blue",
                        children: (
                          <Space direction="vertical" size={2}>
                            <Space wrap>
                              <Typography.Text strong>
                                第 {stage.stageOrder} 级：{stage.stageDisplayName}
                              </Typography.Text>
                              <Typography.Text type="secondary">{stage.status}</Typography.Text>
                            </Space>
                            <Typography.Text type="secondary">
                              审批人：{stage.approverName} ({stage.approverUsername})
                            </Typography.Text>
                            <Typography.Text type="secondary">
                              请求时间：{formatDateTime(stage.requestedAt)}
                              {stage.decidedAt ? ` · 决策时间：${formatDateTime(stage.decidedAt)}` : ""}
                            </Typography.Text>
                            {stage.comment ? <Typography.Text>{stage.comment}</Typography.Text> : null}
                          </Space>
                        ),
                      }))}
                    />
                  ) : (
                    <InlineEmpty description="当前工单暂无审批记录" />
                  ),
                },
                {
                  key: "retrieval",
                  label: "检索引用",
                  children: (
                    <Space direction="vertical" size="large" style={{ width: "100%" }}>
                      <Alert
                        type="info"
                        showIcon
                        message="检索引用优先展示实时检索结果；若尚未手动检索，则回退显示 AI 最近一次运行中的 citations。"
                      />
                      {retrieval?.results?.length ? (
                        <List
                          dataSource={retrieval.results}
                          renderItem={(item) => (
                            <List.Item>
                              <List.Item.Meta
                                title={
                                  <Space wrap>
                                    <Typography.Text strong>{item.title}</Typography.Text>
                                    <Typography.Text type="secondary">
                                      score {item.score.toFixed(2)}
                                    </Typography.Text>
                                  </Space>
                                }
                                description={
                                  <Space direction="vertical" size={2}>
                                    <Typography.Text>{item.contentSnippet}</Typography.Text>
                                    <Typography.Text type="secondary">
                                      {item.metadata.category} / {item.metadata.department} / {item.metadata.version}
                                    </Typography.Text>
                                  </Space>
                                }
                              />
                            </List.Item>
                          )}
                        />
                      ) : aiDecision?.citations?.length ? (
                        <List
                          dataSource={aiDecision.citations}
                          renderItem={(item) => (
                            <List.Item>
                              <List.Item.Meta
                                title={
                                  <Space wrap>
                                    <Typography.Text strong>{item.title}</Typography.Text>
                                    <Typography.Text type="secondary">{item.sourceType}</Typography.Text>
                                  </Space>
                                }
                                description={
                                  <Space direction="vertical" size={2}>
                                    <Typography.Text>{item.snippet}</Typography.Text>
                                    <Typography.Text type="secondary">
                                      {item.sourceRef || `document:${item.documentId ?? "-"}`}
                                    </Typography.Text>
                                  </Space>
                                }
                              />
                            </List.Item>
                          )}
                        />
                      ) : (
                        <InlineEmpty
                          description="暂无引用来源"
                          action={
                            <Button icon={<SearchOutlined />} onClick={handleRetrieval}>
                              立即检索
                            </Button>
                          }
                        />
                      )}
                    </Space>
                  ),
                },
              ]}
            />
          </Card>
        </Space>

        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Card className="page-card" title="主链路动作">
            <Space direction="vertical" size="large" style={{ width: "100%" }}>
              <div className="subtle-note">
                <Typography.Text strong>当前状态</Typography.Text>
                <div style={{ marginTop: 8 }}>
                  <TicketStatusTag value={detail.ticket.status} /> 更新时间 {formatRelative(detail.ticket.updatedAt)}
                </div>
              </div>

              {detail.ticket.status === "WAITING_APPROVAL" ? (
                <Alert
                  type="warning"
                  showIcon
                  message="当前工单处于待审批状态，可跳转审批中心查看 adapter 列表与审批动作。"
                />
              ) : null}

              {canManageTicket ? (
                <>
                  <Form
                    form={statusForm}
                    layout="vertical"
                    onFinish={handleStatus}
                    initialValues={{ status: detail.ticket.status }}
                  >
                    <Form.Item label="更新状态" name="status" rules={[{ required: true }]}>
                      <Select>
                        {ticketStatusOptions.map((status) => (
                          <Select.Option key={status} value={status}>
                            {status}
                          </Select.Option>
                        ))}
                      </Select>
                    </Form.Item>
                    <Form.Item label="状态说明" name="reason">
                      <Input placeholder="例如：已定位原因，准备修复" maxLength={255} />
                    </Form.Item>
                    <Button type="primary" htmlType="submit" loading={statusLoading} block>
                      提交状态更新
                    </Button>
                  </Form>

                  <Divider style={{ margin: 0 }} />

                  <Form form={assignForm} layout="vertical" onFinish={handleAssign}>
                    <Form.Item
                      label="指派处理人 ID"
                      name="assigneeId"
                      rules={[{ required: true, message: "请输入处理人用户 ID" }]}
                    >
                      <Input type="number" placeholder="当前后端暂无用户列表接口，这里直接填写用户 ID" />
                    </Form.Item>
                    <Form.Item label="指派备注" name="note">
                      <Input placeholder="例如：转交 VPN 值班支持" maxLength={255} />
                    </Form.Item>
                    <Button htmlType="submit" loading={assignLoading} block>
                      提交指派
                    </Button>
                  </Form>
                </>
              ) : (
                <Alert
                  type="info"
                  showIcon
                  message="当前账号为只读/协作视角，状态更新与指派动作仅对支持人员或管理员开放。"
                />
              )}
            </Space>
          </Card>

          <Card className="page-card" title="AI 结论摘要">
            {aiDecision ? (
              <Space direction="vertical" size="middle" style={{ width: "100%" }}>
                <Descriptions column={1} size="small">
                  <Descriptions.Item label="分类">{aiDecision.category}</Descriptions.Item>
                  <Descriptions.Item label="置信度">
                    {(aiDecision.confidence * 100).toFixed(1)}%
                  </Descriptions.Item>
                  <Descriptions.Item label="审批判断">
                    {aiDecision.requiresApproval ? "需要审批" : "无需审批"}
                  </Descriptions.Item>
                  <Descriptions.Item label="人工接管">
                    {aiDecision.needsHumanHandoff ? "是" : "否"}
                  </Descriptions.Item>
                </Descriptions>
                <Button icon={<RobotOutlined />} loading={aiLoading} onClick={handleRunAi} block>
                  重新运行 AI
                </Button>
              </Space>
            ) : (
              <InlineEmpty
                description="尚未生成 AI 建议"
                action={
                  <Button type="primary" icon={<RobotOutlined />} onClick={handleRunAi}>
                    开始分析
                  </Button>
                }
              />
            )}
          </Card>
        </Space>
      </div>
    </Space>
  );
}
