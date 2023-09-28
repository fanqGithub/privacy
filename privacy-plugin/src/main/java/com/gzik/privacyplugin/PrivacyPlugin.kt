package com.gzik.privacyplugin

import com.android.build.gradle.AppExtension
import com.didiglobal.booster.gradle.getAndroid
import com.gzik.privacyplugin.transform.CollectTransform
import com.gzik.privacyplugin.transform.PrivacyMethodHookTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author fanqi@inke.com
 * @date 2023-9-26
 * 隐私插件
 */
class PrivacyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        when {
            project.plugins.hasPlugin("com.android.application") -> {
                project.getAndroid<AppExtension>().let { androidExt ->
                    androidExt.registerTransform(CollectTransform(project))
                    androidExt.registerTransform(PrivacyMethodHookTransform(project))
                }
            }
        }
    }
}