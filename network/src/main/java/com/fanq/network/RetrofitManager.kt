package com.fanq.network

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

/**
 * retrofit 管理类
 */

object RetrofitManager {

    var errorHandler: ErrorHandler? = null                                                 // 异常处理监听器

    var successStrategy: SuccessStrategy<*>? = null                                        // 判断是否成功策略

    var retryStrategyClass:Class<*> = DefaultRetryStrategy::class.java    // 重试策略 class

    var buildRetrofitBlock: CustomRetrofitBlock? = null                                     // 构建 retrofit 的方法

    var okHttpFullCustomBlock: OkHttpFulCustomBlock? = null                                 // 自定义 okHttp

    var okHttpCustomBlock: CustomOkHttpBlock? = null                                        // 自定义 okHttp

    var mainHost = ""                                                                       // 主host

    private var hostMap: Map<String, String>? = null                                        // host 列表

    private val httpUrlMap = HashMap<String, HttpUrl?>()                                    // httpUrl 列表

    private val retrofits = HashMap<String, Retrofit?>()                                    // retrofit 实例列表

    /**
     * 网络请求
     * @param serviceClazz         retrofit接口类
     * @param reqAction            调用请求的接口
     * @param success              请求成功回调
     * @param fail                 请求失败回调
     * @param reqBack              请求返回时
     * @param cancel               请求取消回调 判断是否成功策略返回 false ｜ 拦截请求返回 true
     * @param errorHandler         请求异常回调
     * @param coroutineScope       协程作用域
     * @param host                 请求域名
     * @param retryStrategyClass   请求重试策略
     * @param successStrategy      判断请求是否成功策略
     * @param isInterceptResult    是否拦截请求
     */
    fun <T, R> req(
            serviceClazz: Class<T>,
            reqAction: suspend (T) -> R,
            success: (R) -> Unit = {},
            fail: (R) -> Unit = {},
            reqBack: (R?) -> Unit = {},
            cancel: (R) -> Unit = {},
            errorHandler: ErrorHandler? = null,
            coroutineScope: CoroutineScope = MainScope(),
            host: String = "",
            retryStrategyClass: Class<out RetryStrategy<R>>? = null,
            successStrategy: SuccessStrategy<R>? = null,
            isInterceptResult: Boolean = true
    ): Job {
        val serviceHost = serviceClazz.getAnnotation(RequestHost::class.java)?.host ?: ""
        val useHost = when {
            host.isBlank() && serviceHost.isBlank() -> mainHost
            !host.isBlank() -> host
            !serviceHost.isBlank() -> serviceHost
            else -> mainHost
        }
        //异常参数检查
        val service = try {
            createService(serviceClazz, useHost)
        } catch (e: IllegalStateException) {
            Log.e("retrofit error", e.toString())
            reqBack(null)
            val handler = errorHandler ?: RetrofitManager.errorHandler
            if (null != handler)
                handler(e)
            null
        }
        service ?: return Job()
        //参数校验完成
        @Suppress("UNCHECKED_CAST") val retryStrategy: RetryStrategy<R> =
                retryStrategyClass?.newInstance()
                        ?: RetrofitManager.retryStrategyClass.newInstance() as RetryStrategy<R>
        return coroutineScope.launch {
            var retry: Boolean
            do {
                retry = try {
                    val result = reqAction(service)
                    val retryTime = retryStrategy.onRequestBack(result)
                    if (retryTime > -1) {
                        delay(retryTime * 1000L)
                        true
                    } else {
                        val isIntercept = isInterceptResult && InterceptStrategy.execute(result)
                        if (isIntercept) {
                            if (!isActive) return@launch
                            cancel(result)
                        } else {
                            if (!isActive) return@launch
                            reqBack(result)
                            @Suppress("UNCHECKED_CAST") val strategy: SuccessStrategy<R>? =
                                    successStrategy
                                            ?: RetrofitManager.successStrategy as? SuccessStrategy<R>
                            if (null == strategy) {
                                if (!isActive) return@launch
                                success(result)
                            } else {
                                if (strategy(result)) {
                                    if (!isActive) return@launch
                                    success(result)
                                } else {
                                    if (!isActive) return@launch
                                    fail(result)
                                }
                            }
                        }
                        false
                    }
                } catch (e: Exception) {
                    when (e) {
                        is HttpException,
                        is IOException,
                        is IllegalStateException -> {
                            Log.e("retrofit error", e.toString())
                            val retryTime = retryStrategy.onReqException(e)
                            if (retryTime > -1) {
                                delay(retryTime * 1000L)
                                true
                            } else {
                                reqBack(null)
                                val handler = errorHandler ?: RetrofitManager.errorHandler
                                if (null != handler)
                                    handler(e)
                                false
                            }
                        }
                        else -> throw e
                    }

                }
            } while (retry)
        }
    }

