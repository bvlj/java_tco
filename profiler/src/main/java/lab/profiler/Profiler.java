package lab.profiler;

public class Profiler {

  private static volatile Profiler instance;

  private final CallingContextTreeNode root;
  private CallingContextTreeNode visiting;

  private Profiler() {
    root = CallingContextTreeNode.root();
    visiting = root;
  }

  public synchronized static Profiler getInstance() {
    if (instance == null) {
      instance = new Profiler();

      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.out.println("Calling Context Tree:");
        System.out.println(instance.root.toString());
      }));
    }
    return instance;
  }

  public synchronized void enterNewNode(String className, String methodName, String methodDescriptor) {
    visiting = visiting.addChild(className, methodName, methodDescriptor);
  }

  public synchronized void exitCurrentNode() {
    visiting = visiting.getParent()
        .orElseThrow(() -> new IllegalStateException("Can't exit root node"));
  }
}
