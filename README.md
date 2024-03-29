# 基础架构

jdk17 

springboot 3.1.5

mybatis plus

vue3 + elementui plus openlayers

mysql（业务数据处理） 

postgres(标注数据存储)

redis

api地址：http://localhost:7878/jnet/swagger-ui/index.html#/

# 基础服务

## 文件服务

1、大文件分块上传，使用AtomicIntegerArray记录分块上传状态，避免多线程异常。

2、openslide切片处理，wsi分层图片分割处理。

## 标注服务

1、jts封装地理坐标信息json序列化与反序列化方法，封装mybatis的地理信息type-handlers。

2、请求数据压缩、分批加载

## 鉴权网关服务

1、spring security

2、自定义路由

## 管理信息系统服务

1、组织机构

2、人员、角色、权限、菜单

3、字典



4、日志、监控、服务追踪