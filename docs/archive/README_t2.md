# 认证与权限模块说明

本文档用于说明当前仓库中认证与权限模块的使用方式、验证方法，以及其他 thread 如何复用该模块能力。

## 1. 模块目标

本模块负责提供企业级 AI 工单编排系统的通用认证与权限底座，覆盖：

- 用户模型
- 角色模型
- 登录接口
- JWT 鉴权
- Spring Security 接入
- 当前登录用户上下文
- 通用 RBAC 校验能力
- 基础用户与角色初始化

本模块不实现 ticket、approval、document 等具体业务状态机，只提供可复用的身份识别和权限判断能力。

## 2. 当前支持的角色

- `EMPLOYEE`
- `SUPPORT_AGENT`
- `APPROVER`
- `ADMIN`

## 3. 关键代码位置

### 认证入口

- `backend/src/main/java/com/enterprise/ticketing/auth/controller/AuthController.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/service/AuthService.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/service/AuthServiceImpl.java`

### JWT 与安全配置

- `backend/src/main/java/com/enterprise/ticketing/auth/security/JwtTokenProvider.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/enterprise/ticketing/config/SecurityBaselineConfig.java`

### 用户上下文与 RBAC

- `backend/src/main/java/com/enterprise/ticketing/auth/context/UserContext.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/access/AccessControlService.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/access/RoleChecker.java`

### 数据模型与初始化

- `backend/src/main/java/com/enterprise/ticketing/auth/entity/UserEntity.java`
- `backend/src/main/java/com/enterprise/ticketing/auth/entity/RoleEntity.java`
- `backend/src/main/resources/db/migration/V2__init_auth_tables.sql`
- `backend/src/main/java/com/enterprise/ticketing/auth/bootstrap/AuthDataInitializer.java`

## 4. 如何使用

### 4.1 启动前准备

确保以下基础设施可用：

- PostgreSQL
- 应用配置中的数据库连接可正常访问

如果本地使用仓库默认方式，可先在项目根目录执行：

```bash
docker compose up -d
```

再进入后端目录启动应用：

```bash
cd backend
mvn spring-boot:run
```

如果本机 `mvn` 未在 PATH 中，可使用本地 Maven 实际路径执行。

### 4.2 初始化数据

应用启动后会自动执行：

1. Flyway migration，创建认证相关表：
   - `roles`
   - `users`
   - `user_roles`
2. `AuthDataInitializer` 初始化基础角色和演示账号

默认演示账号如下：

- `employee01`
- `support01`
- `approver01`
- `admin01`

默认密码：

```text
ChangeMe123!
```

### 4.3 登录

请求：

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin01",
  "password": "ChangeMe123!"
}
```

返回结果中会包含：

- `accessToken`
- `tokenType`
- `expiresInSeconds`
- `expiresAt`
- 当前用户基础信息与角色

### 4.4 使用 JWT 访问后续接口

将登录返回的 token 放入请求头：

```http
Authorization: Bearer <accessToken>
```

例如：

```http
GET /api/auth/me
Authorization: Bearer <accessToken>
```

### 4.5 JWT 配置项

配置位置：`backend/src/main/resources/application.yml`

可通过以下配置控制 JWT：

- `app.auth.jwt.issuer`
- `app.auth.jwt.secret`
- `app.auth.jwt.access-token-ttl`

对应环境变量：

- `APP_AUTH_JWT_ISSUER`
- `APP_AUTH_JWT_SECRET`
- `APP_AUTH_JWT_ACCESS_TOKEN_TTL`

注意：

- `secret` 建议在真实环境中使用高强度随机值
- 当前代码要求 secret 长度至少 32 字节

## 5. 如何验证

### 5.1 基础验证

验证启动成功：

- 打开 `http://localhost:8080/swagger-ui/index.html`
- 打开 `http://localhost:8080/api/platform/info`

### 5.2 登录验证

使用任一初始化账号调用：

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin01",
    "password": "ChangeMe123!"
  }'
```

预期：

- 返回 `success: true`
- 返回 JWT token

### 5.3 当前用户验证

拿到 token 后调用：

```bash
curl http://localhost:8080/api/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

预期：

- 能返回当前登录用户
- 返回的 `roles` 与登录账号匹配

### 5.4 未登录访问验证

不带 token 直接访问受保护接口：

```bash
curl http://localhost:8080/api/auth/me
```

