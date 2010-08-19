package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AllocatedClassData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  private final Map<Trace, AllocatingTraceData> traces = new ConcurrentHashMap<Trace, AllocatingTraceData>();
  private final Map<String, AllocatingClassData> allocatingClasses = new ConcurrentHashMap<String, AllocatingClassData>();
  
  public synchronized void addObject(String className, long size, Trace trace)
  { 
  }
  
  public synchronized void removeObject(String className, long size, Trace trace)
  {
  }
  
  public synchronized void outputData(StringBuilder str,
                         Formatter fmt,
                         int indent)
  { 
  }
  
  @Override
  public String toString()
  {
    return size + ":" + count;
  }
}
