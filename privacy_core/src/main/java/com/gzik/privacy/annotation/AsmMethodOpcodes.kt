package com.gzik.privacy.annotation

import org.objectweb.asm.Opcodes

/**
 * Opcodes转换
 */
object AsmMethodOpcodes {
    const val INVOKESTATIC = Opcodes.INVOKESTATIC
    const val INVOKEVIRTUAL = Opcodes.INVOKEVIRTUAL
    const val INVOKESPECIAL = Opcodes.INVOKESPECIAL
    const val INVOKEDYNAMIC = Opcodes.INVOKEDYNAMIC
    const val INVOKEINTERFACE = Opcodes.INVOKEINTERFACE
}