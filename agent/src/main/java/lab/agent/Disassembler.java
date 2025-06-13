package lab.agent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;

/**
 * A Disassembler can disassemble Java class files.
 *
 * <p>
 * It presents an output similar to <code>javap -c</code>. Given the name of the class file as a
 * command line argument, it prints the name of the class, a list of all methods, and for each
 * method, the list of all Java bytecode instructions.
 *
 * <p>
 * The format of the disassembled bytecode includes the opcodes (in the form of mnemonics such as
 * "ILOAD") and all the operands. Some operands can be printed as simple integers, while others have
 * to be printed in a more understandable form (e.g. method or field names and descriptors).
 * Operands of branch instructions are shown as an "id" of the targeted instruction. For this, all
 * instructions of a method, including ASM's pseudo-instructions (LABEL, LINE, FRAME), are numbered,
 * starting at 0. The instruction id allows you to look up the corresponding instruction object in
 * the instruction list: AbstractInsnNode target = instructionList.get(targetId);
 *
 * <p>
 * An example output:
 *
 * <pre>
 * Class: ExampleClass
 *     ...
 *   Method: switchMethod2(I)I
 *   0:   // label
 *   1:   // line number information
 *   2:   ICONST_0
 *   3:   ISTORE 2
 *   4:   // label
 *   5:   // line number information
 *   6:   ILOAD 1
 *   7:   LOOKUPSWITCH 0: 8, 1000: 13, 2000: 18, default: 23
 *   8:   // label
 *   9:   // line number information
 *   10:  ICONST_0
 *   11:  ISTORE 2
 *   12:  GOTO 27
 *   13:  // label
 *   14:  // line number information
 *   15:  ICONST_1
 *   16:  ISTORE 2
 *   17:  GOTO 27
 *   18:  // label
 *   19:  // line number information
 *   20:  ICONST_2
 *   21:  ISTORE 2
 *   22:  GOTO 27
 *   23:  // label
 *   24:  // line number information
 *   25:  ICONST_M1
 *   26:  ISTORE 2
 *   27:  // label
 *   28:  // line number information
 *   29:  ILOAD 2
 *   30:  IRETURN
 *   31:  // label
 * </pre>
 *
 * @author Matthias.Hauswirth@usi.ch
 */
