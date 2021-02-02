package org.parboiled.transform;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 *
 */
public class ASMSettings {
    public static final int ASM_API     = Opcodes.ASM8;
    public static final int JDK_VERSION = Opcodes.V1_8;
    public static final int FRAMES      = ClassWriter.COMPUTE_FRAMES;
}
