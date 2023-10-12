package com.gzik.privacy.core

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.ContentResolver
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.VersionedPackage
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.DhcpInfo
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import com.gzik.privacy.core.annotation.AsmMethodOpcodes
import com.gzik.privacy.core.annotation.PrivacyMethodReplace
import java.net.NetworkInterface
import java.util.*

/**
 * @author fanqi@inke.com
 * @date 2023-9-28
 * 隐私调用方法管理
 * 集缓存+是否同意隐私政策一体
 */
@Keep
object PrivacyManager {

    private const val TAG = "PrivacyManager"

    //已同意隐私？
    var isAgreePrivacy: Boolean = false

    //是否使用缓存
    var isUseCache: Boolean = true
    private var anyCache = HashMap<String, Any>()

    private fun checkAgreePrivacy(name: String): Boolean {
        if (!isAgreePrivacy) {
            //没有同意隐私权限，打印堆栈
            logD("未同意隐私协议 调用了：$name ，调用栈= " + Log.getStackTraceString(Throwable()))
            return false
        }
        return true
    }

    private fun <T> getListCache(key: String): List<T>? {
        if (!isUseCache) {
            return null
        }
        val cache = anyCache[key]
        if (cache != null && cache is List<*>) {
            try {
                return (cache as List<T>).also {
                    logD("隐私方法:$key,getListCache 命中缓存")
                }
            } catch (e: Exception) {
                logW("getListCache: key=$key,e=${e.message}")
            }
        }
        logD("getListCache key=$key,return null")
        return null
    }

    private fun <T> getCache(key: String): T? {
        if (!isUseCache) {
            return null
        }
        val cache = anyCache[key]
        if (cache != null) {
            try {
                return (cache as T).also {
                    logD("隐私方法:$key,命中缓存=$it")
                }
            } catch (e: Exception) {
                logW("getCache: key=$key,e=${e.message}")
            }
        }
        logD("getCache key=$key,return null")
        return null
    }


