我看到你已经成功反编译了 `NNBaseLib.dll`，并获得了源代码。这对我们理解数据库的加密机制和表结构非常有帮助。

让我读取反编译后的关键文件，特别是加密逻辑和数据库连接相关的代码，来尝试破解 `NNDBPF.ENC`。
        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\NNClass\EncryptDecrypt.cs
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\NNClass\PasswordEntry.cs
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\NNClass\NNBase.cs
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\Patient\PatientRec.cs
          
太好了！我找到了关键的加密密钥和数据库连接逻辑。

## 关键发现

从 [NNBase.cs](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/NNBase.cs#L266) 中找到了：

### 1. DES 密钥
```csharp
private string sSecretKey = "??S2\"?b!";
```

### 2. 加密算法（[EncryptDecrypt.cs](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/EncryptDecrypt.cs)）
- 使用 `DESCryptoServiceProvider`
- Key 和 IV 相同，均为 `sSecretKey` 的 ASCII 字节

### 3. 密码文件结构
```xml
<root>
  <Setting region="..." dbversion="..." dbname="..." user="..." password="..." />
</root>
```

### 4. 默认数据库连接信息（[NNBase.cs#L48-L100](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/NNBase.cs#L48-L100)）

现在让我用这个密钥解密 `NNDBPF.ENC` 文件：
        
            
toolName: Write
            
status: success
          
            
filePath: d:\work\nova\decrypt_nndbpf.py
          
changes: undefined
          

        
            
toolName: Shell
            
status: success
          
          

        
            
toolName: Shell
            
status: success
          
          

        
            
toolName: Shell
            
status: success
          
          

        
            
toolName: edit_file_search_replace
            
status: success
          
            
filePath: d:\work\nova\decrypt_nndbpf.py
          
changes: undefined
          

        
            
toolName: Shell
            
status: success
          
          
🎉 **成功解密！**
        
            
toolName: todo_write
            
status: success
          
Todos updated: 8 items
          
让我继续读取更多反编译的实体类，获取精确的表结构定义：
        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\Patient\PatientVisitRec.cs
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\InstLocations\InstLocationRec.cs
          

        
            
toolName: view_files
            
status: success
          
            
filePath: d:\work\nova\NovaNet v3.6.12.15 SP12\decompile\LocLastUpdate\LocLastUpdateRec.cs
          
# 🎉 成功破解 NovaNet 数据库！

## 一、加密机制解析

| 项目 | 详情 |
|------|------|
| **加密算法** | DES (CBC 模式) |
| **密钥** | `??S2"?b!`（8 字节） |
| **Key = IV** | 相同（见 [EncryptDecrypt.cs#L19-L20](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/EncryptDecrypt.cs#L19-L20)） |
| **密码文件** | `NNDBPF.ENC` → 解密后为 XML 格式 |

密钥来源：[NNBase.cs#L266](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/NNBase.cs#L266)
```csharp
private string sSecretKey = "??S2\"?b!";
```

## 二、默认数据库凭据（NovaNet v3.6.12.14）

### 核心账号

| 用户 | 密码 | 角色 | 权限 |
|------|------|------|------|
| **`dba`** | **`ab3dq@RND`** | 数据库管理员 | 所有库的 DBA 权限 |
| **`rtmbackend`** | **`ab3dq@B3S`** | 应用后端服务 | 所有库的后端只读/读写 |
| `anywhere` | `8970nbus!` | SQL Anywhere 系统 | 系统级访问 |
| `TechServ` (NovaNet 区域) | `Tech200N0v@` | 技术支持 | 特定区域 DBA |
| `TechServ` (NovaNetJJ 区域) | `JJnn20!15` | 技术支持 | 特定区域 DBA |

### 数据库清单（5 个）

| 数据库名 | ODBC 数据源名 | 说明 |
|----------|--------------|------|
| **Runtime** | `rtmbackend` | 主运行时数据库（患者、就诊、样本、结果等） |
| **History** | `history` | 历史归档数据库 |
| **Strings** | `anywhere_strings` | 字符串/本地化数据库 |
| **ProfileTrack** | `profile_track` | 质控追踪数据库 |
| **Metrics** | `metrics` | 指标监控数据库 |

### 连接字符串示例
```
DSN=rtmbackend;UID=dba;PWD=ab3dq@RND
```

### Sybase OEM 强制签名（必须携带）
```sql
SET TEMPORARY OPTION CONNECTION_AUTHENTICATION=
'Company=Nova Biomedical;
 Application=NovaNet;
 Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158'
```

## 三、数据库设计（基于反编译实体类）

### 3.1 核心表结构

#### `DBA.patients`（患者主表）
字段（来源：[PatientRec.cs](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/Patient/PatientRec.cs)）：
| 字段 | 类型 | 说明 |
|------|------|------|
| `patient_id` | VARCHAR | 患者 ID (外部ID) |
| `medrec_num` | VARCHAR | 病历号 MRN (内部ID) |
| `last_name` | VARCHAR | 姓 |
| `first_name` | VARCHAR | 名 |
| `middle_name` | VARCHAR | 中间名 |
| `prefix` | VARCHAR | 前缀 |
| `suffix` | VARCHAR | 后缀 |
| `sex` | VARCHAR(1) | 性别 (M/F/U) |
| `birthdate` | DATETIME | 出生日期 |
| `race` | VARCHAR | 种族 |
| `facil_num` | VARCHAR | 机构编号 |
| `add_date` | DATETIME | 录入日期 |
| `arch` | VARCHAR(1) | 归档标志 (T/F) |

#### `DBA.patient_visits`（就诊记录表）
字段（来源：[PatientVisitRec.cs#L637-L688](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/Patient/PatientVisitRec.cs#L637-L688)）：
| 字段 | 类型 | 说明 |
|------|------|------|
| `visit_uuid` | VARCHAR | 就诊 UUID (主键) |
| `patient_uuid` | VARCHAR | 患者 UUID (外键) |
| `account_uuid` | VARCHAR | 账户 UUID |
| `visit_num` | VARCHAR | 就诊号 |
| `admit_time` | DATETIME | 入院时间 |
| `discharge_time` | DATETIME | 出院时间 |
| `patient_class` | VARCHAR | 患者类别 |
| `patient_type` | VARCHAR | 患者类型 |
| `loc_num` | VARCHAR | 科室编号 |
| `room_num` | VARCHAR | 房间号 |
| `bed_num` | VARCHAR | 床号 |
| `Attend_Physician` | VARCHAR | 主治医师 |
| `Report_Physician` | VARCHAR | 报告医师 |
| `Consult_Physician` | VARCHAR | 会诊医师 |
| `diagnosis` | VARCHAR | 诊断 |
| `weight` | VARCHAR | 体重 |
| `weight_units` | VARCHAR | 体重单位 |
| `height` | VARCHAR | 身高 |
| `height_units` | VARCHAR | 身高单位 |
| `notes` | VARCHAR | 医师备注 |
| `add_date` | DATETIME | 创建日期 |
| `last_update_date` | DATETIME | 最后更新时间 |
| `deprecated_for_account` | VARCHAR(1) | 废弃标志(账户) |
| `deprecated_for_medrec_num` | VARCHAR(1) | 废弃标志(MRN) |
| `deprecated_for_patient_id` | VARCHAR(1) | 废弃标志(患者ID) |
| `arch` | VARCHAR(1) | 归档标志 |
| `ui_created_record` | VARCHAR(1) | UI 创建记录标志 |
| `d_list_flag` | VARCHAR(1) | D 列表标志 |

#### `DBA.inst_locations`（位置/科室表）
字段（来源：[InstLocationRec.cs](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/InstLocations/InstLocationRec.cs)）：
| 字段 | 类型 | 说明 |
|------|------|------|
| `loc_num` | VARCHAR | 位置编号 (主键) |
| `parent` | VARCHAR | 父级位置编号 |
| `level_num` | SMALLINT | 层级 (1=设施, 2=科室) |
| `loc_name` | VARCHAR | 位置名称 |
| `last_pat_update` | DATETIME | 最后患者更新时间 |
| `last_op_update` | DATETIME | 最后操作员更新时间 |
| `date_timestamp` | DATETIME | 时间戳 |
| `locked_by` | VARCHAR | 锁定者 |
| `is_default` | VARCHAR | 是否默认 |

#### `DBA.loc_last_update`（位置数据同步时间戳）
字段（来源：[LocLastUpdateRec.cs](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/LocLastUpdate/LocLastUpdateRec.cs)）：
| 字段 | 类型 | 说明 |
|------|------|------|
| `loc_num` | VARCHAR | 位置编号 |
| `data_type` | VARCHAR | 数据类型 |
| `inst_type` | VARCHAR | 仪器类型 |
| `inst_class` | VARCHAR | 仪器类别 |
| `last_update_time` | DATETIME | 最后更新时间 |

### 3.2 关键视图

| 视图名 | 用途 |
|--------|------|
| `DBA.patients_view` | 患者综合信息视图（含姓名、性别、地址、医师等） |
| `DBA.visit_by_account_num` | 按账户号查询就诊 |
| `DBA.visit_by_medrec_num` | 按 MRN 查询就诊 |
| `DBA.visit_by_patient_id` | 按患者 ID 查询就诊 |
| `DBA.latest_active_visit_by_account_num` | 最新活跃就诊（按账户） |

### 3.3 系统表

| 表名 | 用途 |
|------|------|
| `DBA.version_info` | 版本信息 |
| `DBA.authorized_services` | 授权服务（含 `runtime_error`, `error_descr`, `enabled` 字段） |
| `DBA.health_ping` | 进程健康心跳 |
| `DBA.pop_info` | POP 站点信息（含 `pop_url`, `ssl_state`） |
| `DBA.ErrorMessages` | 错误日志 |
| `DBA.arch_export_backup` | 归档/备份任务 |
| `DBA.process_control` | 进程控制参数 |

## 四、权限模型

从 [NNBase.cs#L48-L102](file:///d:/work/nova/NovaNet%20v3.6.12.15%20SP12/decompile/NNClass/NNBase.cs#L48-L102) 中可见，每个数据库有独立的 P/U 权限：

| 数据库 | U 账号 (用户) | P 账号 (进程) | DBA U 账号 | DBA P 账号 |
|--------|--------------|--------------|-----------|-----------|
| Runtime | `rtmbackend` | 从 NNDBPF.ENC 读取 | `dba` | 从 NNDBPF.ENC 读取 |
| History | `rtmbackend` | 从 NNDBPF.ENC 读取 | `dba` | 从 NNDBPF.ENC 读取 |
| Strings | `rtmbackend` | 从 NNDBPF.ENC 读取 | — | — |
| ProfileTrack | `rtmbackend` | 从 NNDBPF.ENC 读取 | `dba` | 从 NNDBPF.ENC 读取 |
| Metrics | — | — | `dba` | — |

## 五、使用 Python 解密脚本

我已创建了可复用的解密脚本 [decrypt_nndbpf.py](file:///d:/work/nova/decrypt_nndbpf.py)，可随时解密 NNDBPF.ENC：

```python
from Cryptodome.Cipher import DES
import xml.etree.ElementTree as ET

SECRET_KEY = b'??S2"?b!'  # 从 NNBase.cs 提取的密钥

cipher = DES.new(SECRET_KEY, DES.MODE_CBC, iv=SECRET_KEY)
decrypted = cipher.decrypt(encrypted_data)
# 解析 XML: <root><Setting region="..." dbversion="..." dbname="..." user="..." password="..." /></root>
```

## 六、连接 Sybase SQL Anywhere 的完整步骤

1.  **安装 Sybase SQL Anywhere 12 客户端**（从安装包的 `Sybase12OEM` 目录获取）
2.  **配置 ODBC DSN**：
    ```
    DSN=rtmbackend;ServerName=<主机名>;UID=dba;PWD=ab3dq@RND
    ```
3.  **执行 OEM 认证**：
    ```sql
    SET TEMPORARY OPTION CONNECTION_AUTHENTICATION='Company=Nova Biomedical;Application=NovaNet;Signature=000fa55157edb8e14d818eb4fe3db41447146f1571g5419cd50cabf06a8be6dd4bb58e82d850a1bb158'
    ```
4.  **连接 Runtime 数据库**并查询 `DBA.patients`, `DBA.patient_visits` 等表