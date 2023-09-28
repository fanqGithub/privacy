package com.fanq.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

private const val TIME_OUT_MILLISECONDS = 10 * 1000L
private const val READ_TIME_OUT_MILLISECONDS = 20 * 1000L
private const val WRITE_TIME_OUT_MILLISECONDS = 20 * 1000L

fun buildDefaultOkHttp(builder: OkHttpClient.Builder, customOkHttpBlock: CustomOkHttpBlock?) = builder.run {
    sslSocketFactory(sslContext.socketFactory, x509TrustManager)
    hostnameVerifier(DO_NOT_VERIFY)
    connectTimeout(TIME_OUT_MILLISECONDS, TimeUnit.MILLISECONDS)
    readTimeout(READ_TIME_OUT_MILLISECONDS, TimeUnit.MILLISECONDS)
    writeTimeout(WRITE_TIME_OUT_MILLISECONDS, TimeUnit.MILLISECONDS)
    customOkHttpBlock?.let{
        it(this)
    } ?: build()
}