package org.inmemprofiler.runtime;

public interface ProfilerAPIMBean
{
  public void beginProfilingJMX(String allArgs);

  public void endProfilingJMX();

  public void beginPausedProfilingJMX();

  public void pauseProfilingJMX();

  public void resetDataJMX();

  public void outputDataJMX();
}
