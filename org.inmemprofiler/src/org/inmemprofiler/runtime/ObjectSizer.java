package org.inmemprofiler.runtime;

/**
 * Provides an API for determining the size of an object. This uses the native JVMTI.
 */
public class ObjectSizer
{
  public static native long getObjectSize(Object ref);
}
