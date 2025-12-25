# 基础架构

jdk21

springboot 3.1.5

mybatis plus

vue3 + elementui plus openlayers

mysql（业务数据处理） 

postgres(标注数据存储)

redis

api地址：http://localhost:7878/jnet/swagger-ui/index.html#/

API文档：http://localhost:9200/v3/api-docs

Swagger UI：http://localhost:9200/swagger-ui.html

# 基础服务

## 标注服务

1、jts封装地理坐标信息json序列化与反序列化方法，封装mybatis的地理信息type-handlers。

2、请求数据压缩、分批加载

## WSI图像解析服务
### 图像下载 [https://openslide.cs.cmu.edu/download/openslide-testdata/Aperio/]

### 图像分片上传设计
大文件分片上传是一种常见的前端和后端交互方式，用于高效、稳定地处理大文件上传。以下是该流程的详细说明：

---

#### ✅ 一、整体流程图

```
前端（用户）                     后端（服务）
   |                                 |
   |-------- 初始化上传任务 --------->|
   |<------- 返回 uploadId + 其他信息--|
   |                                 |
   |-------- 分片上传 (多次) -------->|
   |<------- 每个分片返回成功/失败 ----|
   |                                 |
   |-------- 所有分片上传完成后 ------>|
   |-------- 发送合并请求 ------------>|
   |<------- 返回最终结果 -------------|
   |                                 |
```


---

#### ✅ 二、具体步骤详解

##### **1. 初始化上传任务**
- **目的**：通知服务端准备接收一个大文件上传任务，并获取一个唯一标识 `uploadId`。
- **请求方式**：GET
- **接口**：`/attachment/initiateMultipartUpload`
- **响应内容**：
    - `uploadId`：唯一的上传任务 ID
    - 其他配置信息（如分片大小、存储路径等）

###### 示例请求：
```http
GET /attachment/initiateMultipartUpload HTTP/1.1
```


###### 示例响应：
```json
{
  "code": 200,
  "data": {
    "uploadId": "unique_upload_id_123",
    "chunkSize": 10485760 // 10MB
  }
}
```


---

##### **2. 分片上传**
- **目的**：将大文件分割为多个小块，逐个上传到服务器。
- **请求方式**：POST
- **接口**：`/attachment/uploadChunk`
- **参数说明**：
    - `name`: 文件名
    - `uploadId`: 上一步返回的上传任务 ID
    - `chunkMd5`: 当前分片的 MD5 校验值
    - `chunkSize`: 分片大小
    - `chunkTotal`: 总分片数
    - `chunkIndex`: 当前分片索引（从 0 开始）
    - `chunk`: 当前分片数据（MultipartFile）

###### 示例请求：
```http
POST /attachment/uploadChunk HTTP/1.1
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="name"

example.txt
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="uploadId"

unique_upload_id_123
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="chunkMd5"

abc123def456ghi789jkl012mno345pqr678stu901vwx
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="chunkSize"

10485760
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="chunkTotal"

5
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="chunkIndex"

0
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="chunk"; filename="blob"
Content-Type: application/octet-stream

<分片数据>
------WebKitFormBoundary7MA4YWxkTrZu0gW--
```


###### 示例响应：
```json
{
  "code": 200,
  "message": "分片上传成功"
}
```


---

##### **3. 分片合并**
- **目的**：所有分片上传完成后，通知服务端进行分片合并。
- **请求方式**：POST
- **接口**：`/attachment/completeMultipartUpload`
- **参数说明**：
    - `name`: 文件名
    - `uploadId`: 上一步返回的上传任务 ID
    - `chunkTotal`: 总分片数
    - `chunks`: 所有分片的信息（如 MD5、索引等）

###### 示例请求：
```http
POST /attachment/completeMultipartUpload HTTP/1.1
Content-Type: application/json

{
  "name": "example.txt",
  "uploadId": "unique_upload_id_123",
  "chunkTotal": 5,
  "chunks": [
    {"chunkIndex": 0, "chunkMd5": "abc123def456ghi789jkl012mno345pqr678stu901vwx"},
    {"chunkIndex": 1, "chunkMd5": "xyz789ijk456def123stu901vwx678mno345pqr012ghijkl"},
    ...
  ]
}
```


