package com.sintesistudio.parser

data class ParseResult(
    val title: String,
    val format: String,
    val text: String,
    val warnings: List<String> = emptyList()
)
