package org.openhab.binding.bacnet.driver;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.SubscribeCOVPropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

@SuppressWarnings("unused")
public class BacNetObject extends Observable implements Comparable<BacNetObject> {
	public int readThrottle = 0;
	public int writeThrottle = 0;
	protected RemoteDevice device;
	protected Manager manager;
	public ObjectIdentifier objectID;
	protected PropertyIdentifier[] commonProperties = {};
	protected HashMap<PropertyIdentifier, Encodable> properties = new HashMap<PropertyIdentifier, Encodable>();

	public BacNetObject(Manager manager, RemoteDevice device, ObjectIdentifier objectID){
		this.objectID = objectID;
		this.manager = manager;
		this.device = device;
	}
	
	public void setProperty(PropertyIdentifier property, Encodable value){
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
	
	public void readProperties(PropertyIdentifier[] properties, TaskQueue queue) {
		PropertyReadTask task = new PropertyReadTask(this, properties);
		task.throttle = this.readThrottle;
		queue.submit(task);
    }
	
	public void readProperty(PropertyIdentifier property, TaskQueue queue){
		PropertyIdentifier[] properties = {property};
		this.readProperties(properties, queue);
	}
	
	public void writeProperty(PropertyIdentifier property, Encodable value,  TaskQueue queue){
		PropertyWriteTask task = new PropertyWriteTask(this, property, value);
		task.throttle = this.writeThrottle;
		queue.submit(task);
	}
	
	@Override
	public String toString() {
		return objectID.toString();
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
