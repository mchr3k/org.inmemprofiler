package org.inmemprofiler.runtime.data;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


public class AllocationSites
{
  /**
   * Map from classname to a Map from {@link ComparableStackTrace} to allocation counts
   */
  private Map<String,Map<ComparableStackTrace,AtomicLong>> classMap = new ConcurrentHashMap<String, Map<ComparableStackTrace,AtomicLong>>();
  
  public void addAllocationSite(String className)
  {
    Map<ComparableStackTrace,AtomicLong> classEntry = getClassEntry(className);
    ComparableStackTrace trace = getTrace();    
    AtomicLong allocationCount = getAllocationCount(trace, classEntry);
    allocationCount.incrementAndGet();
  }
  
  private AtomicLong getAllocationCount(ComparableStackTrace trace,
      Map<ComparableStackTrace, AtomicLong> classEntry)
  {
    AtomicLong count = classEntry.get(trace);
    if (count == null)
    {
      count = new AtomicLong();
      classEntry.put(trace, count);
    }
    return count;
  }

  private ComparableStackTrace getTrace()
  {
    Exception ex = new Exception();
    StackTraceElement[] stackTrace = ex.getStackTrace();
    StackTraceElement[] fixedTrace = new StackTraceElement[stackTrace.length - 4];
    for (int ii = 4; ii < stackTrace.length; ii++)
    {
      fixedTrace[ii-4] = stackTrace[ii];
    }
    ComparableStackTrace trace = new ComparableStackTrace(fixedTrace);
    return trace;
  }
  
  private synchronized Map<ComparableStackTrace,AtomicLong> getClassEntry(String className)
  {
    Map<ComparableStackTrace,AtomicLong> entry = classMap.get(className);
    
    if (entry == null)
    {
      entry = new ConcurrentHashMap<ComparableStackTrace, AtomicLong>();
      classMap.put(className, entry);
    }
    
    return entry;
  }
  
  @Override
  public String toString()
  {
    StringBuilder sitesStr = new StringBuilder("\nAllocation Sites: \n");
    for (Entry<String,Map<ComparableStackTrace,AtomicLong>> classEntry : classMap.entrySet())
    {
      sitesStr.append(classEntry.getKey());
      sitesStr.append(":\n");
      for (Entry<ComparableStackTrace,AtomicLong> siteEntry : classEntry.getValue().entrySet())
      {
        sitesStr.append(siteEntry.getValue().get());
        sitesStr.append(" allocations with stack trace:\n");
        appendStackTrace(sitesStr, siteEntry.getKey());
      }
    }
    return sitesStr.toString();
  }

  private void appendStackTrace(StringBuilder sitesStr, ComparableStackTrace key)
  {
    for (StackTraceElement frame : key.stackFrames)
    {
      sitesStr.append("  ");
      sitesStr.append(frame);
      sitesStr.append("\n");
    }
  }
}
