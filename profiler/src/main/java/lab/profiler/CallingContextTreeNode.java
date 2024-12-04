package lab.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CallingContextTreeNode {

  private final CallingContextTreeNode parent;
  private final String value;
  private final List<CallingContextTreeNode> children;

  /**
   * Default constructor.
   *
   * @param parent Parent of this node, or <code>null</code> if this is the root node.
   */
  private CallingContextTreeNode(CallingContextTreeNode parent, String value) {
    this.parent = parent;
    this.value = value;
    this.children = new ArrayList<>();
  }

  /**
   * Returns the root node of a calling context tree.
   */
  public static CallingContextTreeNode root() {
    return new CallingContextTreeNode(null, "root");
  }

  /**
   * Add a child to this node.
   *
   * @param className        Name of the class of the invoked method
   * @param methodName       Name of the invoked method
   * @param methodDescriptor Descriptor of the invoked method
   * @return The new child node
   */
  public CallingContextTreeNode addChild(String className,
      String methodName,
      String methodDescriptor) {
    final String childValue = String.format("%1$s.%2$s%3$s",
        className,
        methodName,
        methodDescriptor);
    final CallingContextTreeNode child = new CallingContextTreeNode(this, childValue);
    children.add(child);
    return child;
  }

  /**
   * Get the parent of this node.
   */
  public Optional<CallingContextTreeNode> getParent() {
    return Optional.ofNullable(parent);
  }

  /**
   * Construct a string representation of this node and its children into the given
   * {@link StringBuilder}.
   */
  private void dump(StringBuilder sb, int indent) {
    sb.append(" ".repeat(indent))
        .append(value)
        .append("\n");
    for (final CallingContextTreeNode child : children) {
      child.dump(sb, indent + 1);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    dump(sb, 0);
    return sb.toString();
  }
}
