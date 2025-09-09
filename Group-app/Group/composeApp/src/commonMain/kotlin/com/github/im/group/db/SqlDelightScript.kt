package com.github.im.group.db

import kotlin.reflect.KClass

class SqlDelightScript {
}
fun generateSqlFile(kClass: KClass<*>, tableName: String): String {
    val columns = kClass.memberProperties.joinToString(",\n") { prop ->
        val sqlType = when (prop.returnType.toString()) {
            "kotlin.Long", "kotlin.Long?" -> "INTEGER"
            "kotlin.String", "kotlin.String?" -> "TEXT"
            "kotlin.Int", "kotlin.Int?" -> "INTEGER"
            "kotlin.Boolean", "kotlin.Boolean?" -> "INTEGER"
            else -> "TEXT" // 可扩展类型映射
        }
        "${prop.name} $sqlType${if (prop.name == "msgId") " PRIMARY KEY AUTOINCREMENT" else ""}"
    }

    val columnNames = kClass.memberProperties.joinToString(", ") { it.name }
    val columnParams = kClass.memberProperties.joinToString(", ") { "?" }

    return """
        CREATE TABLE $tableName (
            $columns
        );

        selectAll:
        SELECT * FROM $tableName;

        insert:
        INSERT INTO $tableName($columnNames) VALUES ($columnParams);

        deleteById:
        DELETE FROM $tableName WHERE msgId = ?;
    """.trimIndent()
}
