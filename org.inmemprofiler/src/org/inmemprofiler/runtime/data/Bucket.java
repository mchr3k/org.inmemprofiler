package org.inmemprofiler.runtime.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single bucket of instance counts.
 */
public class Bucket
{
  private final Map<String, AllocatedClassData> perClassData = new ConcurrentHashMap<String, AllocatedClassData>(); 
  
  public Trace addObject(String className, long size, Trace trace, 
                         String[] traceTarget, 
                         String[] traceIgnore)
  {
    AllocatedClassData classData = perClassData.get(className);
    if (classData == null)
    {
      classData = newClass(className);
    }
    return classData.addObject(className, size, trace, traceTarget, traceIgnore);
  }
  
  private synchronized AllocatedClassData newClass(String className)
  {
    AllocatedClassData classData = perClassData.get(className);
    if (classData == null)
    {
      classData = new AllocatedClassData();
      perClassData.put(className, classData);
    }
    return classData;
  }
  
  public void removeObject(String className, long size, Trace trace, 
                           String[] traceTarget, String[] traceIgnore)
  {
    AllocatedClassData classData = perClassData.get(className);
    if (classData != null)
    {
      classData.removeObject(className, size, trace, traceTarget, traceIgnore);
    }    
  }
  
  /**
   * Don't lock this method - this means the sorting will be best effort only
   * @param str
   * @param fmt
   * @param indent
   * @param outputLimit 
   * @param traceAllocs 
   * @param traceIgnore 
   */
  public BucketSummary outputData(StringBuilder str,
                                  Formatter fmt,
                                  int indent, 
                                  long outputLimit, 
                                  boolean traceAllocs)
  {    
    BucketSummary summary = new BucketSummary();
    
    List<Entry<String, AllocatedClassData>> sortedClassData = new LinkedList<Entry<String, AllocatedClassData>>(perClassData.entrySet());
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
     
    int outputCount = 0;
    for (Entry<String, AllocatedClassData> classData : sortedClassData)
    {
      if (outputCount >= outputLimit)
      {
        break;
      }
      
      classData.getValue().outputData(classData.getKey(), str, fmt, 
                                      indent + 1, outputLimit, traceAllocs, 
                                      summary);
      
      outputCount++;
    }
    
    return summary;
  }
  
  public static class BucketSummary
  {
    public long count;
    public long size;
  }
}
