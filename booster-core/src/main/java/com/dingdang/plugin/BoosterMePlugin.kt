package com.dingdang.plugin

import com.android.build.gradle.AppExtension
import com.dingdang.transform.TimeCostTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author fanqi@inke.cn
 * @date 2022/12/30
 * 1、实现
 */
class BoosterMePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        requireNotNull(target.plugins.findPlugin("com.android.application")) {
            "BoosterMePlugin can only apply for application module."
        }
        //创建自定义参数
        target.extensions.create("boosterMe", CustomExtension::class.java)
        val android = target.extensions.findByName("android") as AppExtension
        //apply 方法的调用时机是在 build.gradle 中定义了插件 id 后就会调用，
        // 所以我们如果需要拿到我们需要的扩展属性的话，就需要在 project.afterEvaluate 闭包中拿，否则拿到的会为空。
        // 当在build.gradle中配置好 CustomExtentsion 的属性后，如果直接通过CustomExtentsion.getXxx()是无法获取得到值的
        // 所以需要调用project.afterEvaluate，该闭包会在gradle配置完成后回调，即解析完build.gradle文件后回调
        target.afterEvaluate { _ ->
            log("${target.extensions.getByName("boosterMe") as CustomExtension}")
            // 获取apk包的变体，applicationVariants默认有debug跟release两种变体。
            // 这个可以用来对apk进行相关的操作，比如重命名/对apk进行加固&上传到公司服务/上传到各大应用分发平台&webhook发布钉钉消息
            android.applicationVariants.all { variant ->
                val variantName = variant.name.capitalize()
                log("变体：$variantName")
                if (variantName.lowercase() == "release") {
                    log("release变体，执行打包输出的产物操作")
                }
            }
            //gradle任务准备好了
            target.gradle.taskGraph.whenReady {
                log("target.gradle.taskGraph.whenReady : ${it.allTasks}")
            }
        }
        android.registerTransform(TimeCostTransform())
    }
}