public final class Disassembler {

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("Please specify the class file to disassemble");
    }
    final String classFileName = args[0];

    // Create an empty ClassNode (in-memory representation of a class).
    final ClassNode clazz = new ClassNode();

    // Create a ClassReader that loads the Java .class file specified as the
    // command line argument.
    try (InputStream iStream = Files.newInputStream(Path.of(classFileName))) {
      final ClassReader cr = new ClassReader(iStream);
      // Have the ClassReader read the class file and populate the ClassNode with
      // the corresponding information.
      cr.accept(clazz, 0);
    }

    // Dump the given ClassNode.
    System.out.println(disassembleClass(clazz));
  }

  public static String disassembleClass(ClassNode clazz) {
    final StringBuilder sb = new StringBuilder("Class: ");
    sb.append(clazz.name);
    sb.append("\n");
    // Get the list of all methods in that class
    final List<MethodNode> methods = clazz.methods;
    for (final MethodNode method : methods) {
      sb.append(disassembleMethod(method));
      sb.append("\n");
    }

    return sb.toString();
  }

  /**
   * Disassemble the given method and produce a string representation of it.
   */
  public static String disassembleMethod(MethodNode method) {
    final InsnList insns = method.instructions;
    final String instructions = IntStream.range(0, insns.size())
        // Disassemble instruction
        .mapToObj(i -> Disassembler.disassembleInstruction(insns.get(i), i, insns))
        // Add padding
        .map(s -> "  " + s)
        // Join
        .collect(Collectors.joining("\n"));
    return "  Method: "
        + method.name
        + method.desc
        + "\n"
        + instructions;
  }

  /**
   * Disassemble an instruction and produce a string representation of it.
   *
   * <p>Hint: Check out {@link org.objectweb.asm.MethodVisitor} to determine which instructions
   * (opcodes) have which instruction types (subclasses of {@link AbstractInsnNode}).
   *
   * <p>E.g. the comment in {@link org.objectweb.asm.MethodVisitor#visitIntInsn(int, int)}
   * shows the list of all opcodes that are represented as instructions of type {@link IntInsnNode}.
   * That list e.g. includes the {@link Opcodes#BIPUSH} opcode.
   *
   * @see org.objectweb.asm.MethodVisitor
   */
  public static String disassembleInstruction(AbstractInsnNode instruction,
      int idx,
      InsnList instructions) {
    final StringBuilder sb = new StringBuilder();
    final int opcode = instruction.getOpcode();
    final String mnemonic = opcode == -1
        ? ""
        : Printer.OPCODES[instruction.getOpcode()];
    sb.append(idx)
        .append(":\t")
        .append(mnemonic)
        .append(" ");

    /*
     * There are different subclasses of AbstractInsnNode.
     * AbstractInsnNode.getType() represents the subclass as an int.
     *
     * Note:
     * to check the subclass of an instruction node, we can either use:
     * <pre>
     * int type = instruction.getType();
     * boolean isLabel = type == AbstractInsnNode.LABEL;
     * </pre>
     *
     * or we can use:
     * <pre>
     * boolean isLabel = instruction instanceof LabelNode;
     * </pre>
     *
     * They give the same result, but the first one can be used in a switch.
     */
    switch (instruction) {
      /* Pseudo-instruction: branch or exception target */
      case LabelNode ignored -> sb.append("// label");

      /* Pseudo-instruction: stack frame map */
      case FrameNode ignored -> sb.append("// stack frame map");

      /* Pseudo-instruction: line number information */
      case LineNumberNode ignored -> sb.append("// line number information");

      /* Opcodes: NEWARRAY, BIPUSH, SIPUSH. */
      case IntInsnNode intNode -> {
        final int operand = intNode.operand;
        if (intNode.getOpcode() == Opcodes.NEWARRAY) {
          sb.append(Printer.TYPES[operand]);
        } else {
          sb.append(operand);
        }
      }

      /*
       * Opcodes: IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
       * IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
       * IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
       */
      case JumpInsnNode jumpNode -> sb.append(instructions.indexOf(jumpNode.label));

      /* Opcodes: LDC. */
      case LdcInsnNode ldcNode -> sb.append(ldcNode.cst);

      /* Opcodes: IINC. */
      case IincInsnNode iIncNode -> sb.append(iIncNode.var)
          .append(" ")
          .append(iIncNode.incr);

      /* Opcodes: NEW, ANEWARRAY, CHECKCAST or INSTANCEOF. */
      case TypeInsnNode typeNode -> sb.append(typeNode.desc);

      /*
       * Opcodes: ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE,
       * LSTORE, FSTORE, DSTORE, ASTORE or RET.
       */
      case VarInsnNode varNode -> sb.append(varNode.var);

      /* Opcodes: GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD. */
      case FieldInsnNode fieldNode -> sb.append(fieldNode.owner)
          .append(".")
          .append(fieldNode.name)
          .append(" ")
          .append(fieldNode.desc);

      /*
       * Opcodes: INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC,
       * INVOKEINTERFACE or INVOKEDYNAMIC.
       */
      case MethodInsnNode methodNode -> sb.append(methodNode.owner)
          .append(".")
          .append(methodNode.name)
          .append(" ")
          .append(methodNode.desc);

      /* Opcodes: MULTIANEWARRAY. */
      case MultiANewArrayInsnNode newArrayNode -> sb.append(newArrayNode.desc)
          .append(" ")
          .append(newArrayNode.dims);

      /* Opcodes: LOOKUPSWITCH. */
      case LookupSwitchInsnNode lookupSwitchNode -> {
        sb.append("( ");

        final List<Integer> keys = lookupSwitchNode.keys;
        final List<LabelNode> labels = lookupSwitchNode.labels;
        for (int i = 0; i < keys.size(); i++) {
          final int key = keys.get(i);
          final LabelNode targetInsn = labels.get(i);
          final int targetId = instructions.indexOf(targetInsn);
          sb.append(key)
              .append(": ")
              .append(targetId)
              .append(", ");
        }
        final LabelNode defaultInsn = lookupSwitchNode.dflt;
        final int defaultTargetId = instructions.indexOf(defaultInsn);
        sb.append("default: ")
            .append(defaultTargetId)
            .append(" )");
      }

      /* Opcodes: TABLESWITCH. */
      case TableSwitchInsnNode tableSwitchNode -> {
        sb.append("( ");

        final int minKey = tableSwitchNode.min;
        final List<LabelNode> labels = tableSwitchNode.labels;
        for (int i = 0; i < labels.size(); i++) {
          final int key = minKey + i;
          final LabelNode targetInsn = labels.get(i);
          final int targetId = instructions.indexOf(targetInsn);
          sb.append(key)
              .append(": ")
              .append(targetId)
              .append(", ");
        }
        final LabelNode defaultInsn = tableSwitchNode.dflt;
        final int defaultTargetId = instructions.indexOf(defaultInsn);
        sb.append("default: ")
            .append(defaultTargetId)
            .append(" )");
      }

      /*
       * Opcodes: NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2,
       * ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0,
       * FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD, FALOAD,
       * DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE,
       * DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP,
       * DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP, IADD, LADD, FADD,
       * DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
       * FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL,
       * LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
       * I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B,
       * I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN,
       * FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW,
       * MONITORENTER, or MONITOREXIT.
       */
      case InsnNode ignored -> { /* Zero operands: nothing to print */ }

      default -> { /* NOOP */ }
    }
    return sb.toString();
  }
}
