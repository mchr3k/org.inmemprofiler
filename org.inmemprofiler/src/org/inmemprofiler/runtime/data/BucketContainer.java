package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.util.Util;

/**
 * Instance counts split out into one or more instance lifetime buckets and a single live instance bucket.
 * Each instance is placed in the live instances bucket at allocation time and is moved to one of the
 * instance lifetime buckets when it is collected. 
 */
public class BucketContainer
{
  private Map<Long,Bucket> collectedInstanceBuckets = new ConcurrentHashMap<Long, Bucket>();
  public final long[] bucketIntervals;
  
  public BucketContainer(long[] buckets)
  {
    this.bucketIntervals = buckets;
    for (long bucketInterval : buckets)
    {
      collectedInstanceBuckets.put(bucketInterval, new Bucket());
    }
  }
  
  public void collectObject(String className, 
                                         long size, 
                                         Trace trace, 
                                         long lifetime)
  {
    long bucketIndex = getBucketIndex(lifetime);
    Bucket bucket = collectedInstanceBuckets.get(bucketIndex);
    bucket.addObject(className, size, trace);  
  }

  private long getBucketIndex(long instanceLifetime)
  {
    for (long bucketInterval : bucketIntervals)
    {
      if (instanceLifetime < bucketInterval)
      {
        return bucketInterval;
      }
    }
    return bucketIntervals[bucketIntervals.length - 1];
  }
  
  public void outputData(StringBuilder str,
                         Formatter fmt,
                         int indent, 
                         long outputLimit, 
                         boolean traceAllocs, 
                         String[] traceClassFilter)
  {   
    long lastLong = 0;
    for (int ii = 0; ii < bucketIntervals.length; ii++)
    {
      long bucketInterval = bucketIntervals[ii];
      
      Util.indent(str, indent);
      str.append(lastLong);
      str.append("(s) - ");
      str.append(printLong(bucketInterval));
      str.append("(s) :\n");
      
      Bucket bucket = collectedInstanceBuckets.get(bucketInterval);
      bucket.outputData(str, fmt, indent + 1, outputLimit, traceAllocs, traceClassFilter);
      
      lastLong = bucketInterval;
    }    
  }
  
  private String printLong(long val)
  {
    if (val == Long.MAX_VALUE)
    {
      return "inf";
    }
    else
    {
      return Long.toString(val);
    }
  }
}
