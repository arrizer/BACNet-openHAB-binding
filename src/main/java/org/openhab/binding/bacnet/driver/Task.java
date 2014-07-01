package org.openhab.binding.bacnet.driver;

public abstract class Task implements Runnable{
	public TaskQueue queue;
	public int throttle = 0;
	protected BacNetObject bacNetObject;
	
	public Task(BacNetObject bacNetObject){
		this.bacNetObject = bacNetObject;
	}
	
	protected void finish(){
		if(throttle > 0){
			try {
				Thread.sleep(throttle);
			} catch (InterruptedException e) { }
		}
		queue.finishTask(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Task){
			Task otherTask = (Task)obj;
			return (otherTask.bacNetObject.objectID.equals(this.bacNetObject.objectID));
		}
		return super.equals(obj);
	}
}
