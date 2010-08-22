package org.inmemprofiler.runtime.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Trace
{
  public final StackTraceElement[] stackFrames;
  private int hashCode = 0;
  private boolean computedHashcode = false;

  public Trace(StackTraceElement[] stackFrames)
  {
    this.stackFrames = stackFrames;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj instanceof Trace)
    {
      Trace otherKey = (Trace) obj;
      if (otherKey.stackFrames == stackFrames)
      {
        return true;
      }
      if ((otherKey.stackFrames != null) && (stackFrames != null))
      {
        if (otherKey.stackFrames.length != stackFrames.length)
        {
          return false;
        }
        int ii = 0;
        for (StackTraceElement otherFrame : otherKey.stackFrames)
        {
          if ((stackFrames[ii] == null) &&
              (otherFrame == null))
          {
            // Both null
          }
          else if ((stackFrames[ii] != null) &&
                   (otherFrame != null))
          {
            // Both non null            
            if (!stackFrames[ii].equals(otherFrame))
            {
              return false;
            }
          }
          else
          {
            // One is null, the other isn't
            return false;
          }
          ii++;
        }
        return true;
      }
    }
    return false;
  }
  
  @Override
  public int hashCode()
  {
    if (!computedHashcode)
    {
      if (stackFrames == null)
      {
        hashCode = super.hashCode();
      }
      else
      {
        int newHashCode = 0;
        for (StackTraceElement frame : stackFrames)
        {
          if (frame != null)
          {
            newHashCode += frame.hashCode(); 
          }        
        }
        hashCode = newHashCode;
      }
      computedHashcode = true;
    }
    return hashCode;
  }
  
  public static Trace getTrace(int ignoreDepth, String className)
  {
    Exception ex = new Exception();
    StackTraceElement[] stackTrace = ex.getStackTrace();
        
    if (!className.startsWith("["))
    {
      // Non array object - count number of frames until constructor frame
      for (int ii = ignoreDepth; ii < stackTrace.length; ii++)
      {
        if (className.equals(stackTrace[ii].getClassName()) &&
            "<init>".equals(stackTrace[ii].getMethodName()))
        {
          ignoreDepth = ii + 1;
          break;
        }
      }
    }
    
    StackTraceElement[] fixedTrace = new StackTraceElement[stackTrace.length - ignoreDepth];
    for (int ii = ignoreDepth; ii < stackTrace.length; ii++)
    {
      fixedTrace[ii-ignoreDepth] = new StackTraceElement(stackTrace[ii].getClassName(),
                                                         stackTrace[ii].getMethodName(),
                                                         stackTrace[ii].getFileName(),
                                                         -1);
    }
    
    Trace trace = new Trace(fixedTrace);
    return trace;
  }
    
  public static Map<String,Set<String>> getPerClassMethods(Trace trace, 
                                                     String[] allocatingClassTargets)
  {
    Map<String,Set<String>> perClassMethods = new HashMap<String, Set<String>>(1);
    
    String lastMatchingTarget = null;
    for (StackTraceElement element : trace.stackFrames)
    {
      String className = element.getClassName();
      
      String matchingTarget = getMatchingTarget(className, allocatingClassTargets);
      if (lastMatchingTarget == null)
      {
        lastMatchingTarget = matchingTarget;
      }
      else if (!lastMatchingTarget.equals(matchingTarget))
      {
        break;
      }
      
      Set<String> methods = perClassMethods.get(className);
      if (methods == null)
      {
        methods = new HashSet<String>(1);
        perClassMethods.put(className, methods);
      }
      methods.add(element.getMethodName());
    }
    
    return perClassMethods;
  }
  
  public static String getMatchingTarget(String className,
                                   String[] allocatingClassTargets)
  {
    for (String target : allocatingClassTargets)
    {
      if (className.startsWith(target))
      {
        return target;
      }
    }
    return null;
  }
}
