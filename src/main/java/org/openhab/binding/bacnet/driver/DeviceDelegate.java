package org.openhab.binding.bacnet.driver;

public interface DeviceDelegate {
	public void deviceDidDiscoverSelf(Device device);
	public void deviceDidDiscoverEndpoint(Device device, Endpoint endpoint);
	public void deviceDidFinishDiscoveringAllEndpoints(Device device);
}
