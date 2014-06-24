package org.openhab.binding.bacnet.driver;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyRequest;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RequestUtils;

public class DeviceExplorer {
    private LocalDevice localDevice;
    private DeviceExplorerDelegate delegate;
    public static final BlockingQueue<RemoteDevice> discoveredDevices = new LinkedBlockingQueue<RemoteDevice>();

    public DeviceExplorer(LocalDevice localDevice, DeviceExplorerDelegate delegate) {
    	this.localDevice = localDevice;
    	this.delegate = delegate;
    }
    
    private class ListenerThread extends Thread {
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
    	ListenerThread thread = new ListenerThread();
    	thread.start();
        try {
            localDevice.getEventHandler().addListener(new Listener());
            localDevice.sendGlobalBroadcast(new WhoIsRequest());
        }
        catch (BACnetException e) {
        	System.err.println("Failed to initialize local BacNet device: " + e);
        }
    }
    
    class Listener extends DeviceEventAdapter {
        @Override
        public void iAmReceived(RemoteDevice device) {
        	try {
				discoveredDevices.put(device);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }
    
    private void discoveredDevice(RemoteDevice device) {
        try {
            device.setSegmentationSupported(Segmentation.noSegmentation);
            getExtendedDeviceInformation(device);
            @SuppressWarnings("unchecked")
			List<ObjectIdentifier> objectIDs = ((SequenceOf<ObjectIdentifier>) RequestUtils.sendReadPropertyAllowNull(localDevice, device, 
            		device.getObjectIdentifier(), PropertyIdentifier.objectList)).getValues();
            delegate.explorerDidDiscoverDevice(this, new Device(device, objectIDs, localDevice));
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
    		ack = (ReadPropertyAck) localDevice.send(device, new ReadPropertyRequest(oid, PropertyIdentifier.objectName));
    		device.setName(ack.getValue().toString());
    		ack = (ReadPropertyAck) localDevice.send(device, new ReadPropertyRequest(oid, PropertyIdentifier.protocolVersion));
    		device.setProtocolVersion((UnsignedInteger) ack.getValue());
    	}catch (BACnetException e){
    		
    	}
    }
    
//    private HashMap<PropertyIdentifier, Encodable> readAllProperties(RemoteDevice device, ObjectIdentifier oid) {
//    	HashMap<PropertyIdentifier, Encodable> map = new HashMap<PropertyIdentifier, Encodable>();
//        for(PropertyIdentifier id : PropertyIdentifier.ALL){
//        	try {
//        		ReadPropertyAck ack = (ReadPropertyAck) localDevice.send(device, new ReadPropertyRequest(oid, id));
//            	map.put(id, ack.getValue());
//        	}catch (BACnetException e){
//            	
//            }
//        }
//        return map;
//    }
}