    /**
     * 网络请求
     * 同步
     * @param serviceClazz         retrofit接口类
     * @param reqAction            调用请求的接口
     * @param success              请求成功回调
     * @param fail                 请求失败回调
     * @param reqBack              请求返回时
     * @param cancel               请求取消回调 判断是否成功策略返回 false ｜ 拦截请求返回 true
     * @param errorHandler         请求异常回调
     * @param coroutineScope       协程作用域
     * @param host                 请求域名
     * @param retryStrategyClass   请求重试策略
     * @param successStrategy      判断请求是否成功策略
     * @param isInterceptResult    是否拦截请求
     */
    fun <T, R> reqSync(
            serviceClazz: Class<T>,
            reqAction: suspend (T) -> R,
            success: (R) -> Unit = {},
            fail: (R) -> Unit = {},
            reqBack: (R?) -> Unit = {},
            cancel: (R) -> Unit = {},
            errorHandler: ErrorHandler? = null,
            host: String = "",
            retryStrategyClass: Class<out RetryStrategy<R>>? = null,
            successStrategy: SuccessStrategy<R>? = null,
            isInterceptResult: Boolean = true
    ) {
        runBlocking {
            req(
                    serviceClazz,
                    reqAction,
                    success,
                    fail,
                    reqBack,
                    cancel,
                    errorHandler,
                    this,
                    host,
                    retryStrategyClass,
                    successStrategy,
                    isInterceptResult
            ).join()
        }
    }

