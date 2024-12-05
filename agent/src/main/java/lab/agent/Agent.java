package lab.agent;

import java.lang.instrument.Instrumentation;

public final class Agent {

  public static void premain(String agentArgs, Instrumentation instrumentation) {
    instrumentation.addTransformer(new Transformer());
  }
}
