# Runtime Tail-Call-Optimization for Java

Applies a simple bytecode-based tail-call-optimization to any java program at
runtime through the use of JVM agents and ASM.

## Build & Run

Build the `agent` target:

```bash
./gradlew ":agent:assemble"
```

Run example application with Gradle...:

```bash
./gradlew ":example:run"
```

...or manually:

```bash
# Running without the agent will trigger a StackOverflow exception
java -javaagent:agent/build/libs/agent.jar \
     -cp example/build/libs/example.jar \
     lab.example.Application
```

## Limitations

- Only supports methods that return a value (both static and instance methods).
  Methods of type void are not supported.
- Does not optimize methods that make use of the `switch` statement instead of
  the `if` statement, but would still be eligible for tail-recursion
  optimization.
