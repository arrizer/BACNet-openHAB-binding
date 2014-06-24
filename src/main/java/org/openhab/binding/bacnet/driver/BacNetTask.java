package org.openhab.binding.bacnet.driver;

public abstract class BacNetTask implements Runnable{
	public BacNetTaskQueue queue;
	
	protected void finish(){
		queue.finishTask(this);
	}
}
