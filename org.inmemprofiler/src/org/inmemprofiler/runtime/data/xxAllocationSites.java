package org.inmemprofiler.runtime.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class xxAllocationSites
{
  /**
   * Map from classname to a Map from {@link Trace} to allocation counts
   */
  private Map<String,Map<Trace,AtomicLong>> classMap = new ConcurrentHashMap<String, Map<Trace,AtomicLong>>();
  
  public void addAllocationSite(String className)
  {
    Map<Trace,AtomicLong> classEntry = getClassEntry(className);
    Trace trace = null;//getTrace();    
    AtomicLong allocationCount = getAllocationCount(trace, classEntry);
    allocationCount.incrementAndGet();
  }
  
  private AtomicLong getAllocationCount(Trace trace,
      Map<Trace, AtomicLong> classEntry)
  {
    AtomicLong count = classEntry.get(trace);
    if (count == null)
    {
      count = new AtomicLong();
      classEntry.put(trace, count);
    }
    return count;
  }
  
  private synchronized Map<Trace,AtomicLong> getClassEntry(String className)
  {
    Map<Trace,AtomicLong> entry = classMap.get(className);
    
    if (entry == null)
    {
      entry = new ConcurrentHashMap<Trace, AtomicLong>();
      classMap.put(className, entry);
    }
    
    return entry;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sitesStr = new StringBuilder("\nAllocation Sites: \n");
    for (Entry<String,Map<Trace,AtomicLong>> classEntry : classMap.entrySet())
    {
      sitesStr.append(classEntry.getKey());
      sitesStr.append(":\n");
      for (Entry<Trace,AtomicLong> siteEntry : classEntry.getValue().entrySet())
      {
        sitesStr.append(siteEntry.getValue().get());
        sitesStr.append(" allocations with stack trace:\n");
        appendStackTrace(sitesStr, siteEntry.getKey());
      }
    }
    return sitesStr.toString();
  }

  private void appendStackTrace(StringBuilder sitesStr, Trace key)
  {
    for (StackTraceElement frame : key.stackFrames)
    {
      sitesStr.append("  ");
      sitesStr.append(frame);
      sitesStr.append("\n");
    }
  }
}
