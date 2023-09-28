package com.gzik.privacyplugin.transform

import com.didiglobal.booster.transform.Transformer
import com.didiglobal.booster.transform.asm.ClassTransformer
import com.google.auto.service.AutoService
import com.gzik.privacyplugin.asmtransformer.BaseAsmTransformer
import org.gradle.api.Project

/**
 * @author fanqi@inke.com
 * 隐私调用的替换/hook-transform
 */
@AutoService(ClassTransformer::class)
class PrivacyMethodHookTransform(androidProject: Project) : BaseTransform(androidProject) {

    override val transformers = listOf<Transformer>(
        BaseAsmTransformer(
            listOf(PrivacyMethodReplaceTransformer())
        )
    )

    override fun getName(): String {
        return "PrivacyMethodHookTransform"
    }

}
