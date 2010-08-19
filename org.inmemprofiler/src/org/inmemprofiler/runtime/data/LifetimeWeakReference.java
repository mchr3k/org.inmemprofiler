package org.inmemprofiler.runtime.data;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class LifetimeWeakReference extends WeakReference<Object>
{
  public final String className;
  public final long creationTime;
  public final long size;
  public final ProfilerData data;
  public final Trace trace;

  public LifetimeWeakReference(Object referent, 
                               ReferenceQueue<Object> q,
                               String className,
                               long creationTime, 
                               long size,
                               Trace trace,
                               ProfilerData data)
  {
    super(referent, q);
    this.className = className;
    this.creationTime = creationTime;
    this.size = size;
    this.trace = trace;
    this.data = data;
  }

}
