package com.fanq.network

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * ssl 支持
 */

val DO_NOT_VERIFY: HostnameVerifier = HostnameVerifier { _, _ -> true }
val x509TrustManager: X509TrustManager = object : X509TrustManager {
    override fun checkClientTrusted(
        chain: Array<X509Certificate>,
        authType: String
    ) {
    }

    override fun checkServerTrusted(
        chain: Array<X509Certificate>,
        authType: String
    ) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls(0)
    }
}
val sslContext: SSLContext = SSLContext.getInstance("SSL").apply {
    init(
        null,
        arrayOf<TrustManager?>(x509TrustManager),
        SecureRandom()
    )
}