    /**
     * 网络请求
     * 同步
     *
     * <h4>比较复杂 没看懂代码前 不建议 使用/修改</h4>
     *
     * @param serviceClazz         retrofit接口类
     * @param reqAction            调用请求的接口
     * @param success              请求成功回调
     * @param fail                 请求失败回调
     * @param reqBack              请求返回时
     * @param cancel               请求取消回调 判断是否成功策略返回 false ｜ 拦截请求返回 true
     * @param errorHandler         请求异常回调
     * @param host                 请求域名
     * @param retryStrategyClass   请求重试策略
     * @param successStrategy      判断请求是否成功策略
     * @param isInterceptResult    是否拦截请求
     */
    @Suppress("UNCHECKED_CAST")
    fun <T, E, R> reqSyncWithReturn(
            serviceClazz: Class<T>,
            reqAction: suspend (T) -> E,
            success: ((E) -> R)? = null,
            fail: ((E) -> R?)? = null,
            reqBack: (E?) -> Unit = {},
            cancel: ((E) -> R)? = null,
            errorHandler: ((Exception) -> R?)? = null,
            host: String = "",
            retryStrategyClass: Class<out RetryStrategy<E>>? = null,
            successStrategy: SuccessStrategy<E>? = null,
            isInterceptResult: Boolean = true
    ): R? {
        val result = runBlocking {
            val serviceHost = serviceClazz.getAnnotation(RequestHost::class.java)?.host ?: ""
            val useHost = when {
                host.isBlank() && serviceHost.isBlank() -> mainHost
                !host.isBlank() -> host
                !serviceHost.isBlank() -> serviceHost
                else -> mainHost
            }
            //异常参数检查
            val service = try {
                createService(serviceClazz, useHost)
            } catch (e: IllegalStateException) {
                Log.e("retrofit error", e.toString())
                reqBack.invoke(null)
                val handler = (errorHandler ?: {
                    RetrofitManager.errorHandler
                    null
                })
                return@runBlocking handler(e)
            }
            service ?: return@runBlocking null
            //参数校验完成
            val retryStrategy: RetryStrategy<E> =
                    retryStrategyClass?.newInstance()
                            ?: RetrofitManager.retryStrategyClass.newInstance() as RetryStrategy<E>
            do {
                try {
                    val result = reqAction(service)
                    val retryTime = retryStrategy.onRequestBack(result)
                    if (retryTime > -1) {
                        delay(retryTime * 1000L)
                    } else {
                        val isIntercept = isInterceptResult && InterceptStrategy.execute(result)
                        if (!isActive) return@runBlocking null
                        if (isIntercept)
                            return@runBlocking cancel?.invoke(result)
                        else {
                            if (!isActive) return@runBlocking null
                            reqBack.invoke(result)
                            val strategy: SuccessStrategy<E>? =
                                    successStrategy
                                            ?: RetrofitManager.successStrategy as? SuccessStrategy<E>
                            return@runBlocking if (null == strategy) {
                                if (!isActive) return@runBlocking null
                                success?.invoke(result)
                            } else {
                                if (strategy(result)) {
                                    if (!isActive) return@runBlocking null
                                    success?.invoke(result)
                                } else {
                                    if (!isActive) return@runBlocking null
                                    fail?.invoke(result)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is HttpException,
                        is IOException,
                        is IllegalStateException -> {
                            Log.e("retrofit error", e.toString())
                            val retryTime = retryStrategy.onReqException(e)
                            if (retryTime > -1) {
                                delay(retryTime * 1000L)
                            } else {
                                reqBack.invoke(null)
                                val handler = (errorHandler ?: {
                                    RetrofitManager.errorHandler
                                    null
                                })
                                return@runBlocking handler.invoke(e)
                            }
                        }
                        else -> throw e
                    }

                }
            } while (true)
        }
        return result as? R
    }

    /**
     * 网络请求
     * 同步
     *
     * <h4>比较复杂 没看懂代码前 不建议 使用/修改</h4>
     *
     * @param serviceClazz         retrofit接口类
     * @param reqAction            调用请求的接口
     * @param success              请求成功回调
     * @param fail                 请求失败回调
     * @param reqBack              请求返回时
     * @param cancel               请求取消回调 判断是否成功策略返回 false ｜ 拦截请求返回 true
     * @param errorHandler         请求异常回调
     * @param coroutineScope       协程作用域
     * @param host                 请求域名
     * @param retryStrategyClass   请求重试策略
     * @param successStrategy      判断请求是否成功策略
     * @param isInterceptResult    是否拦截请求
     */
    @Suppress("UNCHECKED_CAST", "UNREACHABLE_CODE")
    fun <T, E, R> reqDeferred(
            serviceClazz: Class<T>,
            reqAction: suspend (T) -> E,
            success: ((E) -> R)? = null,
            fail: ((E) -> R?)? = null,
            reqBack: (E?) -> Unit = {},
            cancel: ((E) -> R)? = null,
            errorHandler: ((Exception) -> R?)? = null,
            coroutineScope: CoroutineScope = MainScope(),
            host: String = "",
            retryStrategyClass: Class<out RetryStrategy<E>>? = null,
            successStrategy: SuccessStrategy<E>? = null,
            isInterceptResult: Boolean = true
    ): Deferred<R?> {
        return coroutineScope.async {
            val serviceHost = serviceClazz.getAnnotation(RequestHost::class.java)?.host ?: ""
            val useHost = when {
                host.isBlank() && serviceHost.isBlank() -> mainHost
                !host.isBlank() -> host
                !serviceHost.isBlank() -> serviceHost
                else -> mainHost
            }
            //异常参数检查
            val service = try {
                createService(serviceClazz, useHost)
            } catch (e: IllegalStateException) {
                Log.e("retrofit error", e.toString())
                reqBack.invoke(null)
                val handler = (errorHandler ?: {
                    RetrofitManager.errorHandler
                    null
                })
                return@async handler(e)
            }
            service ?: return@async null
            //参数校验完成
            val retryStrategy: RetryStrategy<E> =
                    retryStrategyClass?.newInstance()
                            ?: RetrofitManager.retryStrategyClass.newInstance() as RetryStrategy<E>
            do {
                try {
                    val result = reqAction(service)
                    val retryTime = retryStrategy.onRequestBack(result)
                    if (retryTime > -1) {
                        delay(retryTime * 1000L)
                    } else {
                        val isIntercept = isInterceptResult && InterceptStrategy.execute(result)
                        if (!isActive) return@async null
                        if (isIntercept)
                            return@async cancel?.invoke(result)
                        else {
                            if (!isActive) return@async null
                            reqBack.invoke(result)
                            val strategy: SuccessStrategy<E>? =
                                    successStrategy
                                            ?: RetrofitManager.successStrategy as? SuccessStrategy<E>
                            return@async if (null == strategy) {
                                if (!isActive) return@async null
                                success?.invoke(result)
                            } else {
                                if (strategy(result)) {
                                    if (!isActive) return@async null
                                    success?.invoke(result)
                                } else {
                                    if (!isActive) return@async null
                                    fail?.invoke(result)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is HttpException,
                        is IOException,
                        is IllegalStateException -> {
                            Log.e("retrofit error", e.toString())
                            val retryTime = retryStrategy.onReqException(e)
                            if (retryTime > -1) {
                                delay(retryTime * 1000L)
                            } else {
                                reqBack.invoke(null)
                                val handler = (errorHandler ?: {
                                    RetrofitManager.errorHandler
                                    null
                                })
                                return@async handler(e)
                            }
                        }
                        else -> throw e
                    }

                }
            } while (true)
            // 理论不可达 Lambda 必要返回
            null
        }
    }

    @Synchronized
    fun changeHost(host: Map<String, String>, mainHost: String) {
        if (null == hostMap) { // 原列表为空 全量生成 value 中的 host
            host.forEach { entry ->
                httpUrlMap[entry.key] = "${entry.value}/".toHttpUrl()
            }
        } else {
            // 替换已存在的 host 的 httpUrl
            host.forEach { entry ->
                val isContain = hostMap?.contains(entry.key) ?: false
                if (isContain) {
                    val httpUrl = httpUrlMap[entry.key]
                            ?: throw IllegalStateException("has not httpUrl please check code")
                    val uri = Uri.parse(entry.value)
                    if (httpUrl.host != uri.host || httpUrl.scheme != uri.scheme) {
                        if (uri.host == null || uri.scheme == null) {
                            throw IllegalStateException("${entry.value} host can not be null")
                        } else {
                            httpUrl.newBuilder()
                                .host(uri.host!!)
                                .scheme(uri.scheme!!)
                                .port(HttpUrl.defaultPort(uri.scheme!!))
                                .build()
                        }
                    }
                } else {
                    httpUrlMap[entry.key] = "${entry.value}/".toHttpUrl()
                }
            }
            // 删除已经去除的 host
            if (hostMap?.size != host.size) { // map 大小有差异 删除差异部分
                hostMap?.forEach { entry ->
                    val isContain = host.contains(entry.key)
                    if (!isContain) {
                        httpUrlMap[entry.key] = null
                        retrofits[entry.key] = null
                    }
                }
            }
        }
        hostMap = host
        changeRetrofit()
        RetrofitManager.mainHost = mainHost
        Log.e("","retrofit changeHost success")
    }

    /**
     * host key 是否已被注册
     */
    fun hasHostKey(hostKey: String): Boolean {
        return hostMap?.contains(hostKey) ?: false
    }

    private fun changeRetrofit() {
        httpUrlMap.forEach { entry ->
            buildRetrofit(entry.key)
        }
    }

    private fun buildRetrofit(host: String) {
        retrofits[host] = retrofits[host] ?: buildRetrofitBlock?.invoke(host) ?: Retrofit.Builder()
            .baseUrl(
                httpUrlMap[host]
                    ?: throw IllegalStateException("please check your code.$host")
            ) // 已经初始化 不可能为空
            .client(
                okHttpFullCustomBlock?.invoke() ?: buildDefaultOkHttp(
                    OkHttpClient.Builder(),
                    okHttpCustomBlock
                )
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getRetrofit(tab: String) =
            retrofits[tab] ?: throw IllegalStateException("please invoke changeHost.$tab")

    /**
     * 获取 service
     */
    private fun <T> createService(clazz: Class<T>, tab: String): T = getRetrofit(tab).create(clazz)
}