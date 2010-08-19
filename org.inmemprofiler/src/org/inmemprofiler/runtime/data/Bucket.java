package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.inmemprofiler.runtime.util.Util;

/**
 * A single bucket of instance counts.
 */
public class Bucket
{
  private final Map<String, AllocatedClassData> allClassStats = new ConcurrentHashMap<String, AllocatedClassData>(); 
  
  public void addObject(String className, long size, Trace trace)
  {
    AllocatedClassData classStats = allClassStats.get(className);
    if (classStats == null)
    {
      classStats = newClass(className);
    }
    classStats.addObject(className, size, trace);
  }
  
  private synchronized AllocatedClassData newClass(String className)
  {
    AllocatedClassData classStats = allClassStats.get(className);
    if (classStats == null)
    {
      classStats = new AllocatedClassData();
      allClassStats.put(className, classStats);
    }
    return classStats;
  }
  
  public void removeObject(String className, long size, Trace trace)
  {
    AllocatedClassData classStats = allClassStats.get(className);
    if (classStats != null)
    {
      classStats.removeObject(className, size, trace);
    }    
  }
  
  /**
   * Don't lock this method - this means the sorting will be best effort only
   * @param str
   * @param fmt
   * @param indent
   */
  public void outputData(StringBuilder str,
                         Formatter fmt,
                         int indent)
  {    
    List<Entry<String, AllocatedClassData>> sortedClassData = new LinkedList<Entry<String, AllocatedClassData>>(allClassStats.entrySet());
    Collections.sort(sortedClassData, new Comparator<Entry<String, AllocatedClassData>>() {
        @Override
        public int compare(Entry<String, AllocatedClassData> o1,
                           Entry<String, AllocatedClassData> o2)
        {
          Long o1Val = o1.getValue().size.get();
          Long o2Val = o2.getValue().size.get();
          return -1 * o1Val.compareTo(o2Val);
        }
    });
    
    for (Entry<String, AllocatedClassData> classData : sortedClassData)
    {
      Util.indent(str, indent);
      str.append(classData.getKey());
      str.append(":\n");
      classData.getValue().outputData(str, fmt, indent + 1);
      str.append("\n");
    }
  }
}
