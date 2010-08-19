package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single bucket of instance counts.
 */
public class Bucket
{
  public final AtomicLong totalContents = new AtomicLong();
  public final AtomicLong totalSize = new AtomicLong();
  private final Map<String, AllocatedClassData> allClassStats = new ConcurrentHashMap<String, AllocatedClassData>(); 
  
  public void addObject(String className, long size, Trace trace)
  {
    totalContents.incrementAndGet();
    totalSize.addAndGet(size);
    AllocatedClassData classStats = allClassStats.get(className);
    if (classStats == null)
    {
      classStats = new AllocatedClassData();
      classStats.count.set(1);
      classStats.size.set(size);
      allClassStats.put(className, classStats);
    }
    else
    {
      classStats.count.incrementAndGet();
      classStats.size.addAndGet(size);
    }    
  }
  
  public void removeObject(String className, long size, Trace trace)
  {
    totalContents.decrementAndGet();
    totalSize.addAndGet(-1 * size);
    AllocatedClassData classStats = allClassStats.get(className);
    if (classStats != null)
    {
      classStats.size.addAndGet(-1 * size);
      long val = classStats.count.decrementAndGet();
      if (val == 0)
      {
        allClassStats.remove(className);
      }
    }    
  }
  
  public void outputData(StringBuilder str,
                         Formatter fmt,
                         int indent)
  { 
  }
}
