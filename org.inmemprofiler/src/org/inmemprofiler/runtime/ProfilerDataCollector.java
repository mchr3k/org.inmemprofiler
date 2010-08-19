package org.inmemprofiler.runtime;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.data.BucketContainer;
import org.inmemprofiler.runtime.data.xxAllocationSites;
import org.inmemprofiler.runtime.data.LifetimeWeakReference;
import org.inmemprofiler.runtime.data.ProfilerData;
import org.inmemprofiler.runtime.data.Trace;
import org.inmemprofiler.runtime.util.FileOutput;

public class ProfilerDataCollector
{

  private static final long[] defaultBuckets = new long[]
  { 5, 30, 60, 5 * 60, 30 * 60, Long.MAX_VALUE };

  // Recorded data
  private static ProfilerData data;

  // Maps for holding class names and creation times
  private static final Map<LifetimeWeakReference, Object> weakRefSet = new ConcurrentHashMap<LifetimeWeakReference, Object>();
  private static final Object setValue = new Object();

  // Reference Q used for recording collection times
  private static final ReferenceQueue<Object> objectCollectedQueue = new ReferenceQueue<Object>();

  public static void profileNewObject(Object ref)
  {
    String className = ref.getClass().getName();

    boolean earlyReturn = false;
    
    // Check whether this class is explicitly allowed
    if (classPrefixes != null)
    {
      earlyReturn = true;
      if (exactMatch)
      {
        for (String classPrefix : classPrefixes)
        {
          if (className.equals(classPrefix))
          {
            earlyReturn = false;
            break;
          }
        }
      }
      else
      {
        for (String classPrefix : classPrefixes)
        {
          if (className.startsWith(classPrefix))
          {
            earlyReturn = false;
            break;
          }
        }
      }
    }

    if (earlyReturn)
    {
      return;
    }
    
    // Check whether this class is explicitly not allowed
    if (excludeClassPrefixes != null)
    {
      earlyReturn = false;
      if (exactMatch)
      {
        for (String classPrefix : excludeClassPrefixes)
        {
          if (className.equals(classPrefix))
          {
            earlyReturn = true;
            break;
          }
        }        
      }
      else
      {
        for (String classPrefix : excludeClassPrefixes)
        {
          if (className.startsWith(classPrefix))
          {
            earlyReturn = true;
            break;
          }
        }
      }
    }

    if (earlyReturn)
    {
      return;
    }

    long size = ObjectSizer.getObjectSize(ref);
    Trace trace = getTrace();
    
    // Lifetime buckets
    ProfilerData lData = data;
    lData.newObject(className, size, trace);
    LifetimeWeakReference key = new LifetimeWeakReference(ref, 
                                                          objectCollectedQueue, 
                                                          className, 
                                                          System.currentTimeMillis(),
                                                          size,
                                                          trace,
                                                          lData);
    weakRefSet.put(key, setValue);
  }  

  private static Trace getTrace()
  {
    Exception ex = new Exception();
    StackTraceElement[] stackTrace = ex.getStackTrace();
    StackTraceElement[] fixedTrace = new StackTraceElement[stackTrace.length - 2];
    for (int ii = 2; ii < stackTrace.length; ii++)
    {
      fixedTrace[ii-2] = stackTrace[ii];
    }
    Trace trace = new Trace(fixedTrace);
    return trace;
  }

  public static void outputData(StringBuilder str)
  {
    str.append(data.toString());
    FileOutput.writeOutput(str.toString());
  }
  
  static void resetData()
  {
    data = new ProfilerData(data.bucketIntervals);
    FileOutput.writeOutput("Instance data reset\n");
  }

  /**
   * Profiler data collector
   */
  private static class ProfilerDataCollectorThread implements Runnable
  {
    public Thread th;
    
    /**
     * Start this thread
     */
    public void start()
    {
      th = new Thread(this);
      th.setDaemon(true);
      th.setName("InMemProfiler-DataCollector");
      th.setPriority(Thread.MAX_PRIORITY);
      th.start();
    }

    @Override
    public void run()
    {
      try
      {
        while (true)
        {
          LifetimeWeakReference ref = (LifetimeWeakReference)objectCollectedQueue.remove();
          
          String className = ref.className;        
          long size = ref.size;
          Trace trace = ref.trace;
          long instanceCreationTime = ref.creationTime;
          
          long instanceCollectionTime = System.currentTimeMillis();
          long instanceLifeTime = instanceCollectionTime
              - instanceCreationTime;

          ref.data.collectObject(className, size, trace,
                                 instanceLifeTime / 1000);
        }
      }
      catch (InterruptedException e)
      {
        // Discard
      }
    }
  }

