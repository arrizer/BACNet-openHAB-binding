package org.openhab.binding.bacnet.driver;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

public class BacNetTaskQueue {
	private ExecutorService executor = null;
	private Integer pendingTasks = 0;
	private LinkedBlockingQueue<BacNetTask> waitingTasks = new LinkedBlockingQueue<BacNetTask>();
	private BacNetTaskQueueDelegate delegate;
	
	public BacNetTaskQueue(BacNetTaskQueueDelegate delegate){
		this.delegate = delegate;
	}
	
	private void beginTask(){
		synchronized (pendingTasks) {
			pendingTasks++;
		}
	}
	public void finishTask(BacNetTask task){
		synchronized (pendingTasks) {
			pendingTasks--;
		}
		delegate.queueDidFinishTask(this, task);
		if(pendingTasks == 0){
			delegate.queueDrained(this);
		}
	}
	
	public int getPendingRequests(){
		synchronized (pendingTasks) {
			return pendingTasks;
		}
	}
	
	public void submit(BacNetTask task){
		task.queue = this;
		beginTask();
		waitingTasks.add(task);
		//executor.submit(task);
	}
	
	public void start(){
		if(executor == null){
			executor = java.util.concurrent.Executors.newFixedThreadPool(1);
		}
		while(!(waitingTasks.isEmpty())){
			executor.submit(waitingTasks.remove());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void stop(){
		waitingTasks.addAll((Collection<? extends BacNetTask>) executor.shutdownNow());
	}
}
