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

public class AllocatingClassData
{
  public final AtomicLong count = new AtomicLong();
  public final AtomicLong size = new AtomicLong();
  private final Map<String, AllocatingMethodData> methods = new ConcurrentHashMap<String, AllocatingMethodData>();
  
  public void addMethods(long size, Set<String> allocMethods)
  { 
    this.count.incrementAndGet();
    this.size.addAndGet(size);
    
    for (String method : allocMethods)
    {
      AllocatingMethodData methodData = methods.get(method);
      if (methodData == null)
      {
        methodData = new AllocatingMethodData();
        methods.put(method, methodData);      
      }    
      methodData.addAllocation(size);
    }
  }
  
  public void removeMethods(long size, Set<String> allocMethods)
  {
    this.count.decrementAndGet();
    this.size.addAndGet(-1 * size);
    
    for (String method : allocMethods)
    {
      AllocatingMethodData methodData = methods.get(method);
      if (methodData != null)
      {
        methodData.removeAllocation(size);
      }
    }
  }
  
  public void outputData(String className, 
                         StringBuilder str,
                         Formatter fmt,
                         int indent, 
                         long outputLimit)
  {
    if (size.get() == 0)
    {
      return;
    }
    
    Util.indent(str, indent - 1);
    str.append(className);
    str.append("\n");    
    
    Util.indent(str, indent);    
    str.append(size.get());
    str.append(":");
    str.append(count.get());
    str.append(" - All methods\n");
    
    List<Entry<String, AllocatingMethodData>> sortedMethods = new LinkedList<Entry<String, AllocatingMethodData>>(methods.entrySet());
    Collections.sort(sortedMethods, new Comparator<Entry<String, AllocatingMethodData>>() {
        @Override
        public int compare(Entry<String, AllocatingMethodData> o1,
                           Entry<String, AllocatingMethodData> o2)
        {
          Long o1Val = o1.getValue().size.get();
          Long o2Val = o2.getValue().size.get();
          return -1 * o1Val.compareTo(o2Val);
        }
    }); 
    int outputCount = 0;
    for (Entry<String, AllocatingMethodData> methodData : sortedMethods)
    {
      if (outputCount >= outputLimit)
      {
        break;
      }
      
      Util.indent(str, indent + 1);      
      methodData.getValue().outputData(str, fmt);      
      str.append(" - ");
      str.append(methodData.getKey());
      str.append("\n");
            
      outputCount++;
    }
  }
  
  @Override
  public String toString()
  {
    return size + ":" + count;
  }
}
