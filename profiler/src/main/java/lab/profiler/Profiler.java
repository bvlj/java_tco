package lab.profiler;

@SuppressWarnings("unused")
public final class Profiler {

  private static volatile Profiler instance;

  /**
   * Root of the Calling Context Tree.
   */
  private final CallingContextTreeNode root;

  /**
   * CCT node that is currently being visited by the profiler.
   */
  private CallingContextTreeNode visiting;

  private Profiler() {
    root = CallingContextTreeNode.root();
    visiting = root;
  }

  /**
   * Get the singleton instance of the {@link Profiler}.
   */
  public static synchronized Profiler getInstance() {
    if (instance == null) {
      instance = new Profiler();

      Runtime.getRuntime().addShutdownHook(new Thread(instance::close));
    }
    return instance;
  }

  /**
   * Mark the entrance into a new child node of the CCT.
   */
  public synchronized void enterNewNode(String className, String methodName, String methodDescriptor) {
    visiting = visiting.addChild(className, methodName, methodDescriptor);
  }

  /**
   * Mark the exit of the current node and move back into its parent.
   */
  public synchronized void exitCurrentNode() {
    visiting = visiting.getParent()
        .orElseThrow(() -> new IllegalStateException("Can't exit root node"));
  }

  private void close() {
    System.out.println("Calling Context Tree:");
    System.out.println(root.toString());
  }
}
