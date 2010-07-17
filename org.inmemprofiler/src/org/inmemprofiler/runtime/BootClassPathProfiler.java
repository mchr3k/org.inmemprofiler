package org.inmemprofiler.runtime;

public class BootClassPathProfiler
{
  public static boolean profilingEnabled = false;
  private static boolean profilingAllowed = true;
  
  /**
   * Ideally we would like to allow separate threads to allocate objects concurrently.
   * However, we need to ensure that we don't end up recursing back into this function
   * when an Object is allocated by the {@link ProfilerDataCollector}.
   * <p>
   * This could be done on a per thread basis using a ThreadLocal member variable. However,
   * I have so far failed to get this approach to work as ThreadLocal.get() can result
   * in an Object creation.
   * <p>
   * This method is therefore synchronized which has the unfortunate side effect of serializing
   * all object creations. 
   * @param ref
   */
  public static synchronized void newObject(Object ref)
  {
    if (profilingEnabled)
    {
      if (profilingAllowed &&
          ProfilerDataCollector.isProfilingAllowed())
      {
        try
        {
          profilingAllowed = false;
          ProfilerDataCollector.profileNewObject(ref);
        }
        finally
        {
          profilingAllowed = true;
        }
      }
    }
  }
}
