import { expect, test, type Page } from "@playwright/test";
import path from "node:path";

const ticketTitle = `Playwright 审批烟测 ${Date.now()}`;
const rejectedTicketTitle = `Playwright 驳回烟测 ${Date.now()}`;
const accessRequestDescription =
  "需要开通生产环境日志只读权限，用于排查线上问题，请尽快审批。";
const rejectRequestDescription =
  "需要申请生产数据库访问权限，但缺少变更窗口和审批依据，请驳回这条申请。";
const documentTitle = `Playwright 知识文档 ${Date.now()}`;
const documentFixturePath = path.resolve(__dirname, "../fixtures/knowledge-upload.txt");
let createdTicketId: number | null = null;
let rejectedTicketId: number | null = null;

async function login(page: Page, username: string, password = "ChangeMe123!") {
  await page.goto("/login");
  await page.getByLabel("用户名").fill(username);
  await page.getByLabel("密码").fill(password);
  await page.getByRole("button", { name: "登录控制台" }).click();
  await page.waitForURL("**/tickets");
}

async function logout(page: Page) {
  await page.getByRole("button", { name: "退出" }).click();
  await page.waitForURL("**/login");
}

async function createApprovalTicket(page: Page, title: string, description: string) {
  await page.getByRole("button", { name: "新建工单" }).click();
  await page.getByLabel("标题").fill(title);
  await page.getByLabel("描述").fill(description);
  await page.getByLabel("分类").click();
  await page.getByText("权限申请 (ACCESS_REQUEST)").last().click();
  await page.getByRole("button", { name: "提交工单" }).click();

  await page.waitForURL(/\/tickets\/\d+$/);
  const ticketId = Number(page.url().match(/\/tickets\/(\d+)$/)?.[1] ?? "0");
  expect(ticketId).toBeGreaterThan(0);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
  await expect(page.getByText(description)).toBeVisible();

  await page.getByRole("button", { name: "运行 AI 分析" }).click();
  await expect(page.getByText("AI 分析已完成")).toBeVisible();
  await expect(page.getByText("需要审批")).toBeVisible();

  return ticketId;
}

async function approveTicket(page: Page, ticketId: number, comment: string) {
  const approveButton = page.getByTestId(`approve-ticket-${ticketId}`);
  await expect(approveButton).toBeVisible({ timeout: 15_000 });
  await approveButton.click();

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByTestId("approval-comment-input").fill(comment);
  await dialog.getByTestId("approval-submit-button").click();
  await expect(dialog).toBeHidden({ timeout: 15_000 });
}

async function rejectTicket(page: Page, ticketId: number, comment: string) {
  const rejectButton = page.getByTestId(`reject-ticket-${ticketId}`);
  await expect(rejectButton).toBeVisible({ timeout: 15_000 });
  await rejectButton.click();

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByTestId("approval-comment-input").fill(comment);
  await dialog.getByTestId("approval-submit-button").click();
  await expect(dialog).toBeHidden({ timeout: 15_000 });
}

test.describe.configure({ mode: "serial" });

test("admin can create a ticket and trigger approval-required AI flow", async ({ page }) => {
  await login(page, "admin01");

  await expect(page.getByRole("heading", { name: "工单列表" })).toBeVisible();
  await expect(page.getByRole("menuitem", { name: "审批中心" })).toBeVisible();
  await expect(page.getByRole("menuitem", { name: "文档管理" })).toBeVisible();

  createdTicketId = await createApprovalTicket(page, ticketTitle, accessRequestDescription);

  await logout(page);
});

test("admin can approve the pending ticket from the approvals center", async ({ page }) => {
  const ticketId = createdTicketId ?? 0;
  expect(ticketId).toBeGreaterThan(0);

  await login(page, "admin01");

  await page.getByRole("menuitem", { name: "审批中心" }).click();
  await expect(page.getByRole("heading", { name: "审批中心" })).toBeVisible();
  await expect(page.getByText("待审批列表")).toBeVisible();
  await expect(page.getByTestId(`approval-ticket-${ticketId}`)).toBeVisible({ timeout: 15_000 });
  await expect(page.getByRole("link", { name: ticketTitle })).toBeVisible();

  let processedApprovals = 0;
  const approvalComments = ["Playwright 一审通过", "Playwright 二审通过"];

  for (const comment of approvalComments) {
    const approveButton = page.getByTestId(`approve-ticket-${ticketId}`);
    if (!(await approveButton.count())) {
      break;
    }
    await approveTicket(page, ticketId, comment);
    processedApprovals += 1;
  }

  expect(processedApprovals).toBeGreaterThan(0);
  await expect(page.getByTestId(`approve-ticket-${ticketId}`)).toHaveCount(0);
  await expect(page.getByTestId(`approval-ticket-${ticketId}`)).toHaveCount(0);

  await page.getByRole("menuitem", { name: "工单管理" }).click();
  await expect(page.getByRole("heading", { name: "工单列表" })).toBeVisible();
  await page.locator(`a[href='/tickets/${ticketId}']`).first().click();
  await page.waitForURL(new RegExp(`/tickets/${ticketId}$`));
  await expect(page.getByRole("heading", { name: ticketTitle })).toBeVisible();
  await page.getByRole("tab", { name: /审批记录/ }).click();
  await expect(page.getByText("Line manager approval")).toBeVisible();
  await expect(page.getByText("Playwright 一审通过")).toBeVisible();

  if (processedApprovals > 1) {
    await expect(page.getByText("System administrator approval")).toBeVisible();
    await expect(page.getByText("Playwright 二审通过")).toBeVisible();
  }

  await logout(page);
});

