package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.inmemprofiler.runtime.util.Util;

public class AllocatedClassData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  private final Map<Trace, AllocatingTraceData> traces = new ConcurrentHashMap<Trace, AllocatingTraceData>();
  private final Map<String, AllocatingClassData> allocatingClasses = new ConcurrentHashMap<String, AllocatingClassData>();
  
  public synchronized void addObject(String className, long size, Trace trace)
  { 
    this.count.incrementAndGet();
    this.size.addAndGet(size);
    
    AllocatingTraceData traceData = traces.get(trace);
    if (traceData == null)
    {
      traceData = new AllocatingTraceData();
      traces.put(trace, traceData);
    }    
    traceData.addObject(className, size, trace);
    
    for (StackTraceElement element : trace.stackFrames)
    {
      String allocatingClassName = element.getClassName();
      AllocatingClassData classData = allocatingClasses.get(allocatingClassName);
      if (classData == null)
      {
        classData = new AllocatingClassData();
        allocatingClasses.put(allocatingClassName, classData);
        classData.addMethod(allocatingClassName, size, element.getMethodName());
      }          
    }
  }
  
  public synchronized void removeObject(String className, long size, Trace trace)
  {
    this.count.decrementAndGet();
    this.size.addAndGet(-1 * size);
  }
  
  public synchronized void outputData(StringBuilder str,
                         Formatter fmt,
                         int indent)
  {
    Util.indent(str, indent);
    str.append(size.get());
    str.append(":");
    str.append(count.get());
    str.append("\n");
    
    str.append("\n");
    str.append("Allocation Sites:\n");
    List<Entry<Trace, AllocatingTraceData>> sortedTraces = new LinkedList<Entry<Trace, AllocatingTraceData>>(traces.entrySet());
    Collections.sort(sortedTraces, new Comparator<Entry<Trace, AllocatingTraceData>>() {
        @Override
        public int compare(Entry<Trace, AllocatingTraceData> o1,
                           Entry<Trace, AllocatingTraceData> o2)
        {
          Long o1Val = o1.getValue().size.get();
          Long o2Val = o2.getValue().size.get();
          return -1 * o1Val.compareTo(o2Val);
        }
    });    
    for (Entry<Trace, AllocatingTraceData> traceData : sortedTraces)
    {      
      traceData.getValue().outputData(str, fmt, indent + 1);
      
      appendStackTrace(str, traceData.getKey(), indent + 1);
    }
    
    str.append("\n");
    str.append("Allocation Classes:\n");
    List<Entry<String, AllocatingClassData>> sortedAllocClasses = new LinkedList<Entry<String, AllocatingClassData>>(allocatingClasses.entrySet());
    Collections.sort(sortedAllocClasses, new Comparator<Entry<String, AllocatingClassData>>() {
        @Override
        public int compare(Entry<String, AllocatingClassData> o1,
                           Entry<String, AllocatingClassData> o2)
        {
          Long o1Val = o1.getValue().size.get();
          Long o2Val = o2.getValue().size.get();
          return -1 * o1Val.compareTo(o2Val);
        }
    });    
    for (Entry<String, AllocatingClassData> classData : sortedAllocClasses)
    {      
      classData.getValue().outputData(str, fmt, indent + 1);
    }
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

