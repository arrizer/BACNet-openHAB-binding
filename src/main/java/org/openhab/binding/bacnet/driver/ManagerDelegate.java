package org.openhab.binding.bacnet.driver;


public interface ManagerDelegate {
	public void managerDidDiscoverRemoteDevice(Manager localDevice, Device remoteDevice);
}
