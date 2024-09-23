package lab.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CallingContextTreeNode {

  private final CallingContextTreeNode parent;
  private final String value;
  private final List<CallingContextTreeNode> children;

  private final int indent;

  private CallingContextTreeNode(CallingContextTreeNode parent, String value, int indent) {
    this.parent = parent;
    this.value = value;
    this.indent = indent;
    this.children = new ArrayList<>();
  }

  public static CallingContextTreeNode root() {
    return new CallingContextTreeNode(null, "root", 0);
  }

  public CallingContextTreeNode addChild(String className,
      String methodName,
      String methodDescriptor) {
    final String childValue = String.format("%1$s.%2$s%3$s",
        className,
        methodName,
        methodDescriptor);
    final CallingContextTreeNode child = new CallingContextTreeNode(this, childValue, indent + 1);
    children.add(child);
    return child;
  }

  public Optional<CallingContextTreeNode> getParent() {
    return Optional.ofNullable(parent);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("  ".repeat(Math.max(0, indent)))
        .append(value)
        .append('\n');
    for (CallingContextTreeNode child : children) {
      sb.append(child.toString());
    }
    return sb.toString();
  }
}
