package org.inmemprofiler.runtime;

import java.util.Date;

/**
 * Public API for begining profiling, resetting the stored data and requesting output.
 * This API allows the profiler to be used without needing to use the Java Agent.
 */
public class Profiler
{
  public static void beginProfiling(String allArgs)
  {
    System.out.println(new Date().toString() + " ## Enable InMemProfiler : Args : " + allArgs);
    
    // Read args
    long[] buckets = null;
    String[] prefixes = null;
    String[] excludePrefixes = null;
    long gcInterval = -1;
    long periodicInterval = -1;
    String path = null;
    
    if ((allArgs != null) && (allArgs.indexOf('[') > -1))
    {
      String[] args = allArgs.split("\\[");
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
        else if (arg.startsWith("excludeclasses-"))
        {
          arg = arg.substring("excludeclasses-".length());
          if (arg.indexOf(",") > -1)
          {
            String[] prefixStrings = arg.split(",");
            excludePrefixes = prefixStrings;
          }
          else
          {
            excludePrefixes = new String[] {arg};
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
        else if (arg.startsWith("output-"))
        {
          arg = arg.substring("output-".length());
          if (arg.length() > 0)
          {
            path = arg;
          }
        }
        else if (arg.length() > 0)
        {
          System.out.println("## InMemProfiler: Unrecognised argument: " + arg);
        }
      }
    }
    
    // Load profiler classes
    ProfilerDataCollector.beginProfiling(buckets, 
                                         prefixes,
                                         excludePrefixes,
                                         gcInterval, 
                                         periodicInterval, 
                                         path);
  }
  
  public static void resetData()
  {
    System.out.println(new Date().toString() + " ## Reset InMemProfiler Data");
    ProfilerDataCollector.resetData();
  }
  
  public static void outputData()
  {
    ProfilerDataCollector.outputData(new StringBuilder("Reason for output: API request\n"));
  }
}
