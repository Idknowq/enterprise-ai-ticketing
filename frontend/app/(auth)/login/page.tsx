"use client";

import { useAuth } from "@/components/app-provider";
import { PageLoading } from "@/components/page-state";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { App, Button, Card, Divider, Form, Input, Space, Typography } from "antd";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

export default function LoginPage() {
  const { message } = App.useApp();
  const { login, user, authLoading } = useAuth();
  const router = useRouter();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!authLoading && user) {
      router.replace("/tickets");
    }
  }, [authLoading, user, router]);

  if (authLoading) {
    return <PageLoading tip="正在加载登录态..." />;
  }

  async function handleSubmit(values: { username: string; password: string }) {
    setSubmitting(true);
    try {
      await login(values);
      message.success("登录成功");
      router.replace("/tickets");
    } catch (error) {
      const nextMessage = error instanceof Error ? error.message : "登录失败";
      message.error(nextMessage);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div>
          <Typography.Text style={{ color: "#bfdbfe" }}>Enterprise Internal Console</Typography.Text>
          <Typography.Title level={1} style={{ color: "#eff6ff", marginTop: 16 }}>
            企业级 AI 工单编排系统
          </Typography.Title>
          <Typography.Paragraph style={{ color: "#dbeafe", fontSize: 16, maxWidth: 520 }}>
            面向企业 IT 服务台的后台控制台，承载工单提交、工单处理、审批流演示、知识文档管理和基础观测展示。
          </Typography.Paragraph>
        </div>

        <Space direction="vertical" size="large">
          <div className="subtle-note" style={{ background: "rgba(255,255,255,0.08)", borderColor: "rgba(255,255,255,0.18)" }}>
            <Typography.Text style={{ color: "#eff6ff" }}>
              演示主链路：登录 → 创建工单 → 查看详情 → 运行 AI 建议 → 进入审批/查看监控。
            </Typography.Text>
          </div>
          <div className="subtle-note" style={{ background: "rgba(255,255,255,0.08)", borderColor: "rgba(255,255,255,0.18)" }}>
            <Typography.Text style={{ color: "#eff6ff" }}>
              文档页对接后端知识库上传与列表接口；审批页目前使用 adapter 模式承接待审批演示。
            </Typography.Text>
          </div>
        </Space>
      </section>

      <section className="login-panel">
        <Card className="login-card">
          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            <div>
              <Typography.Title level={3} style={{ marginBottom: 8 }}>
                用户登录
              </Typography.Title>
              <Typography.Text type="secondary">
                使用后端初始化账号登录，进入控制台联调各模块。
              </Typography.Text>
            </div>

            <Form layout="vertical" onFinish={handleSubmit} initialValues={{ username: "admin01", password: "ChangeMe123!" }}>
              <Form.Item
                label="用户名"
                name="username"
                rules={[{ required: true, message: "请输入用户名" }]}
              >
                <Input prefix={<UserOutlined />} placeholder="例如：admin01" />
              </Form.Item>
              <Form.Item
                label="密码"
                name="password"
                rules={[{ required: true, message: "请输入密码" }]}
              >
                <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={submitting}>
                登录控制台
              </Button>
            </Form>

            <Divider style={{ margin: 0 }} />

            <div className="subtle-note">
              <Typography.Text strong>演示账号</Typography.Text>
              <div style={{ marginTop: 8 }}>
                <Typography.Paragraph style={{ marginBottom: 6 }}>员工：`employee01 / ChangeMe123!`</Typography.Paragraph>
                <Typography.Paragraph style={{ marginBottom: 6 }}>支持：`support01 / ChangeMe123!`</Typography.Paragraph>
                <Typography.Paragraph style={{ marginBottom: 6 }}>审批：`approver01 / ChangeMe123!`</Typography.Paragraph>
                <Typography.Paragraph style={{ marginBottom: 0 }}>管理员：`admin01 / ChangeMe123!`</Typography.Paragraph>
              </div>
            </div>
          </Space>
        </Card>
      </section>
    </div>
  );
}

