package lab.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class Transformer implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classFileBuffer) {
    // Skip STDLIB, JVM and agent classes
    return loader == null
        || className.startsWith("java/")
        || className.startsWith("sun/")
        || className.startsWith("lab/agent")
        ? classFileBuffer
        : instrument(classFileBuffer);
  }

  /**
   * Instrument the given class to perform tail call optimization.
   */
  private byte[] instrument(byte[] bytes) {
    final ClassReader cr = new ClassReader(bytes);
    final ClassNode cn = new ClassNode();
    cr.accept(cn, ClassReader.SKIP_FRAMES);

    patchMethods(cn);

    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    return cw.toByteArray();
  }

  private void patchMethods(ClassNode cn) {
    for (final MethodNode mn : cn.methods) {
      if (TailCallOptimization.isTailRecursive(cn, mn)) {
        TailCallOptimization.optimize(cn, mn);
      }
    }
  }
}
