package org.inmemprofiler.runtime;

import java.lang.instrument.Instrumentation;

/**
 * Agent used to parse args and ensure all our classes have been loaded.
 * <p>
 * Supported args:
 * <ul>
 * <li>[bucket-5,15,25,35,45
 * <li>[classes-java,sun.net
 * <li>[gc-1
 * <li>[periodic-10
 * </ul>
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
    System.out.println("## Loaded InMemProfiler Agent");
    ProfilerAPI.beginProfiling(agentArgs);
  }
}
