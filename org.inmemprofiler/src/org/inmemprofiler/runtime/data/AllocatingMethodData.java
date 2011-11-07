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
                         Formatter fmt,
                         long classTotalAlloc)
  {
    double thisAlloc = size.get();
    double totalAlloc = classTotalAlloc;
    double allocPercent = (thisAlloc / totalAlloc) * 100;
    long allocPercentRounded = Math.round(allocPercent);

    str.append(String.format("%3d", allocPercentRounded));
    str.append("%:");
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
