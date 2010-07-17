package org.inmemprofiler.runtime;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.data.AllBucketData;

public class ProfilerDataCollector
{

  private static final long[] defaultBuckets = new long[]
  { 5, 30, 60, 5 * 60, 30 * 60, 999999999 };

  // Recorded data
  private static AllBucketData bucketInstances;

  // Maps for holding class names and creation times
  private static final Map<WeakReference<Object>, Long> instanceCreationTimes = new ConcurrentHashMap<WeakReference<Object>, Long>();
  private static final Map<WeakReference<Object>, String> instanceClassName = new ConcurrentHashMap<WeakReference<Object>, String>();

  // Q for recording collection times
  private static final ReferenceQueue<Object> objectCollectedQueue = new ReferenceQueue<Object>();

  public static synchronized void profileNewObject(Object ref)
  {
    String className = ref.getClass().getName();

    boolean earlyReturn = false;
    if (classPrefixes != null)
    {
      earlyReturn = true;
      for (String classPrefix : classPrefixes)
      {
        if (className.startsWith(classPrefix))
        {
          earlyReturn = false;
        }
      }
    }

    if (earlyReturn)
    {
      return;
    }

    WeakReference<Object> key = new WeakReference<Object>(ref,
        objectCollectedQueue);
    instanceCreationTimes.put(key, System.currentTimeMillis());
    instanceClassName.put(key, className);
    bucketInstances.addLiveInstance(className);
  }

  public static synchronized void outputData()
  {
    try
    {
      enterWorkBlock();
      
      System.out.println(bucketInstances.toString());
    }
    finally
    {
      exitWorkBlock();
    }
  }

  /**
   * Profiler data collector
   */
  private static class ProfilerDataCollectorThread implements Runnable
  {
    /**
     * Start this thread
     */
    public void start()
    {
      Thread th = new Thread(this);
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
          Reference<? extends Object> ref = objectCollectedQueue.remove();
          try
          {
            ProfilerDataCollector.enterWorkBlock();
            String className = ProfilerDataCollector.instanceClassName.get(ref);
            long instanceCreationTime = ProfilerDataCollector.instanceCreationTimes
                .remove(ref);
            long instanceCollectionTime = System.currentTimeMillis();
            long instanceLifeTime = instanceCollectionTime
                - instanceCreationTime;

            ProfilerDataCollector.bucketInstances.addCollectedInstance(
                className, instanceLifeTime / 1000);
          }
          finally
          {
            ProfilerDataCollector.exitWorkBlock();
          }
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
    /**
     * Start this thread
     */
    public void start()
    {
      Thread th = new Thread(this);
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
          Thread.sleep(1 * 1000);
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
   * Force GC thread
   */
  private static class PeriodicOutputThread implements Runnable
  {
    /**
     * Start this thread
     */
    public void start()
    {
      Thread th = new Thread(this);
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
          Thread.sleep(60 * 1000);
          System.out.println("Periodic output:");
          ProfilerDataCollector.outputData();
        }
      }
      catch (InterruptedException e)
      {
        // Discard
      }
    }
  }

  // Profiling control code
  private static long workerThreadID = -1;
  static String[] classPrefixes;

  static synchronized void enterWorkBlock()
  {
    workerThreadID = Thread.currentThread().getId();
  }

  static synchronized void exitWorkBlock()
  {
    workerThreadID = -1;
  }

  static synchronized boolean isProfilingAllowed()
  {
    return (Thread.currentThread().getId() != workerThreadID);
  }

  public static void beginProfiling(long[] buckets,
      String[] allowedClassPrefixes)
  {
    if (buckets == null)
    {
      buckets = defaultBuckets;
    }
    bucketInstances = new AllBucketData(buckets);
    classPrefixes = allowedClassPrefixes;

    new ForceGCThread().start();
    new PeriodicOutputThread().start();
    new ProfilerDataCollectorThread().start();

    // Ensure output happens during shutdown
    Runtime.getRuntime().addShutdownHook(new Thread()
    {
      @Override
      public void run()
      {
        System.out.println("Shutdown output:");
        ProfilerDataCollector.outputData();
      }
    });

    BootClassPathProfiler.profilingEnabled = true;
  }
}
