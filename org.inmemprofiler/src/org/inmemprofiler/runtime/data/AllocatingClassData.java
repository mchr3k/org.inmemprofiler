package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AllocatingClassData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  private final Map<String, AllocatingMethodData> methods = new ConcurrentHashMap<String, AllocatingMethodData>();
  
  public void addMethod(String className, long size, String method)
  { 
  }
  
  public void removeMethod(String className, long size, String method)
  {
  }
  
  public void outputData(StringBuilder str,
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
