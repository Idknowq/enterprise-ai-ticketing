"use client";

import { Button, Empty, Result, Space, Spin, Typography } from "antd";

export function PageLoading({ tip = "页面加载中..." }: { tip?: string }) {
  return (
    <div className="page-state">
      <Space direction="vertical" size="middle" align="center">
        <Spin size="large" />
        <Typography.Text type="secondary">{tip}</Typography.Text>
      </Space>
    </div>
  );
}

export function PageError({
  title = "页面加载失败",
  message,
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <Result
      status="error"
      title={title}
      subTitle={message || "请检查后端服务、登录状态或接口权限。"}
      extra={
        onRetry ? (
          <Button type="primary" onClick={onRetry}>
            重新加载
          </Button>
        ) : null
      }
    />
  );
}

export function InlineEmpty({
  description = "暂无数据",
  action,
}: {
  description?: string;
  action?: React.ReactNode;
}) {
  return <Empty description={description}>{action}</Empty>;
}