  /**
   * Force GC thread
   */
  private static class ForceGCThread implements Runnable
  {
    private final long gcInterval;
    public Thread th;
    /*
     * Record the InMem threads so we don't record allocations by these threads!
     */

    public ForceGCThread(long gcInterval)
    {
      this.gcInterval = gcInterval;
    }

    /**
     * Start this thread
     */
    public void start()
    {
      th = new Thread(this);
      th.setDaemon(true);
      th.setName("InMemProfiler-ForceGC");
      th.start();
    }

    @Override
    public void run()
    {
      try
      {
        while (true)
        {
          Thread.sleep(gcInterval);
          System.gc();
        }
      }
      catch (InterruptedException e)
      {
        // Discard
      }
    }
  }

  /**
   * Periodic output thread
   */
  private static class PeriodicOutputThread implements Runnable
  {
    public Thread th;
    private final long periodicInterval;
    private long numResets;
    
    public PeriodicOutputThread(long periodicInterval, long numResets)
    {
      this.periodicInterval = periodicInterval;
      this.numResets = numResets;
    }

    /**
     * Start this thread
     */
    public void start()
    {
      th = new Thread(this);
      th.setDaemon(true);
      th.setName("InMemProfiler-PeriodicOutput");
      th.start();
    }

    @Override
    public void run()
    {
      try
      {
        while (true)
        {
          Thread.sleep(periodicInterval);          
          StringBuilder str = new StringBuilder("Reason for output: Periodic output\n");
          ProfilerDataCollector.outputData(str);
                    
          if (numResets != 0)
          {
            Profiler.resetData();
          }
          
          if (numResets > 0)
          {
            numResets--;
          }
        }
      }
      catch (InterruptedException e)
      {
        // Discard
      }
    }
  }

  // Profiling control code
  private static String[] classPrefixes;
  private static String[] excludeClassPrefixes;
  private static boolean exactMatch;

  public static void beginProfiling(long[] buckets,
                                    String[] allowedPrefixes, 
                                    String[] excludePrefixes, 
                                    boolean exactmatch, 
                                    long gcInterval, 
                                    long periodicInterval,
                                    long numResets, 
                                    String path, 
                                    String allArgs)
  {
    if (path == null)
    {
      path = "./";
    }
    
    try
    {
      FileOutput.writer = new BufferedWriter(
                            new OutputStreamWriter(
                              new FileOutputStream(path + "./inmemprofiler.log", true)));
      FileOutput.writeOutput("## InMemProfiler Log Initialized");
      FileOutput.writeOutput("## InMemProfiler : Args : " + allArgs);
    }
    catch (FileNotFoundException ex)
    {
      ex.printStackTrace();
    }
    
    if (buckets == null)
    {
      buckets = defaultBuckets;
    }
    data = new ProfilerData(buckets);
    classPrefixes = allowedPrefixes;
    excludeClassPrefixes = excludePrefixes;
    exactMatch = exactmatch;

    Thread[] workThreads = new Thread[4];
    if (gcInterval > -1)
    {
      ForceGCThread gcTh = new ForceGCThread(gcInterval);
      gcTh.start();
      workThreads[0] = gcTh.th;
    }
    if (periodicInterval > -1)
    {
      PeriodicOutputThread poTh = new PeriodicOutputThread(periodicInterval, numResets);
      poTh.start();
      workThreads[1] = poTh.th;
    }
    {
      ProfilerDataCollectorThread pdcTh = new ProfilerDataCollectorThread();
      pdcTh.start();
      workThreads[2] = pdcTh.th;
    }
    {
      // Ensure output happens during shutdown
      workThreads[3] = new Thread()
      {
        @Override
        public void run()
        {
          StringBuilder str = new StringBuilder("Reason for output: JVM Shutdown\n");
          ProfilerDataCollector.outputData(str);
        }
      };
      
      Runtime.getRuntime().addShutdownHook(workThreads[3]);
    }        

    ObjectProfiler.ignoreThreads = workThreads;
    ObjectProfiler.profilingEnabled = true;
  }
}
