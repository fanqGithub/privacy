package com.dingdang.transform

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.dingdang.plugin.log

/**
 * @author fanqi@inke.cn
 * @date 2023/1/3
 * 统计耗时方法transform
 */
class TimeCostTransform : Transform() {
    override fun getName(): String = "TimeCostTransform"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean = false

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        makeTransform(transformInvocation)
    }

    private fun makeTransform(transformInvocation: TransformInvocation){
        val start = System.currentTimeMillis()

        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental && this.isIncremental
        log("isIncremental=$isIncremental")
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        ////消费型输入，可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        for (input in transformInvocation.inputs) {
            for (directoryInput in input.directoryInputs) {
                log("directoryInput $directoryInput")
            }
            for (jarInput in input.jarInputs) {
                log("jarInput $jarInput")
            }
        }



        val cost = System.currentTimeMillis() - start
        log("makeTransform cost time: ${cost}ms.")
    }
}