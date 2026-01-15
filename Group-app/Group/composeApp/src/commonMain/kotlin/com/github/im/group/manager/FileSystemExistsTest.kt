package com.github.im.group.manager

import okio.FileSystem
import okio.Path.Companion.toPath
import io.github.aakira.napier.Napier

/**
 * 验证Okio fileSystem.exists() 方法在不同路径情况下的行为
 */
fun verifyFileSystemExistsBehavior() {
    Napier.d("开始验证 fileSystem.exists() 行为")
    
    val fileSystem = FileSystem.SYSTEM
    
    // 测试各种路径情况
    val testPaths = listOf(
        "" to "空字符串路径",
        "/" to "根目录",
        "/nonexistent" to "不存在的根下路径", 
        "/tmp/nonexistent" to "不存在的子路径",
        "nonexistent.txt" to "当前目录下不存在的文件",
        "." to "当前目录",
        ".." to "上级目录"
    )
    
    testPaths.forEach { (path, description) ->
        try {
            val result = if(path.isEmpty()) {
                // 特殊处理空字符串路径，因为 toPath() 可能会抛出异常
                try {
                    "".toPath()
                    fileSystem.exists("".toPath())
                } catch(e: IllegalArgumentException) {
                    Napier.d("空路径转换异常: ${e.message}")
                    false  // 空路径应该被视为不存在
                }
            } else {
                fileSystem.exists(path.toPath())
            }
            
            Napier.d("$description (路径: '$path') -> $result")
        } catch (e: Exception) {
            Napier.d("$description (路径: '$path') -> 异常: ${e.message}")
        }
    }
}

// 添加一个简单的单元测试来验证行为
fun testEmptyPathBehavior() {
    val fileSystem = FileSystem.SYSTEM
    
    println("Testing empty path behavior:")
    
    // 测试空路径的转换
    try {
        val path = "".toPath()
        println("Empty string converted to path: $path")
        
        val exists = fileSystem.exists(path)
        println("fileSystem.exists(emptyPath): $exists")
    } catch (e: Exception) {
        println("Converting empty string to path threw exception: ${e.message}")
    }
    
    // 测试空字符串直接使用
    println("Testing empty string directly:")
    try {
        val exists = fileSystem.exists("".toPath())
        println("fileSystem.exists(''.toPath()): $exists")
    } catch (e: Exception) {
        println("Direct empty string path threw exception: ${e.message}")
    }
}