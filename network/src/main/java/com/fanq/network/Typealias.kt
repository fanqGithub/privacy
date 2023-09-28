package com.fanq.network

import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import kotlin.Exception

/**
 * @param host
 *
 * @return
 */
typealias CustomRetrofitBlock = (String) -> Retrofit

typealias OkHttpFulCustomBlock = () -> OkHttpClient

typealias CustomOkHttpBlock = (OkHttpClient.Builder) -> OkHttpClient

typealias ErrorHandler = (Exception) -> Unit

/**
 * 判断请求是否成功 回调
 * @return 请求是否成功
 */
typealias SuccessStrategy<T> = (T) -> Boolean

/**
 * @return 是否拦截请求 回调 success ｜ fail
 */
typealias InterceptAction<T> = (T) -> Boolean