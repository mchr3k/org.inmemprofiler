package org.inmemprofiler.runtime;

public class StoppableThread extends Thread
{
	private volatile boolean stopped;

	public StoppableThread() {
		super();
	}
	
	public StoppableThread(Runnable target) {
		super(target);
	}

	public void stopThread() {
		this.stopped = true;
	}
	
	public boolean isStopped() {
		return this.stopped;
	}
}
