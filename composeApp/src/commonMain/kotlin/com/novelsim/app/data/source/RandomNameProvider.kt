package com.novelsim.app.data.source

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * 性别
 */
enum class Gender {
    MALE,
    FEMALE,
    NEUTRAL
}

@Serializable
data class NameTemplate(
    val id: String,
    val pattern: String,
    val description: String = "",
    val parts: Map<String, TemplatePart> = emptyMap(),
    val variants: List<TemplateVariant> = emptyList()
)

@Serializable
data class TemplateVariant(
    val pattern: String,
    val weight: Int = 1,
    val parts: Map<String, TemplatePart> = emptyMap()
)

@Serializable
data class TemplatePart(
    val file: String,
    val path: String,
    val weight: Int = 1,
    val optional: Boolean = false,
    val probability: Double = 1.0
)

@Serializable
data class NameRules(
    val templates: List<NameTemplate>
)

/**
 * 随机名称提供器 (优化版)
 * 使用模板驱动 + 按需加载数据，提高性能并增强灵活性
 */
class RandomNameProvider(
    private val random: Random = Random.Default
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // 模板缓存
    private var rules: NameRules? = null
    
    // 数据源缓存 (文件名 -> (路径 -> 列表))
    // 例如: "cnname.json" -> { "danxing" -> ["李", "王"], "fuxing" -> ["欧阳"] }
    private val loadedData = mutableMapOf<String, Map<String, List<String>>>()
    
    // JSON 加载器引用
    private lateinit var jsonLoader: suspend (String) -> String

    /**
     * 初始化
     * @param loader 加载 JSON 文件内容的函数
     */
    suspend fun initialize(loader: suspend (String) -> String) {
        this.jsonLoader = loader
        try {
            // 仅加载规则文件，不加载数据文件
            val rulesJson = loader("name_rules.json")
            rules = json.decodeFromString<NameRules>(rulesJson)
        } catch (e: Exception) {
            println("RandomNameProvider initialize failed: ${e.message}")
        }
    }
    
    fun getTemplates(): List<NameTemplate> {
        return rules?.templates ?: emptyList()
    }
    
    /**
     * 生成随机名称
     * @param templateId 模板ID (如 "chinese_name_male", "place_name")
     */
    suspend fun generate(templateId: String): String {
        val template = rules?.templates?.find { it.id == templateId } ?: return "未知模板: $templateId"
        
        // 选择变体 (主模板也是一种变体，作为默认)
        val activeVariant = selectVariant(template)
        val pattern = activeVariant.pattern
        
        // 合并主模板和变体的 parts 定义 (变体优先)
        val combinedParts = template.parts + activeVariant.parts
        
        // 解析模式字符串 "{surname}{given}"
        return replacePlaceholders(pattern, combinedParts)
    }
    
    // ===== 便捷方法 =====
    
    suspend fun getChineseName(gender: Gender = Gender.NEUTRAL): String {
        val templateId = when (gender) {
            Gender.MALE -> "chinese_name_male"
            Gender.FEMALE -> "chinese_name_female"
            Gender.NEUTRAL -> if (random.nextBoolean()) "chinese_name_male" else "chinese_name_female"
        }
        return generate(templateId)
    }
    
    suspend fun getPlaceName(): String = generate("place_name")
    
    suspend fun getSkillName(): String = generate("skill_name")
    
    suspend fun getEquipmentName(type: String = "sword"): String = generate("equipment_$type")

    suspend fun getEnglishName(): String = generate("english_name")
    
    suspend fun getVariableName(): String = generate("variable_name")
    
    suspend fun getEnemyName(type: String? = null): String {
        return if (type != null) generate("enemy_$type") else generate("enemy_generic")
    }
    
    suspend fun getItemName(type: String): String = generate("item_$type")
    
    suspend fun getOtherName(category: String): String {
        // 尝试直接使用 category 作为模板 ID，或者查找数据
        // 这里做一个简单的映射作为临时修复，更完善的做法是在 rules.json 添加更多模板
        return when (category) {
            "天材地宝" -> generate("treasure")
            else -> getRandomValue("other.json", category) // 直接从 other.json 查找路径
        }
    }

    // ===== 内部逻辑 =====

    private fun selectVariant(template: NameTemplate): TemplateVariant {
        if (template.variants.isEmpty()) {
            return TemplateVariant(template.pattern)
        }
        
        // 简单权重随机
        val totalWeight = template.variants.sumOf { it.weight } + 100 // 主模板默认权重100
        var r = random.nextInt(totalWeight)
        
        if (r < 100) return TemplateVariant(template.pattern) // 选中主模板
        r -= 100
        
        for (variant in template.variants) {
            r -= variant.weight
            if (r < 0) return variant
        }
        return TemplateVariant(template.pattern)
    }

    private suspend fun replacePlaceholders(pattern: String, parts: Map<String, TemplatePart>): String {
        val regex = "\\{(\\w+)\\}".toRegex()
        var result = pattern
        
        // 查找所有占位符
        val matches = regex.findAll(pattern).toList()
        
        for (match in matches) {
            val key = match.groupValues[1]
            val part = parts[key]
            
            val replacement = if (part != null) {
                // 检查可选性
                if (part.optional && random.nextDouble() > part.probability) {
                    ""
                } else {
                    getRandomValue(part.file, part.path)
                }
            } else {
                "{MISSING:$key}"
            }
            
            // 每次只替换一个（如果有多个相同占位符，会全部替换，这里简单处理）
            result = result.replaceFirst("{$key}", replacement)
        }
        
        return result
    }

    private suspend fun getRandomValue(fileName: String, jsonPath: String): String {
        // 1. 确保文件已加载
        ensureFileLoaded(fileName)
        
        // 2. 获取路径对应的数据列表
        val fileData = loadedData[fileName] ?: return "ErrorLoad:$fileName"
        
        // Try strict match
        var list = fileData[jsonPath] 
        
        // Fallback: Try finding a key that ends with the path or is contained
        if (list == null) {
            // 尝试常见变体
            val candidates = listOf("list", "list.list", "root.list")
            for (candidate in candidates) {
                list = fileData[candidate]
                if (list != null) break
            }
            
            // 如果还是找不到，尝试搜索
            if (list == null) {
                val foundKey = fileData.keys.find { it.endsWith(jsonPath) || jsonPath.endsWith(it) }
                if (foundKey != null) {
                    list = fileData[foundKey]
                }
            }
        }
    
        if (list == null || list.isEmpty()) {
            // Debug info
            // println("Keys available in $fileName: ${fileData.keys}")
            return "NoPath:$jsonPath"
        }
        
        return list.randomOrNull(random) ?: "EmptyList"
    }

    private suspend fun ensureFileLoaded(fileName: String) {
        if (loadedData.containsKey(fileName)) return
        
        try {
            val content = jsonLoader(fileName)
            val parsedData = parseJsonToMap(fileName, content)
            loadedData[fileName] = parsedData
        } catch (e: Exception) {
            println("Failed to load $fileName: ${e.message}")
            loadedData[fileName] = emptyMap()
        }
    }
    
    /**
     * 解析不同结构的 JSON 为统一的 Map<Path, List<String>> 格式
     */
    private fun parseJsonToMap(fileName: String, content: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        val jsonElement = json.parseToJsonElement(content)
        
        // 递归展平 JSON
        flattenJson(jsonElement, "", result)
        
        println("Loaded $fileName, keys found: ${result.keys}")
        return result
    }
    
    private fun flattenJson(element: kotlinx.serialization.json.JsonElement, prefix: String, result: MutableMap<String, MutableList<String>>) {
        if (element is kotlinx.serialization.json.JsonObject) {
            for ((key, value) in element) {
                // 特殊处理 "list" 结构 (如 zhuangbei.json)
                if (key == "list" && value is kotlinx.serialization.json.JsonArray) {
                    // 检查这是否是一个分类列表 [{name: "剑", list: [...]}, ...]
                    val firstItem = value.firstOrNull()
                    if (firstItem is kotlinx.serialization.json.JsonObject && firstItem.containsKey("name") && firstItem.containsKey("list")) {
                        // 这是一个分类数组，展平它
                        for (item in value) {
                            if (item is kotlinx.serialization.json.JsonObject) {
                                val categoryName = item["name"]?.toString()?.trim('"') ?: "unknown"
                                
                                // 遍历所有以 list 开头的 key
                                for ((subKey, subValue) in item) {
                                    if (subKey == "list") {
                                        flattenJson(subValue, categoryName, result)
                                    } else if (subKey.startsWith("list")) {
                                        flattenJson(subValue, "$categoryName.$subKey", result)
                                    }
                                }
                            }
                        }
                    } else {
                        // 普通数组或对象数组 (如 mijizhaoshi.json)
                        flattenJson(value, if (prefix.isEmpty()) key else "$prefix.$key", result)
                    }
                } else {
                    flattenJson(value, if (prefix.isEmpty()) key else "$prefix.$key", result)
                }
            }
        } else if (element is kotlinx.serialization.json.JsonArray) {
            // 如果数组全是字符串，则它是一个数据源
            val isStringArray = element.all { it is kotlinx.serialization.json.JsonPrimitive && it.isString }
            if (isStringArray) {
                val list = element.map { it.toString().trim('"') }
                result.getOrPut(prefix) { mutableListOf() }.addAll(list)
            } else {
                // 如果是包含对象的数组，递归处理 (保持前缀不变，从而合并同构数据)
                element.forEach { 
                    flattenJson(it, prefix, result)
                }
            }
        }
    }
}
