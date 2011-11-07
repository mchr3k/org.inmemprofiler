package org.inmemprofiler.runtime.data;

import java.util.Formatter;
import java.util.concurrent.atomic.AtomicLong;

import org.inmemprofiler.runtime.util.Util;

public class AllocatingTraceData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();

  public void addObject(long size)
  {
    this.count.incrementAndGet();
    this.size.addAndGet(size);
  }

  public void removeObject(long size)
  {
    this.count.decrementAndGet();
    this.size.addAndGet(-1 * size);
  }

  public void outputData(Trace trace,
                         StringBuilder str,
                         Formatter fmt,
                         int indent,
                         long classTotalAlloc)
  {
    if (count.get() == 0)
    {
      return;
    }

    double thisAlloc = size.get();
    double totalAlloc = classTotalAlloc;
    double allocPercent = (thisAlloc / totalAlloc) * 100;
    long allocPercentRounded = Math.round(allocPercent);

    str.append("\n");
    Util.indent(str, indent);
    str.append(allocPercentRounded);
    str.append("%:");
    str.append(size.get());
    str.append(":");
    str.append(count.get());
    str.append("\n");

    appendStackTrace(str, trace, indent);
  }

  private void appendStackTrace(StringBuilder str, Trace key, int indent)
  {
    for (StackTraceElement frame : key.stackFrames)
    {
      Util.indent(str, indent);
      str.append("  ");
      str.append(frame);
      str.append("\n");
    }
  }

  @Override
  public String toString()
  {
    return size + ":" + count;
  }
}
