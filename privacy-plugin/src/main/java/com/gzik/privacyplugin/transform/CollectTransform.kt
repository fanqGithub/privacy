package com.gzik.privacyplugin.transform

import com.didiglobal.booster.transform.Transformer
import com.gzik.privacyplugin.asmtransformer.BaseAsmTransformer
import org.gradle.api.Project

/**
 * @author fanqi@inke.com
 * @date 2023-9-27
 * 收集隐私调用transform
 */
class CollectTransform(androidProject: Project) : BaseTransform(androidProject) {
    override val transformers = listOf<Transformer>(
        BaseAsmTransformer(
            listOf(CollectReplacePrivacyTransformer())
        )
    )

    override fun getName(): String = "CollectTransform"
}
