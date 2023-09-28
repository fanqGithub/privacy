package com.gzik.privacyplugin.transform

import com.didiglobal.booster.transform.TransformContext
import com.didiglobal.booster.transform.asm.ClassTransformer
import com.didiglobal.booster.transform.asm.className
import com.gzik.privacyplugin.utils.isRelease
import org.objectweb.asm.tree.ClassNode

/**
 * 借助booster的
 * 提供拦截方法，可以统一过滤不需要hook的类
 */
open class AbsClassTransformer : ClassTransformer {

    fun onInterceptor(context: TransformContext, klass: ClassNode): Boolean {
//        "===onInterceptor--->$this====${klass.className}===".println()
        if (context.isRelease()) {
            return true
        }
        //过滤kotlin module-info
        if (klass.className == "module-info") {
            return true
        }
        return false
    }
}