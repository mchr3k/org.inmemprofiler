package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.data.InstanceBucket.BucketSnapshot;
import org.inmemprofiler.runtime.data.InstanceBucket.ClassStats;

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
    
    // Snapshot all buckets
    BucketSnapshot[] snaps = new BucketSnapshot[bucketIntervals.length];
    int ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      InstanceBucket data = collectedInstanceBuckets.get(bucketInterval);
      BucketSnapshot dataSnap = data.getSnapshot();
      snaps[ii] = dataSnap;
      ii++;
    }
    BucketSnapshot liveSnap = liveInstanceBucket.getSnapshot();
    
    // Output bucket summaries
    str.append("\nBucket Summary:\n");
    long lastLong = 0;
    ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      BucketSnapshot dataSnap = snaps[ii];
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
    
    // Compute dead class summary
    Map<String, ClassStats> allclassStats = new HashMap<String, ClassStats>();
    for (BucketSnapshot dataSnap : snaps)
    {
      for (Entry<String, ClassStats> entry : dataSnap.instanceData.entrySet())
      {
        String className = entry.getKey();
        ClassStats statsEntry = entry.getValue();
        ClassStats statsTotal = allclassStats.get(className);
        if (statsTotal == null)
        {
          statsTotal = new ClassStats();
          allclassStats.put(className, statsTotal);
        }
        statsTotal.count.addAndGet(statsEntry.count.get());
        statsTotal.size.addAndGet(statsEntry.size.get());
      }
    }
    
    // Output dead class summary
    List<Entry<String, ClassStats>> list = new LinkedList<Entry<String, ClassStats>>(allclassStats.entrySet());
    Collections.sort(list, new Comparator<Entry<String, ClassStats>>() {
        @Override
        public int compare(Entry<String, ClassStats> o1,
                           Entry<String, ClassStats> o2)
        {
          Long o1Val = o1.getValue().size.get();
          Long o2Val = o2.getValue().size.get();
          return -1 * o1Val.compareTo(o2Val);
        }
    });
    str.append("\nDead Instances Summary:\n");
    for (Entry<String, ClassStats> entry : list)
    {      
      ClassStats stats = entry.getValue();
      str.append(stats.size.get());
      str.append(":");
      str.append(stats.count.get());
      str.append("\t: " + entry.getKey());        
      str.append("\n");
    }
    
    // Output bucket details
    str.append("\n");
    lastLong = 0;
    ii = 0;
    for (long bucketInterval : bucketIntervals)
    {
      BucketSnapshot dataSnap = snaps[ii];
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
