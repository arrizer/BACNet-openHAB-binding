package org.openhab.binding.bacnet.driver;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;

@SuppressWarnings("unused")
public class Manager {
    protected LocalDevice localDevice;
    private ManagerDelegate delegate;
    public static final BlockingQueue<RemoteDevice> discoveredDevices = new LinkedBlockingQueue<RemoteDevice>();
    public ConcurrentHashMap<Integer, Device> devices = new ConcurrentHashMap<Integer, Device>(); 

    public Manager(ManagerDelegate delegate) {
        IpNetwork network = new IpNetwork();
        Transport transport = new Transport(network);
        transport.setTimeout(15000);
        transport.setSegTimeout(15000);
        localDevice = new LocalDevice(1337, transport);
        try {
			localDevice.initialize();
			localDevice.getEventHandler().addListener(new Listener());
		} catch (Exception e) {
			e.printStackTrace();
		}
    	this.delegate = delegate;
    }
    
    private class DiscoveryThread extends Thread {
    	@Override
    	public void run() {
    		while(true){
    			try {
					RemoteDevice device = discoveredDevices.take();
					discoveredDevice(device);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
    
    public void discover(){
    	DiscoveryThread thread = new DiscoveryThread();
    	thread.start();
        try {
            localDevice.sendGlobalBroadcast(new WhoIsRequest());
        }
        catch (BACnetException e) {
        	System.err.println("Failed to initialize local BacNet device: " + e);
        }
    }
    
    public void terminate(){
    	localDevice.terminate();
    }
    
    public void addDevice(int instanceID, Address address, OctetString linkService){
    	try {
			RemoteDevice device = localDevice.findRemoteDevice(address, linkService, instanceID);
			discoveredDevices.put(device);
		} catch (BACnetException e) {
			System.err.println("Could not manuall discover device " + instanceID);
		} catch (InterruptedException e) {

		}
    }
    
    private void discoveredDevice(RemoteDevice remoteDevice) {
        try {
        	Integer key = remoteDevice.getObjectIdentifier().getInstanceNumber();
    		if(!devices.containsKey(key)){
    			remoteDevice.setSegmentationSupported(Segmentation.noSegmentation);
            	getExtendedDeviceInformation(remoteDevice);
            	@SuppressWarnings("unchecked")
				List<ObjectIdentifier> objectIDs = ((SequenceOf<ObjectIdentifier>) RequestUtils.sendReadPropertyAllowNull(localDevice, remoteDevice, 
						remoteDevice.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();
            	Device device = new Device(this, remoteDevice, objectIDs);
            	devices.put(key, device);
            	delegate.managerDidDiscoverRemoteDevice(this, device);
    		}
        }
        catch (BACnetException e) {
            e.printStackTrace();
        }
    }
    
    private void getExtendedDeviceInformation(RemoteDevice device) {
    	try{
    		ObjectIdentifier oid = device.getObjectIdentifier();
    		ReadPropertyAck ack = (ReadPropertyAck) localDevice.send(device, new ReadPropertyRequest(oid, PropertyIdentifier.protocolServicesSupported));
    		device.setServicesSupported((ServicesSupported) ack.getValue());
    		ack = (ReadPropertyAck) localDevice.send(device, new ReadPropertyRequest(oid, PropertyIdentifier.protocolVersion));
    		device.setProtocolVersion((UnsignedInteger) ack.getValue());
    	}catch (BACnetException e){
    		
    	}
    }
    
    private class Listener extends DeviceEventAdapter {
        @Override
        public void iAmReceived(RemoteDevice device) {
        	try {
				discoveredDevices.put(device);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }
}