test("admin can reject a pending ticket from the approvals center", async ({ page }) => {
  await login(page, "admin01");
  rejectedTicketId = await createApprovalTicket(page, rejectedTicketTitle, rejectRequestDescription);

  await page.getByRole("menuitem", { name: "审批中心" }).click();
  await expect(page.getByRole("heading", { name: "审批中心" })).toBeVisible();
  await expect(page.getByTestId(`approval-ticket-${rejectedTicketId}`)).toBeVisible({ timeout: 15_000 });

  await rejectTicket(page, rejectedTicketId, "Playwright 驳回: 缺少审批依据和变更窗口");
  await expect(page.getByTestId(`reject-ticket-${rejectedTicketId}`)).toHaveCount(0);
  await expect(page.getByTestId(`approval-ticket-${rejectedTicketId}`)).toHaveCount(0);

  await page.getByRole("menuitem", { name: "工单管理" }).click();
  await expect(page.getByRole("heading", { name: "工单列表" })).toBeVisible();
  await page.locator(`a[href='/tickets/${rejectedTicketId}']`).first().click();
  await page.waitForURL(new RegExp(`/tickets/${rejectedTicketId}$`));
  await expect(page.getByRole("heading", { name: rejectedTicketTitle })).toBeVisible();
  await expect(page.getByText("已驳回", { exact: true }).first()).toBeVisible();
  await page.getByRole("tab", { name: /审批记录/ }).click();
  await expect(page.getByText("Line manager approval")).toBeVisible();
  await expect(page.getByText("Playwright 驳回: 缺少审批依据和变更窗口")).toBeVisible();

  await logout(page);
});

test("admin can upload and filter a knowledge document", async ({ page }) => {
  await login(page, "admin01");

  await page.getByRole("menuitem", { name: "文档管理" }).click();
  await expect(page.getByRole("heading", { name: "文档管理" })).toBeVisible();
  await expect(page.getByText("知识文档管理")).toBeVisible();

  await page.getByRole("button", { name: "上传文档" }).click();
  const uploadDialog = page.getByRole("dialog", { name: "上传知识文档" });
  await expect(uploadDialog).toBeVisible();
  await uploadDialog.locator("input[type='file']").setInputFiles(documentFixturePath);
  await uploadDialog.getByLabel("标题").fill(documentTitle);
  await uploadDialog.getByLabel("分类").click();
  await page.getByText("通用 FAQ (GENERAL_FAQ)").last().click();
  await uploadDialog.getByPlaceholder("例如：IT，默认可留空").fill("QA");
  await uploadDialog.getByPlaceholder("例如：v1.0").fill("v2.0");
  await uploadDialog.getByRole("button", { name: "上传并索引" }).click();
  await expect(uploadDialog).toBeHidden({ timeout: 20_000 });
  await expect(page.getByText(documentTitle)).toBeVisible({ timeout: 20_000 });

  await page.getByPlaceholder("标题/文件名关键词").fill(documentTitle);
  await page.getByRole("button", { name: /筛\s*选/ }).click();
  const uploadedRow = page.locator("tr").filter({ hasText: documentTitle }).first();
  await expect(uploadedRow).toBeVisible({ timeout: 15_000 });
  await expect(uploadedRow).toContainText("knowledge-upload.txt");
  await expect(uploadedRow).toContainText("通用 FAQ (GENERAL_FAQ) / QA");

  await logout(page);
});

test("admin can view the monitoring dashboard", async ({ page }) => {
  await login(page, "admin01");

  await page.getByRole("menuitem", { name: "基础监控" }).click();
  await expect(page.getByRole("heading", { name: "基础监控" })).toBeVisible();
  await expect(page.getByText("当前监控页已切换到真实观测接口")).toBeVisible();
  await expect(page.getByText("工单总数")).toBeVisible();
  await expect(page.getByText("AI 成功率", { exact: true })).toBeVisible();
  await expect(page.getByText("状态分布", { exact: true })).toBeVisible();
  await expect(page.getByText("审批与 Workflow", { exact: true })).toBeVisible();
  await expect(page.getByText("指标说明", { exact: true })).toBeVisible();
  await expect(page.getByText("待审批数", { exact: true })).toBeVisible();

  await logout(page);
});
