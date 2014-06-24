package org.openhab.binding.bacnet.driver;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observable;
import java.util.Set;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class BacNetObject extends Observable implements Comparable<BacNetObject> {
	protected RemoteDevice device;
	private LocalDevice localDevice;
	public ObjectIdentifier objectID;
	public PropertyIdentifier[] commonProperties = {};
	protected HashMap<PropertyIdentifier, Encodable> properties = new HashMap<PropertyIdentifier, Encodable>();
	
	public BacNetObject(RemoteDevice device, ObjectIdentifier objectID, LocalDevice localDevice){
		this.device = device;
		this.objectID = objectID;
		this.localDevice = localDevice;
	}
	
	public void setProperty(PropertyIdentifier property, Encodable value){
		Encodable oldValue = this.properties.get(property);
		if(oldValue == null || !(oldValue.equals(value))){
			
		}
		this.properties.put(property, value);
		this.setChanged();
		this.notifyObservers(property);
	}
	
	public Encodable getProperty(PropertyIdentifier property){
		return properties.get(property);
	}
	
	public boolean hasProperty(PropertyIdentifier property){
		return properties.containsKey(property);
	}
	
	public Set<PropertyIdentifier> allProperties(){
		return properties.keySet();
	}
	
	public void readProperties(PropertyIdentifier[] properties, BacNetTaskQueue queue) {
		queue.submit(new PropertyReadTask(properties));
    }
	
	public void readProperty(PropertyIdentifier property, BacNetTaskQueue queue){
		PropertyIdentifier[] properties = {property};
		this.readProperties(properties, queue);
	}
	
	public void writeProperty(PropertyIdentifier property, Encodable value,  BacNetTaskQueue queue){
		queue.submit(new PropertyWriteTask(property, value));
	}
	
	@Override
	public String toString() {
		return objectID.toString();
	}
	
	private class PropertyReadTask extends BacNetTask {
		private PropertyIdentifier[] properties;
		
		public PropertyReadTask(PropertyIdentifier[] properties){
			this.properties = properties;
		}
		
		@Override
		public void run() { 
				LinkedList<ReadAccessSpecification> accessSpecifications = new LinkedList<ReadAccessSpecification>();
				for(PropertyIdentifier property : properties){
					accessSpecifications.add(new ReadAccessSpecification(objectID, property));
				}
				ReadPropertyMultipleRequest request = new ReadPropertyMultipleRequest(new SequenceOf<ReadAccessSpecification>(accessSpecifications));
        		ReadPropertyMultipleAck ack = null;
				try {
					ack = (ReadPropertyMultipleAck)localDevice.send(device, request);
				} catch (BACnetException e) {
					System.err.println(e);
				}
				if(ack != null){
					for(ReadAccessResult readResult : ack.getListOfReadAccessResults()){
						for(Result result : readResult.getListOfResults()){
							if(!result.isError()){
								setProperty(result.getPropertyIdentifier(), result.getReadResult().getDatum());
							}
						}
					}
				}
				super.finish();
		}
	}
	
	private class PropertyWriteTask extends BacNetTask {
		private PropertyIdentifier property;
		private Encodable value;
		
		public PropertyWriteTask(PropertyIdentifier property, Encodable value){
			this.property = property;
			this.value = value;
		}
		
		@Override
		public void run() {
			WritePropertyRequest request = new WritePropertyRequest(objectID, property, null, value, new UnsignedInteger(8));
			try {
				localDevice.send(device, request);
			} catch (BACnetException e) {
				System.err.println("Failed to write to BACNet device: " + e);
				e.printStackTrace();
			}
			super.finish();
		}
	}

	@Override
	public int compareTo(BacNetObject o) {
		int idA = this.objectID.getInstanceNumber();
		int idB = o.objectID.getInstanceNumber();
		if(idA < idB){
			return -1;
		}else if(idA > idB){
			return 1;
		}else{
			return 0;
		}
		
	}
}
