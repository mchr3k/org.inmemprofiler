package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.concurrent.atomic.AtomicLong;

public class AllocatingMethodData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  
  public void addObject(String className, long size, Trace trace)
  { 
  }
  
  public void removeObject(String className, long size, Trace trace)
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
