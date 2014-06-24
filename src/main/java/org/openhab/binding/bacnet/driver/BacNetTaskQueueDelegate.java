package org.openhab.binding.bacnet.driver;

public interface BacNetTaskQueueDelegate {
	public void queueDidFinishTask(BacNetTaskQueue queue, BacNetTask task);
	public void queueDrained(BacNetTaskQueue queue);
}
