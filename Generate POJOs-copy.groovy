import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

packageName = "com.example.demo"  // 基础包名
baseDir = ""  // 基础目录路径

typeMapping = [
        (~/(?i)bigint|int8/)         : "Long",
        (~/(?i)int|integer|int4/)    : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)  : "Date",
        (~/(?i)date/)                : "Date",
        (~/(?i)time/)                : "java.sql.Time",
        (~/(?i)tinyint\(1\)|bit/)    : "Boolean",
        (~/(?i)tinyint|smallint/)    : "Integer",
        (~/(?i)text|longtext|varchar|char|string/): "String",
        (~/(?i)/)                    : "String"
]


layerPackages = [
        domain  : "domain",
        mapper  : "mapper",
        service : "service",
        impl    : "service/impl",
        controller: "controller"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    baseDir = dir
    SELECTION.filter { it instanceof DasTable }.each { generate(it) }
}

def generate(table) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)

    // 创建各层级目录（直接基于基础目录）
    layerPackages.each { layer, relativePath ->
        new File("$baseDir/$relativePath").mkdirs()
    }

    // 生成各层级文件
    generateDomain(className, fields, table)
    generateMapper(className)
    generateMapperXml(className)
    generateService(className)
    generateServiceImpl(className)
    generateController(className)
}


def generateDomain(className, fields, table) {
    // 原始domain目录
    def domainDir = new File("$baseDir/${layerPackages.domain}")
    domainDir.mkdirs()

    // 创建create子目录
    def createDir = new File(domainDir, "create")
    createDir.mkdirs()

    // 创建update子目录
    def updateDir = new File(domainDir, "update")
    updateDir.mkdirs()

    // 生成原始domain类（保持不变）
    generateClassFile(domainDir, "${packageName}.${layerPackages.domain}", className, fields, table)

    // 生成Create后缀类（在create目录）
    generateClassFile(createDir, "${packageName}.${layerPackages.domain}.create", "${className}Create", fields, table)

    // 生成Update后缀类（在update目录）
    generateClassFile(updateDir, "${packageName}.${layerPackages.domain}.update", "${className}Update", fields, table)
}

def generateClassFile(outputDir, packageNameStr, classNameParam, fields, table) {
    def file = new File(outputDir, "${classNameParam}.java")
    file.withWriter("UTF-8") { writer ->
        def out = new PrintWriter(writer)

        // 包声明
        out.println "package $packageNameStr;"
        out.println ""

        // 动态导入
        def imports = [
                "lombok.Data",
                "lombok.AllArgsConstructor",
                "lombok.NoArgsConstructor",
                "com.baomidou.mybatisplus.annotation.*",
                "java.io.Serializable",
                "io.swagger.annotations.ApiModel",
                "io.swagger.annotations.ApiModelProperty"
        ] as Set

        // 特殊类型导入
        fields.each { field ->
            if (field.type == "LocalDateTime") imports.add("java.time.LocalDateTime")
            if (field.type == "Date") imports.add("java.util.Date")
            if (field.type == "LocalTime") imports.add("java.time.LocalTime")
        }

        imports.each { pkg -> out.println "import $pkg;" }
        out.println ""

        // 表注释处理
        def tableComment = getTableComment(table).replace("\"", "\\\"")

        // 类注解
        out.println "@ApiModel(value = \"${tableComment}\")"
        out.println "@Data"
        out.println "@NoArgsConstructor"
        out.println "@AllArgsConstructor"
        out.println "@TableName(value = \"${table.getName()}\")"
        out.println "public class $classNameParam {"
        out.println ""

        // 字段生成
        fields.each { field ->
            def fieldComment = (field.comment ?: "").replace("\"", "\\\"")

            out.println "    @ApiModelProperty(value = \"${fieldComment}\")"

            if (field.isPrimaryKey) {
                out.println "    @TableId(type = IdType.AUTO)"
            } else {
                out.println "    @TableField(value = \"${field.colName}\")"
            }
            out.println "    private ${field.type} ${field.name};"
            out.println ""
        }
        out.println "}"
    }
}

