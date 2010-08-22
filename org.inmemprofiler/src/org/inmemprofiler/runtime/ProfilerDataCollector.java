package org.inmemprofiler.runtime;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.ReferenceQueue;
import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
  
  // Limit amount of output data
  private static long outputLimit = Integer.MAX_VALUE;
  private static long sampleEvery = 1;
  private static AtomicLong sampleCount = new AtomicLong(0);
  private static boolean traceAllocs = false;

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

    long sampleIndex = sampleCount.incrementAndGet();
    if ((sampleIndex % sampleEvery) > 0)
    {
      earlyReturn = true;
    }
    
    if (earlyReturn)
    {
      return;
    }

    long size = ObjectSizer.getObjectSize(ref);
    Trace trace = null;
    if (traceAllocs)
    {
      trace = Trace.getTrace(3, className);
    }
    
    // Lifetime buckets
    ProfilerData lData = data;
    trace = lData.newObject(className, size, trace, allocatingClassTargets);
    
    if (trackCollection)
    {
      LifetimeWeakReference key = new LifetimeWeakReference(ref, 
                                                            objectCollectedQueue, 
                                                            className, 
                                                            System.currentTimeMillis(),
                                                            size,
                                                            trace,
                                                            lData);
      weakRefSet.put(key, setValue);
    }
  }  

  public static void outputData(StringBuilder str)
  {
    data.outputData(str, new Formatter(str), outputLimit, traceAllocs, 
                    traceClassFilter, trackCollection);
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
                                 instanceLifeTime / 1000,
                                 allocatingClassTargets);
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
          if (ObjectProfiler.profilingEnabled)
          {
            System.gc();
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
          if (ObjectProfiler.profilingEnabled)
          {
            StringBuilder str = new StringBuilder("Reason for output: Periodic output\n");
            ProfilerDataCollector.outputData(str);
                      
            if (numResets != 0)
            {
              ProfilerAPI.resetData();
            }
            
            if (numResets > 0)
            {
              numResets--;
            }
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
  private static String[] traceClassFilter;
  private static String[] allocatingClassTargets;
  private static boolean exactMatch;
  private static boolean trackCollection;  

  public static void beginProfiling(long[] buckets,
                                    String[] allowedPrefixes, 
                                    String[] excludePrefixes, 
                                    boolean exactmatch, 
                                    long gcInterval, 
                                    long periodicInterval,
                                    long outputlimit, 
                                    long sampleevery, 
                                    long numResets, 
                                    boolean traceallocs, 
                                    boolean trackcollection, 
                                    boolean delayprofiling, 
                                    String[] traceclassfilter, 
                                    String[] allocatingclasstargets, 
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
    outputLimit = outputlimit;
    sampleEvery = sampleevery;
    traceAllocs = traceallocs;
    traceClassFilter = traceclassfilter;
    allocatingClassTargets = allocatingclasstargets;
    trackCollection = trackcollection;

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
    if (trackCollection)
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
    
    if (delayprofiling)
    {
      FileOutput.writeOutput("## Profiling delayed");      
    }
    else
    {
      ObjectProfiler.profilingEnabled = true;
    }
  }

  public static void beginPausedProfiling()
  {
    FileOutput.writeOutput("## Profiling enabled");
    ObjectProfiler.profilingEnabled = true;
  }

  public static void pauseProfiling()
  {
    FileOutput.writeOutput("## Profiling disabled");
    ObjectProfiler.profilingEnabled = true;
  }
}
