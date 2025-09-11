package com.github.im.group.repository

import com.github.im.group.api.FileMeta
import com.github.im.group.db.AppDatabase
import db.FileResource

/**
 * 文件记录
 *
 */
class FilesRepository(
    private val db: AppDatabase
) {

    private val STORE_PATH : String = "files"


    /**
     * 添加文件记录到数据库
     * @param metaFileMeta
     */
    fun addFile(metaFileMeta : FileMeta){

    }

    /**
     * 获取文件记录
     */
    fun getFile(fileId: String): FileResource?{
        return null
    }
}