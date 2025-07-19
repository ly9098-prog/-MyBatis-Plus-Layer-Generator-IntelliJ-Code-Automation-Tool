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
    generateService(className)
    generateServiceImpl(className)
    generateController(className)
}

def generateDomain(className, fields, table) {
    def domainDir = new File("$baseDir/${layerPackages.domain}")
    domainDir.mkdirs()

    def file = new File(domainDir, "${className}.java")
    file.withWriter("UTF-8"){ writer ->
        def out = new PrintWriter(writer)
        // 修复包名：使用完整包路径
        out.println "package ${packageName}.${layerPackages.domain};"
        out.println ""

        // 动态导入需要的包
        def imports = [
                "lombok.Data",
                "lombok.AllArgsConstructor",
                "lombok.NoArgsConstructor",
                "com.baomidou.mybatisplus.annotation.*",
                "java.io.Serializable"
        ] as Set

        // 修复时间类型导入
        fields.each { field ->
            if (field.type == "LocalDateTime") imports.add("java.time.LocalDateTime")
            if (field.type == "Date") imports.add("java.util.Date")
            if (field.type == "LocalTime") imports.add("java.time.LocalTime")
        }

        imports.each { pkg -> out.println "import $pkg;" }
        out.println ""

        out.println "@Data"
        out.println "@NoArgsConstructor"
        out.println "@AllArgsConstructor"
        out.println "@TableName(value = \"${table.getName()}\")"
        out.println "public class $className implements Serializable {"
        out.println ""

        // 生成字段
        fields.each { field ->
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
        out.println ""
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

// 修复calcFields方法 - 确保正确识别字段类型
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
            // 使用正则表达式匹配，而不是完全匹配
            pattern.matcher(spec).find()
        }

        def typeStr = matchedType ? matchedType.value : "String" // 默认使用String

        def colName = col.getName()

        fields += [[
                           name: javaName(colName, false),
                           type: typeStr,
                           colName: colName,
                           isPrimaryKey: pkColumns.contains(colName)
                   ]]
    }
}

def javaName(str, capitalize) {
    def s = str.split(/[^\p{Alnum}]/).collect { it.capitalize() }.join("")
    capitalize ? s : s.uncapitalize()
}