package com.github.im.group.model

import kotlinx.serialization.Serializable

@Serializable
data class CompanyDTO(
    val companyId: Long,
    val name: String,
    val code: String,
    val logo: String? = null
)
