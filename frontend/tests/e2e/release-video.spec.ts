import { expect, test, type Page } from "@playwright/test";

const videoTicketTitle = `发布录屏工单 ${Date.now()}`;
const videoTicketDescription =
  "需要开通生产环境日志只读权限，用于排查上线验证问题，请走审批流程。";

async function login(page: Page) {
  await page.goto("/login");
  await page.getByLabel("用户名").fill("admin01");
  await page.getByLabel("密码").fill("ChangeMe123!");
  await page.getByRole("button", { name: "登录控制台" }).click();
  await page.waitForURL("**/tickets");
  await page.waitForTimeout(800);
}

test("records the MVP frontend release tour", async ({ page }) => {
  await login(page);

  await expect(page.getByRole("heading", { name: "工单列表" })).toBeVisible();
  await page.waitForTimeout(800);

  await page.getByRole("button", { name: "新建工单" }).click();
  await page.getByLabel("标题").fill(videoTicketTitle);
  await page.getByLabel("描述").fill(videoTicketDescription);
  await page.getByLabel("分类").click();
  await page.getByText("权限申请 (ACCESS_REQUEST)").last().click();
  await page.waitForTimeout(700);
  await page.getByRole("button", { name: "提交工单" }).click();

  await page.waitForURL(/\/tickets\/\d+$/);
  const ticketId = Number(page.url().match(/\/tickets\/(\d+)$/)?.[1] ?? "0");
  expect(ticketId).toBeGreaterThan(0);
  await expect(page.getByRole("heading", { name: videoTicketTitle })).toBeVisible();
  await page.waitForTimeout(900);

  await page.getByRole("button", { name: "运行 AI 分析" }).click();
  await expect(page.getByText("AI 分析已完成")).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText("需要审批")).toBeVisible();
  await page.waitForTimeout(900);

  await page.getByRole("menuitem", { name: "审批中心" }).click();
  await expect(page.getByRole("heading", { name: "审批中心" })).toBeVisible();
  await expect(page.getByTestId(`approval-ticket-${ticketId}`)).toBeVisible({ timeout: 20_000 });
  await page.waitForTimeout(900);

  await page.getByTestId(`approve-ticket-${ticketId}`).click();
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByTestId("approval-comment-input").fill("发布录屏审批通过");
  await page.waitForTimeout(700);
  await dialog.getByTestId("approval-submit-button").click();
  await expect(dialog).toBeHidden({ timeout: 20_000 });
  await page.waitForTimeout(900);

  await page.getByRole("menuitem", { name: "文档管理" }).click();
  await expect(page.getByRole("heading", { name: "文档管理" })).toBeVisible();
  await expect(page.getByText("知识文档管理")).toBeVisible();
  await page.waitForTimeout(900);

  await page.getByRole("menuitem", { name: "基础监控" }).click();
  await expect(page.getByRole("heading", { name: "基础监控" })).toBeVisible();
  await expect(page.getByText("工单总数")).toBeVisible();
  await expect(page.getByText("状态分布", { exact: true })).toBeVisible();
  await page.waitForTimeout(1200);

  await page.getByRole("button", { name: "退出" }).click();
  await page.waitForURL("**/login");
  await expect(page.getByRole("heading", { name: "用户登录" })).toBeVisible();
  await page.waitForTimeout(800);
});
