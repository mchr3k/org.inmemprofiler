package org.inmemprofiler.compiler;

import java.lang.instrument.Instrumentation;

/**
 * "Compiler" used to add instrumentation to the standard java.lang.Object class.
 */
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
    System.out.println("## Loaded InMemProfiler Compiler.");
    
    // Construct Transformer
    ClassTransformer t = new ClassTransformer(inst);
    inst.addTransformer(t, true);

    // Ensure loaded classes are traced
    t.instrumentObject();
  }
}
