package org.inmemprofiler.compiler;

import java.lang.instrument.Instrumentation;

public class Agent
{
  /**
   * Entry point when loaded using -agent command line arg.
   * 
   * @param agentArgs
   * @param inst
   */
  public static void premain(String agentArgs, Instrumentation inst)
  {
    initialize(agentArgs, inst);
  }

  /**
   * Entry point when loaded into running JVM.
   * 
   * @param agentArgs
   * @param inst
   */
  public static void agentmain(String agentArgs, Instrumentation inst)
  {
    initialize(agentArgs, inst);
  }

  /**
   * Common init function.
   * 
   * @param agentArgs
   * @param inst
   */
  private static void initialize(String agentArgs, Instrumentation inst)
  {
    System.out.println("## Loaded InMemProfiler Compiler.");
    
    // Construct Transformer
    ClassTransformer t = new ClassTransformer(inst);
    inst.addTransformer(t, true);

    // Ensure loaded classes are traced
    t.instrumentObject();
  }
}
