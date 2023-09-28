package com.dingdang.plugin

/**
 * @author fanqi@inke.cn
 * @date 2023/1/3
 */
open class CustomExtension {
    var cv: String = ""
    var needInsert: Boolean = false

    override fun toString(): String {
        return "CustomExtension(cv='$cv', needInsert='$needInsert')"
    }
}