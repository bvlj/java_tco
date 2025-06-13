package lab.example;

public final class Application {

  public static void main(String[] args) {
    System.out.println(new Application().sum(new int[Integer.MAX_VALUE / 2]));
    System.out.println(factorial(5));
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
