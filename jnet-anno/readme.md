# 记一次轮廓数据入库问题排查过程
## jvm监控
1. jvm监控工具（jconsole、jvisualvm、jprofiler、jmc、jstack、jmap、jhat、jmc），使用jprofiler监控内存及cup等资源变化。
## 中间件
1. 配置文件（durid数据库连接池配置、rabbitmq配置、jdbc配置）
2. postgresql基础数据膨胀（单表多列大字段、单表一列大字段、分区表单多列大字段）
3. postgresql管理工具（pgAdmin4）
4. rabbitmq管理界面，查看消息队列（rabbitmq_management插件），查看ready、unacked、total消息数
5. pgadmin4管理界面，查看数据库大小、表大小、索引大小、表数据量、表索引量、toast块大小、toast块数据量、toast块索引量
6. pgadmin4管理界面，Dashboard，查看Activity中Tuples in（inserted、updated、deleted）、Tuples out（fetched、returned）
7. pgadmin4管理界面，Dashboard，查看State中Sessions状态，pid、client、Application、user、state、Blocking PIDs（等待持有锁的pid），
Locks状态，target relation（包含表、索引、toast块）、mode（锁类型）、Granted（是否已获得锁）
## 排查结果
1. 插入基础数据时，由于数据量不断增长，单次批量插入时会产生io异常，需控制批量插入数据大小（坐标存储双精度浮点数值，占用4字节）。
2. 业务中大量查询或删除操作，数据库处理时间过长，数据库会将session断开（could not receive data from client: Connection reset by peer）
jdbc client无法感知数据库连接已断开，一直处于等待状态，导致连接池资源耗尽并业务无法正常执行。