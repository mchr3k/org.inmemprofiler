package org.inmemprofiler.runtime.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AllBucketData
{
  private final BucketData liveInstances = new BucketData();
  private Map<Long,BucketData> collectedInstances = new ConcurrentHashMap<Long, BucketData>();
  private final long[] bucketIntervals;
  
  public AllBucketData(long[] buckets)
  {
    this.bucketIntervals = buckets;
    for (long bucketInterval : buckets)
    {
      collectedInstances.put(bucketInterval, new BucketData());
    }
  }
    
  public synchronized void addLiveInstance(String className)
  {
    liveInstances.addData(className);
  }
  
  public synchronized void addCollectedInstance(String className, long instanceLifetime)
  {
    liveInstances.removeData(className);
    long bucketIndex = getBucketIndex(instanceLifetime);
    BucketData bucket = collectedInstances.get(bucketIndex);
    bucket.addData(className);
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
  
  public String toString()
  {
    StringBuilder str = new StringBuilder("\nSummary:\n");
    long lastLong = 0;
    for (long bucketInterval : bucketIntervals)
    {
      BucketData data = collectedInstances.get(bucketInterval);
      if (data.totalContents.get() > 0)
      {
        str.append(data.totalContents);
        str.append("\t: " + lastLong + "s - " + bucketInterval + "s");        
        str.append("\n");
      }
      lastLong = bucketInterval;
    }
    str.append(liveInstances.totalContents + "\t: live objects\n");    
    str.append("\n");
    lastLong = 0;
    for (long bucketInterval : bucketIntervals)
    {
      BucketData data = collectedInstances.get(bucketInterval);
      if (data.totalContents.get() > 0)
      {
        str.append(data.totalContents + " objects in bucket " + lastLong + "s to " + bucketInterval + "s:\n");
        str.append(data);
        str.append("\n");
      }
      lastLong = bucketInterval;
    }
    if (liveInstances.totalContents.get() > 0)
    {
      str.append(liveInstances.totalContents + " live objects:\n");
      str.append(liveInstances);
      str.append("\n");
    }
    return str.toString();
  }
}