###### 示例响应：
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "fileUrl": "/uploads/example.txt"
  }
}
```


---

#### ✅ 三、关键功能与注意事项

##### **1. 前端处理逻辑**
- **文件切片**：使用 `File.slice()` 方法将文件切割成固定大小的分片。
- **计算 MD5**：每个分片生成一个 MD5 校验值，用于校验完整性。
- **并发控制**：可以使用 Promise Pool 或异步队列控制并发上传数量，提高效率。
- **进度监控**：通过回调函数实时更新上传进度。

##### **2. 后端处理逻辑**
- **分片存储**：每个分片单独保存，通常以 [uploadId](file://D:\work\dict\jnet\front\jnet-slide\src\components\fileChunkUploader\index.vue#L11-L11) 和 [chunkIndex](file://D:\work\dict\jnet\front\jnet-slide\src\components\fileChunkUploader\index.vue#L6-L6) 命名。
- **MD5 校验**：在分片上传时，验证分片的 MD5 是否一致，确保数据完整性。
- **断点续传**：支持重新上传未完成的分片。
- **分片合并**：将所有分片按顺序合并为完整文件。
---

如需我为你生成完整的分片上传接口、断点续传逻辑、或并发上传工具类，请随时告诉我！我可以为你提供完整代码。

## 鉴权网关服务

1、spring security

2、自定义路由

## 认证服务
1、提供获取当前用户信息接口

在 Spring Security 中，`GrantedAuthority` 是一个接口，用于表示 **认证主体（如用户）所拥有的权限**。它既可以用来表示角色（Role），也可以用来表示资源权限（Authority），具体含义取决于你如何使用它。

---

### ✅ 一、基本定义

```java
public interface GrantedAuthority extends Serializable {
    String getAuthority();
}
```


- [getAuthority()](file://D:\work\dict\jnet\jnet-common\jnet-common-security\src\main\java\com\jnet\common\core\security\bean\GrantedAuthorityCustom.java#L22-L25) 返回的是一个字符串，通常用于表示权限或角色。
- 它是 `Authentication` 对象的一部分，表示当前用户拥有哪些权限。

---

### ✅ 二、`GrantedAuthority` 的常见用途

#### 🎯 1. **作为角色（Role）标识**
这是最常见的用法之一。

##### 示例：
```java
GrantedAuthority roleUser = new SimpleGrantedAuthority("ROLE_USER");
GrantedAuthority roleAdmin = new SimpleGrantedAuthority("ROLE_ADMIN");
```


##### 特点：
- 角色名通常以 `ROLE_` 开头（Spring Security 约定）
- 可用于方法级别的权限控制：

```java
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() {
    // 只有 ADMIN 角色可以访问
}
```


---

#### 🎯 2. **作为资源权限（Authority）标识**

你也可以将 `GrantedAuthority` 用作更细粒度的权限控制，比如操作权限、数据权限等。

##### 示例：
```java
GrantedAuthority readBook = new SimpleGrantedAuthority("BOOK_READ");
GrantedAuthority writeBook = new SimpleGrantedAuthority("BOOK_WRITE");
```


##### 特点：
- 不需要以 `ROLE_` 开头
- 更适合做基于权限字符串的细粒度控制

```java
@PreAuthorize("hasAuthority('BOOK_WRITE')")
public void editBook() {
    // 只有具有 BOOK_WRITE 权限的用户可以编辑书籍
}
```


---

### ✅ 三、`SimpleGrantedAuthority` 示例

```java
List<GrantedAuthority> authorities = new ArrayList<>();
authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
authorities.add(new SimpleGrantedAuthority("BOOK_READ"));
authorities.add(new SimpleGrantedAuthority("PERMISSION_DELETE_CONTENT"));
```


---

### ✅ 四、总结对比表

| 用法 | 示例字符串 | 是否推荐前缀 | 适用场景 |
|------|------------|---------------|-----------|
| 🧑‍💼 角色（Role） | `"ROLE_ADMIN"` | ✅ 推荐 | 用户属于某个角色 |
| 🔐 权限（Authority） | `"USER_CREATE"`、`"BOOK_DELETE"` | ❌ 不需要 | 控制特定操作权限 |
| 📁 资源权限 | `"RESOURCE:1001:READ"` | ⚠️ 自定义格式 | 细粒度资源控制（如某篇文章可读） |

---

### ✅ 五、实际开发建议

| 场景 | 推荐方式 |
|------|----------|
| 简单 RBAC 模型 | 使用 `ROLE_XXX` 表示角色 |
| 基于权限的控制 | 使用普通字符串如 `USER_CREATE` |
| 多租户/资源级控制 | 使用结构化权限字符串，如 `RESOURCE_TYPE:ID:ACTION` |

#### 示例：多租户资源权限
```java
new SimpleGrantedAuthority("DOCUMENT:1001:READ");  // 文档 ID=1001 的读权限
new SimpleGrantedAuthority("DOCUMENT:1001:WRITE"); // 写权限
```


---

### ✅ 六、和 `UserDetails` 的关系

`GrantedAuthority` 通常被封装在 `UserDetails` 实现类中：

```java
public class UserDetailsImpl implements UserDetails {
    private final List<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // 其他方法省略...
}
```


---

### ✅ 七、扩展建议

如果你希望支持更复杂的权限模型，可以自定义 `GrantedAuthority` 实现类：

```java
public class CustomAuthority implements GrantedAuthority {
    private final String type;
    private final String resource;
    private final String action;

    public CustomAuthority(String type, String resource, String action) {
        this.type = type;
        this.resource = resource;
        this.action = action;
    }

    @Override
    public String getAuthority() {
        return String.format("%s:%s:%s", type, resource, action);
    }
}
```


使用示例：

```java
new CustomAuthority("resource", "book", "read"); // => resource:book:read
```


---

### ✅ 八、结论

| 问题 | 回答 |
|------|------|
| `GrantedAuthority` 是角色还是资源？ | **都可以**，取决于你怎么用 |
| 推荐角色写法？ | `"ROLE_ADMIN"` |
| 推荐权限写法？ | `"BOOK_READ"` 或 `"resource:book:read"` |
| 如何区分角色和权限？ | Spring Security 本身不区分，由业务逻辑决定 |

---

## 管理信息系统服务

1、组织机构

2、人员、角色、权限、菜单 基础RBAC管理数据管理

3、字典



4、日志、监控、服务追踪



# 前端

todo 前端文件命名格式及代码规范待完善