# API 服务配置说明

## 概述

本项目采用多后端服务架构，不同业务模块对应不同的后端服务。为了统一管理API路径前缀，我们引入了集中化的服务配置方案。

## 架构说明

### 后端服务列表

| 服务名称 | 服务标识 | 端口 | 路径前缀 | 说明 |
|---------|---------|------|---------|------|
| jnet-biz | `SERVICES.BIZ` | 9203 | `/biz` | 业务服务（图像、项目、批次、标签管理等） |
| jnet-anno | `SERVICES.ANNO` | 9005 | `/anno` | 标注服务（标注管理、空间测量等） |

### Axios 配置

**重要：** `src/utils/request.ts` 中的 axios 实例**不设置 baseURL**，因为：

1. 项目采用多服务架构，不同 API 需要访问不同的后端服务
2. 每个 API 模块自行管理完整路径（如 `/biz/api/v1/images`、`/anno/api/v1/annotation`）
3. 通过 Vite 代理将请求转发到对应的后端服务

```typescript
// src/utils/request.ts
const service: AxiosInstance = axios.create({
  baseURL: '', // 不设置基础URL，由各API模块自行指定完整路径
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})
```

### Vite 代理配置

开发环境下，通过 Vite 代理将请求转发到对应的后端服务：

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/biz': {
      target: 'http://localhost:9203',
      changeOrigin: true
    },
    '/anno': {
      target: 'http://localhost:9005',
      changeOrigin: true
    }
  }
}
```

## 使用方式

### 1. 服务配置文件

所有服务配置集中在 `src/config/services.ts` 文件中：

```typescript
export const SERVICES = {
  BIZ: '/biz',      // 业务服务
  ANNO: '/anno',    // 标注服务
} as const

export function createApiPath(service: ServiceKey, path: string): string {
  return `${SERVICES[service]}${path}`
}
```

### 2. API 文件中使用

在每个 API 模块文件中，导入并使用服务配置：

```typescript
import request from '@/utils/request'
import { createApiPath, SERVICES } from '@/config/services'

// 定义 BASE_URL
const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/images')

// 使用示例
export function getImageById(id: number) {
  return request.get(`${BASE_URL}/${id}`)
}
```

### 3. 完整示例

**images.ts（业务服务）**
```typescript
import { createApiPath, SERVICES } from '@/config/services'

const BASE_URL = createApiPath(SERVICES.BIZ, '/api/v1/images')

export function getThumbnailUrl(id: number, maxSize: number = 512): string {
  const queryString = new URLSearchParams({
    maxSize: String(maxSize)
  }).toString()
  return `${BASE_URL}/${id}/thumbnail?${queryString}`
}
```

**annotations.ts（标注服务）**
```typescript
import { createApiPath, SERVICES } from '@/config/services'

export function addAnnotation(data: AnnotationDTO) {
  return request.post<string>(
    createApiPath(SERVICES.ANNO, '/api/v1/annotation'), 
    data
  )
}
```

## 添加新服务

如果需要添加新的后端服务，按以下步骤操作：

### 1. 更新服务配置

在 `src/config/services.ts` 中添加新服务：

```typescript
export const SERVICES = {
  BIZ: '/biz',
  ANNO: '/anno',
  AUTH: '/auth',         // 新增认证服务
  ANALYSIS: '/analysis', // 新增AI分析服务
} as const
```

### 2. 配置 Vite 代理

在 `vite.config.ts` 中添加代理规则：

```typescript
proxy: {
  '/biz': { target: 'http://localhost:9203', changeOrigin: true },
  '/anno': { target: 'http://localhost:9005', changeOrigin: true },
  '/auth': { target: 'http://localhost:9100', changeOrigin: true },      // 新增
  '/analysis': { target: 'http://localhost:9300', changeOrigin: true },  // 新增
}
```

### 3. 在 API 文件中使用

```typescript
import { createApiPath, SERVICES } from '@/config/services'

const BASE_URL = createApiPath(SERVICES.AUTH, '/api/v1/auth')
```

## 优势

1. **集中管理**：所有服务路径前缀在一个文件中维护，便于修改和扩展
2. **类型安全**：使用 TypeScript 类型约束，避免拼写错误
3. **易于维护**：添加新服务只需修改配置文件，无需改动每个 API 文件
4. **环境无关**：开发和生产环境使用相同的路径格式，通过代理或 Nginx 配置处理差异
5. **代码清晰**：明确标识每个 API 所属的服务，提高代码可读性

## 注意事项

1. **不要硬编码服务前缀**：始终使用 `createApiPath()` 函数生成 API 路径
2. **保持一致性**：同一服务的所有 API 应使用相同的 `SERVICES` 常量
3. **URL 生成函数**：对于直接返回 URL 的函数（如 `getThumbnailUrl`），也应使用 `BASE_URL` 确保路径正确
4. **生产部署**：生产环境需要配置 Nginx 或其他反向代理，将 `/biz`、`/anno` 等路径转发到对应的后端服务

## 故障排查

### 问题：API 请求返回 404

**可能原因：**
- 服务前缀配置错误
- Vite 代理未正确配置
- 后端服务未启动

**解决方法：**
1. 检查浏览器控制台，确认请求 URL 是否正确包含服务前缀（如 `/biz/api/v1/images`）
2. 检查 `vite.config.ts` 中的代理配置
3. 确认对应的后端服务正在运行

### 问题：缩略图加载失败，返回 304

**可能原因：**
- 缺少服务前缀导致请求未被正确代理
- 浏览器缓存了错误的响应

**解决方法：**
1. 清除浏览器缓存（Ctrl+Shift+Delete）
2. 确认 `getThumbnailUrl()` 返回的 URL 包含正确的服务前缀
3. 检查网络面板，确认请求被转发到正确的后端服务

## 相关文件

- `src/config/services.ts` - 服务配置文件
- `src/api/*.ts` - API 接口文件
- `vite.config.ts` - Vite 代理配置
- `.env.development` - 开发环境变量
