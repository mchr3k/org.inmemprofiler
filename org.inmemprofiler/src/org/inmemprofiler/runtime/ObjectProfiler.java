package org.inmemprofiler.runtime;

import java.lang.Thread.UncaughtExceptionHandler;

import org.inmemprofiler.runtime.data.PlaceholderHandler;

public class ObjectProfiler
{
  public static Thread[] ignoreThreads = null;
  public static boolean profilingEnabled = false;
  public static final PlaceholderHandler CRITICAL_BLOCK = new PlaceholderHandler();
   
  /**
   * This method calls through to {@link ProfilerDataCollector} to record object allocation.
   * However, we must avoid having this call result in further object allocations which
   * we attempt to record. If this was to happen we would hit a stack overflow.
   * <p>
   * This method relies on setting the UncaughtExceptionHandler as a marker that any
   * further allocations by this thread should not be recorded until the 
   * UncaughtExceptionHandler is restored.
   * <p>
   * The only case in which this would cause a problem is if one of the allocated
   * objects relied on setting the UncaughtExceptionHandler as this update would be
   * lost when this method returned and restored the previous handler. Thankfully, none
   * of the objects currently used by the {@link ProfilerDataCollector} rely on
   * setting the UncaughtExceptionHandler.
   * @param ref
   */
  public static void newObject(Object ref)
  {
    if (profilingEnabled)
    {
      Thread currentThread = Thread.currentThread();
      UncaughtExceptionHandler prevVal = currentThread.getUncaughtExceptionHandler();
      if ((prevVal != CRITICAL_BLOCK) &&
           profileThread(currentThread))
      {
        try
        {
          currentThread.setUncaughtExceptionHandler(CRITICAL_BLOCK);
          
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
