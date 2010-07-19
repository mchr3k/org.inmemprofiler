package org.inmemprofiler.runtime.data;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public class LifetimeWeakReference extends WeakReference<Object>
{
  public final String className;
  public final long creationTime;
  public final AllInstanceBuckets bucketInstances;

  public LifetimeWeakReference(Object referent, 
                               ReferenceQueue<Object> q,
                               String className,
                               long creationTime, 
                               AllInstanceBuckets bucketInstances)
  {
    super(referent, q);
    this.className = className;
    this.creationTime = creationTime;
    this.bucketInstances = bucketInstances;
  }

}
