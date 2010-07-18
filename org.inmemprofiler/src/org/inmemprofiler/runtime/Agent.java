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
    System.out.println("## Loaded InMemProfiler Agent.");

    // Read args
    long[] buckets = null;
    String[] prefixes = null;
    long gcInterval = -1;
    long periodicInterval = -1;
    
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
              argBuckets[argBuckets.length - 1] = Long.MAX_VALUE;
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
        else if (arg.startsWith("gc-"))
        {
          arg = arg.substring("gc-".length());
          try
          {
            long argVal = Long.parseLong(arg);
            if (argVal > 0)
            {
              gcInterval = 1000 * argVal;
            }
          }
          catch (NumberFormatException ex)
          {
            ex.printStackTrace();
          }
        }
        else if (arg.startsWith("periodic-"))
        {
          arg = arg.substring("periodic-".length());
          try
          {
            long argVal = Long.parseLong(arg);
            if (argVal > 0)
            {
              periodicInterval = 1000 * argVal;
            }
          }
          catch (NumberFormatException ex)
          {
            ex.printStackTrace();
          }
        }
      }
    }
    
    // Load profiler classes
    ProfilerDataCollector.beginProfiling(buckets, prefixes, gcInterval, periodicInterval);
  }
}
