package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
                         Formatter fmt)
  {   
    // TODO: Collect bucket summaries
//    str.append("\nBucket Summary:\n");
    
//    long lastLong = 0;
//    ii = 0;
//    for (long bucketInterval : bucketIntervals)
//    {
//      BucketSnapshot dataSnap = snaps[ii];
//      if (dataSnap.totalCount > 0)
//      {
//        str.append(dataSnap.totalSize);
//        str.append(":");
//        str.append(dataSnap.totalCount);
//        str.append("\t: " + lastLong + "(s) - " + printLong(bucketInterval) + "(s)");        
//        str.append("\n");
//      }
//      lastLong = bucketInterval;
//      ii++;
//    }
//    str.append(liveSnap.totalSize);
//    str.append(":");
//    str.append(liveSnap.totalCount + "\t: live instances\n");    
    
    // TODO: Sorting code
//    List<Entry<String, ClassStats>> list = new LinkedList<Entry<String, ClassStats>>(allclassStats.entrySet());
//    Collections.sort(list, new Comparator<Entry<String, ClassStats>>() {
//        @Override
//        public int compare(Entry<String, ClassStats> o1,
//                           Entry<String, ClassStats> o2)
//        {
//          Long o1Val = o1.getValue().size.get();
//          Long o2Val = o2.getValue().size.get();
//          return -1 * o1Val.compareTo(o2Val);
//        }
//    });
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
