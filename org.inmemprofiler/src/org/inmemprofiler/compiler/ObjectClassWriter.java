package org.inmemprofiler.compiler;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ALOAD;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Transform java.lang.Object class to include instrumentation call in the constructor.
 */
public class ObjectClassWriter extends ClassWriter
{
  /**
   * cTor
   * 
   * @param xiClassName
   * @param xiReader
   * @param analysis
   */
  public ObjectClassWriter(ClassReader xiReader)
  {
    super(xiReader, COMPUTE_MAXS);
  }

  /**
   * Instrument a particular method.
   */
  @Override
  public MethodVisitor visitMethod(int access, String name, String desc,
                                   String signature, String[] exceptions)
  {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature,
                                         exceptions);

    if ("<init>".equals(name))
    {
      return new ProfiledMethodWriter(mv);
    }
    else
    {
      return mv;
    }
  }

  /**
   * ASM2 MethodVisitor used to instrument methods.
   */
  private static class ProfiledMethodWriter extends MethodAdapter
  {
    private static final String HELPER_CLASS = "org/inmemprofiler/runtime/ObjectProfiler";

    /**
     * cTor
     * 
     * @param xiMethodVisitor
     * @param access
     * @param xiClassName
     * @param xiMethodName
     * @param xiDesc
     * @param xiBranchTraceLines
     * @param entryLine
     */
    public ProfiledMethodWriter(MethodVisitor xiMethodVisitor)
    {
      super(xiMethodVisitor);
    }
    
    @Override
    public void visitInsn(int opcode)
    {
      if (opcode == Opcodes.RETURN)
      {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, HELPER_CLASS, "newObject", "(Ljava/lang/Object;)V");
      }
      super.visitInsn(opcode);
    }
  }
}
