package org.inmemprofiler.runtime.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.data.InstanceBucket.InstanceBucketData;

/**
 * Instance counts split out into one or more instance lifetime buckets and a single live instance bucket.
 * Each instance is placed in the live instances bucket at allocation time and is moved to one of the
 * instance lifetime buckets when it is collected. 
 */
public class AllInstanceBuckets
{
  private final InstanceBucket liveInstanceBucket = new InstanceBucket();
  private Map<Long,InstanceBucket> collectedInstanceBuckets = new ConcurrentHashMap<Long, InstanceBucket>();
  public final long[] bucketIntervals;
  
  public AllInstanceBuckets(long[] buckets)
  {
    this.bucketIntervals = buckets;
    for (long bucketInterval : buckets)
    {
      collectedInstanceBuckets.put(bucketInterval, new InstanceBucket());
    }
  }
    
  public void addLiveInstance(String className, long instanceSize)
  {
    liveInstanceBucket.addInstance(className, instanceSize);
  }
  
  public void addCollectedInstance(String className, long instanceSize, long instanceLifetime)
  {
    liveInstanceBucket.removeInstance(className, instanceSize);
    long bucketIndex = getBucketIndex(instanceLifetime);
    InstanceBucket bucket = collectedInstanceBuckets.get(bucketIndex);
    bucket.addInstance(className, instanceSize);
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
    StringBuilder str = new StringBuilder();
    str.append("\nWarning: This summary is constructed without locks - " +
    		       "a single object could show up in both the live bucket and " +
    		       "one of the collected buckets if it is collected while " +
    		       "this summary is being created.\n");
    str.append("\nSummary:\n");
    
    // Snapshot all buckets
    InstanceBucketData[] snaps = new InstanceBucketData[bucketIntervals.length];
    int ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      InstanceBucket data = collectedInstanceBuckets.get(bucketInterval);
      InstanceBucketData dataSnap = data.getSnapshot();
      snaps[ii] = dataSnap;
      ii++;
    }
    InstanceBucketData liveSnap = liveInstanceBucket.getSnapshot();
    
    // Output bucket summaries
    long lastLong = 0;
    ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      InstanceBucketData dataSnap = snaps[ii];
      if (dataSnap.totalCount > 0)
      {
        str.append(dataSnap.totalSize);
        str.append(":");
        str.append(dataSnap.totalCount);
        str.append("\t: " + lastLong + "(s) - " + printLong(bucketInterval) + "(s)");        
        str.append("\n");
      }
      lastLong = bucketInterval;
      ii++;
    }
    str.append(liveSnap.totalSize);
    str.append(":");
    str.append(liveSnap.totalCount + "\t: live instances\n");    
    str.append("\n");
    
    // Output bucket details
    lastLong = 0;
    ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      InstanceBucketData dataSnap = snaps[ii];
      if (dataSnap.totalCount > 0)
      {
        str.append(dataSnap.totalCount + " instances in bucket " + lastLong + "(s) to " + printLong(bucketInterval) + "(s):\n");
        str.append(dataSnap.str);
        str.append("\n");
      }
      lastLong = bucketInterval;
      ii++;
    }
    str.append(liveSnap.totalCount + " live instances:\n");
    str.append(liveSnap.str);
    return str.toString();
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
