package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.inmemprofiler.runtime.util.Util;

public class AllocatedClassData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  private final Map<Trace, AllocatingTraceData> traces = new ConcurrentHashMap<Trace, AllocatingTraceData>();
  private final Map<Trace, Trace> canonicalTraces = new ConcurrentHashMap<Trace, Trace>();
  private final Map<String, AllocatingClassData> allocatingClasses = new ConcurrentHashMap<String, AllocatingClassData>();
  
  public synchronized Trace addObject(String className, 
                                      long size, 
                                      Trace trace, 
                                      String[] allocatingClassTargets)
  { 
    this.count.incrementAndGet();
    this.size.addAndGet(size);
    
    if (trace != null)
    {
      AllocatingTraceData traceData = traces.get(trace);
      if (traceData == null)
      {
        traceData = new AllocatingTraceData();
        traces.put(trace, traceData);
        canonicalTraces.put(trace, trace);
      }    
      trace = canonicalTraces.get(trace);
      traceData.addObject(size);
      
      Map<String,Set<String>> perClassMethods = Trace.getPerClassMethods(trace, 
                                                                   allocatingClassTargets);
      for (Entry<String,Set<String>> element : perClassMethods.entrySet())
      {
        String allocatingClassName = element.getKey();
        AllocatingClassData classData = allocatingClasses.get(allocatingClassName);
        if (classData == null)
        {
          classData = new AllocatingClassData();
          allocatingClasses.put(allocatingClassName, classData);        
        }          
        classData.addMethods(size, element.getValue());
      }
    }
    
    return trace;
  }

  public synchronized void removeObject(String className, 
                                        long size, 
                                        Trace trace, 
                                        String[] allocatingClassTargets)
  {
    this.count.decrementAndGet();
    this.size.addAndGet(-1 * size);
    
    if (trace != null)
    {
      AllocatingTraceData traceData = traces.get(trace);
      if (traceData != null)
      {
        traceData.removeObject(size);
      }    
      
      Map<String,Set<String>> perClassMethods = Trace.getPerClassMethods(trace,
                                                                   allocatingClassTargets);
      for (Entry<String,Set<String>> element : perClassMethods.entrySet())
      {
        String allocatingClassName = element.getKey();
        AllocatingClassData classData = allocatingClasses.get(allocatingClassName);
        if (classData != null)
        {
          classData.removeMethods(size, element.getValue());
        }          
      }
    }
  }
  
  public synchronized void outputData(String className,
                                      StringBuilder str,
                                      Formatter fmt,
                                      int indent, 
                                      long outputLimit, 
                                      boolean traceAllocs, 
                                      String[] traceClassFilter)
  {
    if (count.get() == 0)
    {
      return;
    }
    
    Util.indent(str, indent - 1);
    str.append(size.get());
    str.append(":");
    str.append(count.get());
    str.append(" - ");
    str.append(className);
    str.append("\n");
    
    if (traceAllocs)
    {
      str.append("\n");
      Util.indent(str, indent);
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
      int outputCount = 0;
      for (Entry<Trace, AllocatingTraceData> traceData : sortedTraces)
      {      
        if (outputCount >= outputLimit)
        {
          break;
        }
        
        traceData.getValue().outputData(traceData.getKey(), str, fmt, indent + 1);
        
        outputCount++;
      }
      
      str.append("\n");
      Util.indent(str, indent);
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
      outputCount = 0;
      for (Entry<String, AllocatingClassData> classData : sortedAllocClasses)
      {
        if (outputCount >= outputLimit)
        {
          break;
        }
        
        if ((traceClassFilter == null) ||
            passesFilter(classData.getKey(), traceClassFilter))
        {
          classData.getValue().outputData(classData.getKey(), str, fmt, indent + 2, outputLimit);        
          outputCount++;
        }
      }
      
      str.append("\n");
    }
  }
  
  private boolean passesFilter(String key, String[] traceClassFilter)
  {
    for (String filter : traceClassFilter)
    {
      if (key.startsWith(filter))
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString()
  {
    return size + ":" + count;
  }
}

