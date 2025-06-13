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

    System.out.println(new Application().sum(new int[Integer.MAX_VALUE / 2]));
    System.out.println(factorial(5));
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

  public int sum(int[] array) {
    return sumTailRec(array, 0, 0);
  }

  public int sumTailRec(int[] array, int i, int sum) {
    if (i >= array.length) {
      return sum;
    }
    return sumTailRec(array, i + 1, sum + array[i]);
  }

  static long factorial(int n) {
    return factorialTailRec(n, 1L);
  }

  static long factorialTailRec(int n, long f) {
    if (n < 2) {
      return f;
    }
    return factorialTailRec(n - 1, f * n);
  }
}
