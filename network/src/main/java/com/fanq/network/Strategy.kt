package com.fanq.network

interface RetryStrategy<T>{

    /**
     * 请求异常回调
     * @param e
     *
     * @return 重试时间 s -1 为不重试
     */
    fun onReqException(e: Exception): Int


    /**
     * 请求结果返回
     * @param result
     *
     * @return 重试时间 s -1 为不重试
     */
    fun onRequestBack(result: T): Int
}

object InterceptStrategy{

    val actionList =  ArrayList<InterceptAction<*>>()

    fun<T> execute(result: T): Boolean{
        var intercept = false
        actionList.forEach{
            @Suppress("UNCHECKED_CAST") val action = it as InterceptAction<T>
            if (action(result))
                intercept = true
        }
        return intercept
    }

}