预期：

- 返回 `401`
- 返回统一错误体

### 5.5 非法 token 验证

使用伪造 token 调用受保护接口：

```bash
curl http://localhost:8080/api/auth/me \
  -H 'Authorization: Bearer invalid-token'
```

预期：

- 返回 `401`
- 错误码为认证类错误码

### 5.6 角色区分验证

分别用以下账号登录并调用 `/api/auth/me`：

- `employee01`
- `approver01`
- `admin01`

预期：

- 三者返回的角色不同
- 可作为后续 Ticket、Approval、Document 模块的 RBAC 基础输入

### 5.7 编译验证

当前模块已完成一次编译验证：

```bash
cd backend
/opt/homebrew/var/homebrew/tmp/.cellar/maven/3.9.14/libexec/bin/mvn -q -DskipTests compile
```

## 6. 其他 thread 如何使用

## 6.1 获取当前登录用户

其他模块可直接注入：

```java
private final UserContext userContext;
```

常用方法：

- `userContext.currentUser()`
- `userContext.requireCurrentUser()`
- `userContext.currentUserId()`
- `userContext.requireCurrentUserId()`

适用场景：

- 获取当前操作者 ID
- 记录事件日志 operator
- 查询“当前用户本人”的资源

## 6.2 做角色判断

其他模块可直接注入：

```java
private final AccessControlService accessControlService;
```

常用方法：

- `hasRole(SystemRole role)`
- `hasAnyRole(SystemRole... roles)`
- `checkAnyRole(SystemRole... roles)`

适用场景：

- 仅管理员可访问管理接口
- 仅审批人可处理审批接口
- 支持人员与管理员可访问某类运维接口

## 6.3 做资源归属校验

可直接复用：

- `canAccessOwnedResource(ownerUserId, elevatedRoles...)`
- `checkOwnedResourceAccess(ownerUserId, elevatedRoles...)`

适用场景：

- 普通员工只能查看自己的工单
- 管理员或支持人员可以查看他人工单

示例：

```java
accessControlService.checkOwnedResourceAccess(
    ticket.getRequesterId(),
    SystemRole.SUPPORT_AGENT,
    SystemRole.ADMIN
);
```

## 6.4 做部门级资源校验

可直接复用：

- `canAccessDepartmentResource(department, elevatedRoles...)`
- `checkDepartmentResourceAccess(department, elevatedRoles...)`

适用场景：

- 文档模块按部门做访问边界
- 同部门用户可见，管理员可越权查看

## 6.5 做方法级权限控制

由于已经启用 `@EnableMethodSecurity`，其他模块可以直接写：

```java
@PreAuthorize("@roleChecker.hasAnyRole(authentication, 'ADMIN', 'APPROVER')")
```

或：

```java
@PreAuthorize("@roleChecker.hasRole(authentication, 'ADMIN')")
```

适用场景：

- Controller 方法级保护
- Service 方法级保护

## 6.6 认证相关接口契约

其他 thread 如需依赖登录能力，可参考：

- `AuthService`
- `UserContext`
- `AccessControlService`
- `RoleChecker`
- `JwtTokenProvider`

建议其他模块依赖 `UserContext` 与 `AccessControlService`，尽量不要直接耦合 JWT 解析细节。

## 7. 已实现的边界与约束

本模块已经满足以下边界要求：

- 不直接耦合 ticket 领域逻辑
- 不实现审批工作流引擎
- 不处理知识检索
- 不在认证层写死工单状态机
- 只提供通用的身份识别与权限判断能力

## 8. 后续可扩展点

- 扩展更多角色，不影响现有认证主链路
- 为用户增加更多组织属性，例如岗位、直属主管、租户、访问级别
- 在 JWT claims 中补充更多只读身份信息
- 后续可增加刷新 token 机制
- 后续可增加更细粒度的 permission 体系，而不仅是 role

## 9. 推荐协作方式

对于其他 thread，推荐遵循以下原则：

1. 通过 `UserContext` 获取当前用户
2. 通过 `AccessControlService` 做资源级访问判断
3. 通过 `@PreAuthorize` + `RoleChecker` 做声明式角色限制
4. 不要在业务模块中自行重复解析 JWT
5. 不要把 ticket、approval、document 的业务规则写回 auth 模块

这样可以保持认证层稳定，同时避免 thread 之间的职责交叉。
