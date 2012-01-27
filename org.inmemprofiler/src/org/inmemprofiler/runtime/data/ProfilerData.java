package org.inmemprofiler.runtime.data;

import java.util.Formatter;

import org.inmemprofiler.runtime.data.Bucket.BucketSummary;
import org.inmemprofiler.runtime.util.Util;

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
                        String[] traceTarget, 
                        String[] traceIgnore)
  {
    return liveObjects.addObject(className, size, trace, traceTarget, traceIgnore);    
  }
  
  /**
   * Sync: Lock the collected data while an object is moved from the live bucket
   * to a collected bucket.
   * @param className
   * @param size
   * @param trace
   * @param lifetime
   * @param traceTarget 
   * @param traceIgnore 
   */
  public synchronized void collectObject(String className, 
                                         long size, 
                                         Trace trace, 
                                         long lifetime, 
                                         String[] traceTarget, 
                                         String[] traceIgnore)
  {
    liveObjects.removeObject(className, size, trace, traceTarget, traceIgnore);
    collectedObjects.addObject(className, size, trace, lifetime, traceTarget, traceIgnore);
  }
  
  public synchronized void outputData(StringBuilder str,
                                      Formatter fmt, 
                                      long outputLimit,
                                      boolean traceAllocs, 
                                      boolean trackCollection, 
                                      boolean blameAllocs)
  {
    if (trackCollection)
    {
      str.append("Live objects:\n");
      BucketSummary summary = liveObjects.outputData(str, fmt, 1, outputLimit, 
                                                     traceAllocs, false, blameAllocs);      
      str.append("\n");
      str.append("Live objects summary:\n");
      Util.indent(str, 1);
      str.append(summary.size);
      str.append(":");
      str.append(summary.count);
      str.append("\n\n");
      
      str.append("Collected objects:\n");
      collectedObjects.outputData(str, fmt, 1, outputLimit, traceAllocs, blameAllocs);
    }
    else
    {
      str.append("Allocated objects:\n");
      liveObjects.outputData(str, fmt, 1, outputLimit, traceAllocs, true, blameAllocs);
    }
  }
}
