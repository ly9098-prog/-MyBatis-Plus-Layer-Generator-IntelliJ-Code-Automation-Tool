# MyBatis-Plus 代码生成工具

基于 IntelliJ IDEA 数据库工具的 Groovy 脚本，用于自动生成 MyBatis-Plus 框架的分层代码结构。

---

## 核心功能

1. **分层代码生成**
   - Domain 实体类（含 Lombok 注解）
   - Mapper 接口（继承 BaseMapper）
   - Service 接口（继承 IService）
   - ServiceImpl 实现类（继承 ServiceImpl）
   - Controller 控制器（含 REST 注解）

2. **智能类型映射**
   ```groovy
   // 示例类型映射规则
   typeMapping = [
       (~/(?i)bigint/)      : "Long",
       (~/(?i)datetime/)    : "LocalDateTime",
       (~/(?i)varchar/)     : "String"
   ]
