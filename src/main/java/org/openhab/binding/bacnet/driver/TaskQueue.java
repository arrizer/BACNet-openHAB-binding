package org.openhab.binding.bacnet.driver;
import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;

public class TaskQueue {
	private ThreadPoolExecutor executor = (ThreadPoolExecutor)java.util.concurrent.Executors.newFixedThreadPool(1);
	private Integer pendingTasks = 0;
	private LinkedList<Task> submittedTasks = new LinkedList<Task>();
	private TaskQueueDelegate delegate;
	
	
	public TaskQueue(TaskQueueDelegate delegate){
		this.delegate = delegate;
	}
	
	private void beginTask(){
		synchronized (pendingTasks) {
			pendingTasks++;
		}
	}
	public void finishTask(Task task){
		synchronized (pendingTasks) {
			pendingTasks--;
		}
		synchronized (submittedTasks) {
			submittedTasks.remove(task);
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
	
	public void submit(Task task){
		if(hasTaskQueued(task)){
			return;
		}
		task.queue = this;
		beginTask();
		synchronized (submittedTasks) {
			submittedTasks.add(task);
		}
		executor.submit(task);
	}
	
	public boolean hasTaskQueued(Task newTask){
		synchronized (submittedTasks) {
			for(Task task : submittedTasks){
				if(task.equals(newTask)){
					return true;
				}
			}
		}
		return false;
	}
	
	public void cancel(){
		executor.shutdownNow();
	}
}
