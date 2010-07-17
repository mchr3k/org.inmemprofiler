package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BucketData
{
  public final AtomicLong totalContents = new AtomicLong();
  private final Map<String, AtomicLong> instanceCounts = new ConcurrentHashMap<String, AtomicLong>(); 
  
  public synchronized void addData(String className)
  {
    totalContents.incrementAndGet();
    AtomicLong instanceCount = instanceCounts.get(className);
    if (instanceCount == null)
    {
      instanceCount = new AtomicLong(1);
      instanceCounts.put(className, instanceCount);
    }
    else
    {
      instanceCount.incrementAndGet();
    }    
  }
  
  public synchronized void removeData(String className)
  {
    totalContents.decrementAndGet();
    AtomicLong instanceCount = instanceCounts.get(className);
    if (instanceCount != null)
    {
      long val = instanceCount.decrementAndGet();
      if (val == 0)
      {
        instanceCounts.remove(className);
      }
    }    
  }
  
  @Override
  public String toString()
  {
    List<Entry<String, AtomicLong>> list = new LinkedList<Entry<String, AtomicLong>>(instanceCounts.entrySet());
    Collections.sort(list, new Comparator<Entry<String, AtomicLong>>() {
        @Override
        public int compare(Entry<String, AtomicLong> o1,
                           Entry<String, AtomicLong> o2)
        {
          Long o1Val = o1.getValue().get();
          Long o2Val = o2.getValue().get();
          return -1 * o1Val.compareTo(o2Val);
        }
    });
    
    StringBuilder str = new StringBuilder();
    for (Entry<String, AtomicLong> instanceEntry : list)
    {
      str.append(instanceEntry.getValue());
      str.append("\t: ");
      str.append(instanceEntry.getKey());
      str.append("\n");
    }
    return str.toString();
  }

}
