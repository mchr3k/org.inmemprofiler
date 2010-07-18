package org.inmemprofiler.runtime;

import java.lang.Thread.UncaughtExceptionHandler;

import org.inmemprofiler.runtime.data.PlaceholderHandler;

public class ObjectProfiler
{
  public static Thread[] ignoreThreads = null;
  public static boolean profilingEnabled = false;
  private static final PlaceholderHandler gHandler = new PlaceholderHandler();
   
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
  public static void newObject(Object ref)
  {
    if (profilingEnabled)
    {
      Thread currentThread = Thread.currentThread();
      UncaughtExceptionHandler prevVal = currentThread.getUncaughtExceptionHandler();
      if ((prevVal != gHandler) &&
           profileThread(currentThread))
      {
        try
        {
          currentThread.setUncaughtExceptionHandler(gHandler);
          
          ProfilerDataCollector.profileNewObject(ref);
        }
        finally
        {
          currentThread.setUncaughtExceptionHandler(prevVal);
        }
      }
    }
  }

  private static boolean profileThread(Thread currentThread)
  {
    if (ignoreThreads == null)
    {
      return true;
    }
    else
    {
      for (Thread th : ignoreThreads)
      {
        if (th == currentThread)
        {
          return false;
        }
      }
      return true;
    }
  }
}
