"use client";

import { useAuth } from "@/components/app-provider";
import { PageError, PageLoading } from "@/components/page-state";
import {
  AccessLevelTag,
  DocumentIndexStatusTag,
  accessLevelOptions,
  documentIndexStatusOptions,
} from "@/components/status-tags";
import { formatDateTime } from "@/lib/date";
import { listDocuments, uploadDocument } from "@/lib/services/documents";
import type {
  DocumentListQuery,
  DocumentListResponse,
  DocumentUploadPayload,
  KnowledgeAccessLevel,
} from "@/types/api";
import { InboxOutlined, ReloadOutlined, UploadOutlined } from "@ant-design/icons";
import {
  App,
  Button,
  Card,
  DatePicker,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Typography,
  Upload,
} from "antd";
import dayjs, { type Dayjs } from "dayjs";
import { useCallback, useEffect, useState } from "react";

export default function DocumentsPage() {
  const { message } = App.useApp();
  const { user } = useAuth();
  const [filterForm] = Form.useForm<DocumentListQuery>();
  const [uploadForm] = Form.useForm<
    Omit<DocumentUploadPayload, "file" | "updatedAt"> & { updatedAt: Dayjs }
  >();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadOpen, setUploadOpen] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [filters, setFilters] = useState<DocumentListQuery>({ page: 0, size: 10 });
  const [data, setData] = useState<DocumentListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const canAccess = Boolean(user?.roles.includes("SUPPORT_AGENT") || user?.roles.includes("ADMIN"));
  const canUpload = Boolean(user?.roles.includes("ADMIN"));

  const loadDocuments = useCallback(async (nextFilters = filters) => {
    setLoading(true);
    setError(null);
    try {
      const response = await listDocuments(nextFilters);
      setData(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "文档列表加载失败");
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    if (canAccess) {
      loadDocuments(filters);
    } else {
      setLoading(false);
    }
  }, [canAccess, filters, loadDocuments]);

  async function handleUpload(values: {
    title?: string;
    category: string;
    department?: string;
    accessLevel: KnowledgeAccessLevel;
    version: string;
    updatedAt: Dayjs;
  }) {
    if (!selectedFile) {
      message.error("请选择上传文件");
      return;
    }

    setUploading(true);
    try {
      await uploadDocument({
        file: selectedFile,
        title: values.title,
        category: values.category,
        department: values.department,
        accessLevel: values.accessLevel,
        version: values.version,
        updatedAt: values.updatedAt.toISOString(),
      });
      message.success("文档上传成功");
      setUploadOpen(false);
      setSelectedFile(null);
      uploadForm.resetFields();
      await loadDocuments({ ...filters, page: 0 });
    } catch (uploadError) {
      message.error(uploadError instanceof Error ? uploadError.message : "文档上传失败");
    } finally {
      setUploading(false);
    }
  }

  if (!canAccess) {
    return <PageError title="无文档访问权限" message="当前页面仅对支持人员或管理员开放。" />;
  }

  if (loading && !data) {
    return <PageLoading tip="正在加载文档列表..." />;
  }

  if (error && !data) {
    return <PageError message={error} onRetry={() => loadDocuments(filters)} />;
  }

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      <Card className="page-card">
        <Form
          form={filterForm}
          layout="inline"
          onFinish={(values) =>
            setFilters({
              page: 0,
              size: filters.size,
              keyword: values.keyword,
              category: values.category,
              department: values.department,
              accessLevel: values.accessLevel,
              indexStatus: values.indexStatus,
            })
          }
        >
          <Form.Item name="keyword">
            <Input allowClear placeholder="标题/文件名关键词" style={{ width: 220 }} />
          </Form.Item>
          <Form.Item name="category">
            <Input allowClear placeholder="类别，例如 VPN" style={{ width: 160 }} />
          </Form.Item>
          <Form.Item name="department">
            <Input allowClear placeholder="部门，例如 IT" style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="accessLevel">
            <Select allowClear placeholder="访问级别" style={{ width: 160 }}>
              {accessLevelOptions.map((level) => (
                <Select.Option key={level} value={level}>
                  {level}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="indexStatus">
            <Select allowClear placeholder="索引状态" style={{ width: 160 }}>
              {documentIndexStatusOptions.map((status) => (
                <Select.Option key={status} value={status}>
                  {status}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                筛选
              </Button>
              <Button
                onClick={() => {
                  filterForm.resetFields();
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
        title="知识文档管理"
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={() => loadDocuments(filters)}>
              刷新
            </Button>
            <Button
              type="primary"
              icon={<UploadOutlined />}
              disabled={!canUpload}
              onClick={() => setUploadOpen(true)}
            >
              上传文档
            </Button>
          </Space>
        }
      >
        {!canUpload ? (
          <Typography.Paragraph type="secondary">
            当前账号可查看文档列表，但上传接口仅对管理员开放。
          </Typography.Paragraph>
        ) : null}

        <Table
          rowKey="id"
          dataSource={data?.items || []}
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
          columns={[
            {
              title: "文档",
              render: (_, record) => (
                <Space direction="vertical" size={2}>
                  <Typography.Text strong>{record.title}</Typography.Text>
                  <Typography.Text type="secondary">{record.sourceFilename}</Typography.Text>
                </Space>
              ),
            },
            {
              title: "类型",
              dataIndex: "documentType",
              width: 120,
            },
            {
              title: "分类/部门",
              width: 180,
              render: (_, record) => `${record.metadata.category} / ${record.metadata.department}`,
            },
            {
              title: "访问级别",
              width: 120,
              render: (_, record) => <AccessLevelTag value={record.metadata.accessLevel} />,
            },
            {
              title: "索引状态",
              width: 120,
              render: (_, record) => <DocumentIndexStatusTag value={record.indexStatus} />,
            },
            {
              title: "版本",
              width: 100,
              render: (_, record) => record.metadata.version,
            },
            {
              title: "更新时间",
              width: 180,
              render: (_, record) => formatDateTime(record.metadata.updatedAt),
            },
          ]}
        />
      </Card>

      <Modal
        title="上传知识文档"
        open={uploadOpen}
        onCancel={() => {
          setUploadOpen(false);
          setSelectedFile(null);
          uploadForm.resetFields();
        }}
        footer={null}
        destroyOnHidden
        width={640}
      >
        <Form
          form={uploadForm}
          layout="vertical"
          initialValues={{
            accessLevel: "INTERNAL",
            version: "v1.0",
            updatedAt: dayjs(),
          }}
          onFinish={handleUpload}
        >
          <Form.Item label="文档文件" required>
            <Upload.Dragger
              maxCount={1}
              beforeUpload={(file) => {
                setSelectedFile(file);
                return false;
              }}
              onRemove={() => {
                setSelectedFile(null);
              }}
              accept=".md,.markdown,.txt,.pdf"
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽上传 Markdown / TXT / PDF</p>
            </Upload.Dragger>
          </Form.Item>
          <Form.Item label="标题" name="title">
            <Input placeholder="可选，不填则取文件名" />
          </Form.Item>
          <Form.Item
            label="分类"
            name="category"
            rules={[{ required: true, message: "请输入文档分类" }]}
          >
            <Input placeholder="例如：VPN / 权限 / 开发环境" />
          </Form.Item>
          <Form.Item label="部门" name="department">
            <Input placeholder="例如：IT，默认可留空" />
          </Form.Item>
          <Form.Item
            label="访问级别"
            name="accessLevel"
            rules={[{ required: true, message: "请选择访问级别" }]}
          >
            <Select>
              {accessLevelOptions.map((level) => (
                <Select.Option key={level} value={level}>
                  {level}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            label="版本"
            name="version"
            rules={[{ required: true, message: "请输入版本号" }]}
          >
            <Input placeholder="例如：v1.0" />
          </Form.Item>
          <Form.Item
            label="文档更新时间"
            name="updatedAt"
            rules={[{ required: true, message: "请选择更新时间" }]}
          >
            <DatePicker showTime style={{ width: "100%" }} />
          </Form.Item>
          <Space>
            <Button
              onClick={() => {
                setUploadOpen(false);
                setSelectedFile(null);
              }}
            >
              取消
            </Button>
            <Button type="primary" htmlType="submit" loading={uploading}>
              上传并索引
            </Button>
          </Space>
        </Form>
      </Modal>
    </Space>
  );
}
