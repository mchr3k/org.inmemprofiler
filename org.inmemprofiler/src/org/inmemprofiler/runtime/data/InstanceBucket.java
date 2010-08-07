package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single bucket of instance counts.
 */
public class InstanceBucket
{
  private static class ClassStats
  {
    public final AtomicLong count = new AtomicLong();
    public final AtomicLong size = new AtomicLong();
    
    @Override
    public String toString()
    {
      return size + ":" + count;
    }
  }
  
  public final AtomicLong totalContents = new AtomicLong();
  public final AtomicLong totalSize = new AtomicLong();
  private final Map<String, ClassStats> allClassStats = new ConcurrentHashMap<String, ClassStats>(); 
  
  public void addInstance(String className, long instanceSize)
  {
    totalContents.incrementAndGet();
    totalSize.addAndGet(instanceSize);
    ClassStats classStats = allClassStats.get(className);
    if (classStats == null)
    {
      classStats = new ClassStats();
      classStats.count.set(1);
      classStats.size.set(instanceSize);
      allClassStats.put(className, classStats);
    }
    else
    {
      classStats.count.incrementAndGet();
      classStats.size.addAndGet(instanceSize);
    }    
  }
  
  public void removeInstance(String className, long instanceSize)
  {
    totalContents.decrementAndGet();
    totalSize.addAndGet(-1 * instanceSize);
    ClassStats classStats = allClassStats.get(className);
    if (classStats != null)
    {
      classStats.size.addAndGet(-1 * instanceSize);
      long val = classStats.count.decrementAndGet();
      if (val == 0)
      {
        allClassStats.remove(className);
      }
    }    
  }
  
  public InstanceBucketData getSnapshot()
  {
    List<Entry<String, ClassStats>> list = new LinkedList<Entry<String, ClassStats>>(allClassStats.entrySet());
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
    
    long totalInstances = 0;
    StringBuilder str = new StringBuilder("size:count\n");
    for (Entry<String, ClassStats> instanceEntry : list)
    {
      ClassStats classStats = instanceEntry.getValue();
      totalInstances += classStats.count.get();
      str.append(classStats);
      str.append("\t: ");
      str.append(instanceEntry.getKey());
      str.append("\n");
    }
    return new InstanceBucketData(str.toString(), totalInstances);
  }
  
  @Override
  public String toString()
  {
    return getSnapshot().str;
  }
  
  public static class InstanceBucketData
  {
    public final String str;
    public final long totalCount;
    
    public InstanceBucketData(String str, long totalCount)
    {
      this.str = str;
      this.totalCount = totalCount;
    }
  }

}
