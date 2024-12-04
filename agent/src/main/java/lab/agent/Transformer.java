package lab.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class Transformer implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classFileBuffer) {
    // Skip STDLIB, JVM, agent and profiler classes
    return loader == null
        || className.startsWith("java/")
        || className.startsWith("sun/")
        || className.startsWith("lab/agent")
        || className.startsWith("lab/profiler")
        ? classFileBuffer
        : instrument(classFileBuffer);
  }

  /**
   * Instrument the given class to profile it and compute the CCT.
   */
  private byte[] instrument(byte[] bytes) {
    final ClassReader cr = new ClassReader(bytes);
    final ClassNode cn = new ClassNode();
    cr.accept(cn, ClassReader.SKIP_FRAMES);

    addPatchesToMethods(cn);

    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }

  /**
   * Add patches at the entry and exit points of all methods to compute the CCT.
   */
  private void addPatchesToMethods(ClassNode cn) {
    for (final MethodNode mn : cn.methods) {
      // Add "enter" patch
      mn.instructions.insert(getEnterPatch(cn, mn));

      // Add "exit" patch to every exit point
      final int n = mn.instructions.size();
      for (int i = n - 1; i >= 0; i--) {
        final AbstractInsnNode insnNode = mn.instructions.get(i);
        switch (insnNode.getOpcode()) {
          case Opcodes.RETURN,
               Opcodes.ARETURN,
               Opcodes.DRETURN,
               Opcodes.FRETURN,
               Opcodes.IRETURN,
               Opcodes.LRETURN,
               Opcodes.ATHROW -> mn.instructions.insertBefore(insnNode, getExitPatch());
        }
      }
    }
  }

  /**
   * Enter patch.
   *
   * <p>Generates a patch that contains the following code:
   * {@code lab.profiler.Profiler.getInstance().enterNewNode($CLASS_NAME, $METHOD_NAME,
   * $METHOD_DESC)}
   */
  private static InsnList getEnterPatch(ClassNode cn, MethodNode mn) {
    final InsnList patch = new InsnList();
    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "lab/profiler/Profiler",
        "getInstance",
        "()Llab/profiler/Profiler;"));
    patch.add(new LdcInsnNode(cn.name));
    patch.add(new LdcInsnNode(mn.name));
    patch.add(new LdcInsnNode(mn.desc));
    patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
        "lab/profiler/Profiler",
        "enterNewNode",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"));
    return patch;
  }


  /**
   * Exit patch.
   *
   * <p>Generates a patch that contains the following code:
   * {@code lab.profiler.Profiler.getInstance().exitCurrentNode()}.
   */
  private static InsnList getExitPatch() {
    final InsnList patch = new InsnList();
    patch.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "lab/profiler/Profiler",
        "getInstance",
        "()Llab/profiler/Profiler;"));
    patch.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
        "lab/profiler/Profiler",
        "exitCurrentNode",
        "()V"));
    return patch;
  }
}