def generateMapper(className) {
    // 确保只生成一个文件：${className}Mapper.java
    def mapperDir = new File("$baseDir/${layerPackages.mapper}")
    mapperDir.mkdirs()  // 只创建mapper目录

    // 直接创建 ${className}Mapper.java 文件
    def file = new File(mapperDir, "${className}Mapper.java")
    file.withWriter("UTF-8"){ writer ->
        def out = new PrintWriter(writer)
        // 修复包名：使用完整包路径
        out.println "package ${packageName}.${layerPackages.mapper};"
        out.println ""
        // 修复导入：使用完整包路径
        out.println "import ${packageName}.${layerPackages.domain}.${className};"
        out.println "import com.baomidou.mybatisplus.core.mapper.BaseMapper;"
        out.println "import org.springframework.stereotype.Repository;"
        out.println ""
        out.println "@Repository"
        out.println "public interface ${className}Mapper extends BaseMapper<${className}> {"
        out.println "}"
    }
}

def generateMapperXml(className) {
    // 创建 resources/mapper 目录
    def resourcesDir = new File("$baseDir/../../another/mapper")
    resourcesDir.mkdirs()

    def xmlFile = new File(resourcesDir, "${className}Mapper.xml")
    xmlFile.withWriter("UTF-8") { writer ->
        def out = new PrintWriter(writer)

        // 构建 Mapper 接口的全限定名
        def mapperPackage = layerPackages.mapper.replace('/', '.')
        def fullMapperName = "${packageName}.${mapperPackage}.${className}Mapper"

        out.println "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        out.println "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">"
        out.println "<mapper namespace=\"${fullMapperName}\">"
        out.println ""
        out.println "    <!-- 基础结果映射 -->"
        out.println "    <resultMap id=\"BaseResultMap\" type=\"${packageName}.${layerPackages.domain}.${className}\">"
        out.println "        <!-- 在这里定义字段映射关系 -->"
        out.println "    </resultMap>"
        out.println ""
        out.println "    <!-- 通用查询列 -->"
        out.println "    <sql id=\"Base_Column_List\">"
        out.println "        <!-- 在这里定义通用查询列 -->"
        out.println "    </sql>"
        out.println ""
        out.println "    <!-- 自定义 SQL 语句可以在这里添加 -->"
        out.println "    "
        out.println "</mapper>"
    }
}

def generateService(className) {
    def serviceDir = new File("$baseDir/${layerPackages.service}")
    serviceDir.mkdirs()

    def file = new File(serviceDir, "I${className}Service.java")
    file.withWriter("UTF-8"){ writer ->
        def out = new PrintWriter(writer)
        // 修复包名：使用完整包路径
        out.println "package ${packageName}.${layerPackages.service};"
        out.println ""
        // 修复导入：使用完整包路径
        out.println "import ${packageName}.${layerPackages.domain}.${className};"
        out.println "import com.baomidou.mybatisplus.extension.service.IService;"
        out.println ""
        out.println "public interface I${className}Service extends IService<${className}> {"
        out.println "}"
    }
}

def generateServiceImpl(className) {
    def implDir = new File("$baseDir/${layerPackages.impl}")
    implDir.mkdirs()

    def file = new File(implDir, "${className}ServiceImpl.java")
    file.withWriter("UTF-8"){ writer ->
        def out = new PrintWriter(writer)
        // 修复包名：使用完整包路径
        out.println "package ${packageName}.${layerPackages.impl.replace('/', '.')};"
        out.println ""
        // 修复导入：使用完整包路径
        out.println "import ${packageName}.${layerPackages.domain}.${className};"
        out.println "import ${packageName}.${layerPackages.mapper}.${className}Mapper;"
        out.println "import ${packageName}.${layerPackages.service}.I${className}Service;"
        out.println "import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;"
        out.println "import org.springframework.stereotype.Service;"
        out.println ""
        out.println "@Service"
        out.println "public class ${className}ServiceImpl extends ServiceImpl<${className}Mapper, ${className}> implements I${className}Service {"
        out.println "}"
    }
}

