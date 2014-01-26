package org.inmemprofiler.runtime;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.util.Date;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * Public API for resetting the stored data and requesting output.
 */
public class ProfilerAPI implements ProfilerAPIMBean
{
  private static class ProfilerAPIHolder {
    private static final ProfilerAPI instance = new ProfilerAPI();

    static {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      ObjectName name;
      try {
        name = new ObjectName("org.inmemprofiler.runtime:type=ProfilerAPIMBean");
        mbs.registerMBean(ProfilerAPIHolder.instance, name);
      } catch (MalformedObjectNameException e) {
        e.printStackTrace();
      } catch (NullPointerException e) {
        e.printStackTrace();
      } catch (InstanceAlreadyExistsException e) {
        e.printStackTrace();
      } catch (MBeanRegistrationException e) {
        e.printStackTrace();
      } catch (NotCompliantMBeanException e) {
        e.printStackTrace();
      }
    }
  }

  private volatile boolean parsedArgs = false;

  private ProfilerAPI() {
  }

  private static ProfilerAPI getInstance() {
    return ProfilerAPIHolder.instance;
  }

  @Override
  public void beginProfilingJMX(String allArgs) {
    if (this.parsedArgs) {
      this.endProfilingJMX();
    }

    System.out.println(new Date().toString() + " ## Enable InMemProfiler : Args : " + allArgs);

    // Read args
    long[] buckets = null;
    String[] prefixes = null;
    String[] excludePrefixes = null;
    String[] traceIgnore = null;
    String[] traceTarget = null;

    boolean exactmatch = false;
    boolean traceallocs = false;
    boolean trackcollection = false;
    boolean delayprofiling = false;
    boolean detailedtrace = false;
    boolean blame = true;

    long gcInterval = -1;
    long periodicInterval = -1;
    long outputLimit = Integer.MAX_VALUE;
    long sampleEvery = 1;
    long numResets = 0;
    long largerThan = -1;

    String path = null;      

    if ((allArgs != null) && (allArgs.indexOf('#') > -1))
    {
      String[] args = allArgs.split("#");
      for (String arg : args)
      {
        if (arg.startsWith("bucket-"))
        {
          arg = arg.substring("bucket-".length());
          try
          {
            if (arg.indexOf(",") > -1)
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
            else
            {
              long[] argBuckets = new long[2];
              argBuckets[0] = Long.parseLong(arg);
              argBuckets[argBuckets.length - 1] = Long.MAX_VALUE;
              buckets = argBuckets;
            }
          }
          catch (NumberFormatException ex)
          {
            ex.printStackTrace();
          }
        }
        else if (arg.startsWith("include-"))
        {
          arg = arg.substring("include-".length());
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
        else if (arg.startsWith("exclude-"))
        {
          arg = arg.substring("exclude-".length());
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
        else if (arg.startsWith("traceignore-"))
        {
          arg = arg.substring("traceignore-".length());
          if (arg.indexOf(",") > -1)
          {
            String[] prefixStrings = arg.split(",");
            traceIgnore = prefixStrings;
          }
          else
          {
            traceIgnore = new String[] {arg};
          }
        }
        else if (arg.startsWith("tracetarget-"))
        {
          arg = arg.substring("tracetarget-".length());
          if (arg.indexOf(",") > -1)
          {
            String[] prefixStrings = arg.split(",");
            traceTarget = prefixStrings;
          }
          else
          {
            traceTarget = new String[] {arg};
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
        else if (arg.startsWith("outputlimit-"))
        {
          arg = arg.substring("outputlimit-".length());
          try
          {
            long argVal = Long.parseLong(arg);
            if (argVal > 0)
            {
              outputLimit = argVal;
            }
          }
          catch (NumberFormatException ex)
          {
            ex.printStackTrace();
          }
        }
        else if (arg.startsWith("every-"))
        {
          arg = arg.substring("every-".length());
          try
          {
            long argVal = Long.parseLong(arg);
            if (argVal > 0)
            {
              sampleEvery = argVal;
            }
          }
          catch (NumberFormatException ex)
          {
            ex.printStackTrace();
          }
        }
        else if (arg.startsWith("largerthan-"))
        {
          arg = arg.substring("largerthan-".length());
          try
          {
            long argVal = Long.parseLong(arg);
            if (argVal > 0)
            {
              largerThan = argVal;
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
          if (arg.contains(","))
          {
            String[] argParts = arg.split(",");
            arg = argParts[0];
            try
            {
              numResets = Long.parseLong(argParts[1]);
            }
            catch (NumberFormatException ex)
            {
              ex.printStackTrace();
            }
          }
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
        else if (arg.equals("exactmatch"))
        {
          exactmatch = true;
        }      
        else if (arg.equals("trace"))
        {
          traceallocs = true;
        }
        else if (arg.equals("detailedtrace"))
        {
          detailedtrace = true;
        }
        else if (arg.equals("noblame"))
        {
          blame = false;
        }
        else if (arg.equals("trackcollection"))
        {
          trackcollection = true;
        }
        else if (arg.equals("delayprofiling"))
        {
          delayprofiling = true;
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
        exactmatch,
        gcInterval, 
        periodicInterval,
        outputLimit,
        sampleEvery,
        largerThan,
        numResets,
        traceallocs,
        trackcollection,
        delayprofiling,
        blame,
        detailedtrace,
        traceIgnore, traceTarget, path, allArgs);

    this.parsedArgs = true;
  }

  public void endProfilingJMX() {
    UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
    try
    {
      Thread.currentThread().setUncaughtExceptionHandler(ObjectProfiler.CRITICAL_BLOCK);
      ProfilerDataCollector.endProfiling();
      this.parsedArgs = false;
    }
    finally
    {
      Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
  }

  @Override
  public void beginPausedProfilingJMX() {
    UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
    try
    {
      Thread.currentThread().setUncaughtExceptionHandler(ObjectProfiler.CRITICAL_BLOCK);
      if (this.parsedArgs)
      {
        ProfilerDataCollector.beginPausedProfiling();
      }
    }
    finally
    {
      Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
  }

  @Override
  public void pauseProfilingJMX() {
    UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
    try
    {
      Thread.currentThread().setUncaughtExceptionHandler(ObjectProfiler.CRITICAL_BLOCK);
      ProfilerDataCollector.pauseProfiling();
    }
    finally
    {
      Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
  }

  @Override
  public void resetDataJMX() {
    UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
    try
    { 
      Thread.currentThread().setUncaughtExceptionHandler(ObjectProfiler.CRITICAL_BLOCK);
      System.out.println(new Date().toString() + " ## Reset InMemProfiler Data");
      ProfilerDataCollector.resetData();
    }
    finally
    {
      Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
  }

  @Override
  public void outputDataJMX() {
    UncaughtExceptionHandler handler = Thread.currentThread().getUncaughtExceptionHandler();
    try
    { 
      Thread.currentThread().setUncaughtExceptionHandler(ObjectProfiler.CRITICAL_BLOCK);
      ProfilerDataCollector.outputData(new StringBuilder("Reason for output: API request\n"));
    }
    finally
    {
      Thread.currentThread().setUncaughtExceptionHandler(handler);
    }
  }

  // Static API
  public static synchronized void beginProfiling(String allArgs)
  {
    ProfilerAPI.getInstance().beginProfilingJMX(allArgs);
  }

  public static synchronized void beginPausedProfiling()
  {
    ProfilerAPI.getInstance().beginPausedProfilingJMX();
  }

  public static synchronized void pauseProfiling()
  {
    ProfilerAPI.getInstance().pauseProfilingJMX();
  }

  public static void resetData()
  {  
    ProfilerAPI.getInstance().resetDataJMX();
  }

  public static void outputData()
  {
    ProfilerAPI.getInstance().outputDataJMX();
  }
}
