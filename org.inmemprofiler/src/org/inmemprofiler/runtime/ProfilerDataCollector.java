package org.inmemprofiler.runtime;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.data.AllInstanceBuckets;
import org.inmemprofiler.runtime.data.FileOutput;
import org.inmemprofiler.runtime.data.LifetimeWeakReference;

public class ProfilerDataCollector
{

  private static final long[] defaultBuckets = new long[]
  { 5, 30, 60, 5 * 60, 30 * 60, Long.MAX_VALUE };

  // Recorded data
  private static AllInstanceBuckets bucketInstances;

  // Maps for holding class names and creation times
  private static final Map<LifetimeWeakReference, Object> weakRefSet = new ConcurrentHashMap<LifetimeWeakReference, Object>();
  private static final Object setValue = new Object();

  // Q for recording collection times
  private static final ReferenceQueue<Object> objectCollectedQueue = new ReferenceQueue<Object>();

  public static void profileNewObject(Object ref)
  {
    String className = ref.getClass().getName();

    boolean earlyReturn = false;
    
    // Check whether this class is explicitly allowed
    if (classPrefixes != null)
    {
      earlyReturn = true;
      for (String classPrefix : classPrefixes)
      {
        if (className.startsWith(classPrefix))
        {
          earlyReturn = false;
          break;
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
      for (String classPrefix : excludeClassPrefixes)
      {
        if (className.startsWith(classPrefix))
        {
          earlyReturn = true;
          break;
        }
      }
    }

    if (earlyReturn)
    {
      return;
    }

    AllInstanceBuckets lBucketInstances = bucketInstances;
    lBucketInstances.addLiveInstance(className);
    LifetimeWeakReference key = new LifetimeWeakReference(ref, 
                                                          objectCollectedQueue, 
                                                          className, 
                                                          System.currentTimeMillis(),
                                                          lBucketInstances);
    weakRefSet.put(key, setValue);
  }

  public static void outputData(StringBuilder str)
  {
    str.append(bucketInstances.toString());
    FileOutput.writeOutput(str.toString());
  }
  
  static void resetData()
  {
    bucketInstances = new AllInstanceBuckets(bucketInstances.bucketIntervals);
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
          long instanceCreationTime = ref.creationTime;
          
          long instanceCollectionTime = System.currentTimeMillis();
          long instanceLifeTime = instanceCollectionTime
              - instanceCreationTime;

          ref.bucketInstances.addCollectedInstance(className, 
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
    
    public PeriodicOutputThread(long periodicInterval)
    {
      this.periodicInterval = periodicInterval;
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

  public static void beginProfiling(long[] buckets,
                                    String[] allowedPrefixes, 
                                    String[] excludePrefixes, 
                                    long gcInterval, 
                                    long periodicInterval,
                                    String path)
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
    }
    catch (FileNotFoundException ex)
    {
      ex.printStackTrace();
    }
    
    if (buckets == null)
    {
      buckets = defaultBuckets;
    }
    bucketInstances = new AllInstanceBuckets(buckets);
    classPrefixes = allowedPrefixes;
    excludeClassPrefixes = excludePrefixes;

    Thread[] workThreads = new Thread[4];
    if (gcInterval > -1)
    {
      ForceGCThread gcTh = new ForceGCThread(gcInterval);
      gcTh.start();
      workThreads[0] = gcTh.th;
    }
    if (periodicInterval > -1)
    {
      PeriodicOutputThread poTh = new PeriodicOutputThread(periodicInterval);
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