    private fun <T> putCache(key: String, value: T): T {
        logI("putCache key=$key,value=$value")
        value?.let {
            anyCache[key] = value
        }
        return value
    }


    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = ActivityManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getRunningAppProcesses(manager: ActivityManager): List<RunningAppProcessInfo?> {
        val key = "getRunningAppProcesses"
        val cache = getListCache<RunningAppProcessInfo?>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return emptyList()
        }
        val value = manager.runningAppProcesses
        return putCache(key, value)
    }

    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = ActivityManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getRecentTasks(
        manager: ActivityManager,
        maxNum: Int,
        flags: Int
    ): List<ActivityManager.RecentTaskInfo>? {
        val key = "getRecentTasks"
        val cache = getListCache<ActivityManager.RecentTaskInfo>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return emptyList()
        }
        val value = manager.getRecentTasks(maxNum, flags)
        return putCache(key, value)
    }

    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = ActivityManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getRunningTasks(
        manager: ActivityManager,
        maxNum: Int
    ): List<ActivityManager.RunningTaskInfo>? {
        val key = "getRunningTasks"
        val cache = getListCache<ActivityManager.RunningTaskInfo>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return emptyList()
        }
        val value = manager.getRunningTasks(maxNum)
        return putCache(key, value)

    }

    /**
     * 读取基站信息，需要开启定位
     */
    @JvmStatic
    @SuppressLint("MissingPermission")
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getAllCellInfo(manager: TelephonyManager): List<CellInfo>? {
        val key = "getAllCellInfo"
        val cache = getListCache<CellInfo>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return emptyList()
        }
        val value = manager.allCellInfo
        return putCache(key, value)
    }

    /**
     * 读取基站信息
     */
    @SuppressLint("HardwareIds")
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getDeviceId(manager: TelephonyManager): String? {
        val key = "getDeviceId"
        val cache = getCache<String>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return null
        }
        //READ_PHONE_STATE 已经整改去掉，返回null
        val value = manager.deviceId
        return putCache(key, value)

    }

    /**
     * 读取基站信息
     */
    @SuppressLint("HardwareIds")
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getImei(manager: TelephonyManager): String? {
        val key = "getImei"
        val cache = getCache<String>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return null
        }
        //READ_PHONE_STATE 已经整改去掉，返回null
        return putCache(key, null)
    }

    /**
     * 读取ICCID
     */
    @SuppressLint("HardwareIds")
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getSimSerialNumber(manager: TelephonyManager): String? {
        val key = "getSimSerialNumber"
        val cache = getCache<String>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return ""
        }
        //android.Manifest.permission.READ_PHONE_STATE,不允许App读取，拦截调用
        return putCache(key, null)
    }

    /**
     * 获取SIM服务商信息
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getNetworkOperator(manager: TelephonyManager): String? {
        val key = "getNetworkOperator"
        val cache = getCache<String>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return ""
        }
        val getNetworkOperator = manager.networkOperator
        return putCache(key, getNetworkOperator)
    }

    /**
     * 获取MEID
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = TelephonyManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getMeid(manager: TelephonyManager): String? {
        val key = "getMeid"
        val cache = getCache<String>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return ""
        }
        val meid = manager.meid
        return putCache(key, meid)
    }

    /**
     * 读取WIFI的SSID
     */
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiInfo::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getSSID(manager: WifiInfo): String? {
        val key = "getSSID"
        val cache = getCache<String?>(key)
        if (cache != null) {
            return cache
        }

        if (!checkAgreePrivacy(key)) {
            return ""
        }

        val value = manager.ssid
        return putCache(key, value)
    }

    /**
     * 读取WIFI的SSID
     */
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiInfo::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getBSSID(manager: WifiInfo): String? {
        val key = "getBSSID"
        val cache = getCache<String?>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return ""
        }
        val value = manager.bssid
        return putCache(key, value)
    }

    /**
     * 读取WIFI的SSID
     */
    @SuppressLint("HardwareIds")
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiInfo::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getMacAddress(manager: WifiInfo): String? {
        val key = "getMacAddress"
        val cache = getCache<String?>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return ""
        }
        val value = manager.macAddress
        return putCache(key, value)
    }

    /**
     * 读取AndroidId
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = Settings.System::class,
        oriMethod = "getString",
        oriAccess = AsmMethodOpcodes.INVOKESTATIC
    )
    fun getSysString(resolver: ContentResolver, name: String): String? {
        return getString(resolver, name)
    }

    /**
     * 读取Secure
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = Settings.Secure::class,
        oriAccess = AsmMethodOpcodes.INVOKESTATIC
    )
    fun getString(resolver: ContentResolver, name: String): String? {
        //处理AndroidId
        if (Settings.Secure.ANDROID_ID == name) {
            val key = "ANDROID_ID"
            val cache = getCache<String?>(key)
            if (cache != null) {
                return cache
            }
            if (!checkAgreePrivacy(key)) {
                return ""
            }
            val value = Settings.Secure.getString(resolver, name)
            return putCache(key, value)
        }
        return Settings.System.getString(resolver, name)
    }

    /**
     * 获取MAC
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = NetworkInterface::class,
        oriMethod = "getHardwareAddress",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getHardwareAddress(manager: NetworkInterface): ByteArray? {
        var key = "NetworkInterface-getHardwareAddress"
        val cache = getCache<ByteArray>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            logD("NetworkInterface-getHardwareAddress not agree privacy")
            return ByteArray(1)
        }
        val value = manager.hardwareAddress
        return putCache(key, value)
    }

    /**
     * getSensorList
     */
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = SensorManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getSensorList(manager: SensorManager, type: Int): List<Sensor>? {
        val key = "getSensorList"
        val cache = getListCache<Sensor>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return mutableListOf()
        }
        val value = manager.getSensorList(type)
        return putCache(key, value)

    }

    /**
     * 读取WIFI扫描结果
     */
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiManager::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getScanResults(manager: WifiManager): List<ScanResult>? {
        val key = "getScanResults"
        val cache = getListCache<ScanResult>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy("getScanResults")) {
            return mutableListOf()
        }
        val value = manager.getScanResults()
        return putCache(key, value)
    }

    /**
     * 读取DHCP信息
     */
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiManager::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getDhcpInfo(manager: WifiManager): DhcpInfo? {
        val key = "getDhcpInfo"
        val cache = getCache<DhcpInfo>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return null
        }
        val value = manager.dhcpInfo
        return putCache(key, value)

    }

    /**
     * 读取DHCP信息
     */
    @SuppressLint("MissingPermission")
    @JvmStatic
    @PrivacyMethodReplace(oriClass = WifiManager::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
    fun getConfiguredNetworks(manager: WifiManager): List<WifiConfiguration>? {
        val key = "getConfiguredNetworks"
        val cache = getListCache<WifiConfiguration>(key)
        if (cache != null) {
            return cache
        }
        if (!checkAgreePrivacy(key)) {
            return mutableListOf()
        }
        val value = manager.configuredNetworks
        return putCache(key, value)

    }


    /**
     * 读取位置信息
     */
    @JvmStatic
    @SuppressLint("MissingPermission")
    @PrivacyMethodReplace(
        oriClass = LocationManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getLastKnownLocation(
        manager: LocationManager, provider: String
    ): Location? {
        if (!checkAgreePrivacy("getLastKnownLocation")) {
            return null
        }
        return manager.getLastKnownLocation(provider)
    }


    /**
     * 读取位置信息
     */
//    @JvmStatic
//    @AsmField(oriClass = LocationManager::class, oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL)
//    fun getLastLocation(
//        manager: LocationManager, provider: String
//    ): Location? {
//        log("getLastKnownLocation: isAgreePrivacy=$isAgreePrivacy")
//        if (isAgreePrivacy) {
//            return manager.getLastLocation(provider)
//        }
//        return null
//    }


    /**
     * 监视精细行动轨迹
     */
    @SuppressLint("MissingPermission")
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = LocationManager::class,
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun requestLocationUpdates(
        manager: LocationManager, provider: String, minTime: Long, minDistance: Float,
        listener: LocationListener
    ) {
        if (!checkAgreePrivacy("requestLocationUpdates")) {
            return
        }
        manager.requestLocationUpdates(provider, minTime, minDistance, listener)
    }

    /**
     * 获取包相关
     */
    @RequiresApi(Build.VERSION_CODES.O)
    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getPackageInfo",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getPackageInfo(
        manager: PackageManager,
        versionedPackage: VersionedPackage,
        flags: Int
    ): PackageInfo? {
        if (!checkAgreePrivacy("getPackageInfo")) {
            return null
        }
        logD("安装包-getPackageInfo-${versionedPackage.packageName}")
        return manager.getPackageInfo(versionedPackage, flags)
    }

    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getPackageInfo",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getPackageInfo(
        manager: PackageManager,
        packageName: String,
        flags: Int
    ): PackageInfo? {
        if (!checkAgreePrivacy("getPackageInfo")) {
            return null
        }
        logD("安装包-getPackageInfo-$packageName")
        return manager.getPackageInfo(packageName, flags)
    }


    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getInstalledPackagesAsUser",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getInstalledPackagesAsUser(
        manager: PackageManager,
        flags: Int,
        userId: Int
    ): List<PackageInfo> {
        return getInstalledPackages(manager, flags);
    }

    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getInstalledPackages",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getInstalledPackages(manager: PackageManager, flags: Int): List<PackageInfo> {
        logD("安装包-getInstalledPackages")
        if (!checkAgreePrivacy("getInstalledPackages")) {
            return emptyList()
        }
        return manager.getInstalledPackages(flags)
    }


    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getInstalledApplicationsAsUser",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getInstalledApplicationsAsUser(
        manager: PackageManager, flags: Int,
        userId: Int
    ): List<ApplicationInfo> {
        return getInstalledApplications(manager, flags);
    }

    @JvmStatic
    @PrivacyMethodReplace(
        oriClass = PackageManager::class,
        oriMethod = "getInstalledApplications",
        oriAccess = AsmMethodOpcodes.INVOKEVIRTUAL
    )
    fun getInstalledApplications(manager: PackageManager, flags: Int): List<ApplicationInfo> {
        logD("安装包-getInstalledApplications")
        if (!checkAgreePrivacy("getInstalledApplications")) {
            return emptyList()
        }
        return manager.getInstalledApplications(flags)
    }

    private fun logI(log: String) {
        Log.i(TAG, log)
    }

    private fun logD(log: String) {
        Log.d(TAG, log)
    }

    private fun logW(log: String) {
        Log.w(TAG, log)
    }

}