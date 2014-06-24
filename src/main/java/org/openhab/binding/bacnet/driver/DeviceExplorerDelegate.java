package org.openhab.binding.bacnet.driver;


public interface DeviceExplorerDelegate {
	public void explorerDidDiscoverDevice(DeviceExplorer explorer, Device device);
}
