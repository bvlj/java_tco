package lab.app;

public final class Application {

  public static void main(String[] args) {
    sayHi();
    printArguments(args);

    try {
      exceptionalSayHi();
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

  private static void sayHi() {
    System.out.println("Hello World!");
  }

  private static void printArguments(String[] args) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      sb.append(formatArgument(args, i));
    }
    System.out.println("Arguments: ");
    System.out.println(sb);
  }

  private static String formatArgument(String[] args, int i) {
    return (i + 1)
        + ". "
        + args[i]
        + '\n';
  }

  private static void exceptionalSayHi() throws Exception {
    throw new Exception("Exceptional hello!");
  }
}
