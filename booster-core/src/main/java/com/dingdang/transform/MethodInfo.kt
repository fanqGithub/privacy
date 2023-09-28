package com.dingdang.transform

/**
 * @author fanqi@inke.cn
 * @date 2023/1/5
 */
data class MethodInfo(
    var className: String = "", //类名
    var methodName: String = "", //函数
    var returnParam: Any? = "", //返回
    var time: Float = 0f, //耗时
    var params: ArrayList<Any?> = ArrayList() //参数
) {
    override fun equals(other: Any?): Boolean {
        val m: MethodInfo = other as MethodInfo
        return m.methodName == this.methodName
    }

    override fun toString(): String {
        return "MethodInfo(className='$className', methodName='$methodName', returnParam=$returnParam, time=$time, params=$params)"
    }
}