def generateController(className) {
    // 安全处理类名转变量名
    def varName = className.length() > 1 ?
            className[0].toLowerCase() + className.substring(1) :
            className.toLowerCase()

    def controllerDir = new File("$baseDir/${layerPackages.controller}")
    controllerDir.mkdirs()

    def file = new File(controllerDir, "${className}Controller.java")
    file.withWriter("UTF-8"){ writer ->
        def out = new PrintWriter(writer)
        // 修复包名：使用完整包路径
        out.println "package ${packageName}.${layerPackages.controller};"
        out.println ""
        // 修复导入：使用完整包路径
        out.println "import ${packageName}.${layerPackages.service}.I${className}Service;"
        out.println "import org.springframework.beans.factory.annotation.Autowired;"
        out.println "import org.springframework.web.bind.annotation.*;"
        out.println "import io.swagger.annotations.Api;"
        out.println ""
        out.println "@Api(\"xx模块\")"
        out.println "@RestController"
        out.println "@RequestMapping(\"/${varName}\")"
        out.println "public class ${className}Controller {"
        out.println ""
        out.println "    @Autowired"
        out.println "    private I${className}Service ${varName}Service;"
        out.println ""
        out.println "    // 在这里添加控制器方法"
        out.println "}"
    }
}

def getTableComment(table) {
    try {
        // 1. 尝试标准getComment方法
        def comment = table.getComment() ?: ""
        if (!comment.empty) return comment

        // 2. 尝试getRemarks方法（通过反射）
        try {
            def getRemarksMethod = table.getClass().getMethod("getRemarks")
            if (getRemarksMethod) {
                comment = getRemarksMethod.invoke(table) ?: ""
                if (!comment.empty) return comment
            }
        } catch (Exception e) {
            // 忽略反射异常
        }

        // 3. 尝试getDescription方法
        comment = table.getDescription() ?: ""
        if (!comment.empty) return comment

        // 4. 尝试属性获取（支持多种属性名）
        def attributes = table.getAttributes()
        if (attributes) {
            // 常见属性名列表
            def possibleKeys = ["REMARKS", "COMMENT", "DESCRIPTION", "comment", "remarks"]
            for (key in possibleKeys) {
                def value = attributes.get(key)
                if (value && !value.empty) return value
            }
        }

        // 5. 尝试toString解析（最后手段）
        def strRepr = table.toString()
        if (strRepr.contains("remarks=")) {
            def pattern = /remarks='(.*?)'/
            def matcher = strRepr =~ pattern
            if (matcher.find()) return matcher.group(1)
        }

        return "" // 所有方法都失败
    } catch (Exception e) {
        return "" // 异常时返回空字符串
    }
}

def calcFields(table) {
    def primaryKey = DasUtil.getPrimaryKey(table)
    def pkColumns = []
    if (primaryKey) {
        DasUtil.getColumns(primaryKey).each { col -> pkColumns << col.getName() }
    }

    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDasType().getSpecification())

        // 改进类型匹配逻辑
        def matchedType = typeMapping.find { pattern, type ->
            pattern.matcher(spec).find()
        }

        def typeStr = matchedType ? matchedType.value : "String"
        def colName = col.getName()

        // 获取字段注释（如果为空则使用空字符串）
        def comment = col.getComment() ?: ""

        fields += [[
                           name: javaName(colName, false),
                           type: typeStr,
                           colName: colName,
                           comment: comment,  // 添加注释字段（可能为空字符串）
                           isPrimaryKey: pkColumns.contains(colName)
                   ]]
    }
}

def javaName(str, capitalize) {
    def s = str.split(/[^\p{Alnum}]/).collect { it.capitalize() }.join("")
    capitalize ? s : s.uncapitalize()
}