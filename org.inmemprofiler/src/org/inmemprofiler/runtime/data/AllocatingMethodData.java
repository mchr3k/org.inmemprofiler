package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.concurrent.atomic.AtomicLong;

public class AllocatingMethodData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  
  public void addAllocation(long size)
  { 
    this.count.incrementAndGet();
    this.size.addAndGet(size);
  }
  
  public void removeAllocation(long size)
  {
    this.count.decrementAndGet();
    this.size.addAndGet(-1 * size);
  }
  
  public void outputData(StringBuilder str,
                         Formatter fmt)
  { 
    str.append(size.get());
    str.append(":");
    str.append(count.get());
  }
  
  @Override
  public String toString()
  {
    return size + ":" + count;
  }
}
