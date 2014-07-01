package org.openhab.binding.bacnet.driver;

public interface TaskQueueDelegate {
	public void queueDidFinishTask(TaskQueue queue, Task task);
	public void queueDrained(TaskQueue queue);
}
