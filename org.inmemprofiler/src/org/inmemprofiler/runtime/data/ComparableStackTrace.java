package org.inmemprofiler.runtime.data;

public class ComparableStackTrace
{
  public final StackTraceElement[] stackFrames;
  private int hashCode = 0;
  private boolean computedHashcode = false;

  public ComparableStackTrace(StackTraceElement[] stackFrames)
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
    if (obj instanceof ComparableStackTrace)
    {
      ComparableStackTrace otherKey = (ComparableStackTrace) obj;
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
}
