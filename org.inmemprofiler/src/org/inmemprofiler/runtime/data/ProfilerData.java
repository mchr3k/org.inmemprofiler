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

  public void newObject(String className, 
                        long size, 
                        Trace trace)
  {
    
  }
  
  /**
   * Sync: Lock the collected data while an object is moved from the live bucket
   * to a collected bucket.
   * @param className
   * @param size
   * @param trace
   * @param lifetime
   */
  public synchronized void collectObject(String className, 
                                         long size, 
                                         Trace trace, 
                                         long lifetime)
  {
    
  }
  
  public synchronized void outputData(StringBuilder str,
                                      Formatter fmt)
  {
  
  }
}
