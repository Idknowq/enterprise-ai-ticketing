"use client";

import { RoleTags } from "@/components/status-tags";
import { useAuth } from "@/components/app-provider";
import {
  AuditOutlined,
  BarChartOutlined,
  BookOutlined,
  FileTextOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from "@ant-design/icons";
import { Avatar, Button, Layout, Menu, Space, Spin, Typography } from "antd";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const { Header, Sider, Content } = Layout;

function canSeeApprovals(roles: string[]) {
  return roles.includes("APPROVER") || roles.includes("ADMIN");
}

function canSeeDocuments(roles: string[]) {
  return roles.includes("SUPPORT_AGENT") || roles.includes("ADMIN");
}

function resolveSelectedKey(pathname: string) {
  if (pathname.startsWith("/tickets")) {
    return "/tickets";
  }
  if (pathname.startsWith("/approvals")) {
    return "/approvals";
  }
  if (pathname.startsWith("/documents")) {
    return "/documents";
  }
  if (pathname.startsWith("/monitoring")) {
    return "/monitoring";
  }
  return "/tickets";
}

function resolvePageTitle(pathname: string) {
  if (pathname.startsWith("/tickets/")) {
    return "工单详情";
  }
  if (pathname.startsWith("/tickets")) {
    return "工单列表";
  }
  if (pathname.startsWith("/approvals")) {
    return "审批中心";
  }
  if (pathname.startsWith("/documents")) {
    return "文档管理";
  }
  if (pathname.startsWith("/monitoring")) {
    return "基础监控";
  }
  return "企业级 AI 工单编排系统";
}

export function ConsoleShell({ children }: { children: React.ReactNode }) {
  const { user, authLoading, logout } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) {
      router.replace("/login");
    }
  }, [authLoading, user, router]);

  if (authLoading || !user) {
    return (
      <div className="fullscreen-state">
        <Space direction="vertical" size="middle" align="center">
          <Spin size="large" />
          <Typography.Text type="secondary">正在校验登录状态...</Typography.Text>
        </Space>
      </div>
    );
  }

  const menuItems = [
    {
      key: "/tickets",
      icon: <FileTextOutlined />,
      label: "工单管理",
    },
    ...(canSeeApprovals(user.roles)
      ? [
          {
            key: "/approvals",
            icon: <AuditOutlined />,
            label: "审批中心",
          },
        ]
      : []),
    ...(canSeeDocuments(user.roles)
      ? [
          {
            key: "/documents",
            icon: <BookOutlined />,
            label: "文档管理",
          },
        ]
      : []),
    {
      key: "/monitoring",
      icon: <BarChartOutlined />,
      label: "基础监控",
    },
  ];

  return (
    <Layout className="app-shell">
      <Sider width={240} collapsed={collapsed} theme="light" className="app-sider">
        <div className="brand-block">
          <Typography.Title level={5} className="brand-title">
            {collapsed ? "AIT" : "AI Ticketing"}
          </Typography.Title>
          {!collapsed ? (
            <Typography.Text type="secondary">Enterprise MVP Console</Typography.Text>
          ) : null}
        </div>

        <Menu
          mode="inline"
          selectedKeys={[resolveSelectedKey(pathname)]}
          items={menuItems}
          onClick={({ key }) => router.push(String(key))}
        />
      </Sider>

      <Layout>
        <Header className="app-header">
          <Space size="middle">
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed((value) => !value)}
            />
            <div>
              <Typography.Title level={4} className="page-title">
                {resolvePageTitle(pathname)}
              </Typography.Title>
              <Typography.Text type="secondary">
                后台主链路演示控制台，围绕工单、审批、知识和观测构建。
              </Typography.Text>
            </div>
          </Space>

          <Space align="center" size="middle">
            <Space direction="vertical" size={0} className="user-meta">
              <Typography.Text strong>{user.displayName}</Typography.Text>
              <Typography.Text type="secondary">
                {user.department} · {user.username}
              </Typography.Text>
              <div>
                <RoleTags roles={user.roles} />
              </div>
            </Space>
            <Avatar>{user.displayName.slice(0, 1)}</Avatar>
            <Button icon={<LogoutOutlined />} onClick={() => logout()}>
              退出
            </Button>
          </Space>
        </Header>

        <Content className="app-content">{children}</Content>
      </Layout>
    </Layout>
  );
}

