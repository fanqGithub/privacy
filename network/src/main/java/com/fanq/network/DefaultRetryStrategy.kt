package com.fanq.network

import kotlin.random.Random

class DefaultRetryStrategy<T> : RetryStrategy<T> {

    private val random = Random(System.currentTimeMillis())

    private var timeArr = arrayOf(1 + random.nextInt(3), 1 + random.nextInt(3), -1)

    private var time = -1

    override fun onReqException(e: Exception) =
        if (time < timeArr.size - 1)
             timeArr[(++time).coerceAtMost(timeArr.size - 1)]
        else -1

    override fun onRequestBack(result: T) = -1
}

class OneTimeRetryStrategy<T> : RetryStrategy<T> {
    override fun onReqException(e: Exception) = -1

    override fun onRequestBack(result: T) = -1
}