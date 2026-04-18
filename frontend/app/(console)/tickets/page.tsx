"use client";

import { PageError, PageLoading } from "@/components/page-state";
import { PriorityTag, TicketStatusTag, priorityOptions, ticketStatusOptions } from "@/components/status-tags";
import { createTicket, listTickets } from "@/lib/services/tickets";
import type { CreateTicketRequest, TicketListQuery, TicketListResponse, TicketPriority, TicketStatus } from "@/types/api";
import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Drawer, Form, Input, Select, Space, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import Link from "next/link";
import { useEffect, useState } from "react";
import { formatDateTime } from "@/lib/date";
import { useRouter } from "next/navigation";

type FilterValues = {
  keyword?: string;
  status?: TicketStatus;
  priority?: TicketPriority;
  category?: string;
};

export default function TicketsPage() {
  const { message } = App.useApp();
  const router = useRouter();
  const [form] = Form.useForm<FilterValues>();
  const [createForm] = Form.useForm<CreateTicketRequest>();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [filters, setFilters] = useState<TicketListQuery>({ page: 0, size: 10 });
  const [data, setData] = useState<TicketListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadTickets(nextFilters = filters) {
    setLoading(true);
    setError(null);
    try {
      const response = await listTickets(nextFilters);
      setData(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "工单列表加载失败");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadTickets(filters);
  }, [filters]);

  async function handleCreate(values: CreateTicketRequest) {
    setCreateLoading(true);
    try {
      const ticket = await createTicket(values);
      message.success("工单创建成功");
      setDrawerOpen(false);
      createForm.resetFields();
      await loadTickets({ ...filters, page: 0 });
      router.push(`/tickets/${ticket.id}`);
    } catch (createError) {
      message.error(createError instanceof Error ? createError.message : "工单创建失败");
    } finally {
      setCreateLoading(false);
    }
  }

  const columns: ColumnsType<TicketListResponse["items"][number]> = [
    {
      title: "工单 ID",
      dataIndex: "id",
      width: 100,
      render: (value: number) => <Typography.Text code>#{value}</Typography.Text>,
    },
    {
      title: "标题",
      dataIndex: "title",
      render: (_, record) => (
        <Space direction="vertical" size={2}>
          <Link href={`/tickets/${record.id}`}>{record.title}</Link>
          <Typography.Text type="secondary">{record.category || "未分类"}</Typography.Text>
        </Space>
      ),
    },
    {
      title: "优先级",
      dataIndex: "priority",
      width: 110,
      render: (value) => <PriorityTag value={value} />,
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 120,
      render: (value) => <TicketStatusTag value={value} />,
    },
    {
      title: "提交人",
      width: 160,
      render: (_, record) => record.requester?.displayName || "-",
    },
    {
      title: "处理人",
      width: 160,
      render: (_, record) => record.assignee?.displayName || "未分配",
    },
    {
      title: "更新时间",
      dataIndex: "updatedAt",
      width: 180,
      render: (value) => formatDateTime(value),
    },
    {
      title: "操作",
      width: 120,
      render: (_, record) => (
        <Link href={`/tickets/${record.id}`}>
          <Button type="link">查看详情</Button>
        </Link>
      ),
    },
  ];

  if (loading && !data) {
    return <PageLoading tip="正在加载工单列表..." />;
  }

  if (error && !data) {
    return <PageError message={error} onRetry={() => loadTickets(filters)} />;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card className="page-card">
        <Form
          form={form}
          layout="inline"
          onFinish={(values) =>
            setFilters({
              page: 0,
              size: filters.size,
              keyword: values.keyword,
              status: values.status,
              priority: values.priority,
              category: values.category,
            })
          }
        >
          <Form.Item name="keyword">
            <Input allowClear prefix={<SearchOutlined />} placeholder="标题/描述关键词" style={{ width: 220 }} />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear placeholder="状态" style={{ width: 160 }}>
              {ticketStatusOptions.map((status) => (
                <Select.Option key={status} value={status}>
                  {status}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="priority">
            <Select allowClear placeholder="优先级" style={{ width: 140 }}>
              {priorityOptions.map((priority) => (
                <Select.Option key={priority} value={priority}>
                  {priority}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="category">
            <Input allowClear placeholder="分类，例如 VPN" style={{ width: 180 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                筛选
              </Button>
              <Button
                onClick={() => {
                  form.resetFields();
                  setFilters({ page: 0, size: filters.size });
                }}
              >
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Card
        className="page-card"
        title="工单列表"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => loadTickets(filters)}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setDrawerOpen(true)}>
              新建工单
            </Button>
          </Space>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={data?.items || []}
          loading={loading}
          pagination={{
            current: (data?.page || 0) + 1,
            pageSize: data?.size || 10,
            total: data?.totalElements || 0,
            showSizeChanger: true,
            onChange: (page, pageSize) => {
              setFilters((current) => ({
                ...current,
                page: page - 1,
                size: pageSize,
              }));
            },
          }}
        />
      </Card>

      <Drawer
        title="新建工单"
        open={drawerOpen}
        width={520}
        onClose={() => setDrawerOpen(false)}
        destroyOnClose
      >
        <Form form={createForm} layout="vertical" onFinish={handleCreate}>
          <Form.Item
            label="标题"
            name="title"
            rules={[{ required: true, message: "请输入工单标题" }]}
          >
            <Input placeholder="例如：VPN 证书失效，无法远程办公" maxLength={255} />
          </Form.Item>
          <Form.Item
            label="描述"
            name="description"
            rules={[{ required: true, message: "请输入问题描述" }]}
          >
            <Input.TextArea
              rows={6}
              placeholder="请填写问题现象、影响范围、复现时间、已尝试动作等"
              maxLength={5000}
            />
          </Form.Item>
          <Form.Item label="分类" name="category">
            <Input placeholder="例如：VPN / 权限申请 / 密码重置" maxLength={128} />
          </Form.Item>
          <Form.Item label="优先级" name="priority" initialValue="MEDIUM">
            <Select>
              {priorityOptions.map((priority) => (
                <Select.Option key={priority} value={priority}>
                  {priority}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Space>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
            <Button type="primary" htmlType="submit" loading={createLoading}>
              提交工单
            </Button>
          </Space>
        </Form>
      </Drawer>
    </Space>
  );
}
