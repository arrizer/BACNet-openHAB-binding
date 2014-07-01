package org.openhab.binding.bacnet.driver;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class Device extends BacNetObject implements TaskQueueDelegate {
	private HashMap<ObjectIdentifier, Endpoint> endpoints = new HashMap<ObjectIdentifier, Endpoint>();
	public boolean discovered = false;
	public boolean endpointsDiscovered = false;
	private TaskQueue selfDiscoverQueue = new TaskQueue(this);
	private TaskQueue endpointDiscoverQueue = new TaskQueue(this);
	private TaskQueue endpointReadQueue = new TaskQueue(this);
	private TaskQueue endpointWriteQueue = new TaskQueue(this);
	public DeviceDelegate delegate;
	
	public static ObjectType[] discoverTypes = {
		ObjectType.analogInput,
		ObjectType.analogOutput,
		ObjectType.analogValue,
		ObjectType.binaryInput,
		ObjectType.binaryOutput,
		ObjectType.binaryValue
	};
	
	public Device(Manager manager, RemoteDevice device, List<ObjectIdentifier> endpointObjectIdentifiers) {
		super(manager, device, device.getObjectIdentifier());
		PropertyIdentifier[] p = {
			PropertyIdentifier.vendorName,
			PropertyIdentifier.objectName,
			PropertyIdentifier.description,
			PropertyIdentifier.modelName
		};
		commonProperties = p;
		for(ObjectIdentifier objectID : endpointObjectIdentifiers){
			if(!objectID.getObjectType().equals(ObjectType.device)){
				Endpoint endpoint = new Endpoint(manager, objectID, this);
				endpoints.put(objectID, endpoint);
			}
		}
	}
	
	public Collection<Endpoint> getEndpoints(){
		List<Endpoint> endpoints = new LinkedList<Endpoint>();
		endpoints.addAll(this.endpoints.values());
		Collections.sort(endpoints);
		return endpoints;
	}
	
	public Collection<Endpoint> getEndpoints(ObjectType[] types){
		LinkedList<Endpoint> endpoints = new LinkedList<Endpoint>();
		for(ObjectType type : types){
			endpoints.addAll(this.getEndpoints(type));
		}
		return endpoints;
	}
	
	public Collection<Endpoint> getEndpoints(ObjectType type){
		LinkedList<Endpoint> endpoints = new LinkedList<Endpoint>();
		for(Endpoint endpoint : this.getEndpoints()){
			if(endpoint.objectID.getObjectType().equals(type)){
				endpoints.add(endpoint);
			}
		}
		return endpoints;
	}
	
	public Endpoint getEndpoint(ObjectType type, int id){
		ObjectIdentifier identifier = new ObjectIdentifier(type, id);
		return endpoints.get(identifier);
	}
	
	
	public void discover(){
		this.readProperties(commonProperties, selfDiscoverQueue);
	}
	
	public void discoverEndpoints(){
		Collection<Endpoint> endpoints = this.getEndpoints(discoverTypes);
		if(endpoints.size() > 0){
			for(Endpoint endpoint : endpoints){
				endpoint.readProperties(endpoint.commonProperties, endpointDiscoverQueue);
			}
		}else{
			this.endpointsDiscovered = true;
			delegate.deviceDidFinishDiscoveringAllEndpoints(this);
		}
	}
	
	public void readEndpointValue(Endpoint endpoint){
		endpoint.readProperty(PropertyIdentifier.presentValue, endpointReadQueue);
	}
	
	public void writeEndpointValue(Endpoint endpoint, Encodable value){
		endpoint.writeProperty(PropertyIdentifier.presentValue, value, endpointWriteQueue);
	}
	
	public boolean endpointWriteInProgress(){
		return (endpointWriteQueue.getPendingRequests() > 0);
	}
		
	@Override
	public String toString() {
		return String.format("<%s> %s %s %s",
				super.toString(),
				this.getProperty(PropertyIdentifier.vendorName),
				this.getProperty(PropertyIdentifier.objectName),
				this.getProperty(PropertyIdentifier.modelName),
				this.getProperty(PropertyIdentifier.description)
		);
	}

	@Override
	public void queueDidFinishTask(TaskQueue queue, Task task) {
		
	}

	@Override
	public void queueDrained(TaskQueue queue) {
		if(queue == endpointDiscoverQueue){
			this.endpointsDiscovered = true;
			delegate.deviceDidFinishDiscoveringAllEndpoints(this);
		}else if(queue == selfDiscoverQueue){
			this.discovered = true;
			delegate.deviceDidDiscoverSelf(this);
		}
	}
}
