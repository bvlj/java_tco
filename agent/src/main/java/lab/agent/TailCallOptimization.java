package lab.agent;

import static org.objectweb.asm.Opcodes.*;

import java.util.ListIterator;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class TailCallOptimization {

    private TailCallOptimization() {
    }

    /**
     * When the last statement executed in a procedure body is a recursive call
     * to the same procedure, the call is said to be tail recursive
     */
    public static boolean isTailRecursive(ClassNode cn, MethodNode mn) {
        final ListIterator<AbstractInsnNode> itr = mn.instructions.iterator();
        final int returnOpcode = Type.getReturnType(mn.desc).getOpcode(IRETURN);

        boolean lastWasTailRecursiveCall = false;
        while (itr.hasNext()) {
            lastWasTailRecursiveCall = switch (itr.next()) {
                case MethodInsnNode node -> {
                    if (isRecursiveMethodCall(cn, mn, node) && itr.hasNext()) {
                        final AbstractInsnNode next = itr.next();
                        yield returnOpcode == next.getOpcode();
                    } else {
                        yield false;
                    }
                }
                // Ignore pseudo-instructions
                case LabelNode ignored -> lastWasTailRecursiveCall;
                case FrameNode ignored -> lastWasTailRecursiveCall;
                case LineNumberNode ignored -> lastWasTailRecursiveCall;
                // Not a method invocation
                default -> false;
            };
        }
        return lastWasTailRecursiveCall;
    }

    public static void optimize(ClassNode cn, MethodNode mn) {
        final Type[] argTypes = Type.getArgumentTypes(mn.desc);
        final int returnOpcode = Type.getReturnType(mn.desc).getOpcode(IRETURN);
        final ListIterator<AbstractInsnNode> itr = mn.instructions.iterator();

        LabelNode firstLabel = null;
        while (itr.hasNext()) {
            final AbstractInsnNode node = itr.next();
            // Locate the first label: we want to save this to make a jump
            // instead of calling recursively the method
            if (firstLabel == null && node instanceof LabelNode labelNode) {
                firstLabel = labelNode;
            }
            // Replace recursive call with value update and jump back to the
            // firstLabel
            if (node instanceof MethodInsnNode methodInsnNode
                    && isRecursiveMethodCall(cn, mn, methodInsnNode)
                    && itr.hasNext()) {
                final AbstractInsnNode nextInsn = itr.next();
                if (returnOpcode != nextInsn.getOpcode()) {
                    // Not returning the result of the recursive method call,
                    // we cannot optimize this one...
                    continue;
                }
                final boolean isInstanceMethod = (mn.access & ACC_STATIC) == 0;
                // Remove recursive method invocation and the subsequent return
                itr.previous(); // Move back (return)
                itr.previous(); // Move back (recursive method invocation)
                itr.remove(); // Remove recursive method invocation
                itr.next();
                itr.remove(); // Remove return
                // Pop the values from the stack and store them in the stack
                // frame (parameters local variables) in reverse order
                final int paramsOffset = isInstanceMethod ? 1 : 0;
                for (int i = argTypes.length - 1; i >= 0; i--) {
                    itr.add(new VarInsnNode(argTypes[i].getOpcode(ISTORE), i + paramsOffset));
                }

                if (isInstanceMethod) {
                    // Pop the leftover instance from the recursive instance
                    // method invocation
                    itr.add(new InsnNode(POP));
                }

                // Add jump back to the firstLabel
                itr.add(new JumpInsnNode(GOTO, firstLabel));
                break;
            }
        }
    }

    private static boolean isRecursiveMethodCall(ClassNode cn, MethodNode mn, MethodInsnNode methodInsnNode) {
        return cn.name.equals(methodInsnNode.owner)
                && mn.name.equals(methodInsnNode.name)
                && mn.desc.equals(methodInsnNode.desc);
    }
}
