MyBatis-Plus 代码生成器 (IntelliJ IDEA Plugin)
https://img.shields.io/badge/Java-17%252B-blue
https://img.shields.io/badge/MyBatis--Plus-3.5.1-brightgreen
https://img.shields.io/badge/IntelliJ%2520IDEA-Plugin-yellow

概述
这是一个用于 IntelliJ IDEA 的 Groovy 脚本，能够根据数据库表结构自动生成基于 MyBatis-Plus 的 Spring Boot 项目代码。通过简单的操作，您可以在几秒钟内生成完整的 CRUD 代码，包括实体类、Mapper 接口、Service 接口、ServiceImpl 实现类以及 Controller 控制器。

功能特性
🚀 一键生成多层代码
Domain 层：自动生成带有 Lombok 注解的实体类

Mapper 层：自动生成基于 MyBatis-Plus 的 Mapper 接口

Service 层：

接口层：生成以 I 为前缀的 Service 接口

实现层：生成 Service 实现类

Controller 层：生成基础的 RESTful 控制器

🔍 智能类型映射
支持多种数据库类型到 Java 类型的自动转换：

数据库类型	Java 类型
bigint, int8	Long
int, integer, int4	Integer
float, double, decimal	Double
datetime, timestamp	java.util.Date
date	java.util.Date
time	java.sql.Time
tinyint(1), bit	Boolean
tinyint, smallint	Integer
text, varchar, char	String
其他类型	String (默认)
⚙️ 高级功能
主键自动识别：自动识别数据库主键并添加 @TableId 注解

字段映射：自动添加 @TableField 注解映射数据库字段

Lombok 集成：实体类自动添加 @Data、@NoArgsConstructor、@AllArgsConstructor 注解

包结构配置：可自定义各层级的包名

命名规范：自动转换数据库表名和字段名为驼峰命名法

UTF-8 支持：确保生成文件使用 UTF-8 编码

使用指南
安装与配置
配置基础包名：在脚本开头修改 packageName 变量

groovy
packageName = "com.yourcompany.yourproject"
配置层级包结构：根据需要修改各层级的相对路径

groovy
layerPackages = [
        domain  : "domain",
        mapper  : "mapper",
        service : "service",
        impl    : "service/impl",
        controller: "controller"
]
使用步骤
在 IntelliJ IDEA 中打开数据库视图

选择要生成代码的表（支持多选）

右键点击选中的表

选择 "Scripted Extensions" → "Generate POJOs.groovy"

选择项目中的 src/main/java 目录作为基础目录

脚本会自动生成完整的代码结构

生成的文件结构
text
src/main/java/
├── com/yourcompany/yourproject/
│   ├── controller/
│   │   └── YourEntityController.java
│   ├── domain/
│   │   └── YourEntity.java
│   ├── mapper/
│   │   └── YourEntityMapper.java
│   ├── service/
│   │   └── IYourEntityService.java
│   └── service/impl/
│       └── YourEntityServiceImpl.java
代码示例
生成的实体类 (Domain)
java
package com.example.demo.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "employees")
public class Employees implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(value = "first_name")
    private String firstName;
    
    @TableField(value = "last_name")
    private String lastName;
    
    @TableField(value = "hire_date")
    private Date hireDate;
    
    // 其他字段...
}
生成的 Mapper 接口
java
package com.example.demo.mapper;

import com.example.demo.domain.Employees;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeesMapper extends BaseMapper<Employees> {
}
生成的 Service 接口
java
package com.example.demo.service;

import com.example.demo.domain.Employees;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IEmployeesService extends IService<Employees> {
}
生成的 Controller
java
package com.example.demo.controller;

import com.example.demo.service.IEmployeesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
public class EmployeesController {

    @Autowired
    private IEmployeesService employeesService;

    // 在这里添加控制器方法
}
自定义选项
修改命名风格
在 javaName 方法中修改命名转换逻辑：

groovy
def javaName(str, capitalize) {
    def s = str.split(/[^\p{Alnum}]/).collect { it.capitalize() }.join("")
    capitalize ? s : s.uncapitalize()
}
添加自定义类型映射
在 typeMapping 中添加或修改类型映射：

groovy
typeMapping = [
        (~/(?i)bigint|int8/)         : "Long",
        // 添加自定义映射...
        (~/(?i)jsonb/)               : "Object", // 自定义JSONB类型
        // 其他映射...
]
修改主键生成策略
在实体类生成方法中修改 @TableId 注解：

groovy
if (field.isPrimaryKey) {
    // 修改主键生成策略
    out.println "    @TableId(type = IdType.ASSIGN_ID)"
}
常见问题解决
主键未正确识别
确保数据库表设置了主键，脚本会自动识别主键字段并添加 @TableId 注解

字段类型映射不正确
检查 typeMapping 中的正则表达式是否匹配数据库类型

添加自定义类型映射到 typeMapping 变量

包结构不正确
修改 layerPackages 配置以匹配项目结构：

groovy
layerPackages = [
        domain  : "entity",          // 修改为 entity 包
        mapper  : "dao",             // 修改为 dao 包
        // 其他层级...
]
生成的文件位置错误
确保在最后一步选择了正确的目标目录（通常是项目的 src/main/java 目录）

中文字符编码问题
脚本已内置 UTF-8 编码支持，如果仍有问题请检查：

IntelliJ IDEA 的文件编码设置

项目本身的编码设置
