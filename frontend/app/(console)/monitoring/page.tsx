"use client";

import { PageError, PageLoading } from "@/components/page-state";
import { TicketStatusTag } from "@/components/status-tags";
import { formatDurationMs } from "@/lib/date";
import { getMonitoringOverview } from "@/lib/services/monitoring";
import type { MonitoringOverview } from "@/types/api";
import { ReloadOutlined } from "@ant-design/icons";
import { Alert, Button, Card, Col, Row, Space, Statistic, Typography } from "antd";
import { useEffect, useState } from "react";

export default function MonitoringPage() {
  const [overview, setOverview] = useState<MonitoringOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadOverview() {
    setLoading(true);
    setError(null);
    try {
      const response = await getMonitoringOverview();
      setOverview(response.overview);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "监控概览加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadOverview();
  }, []);

  if (loading && !overview) {
    return <PageLoading tip="正在汇总监控概览..." />;
  }

  if (error && !overview) {
    return <PageError message={error} onRetry={loadOverview} />;
  }

  if (!overview) {
    return <PageError message="暂无监控数据" onRetry={loadOverview} />;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Alert
        type="info"
        showIcon
        message="当前监控页已切换到真实观测接口"
        description={`数据来源：${overview.derivedFrom}。页面直接对接 /api/observability/dashboard，展示后端聚合的工单、AI、检索、审批和 workflow 指标。`}
      />

      <div className="summary-grid">
        <div className="metric-card">
          <span className="metric-label">工单总数</span>
          <div className="metric-value">{overview.ticketTotal}</div>
        </div>
        <div className="metric-card">
          <span className="metric-label">AI 成功率</span>
          <div className="metric-value">
            {overview.aiSuccessRate === null ? "-" : `${overview.aiSuccessRate.toFixed(1)}%`}
          </div>
        </div>
        <div className="metric-card">
          <span className="metric-label">平均 AI 耗时</span>
          <div className="metric-value">{formatDurationMs(overview.averageModelLatencyMs)}</div>
        </div>
        <div className="metric-card">
          <span className="metric-label">平均检索耗时</span>
          <div className="metric-value">{formatDurationMs(overview.averageRetrievalLatencyMs)}</div>
        </div>
      </div>

      <Card
        className="page-card"
        title="状态分布"
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadOverview}>
            刷新
          </Button>
        }
      >
        <div className="distribution-list">
          {overview.statusDistribution.map((item) => (
            <div className="distribution-row" key={item.status}>
              <div>
                <TicketStatusTag value={item.status} />
              </div>
              <div className="distribution-bar">
                <div className="distribution-fill" style={{ width: `${item.ratio}%` }} />
              </div>
              <Typography.Text>{item.count}</Typography.Text>
            </div>
          ))}
        </div>
      </Card>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card className="page-card" title="审批与 Workflow">
            <Row gutter={[16, 16]}>
              <Col span={12}>
                <Statistic title="待审批数" value={overview.pendingApprovals} />
              </Col>
              <Col span={12}>
                <Statistic
                  title="平均审批等待"
                  value={formatDurationMs(overview.averageApprovalWaitMs)}
                />
              </Col>
              <Col span={12}>
                <Statistic title="Workflow 失败数" value={overview.workflowFailureCount} precision={0} />
              </Col>
              <Col span={12}>
                <Statistic title="Workflow 重试数" value={overview.workflowRetryCount} precision={0} />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card className="page-card" title="指标说明">
            <Space direction="vertical" size="middle">
              <Typography.Paragraph>
                AI 成功率来自后端 `AiNodeName.ORCHESTRATION` 的成功/总运行比值。
              </Typography.Paragraph>
              <Typography.Paragraph>
                平均检索耗时来自 `RETRIEVER` 成功节点平均延迟，审批等待时间来自审批记录决策耗时聚合。
              </Typography.Paragraph>
              <Typography.Paragraph style={{ marginBottom: 0 }}>
                工单状态分布与待审批数均来自真实数据库聚合，不再依赖前端采样或 mock 计算。
              </Typography.Paragraph>
            </Space>
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
