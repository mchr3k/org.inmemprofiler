package org.inmemprofiler.runtime;

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
    System.out.println("## Loaded InMemProfiler Agent.");

    // Read args
    long[] buckets = null;
    String[] prefixes = null;
    
    if ((agentArgs != null) && (agentArgs.indexOf('[') > -1))
    {
      String[] args = agentArgs.split("\\[");
      for (String arg : args)
      {
        if (arg.startsWith("bucket-"))
        {
          arg = arg.substring("bucket-".length());
          if (arg.indexOf(",") > -1)
          {
            try
            {
              String[] bucketStrings = arg.split(",");
              long[] argBuckets = new long[bucketStrings.length + 1];
              int ii = 0;
              for (String bucketString : bucketStrings)
              {
                argBuckets[ii] = Long.parseLong(bucketString);
                ii++;
              }
              argBuckets[argBuckets.length - 1] = 999999999;
              buckets = argBuckets;
            }
            catch (NumberFormatException ex)
            {
              ex.printStackTrace();
            }
          }
        }
        else if (arg.startsWith("classes-"))
        {
          arg = arg.substring("classes-".length());
          if (arg.indexOf(",") > -1)
          {
            String[] prefixStrings = arg.split(",");
            prefixes = prefixStrings;
          }
          else
          {
            prefixes = new String[] {arg};
          }
        }
      }
    }
    
    // Load profiler classes
    ProfilerDataCollector.beginProfiling(buckets, prefixes);
  }
}
