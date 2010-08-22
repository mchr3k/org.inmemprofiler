package org.inmemprofiler.runtime.data;

import java.util.Formatter;

public class ProfilerData
{
  private final Bucket liveObjects = new Bucket();
  private final BucketContainer collectedObjects;
  public final long[] bucketIntervals;
  
  public ProfilerData(long[] bucketIntervals)
  {
    this.bucketIntervals = bucketIntervals;
    collectedObjects = new BucketContainer(bucketIntervals);
  }

  public Trace newObject(String className, 
                        long size, 
                        Trace trace, 
                        String[] allocatingClassTargets)
  {
    return liveObjects.addObject(className, size, trace, allocatingClassTargets);    
  }
  
  /**
   * Sync: Lock the collected data while an object is moved from the live bucket
   * to a collected bucket.
   * @param className
   * @param size
   * @param trace
   * @param lifetime
   * @param allocatingClassTargets 
   */
  public synchronized void collectObject(String className, 
                                         long size, 
                                         Trace trace, 
                                         long lifetime, 
                                         String[] allocatingClassTargets)
  {
    liveObjects.removeObject(className, size, trace, allocatingClassTargets);
    collectedObjects.collectObject(className, size, trace, lifetime, allocatingClassTargets);
  }
  
  public synchronized void outputData(StringBuilder str,
                                      Formatter fmt, 
                                      long outputLimit,
                                      boolean traceAllocs, 
                                      String[] traceClassFilter, 
                                      boolean trackCollection)
  {
    if (trackCollection)
    {
      str.append("Live objects:\n");
      liveObjects.outputData(str, fmt, 1, outputLimit, traceAllocs, traceClassFilter);
      str.append("\n");
      str.append("Collected objects:\n");
      collectedObjects.outputData(str, fmt, 1, outputLimit, traceAllocs, traceClassFilter);
    }
    else
    {
      str.append("Allocated objects:\n");
      liveObjects.outputData(str, fmt, 1, outputLimit, traceAllocs, traceClassFilter);
    }
  }
}
