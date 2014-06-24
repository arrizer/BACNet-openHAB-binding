package org.openhab.binding.bacnet.driver;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class Device extends BacNetObject implements Observer, BacNetTaskQueueDelegate, JSONable {
	private HashMap<ObjectIdentifier, DeviceEndpoint> endpoints = new HashMap<ObjectIdentifier, DeviceEndpoint>();
	public boolean discovered = false;
	public boolean endpointsDiscovered = false;
	private BacNetTaskQueue selfDiscoverQueue = new BacNetTaskQueue(this);
	private BacNetTaskQueue endpointDiscoverQueue = new BacNetTaskQueue(this);
	private BacNetTaskQueue endpointReadQueue = new BacNetTaskQueue(this);
	private BacNetTaskQueue endpointWriteQueue = new BacNetTaskQueue(this);
	public DeviceDelegate delegate;
	
	public static ObjectType[] discoverTypes = {
		ObjectType.analogInput,
		ObjectType.analogOutput,
		ObjectType.analogValue,
		ObjectType.binaryInput,
		ObjectType.binaryOutput,
		ObjectType.binaryValue
	};
	
	public Device(RemoteDevice device, List<ObjectIdentifier> endpointObjectIdentifiers, LocalDevice localDevice) {
		super(device, device.getObjectIdentifier(), localDevice);
		PropertyIdentifier[] p = {
			PropertyIdentifier.vendorName,
			PropertyIdentifier.objectName,
			PropertyIdentifier.description,
			PropertyIdentifier.modelName
		};
		commonProperties = p;
		for(ObjectIdentifier objectID : endpointObjectIdentifiers){
			if(!objectID.getObjectType().equals(ObjectType.device)){
				DeviceEndpoint endpoint = new DeviceEndpoint(this.device, objectID, localDevice, this);
				endpoint.addObserver(this);
				endpoints.put(objectID, endpoint);
			}
		}
		this.addObserver(this);
	}
	
	public Collection<DeviceEndpoint> getEndpoints(){
		List<DeviceEndpoint> endpoints = new LinkedList<DeviceEndpoint>();
		endpoints.addAll(this.endpoints.values());
		Collections.sort(endpoints);
		return endpoints;
	}
	
	public Collection<DeviceEndpoint> getEndpoints(ObjectType[] types){
		LinkedList<DeviceEndpoint> endpoints = new LinkedList<DeviceEndpoint>();
		for(ObjectType type : types){
			endpoints.addAll(this.getEndpoints(type));
		}
		return endpoints;
	}
	
	public Collection<DeviceEndpoint> getEndpoints(ObjectType type){
		LinkedList<DeviceEndpoint> endpoints = new LinkedList<DeviceEndpoint>();
		for(DeviceEndpoint endpoint : this.getEndpoints()){
			if(endpoint.objectID.getObjectType().equals(type)){
				endpoints.add(endpoint);
			}
		}
		return endpoints;
	}
	
	public DeviceEndpoint getEndpoint(ObjectType type, int id){
		ObjectIdentifier identifier = new ObjectIdentifier(type, id);
		return endpoints.get(identifier);
	}
	
	
	public void discover(){
		this.readProperties(commonProperties, selfDiscoverQueue);
		selfDiscoverQueue.start();
	}
	
	public void discoverEndpoints(){
		Collection<DeviceEndpoint> endpoints = this.getEndpoints(discoverTypes);
		if(endpoints.size() > 0){
			for(DeviceEndpoint endpoint : endpoints){
				endpoint.readProperties(endpoint.commonProperties, endpointDiscoverQueue);
			}
			endpointDiscoverQueue.start();
		}else{
			this.endpointsDiscovered = true;
			delegate.deviceDidFinishDiscoveringAllEndpoints(this);
		}
	}
	
	public void readEndpointValue(DeviceEndpoint endpoint){
		endpoint.readProperty(PropertyIdentifier.presentValue, endpointReadQueue);
		endpointReadQueue.start();
	}
	
	public void writeEndpointValue(DeviceEndpoint endpoint, Encodable value){
		endpoint.writeProperty(PropertyIdentifier.presentValue, value, endpointWriteQueue);
		endpointWriteQueue.start();
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
	public void update(Observable o, Object arg) {
//		if(o == this){
//			if(!discovered && selfDiscoverQueue.getPendingRequests() == 0){
//				discovered = true;
//				delegate.deviceDidDiscoverSelf(this);
//			}
//		}else if(o instanceof DeviceEndpoint){
//			DeviceEndpoint endpoint = (DeviceEndpoint)o;
//			delegate.deviceDidDiscoverEndpoint(this, endpoint);
//			if(!endpointsDiscovered && endpointDiscoverQueue.getPendingRequests() == 0){
//				endpointsDiscovered = true;
//				delegate.deviceDidFinishDiscoveringAllEndpoints(this);
//			}
//		}
	}

	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", objectID.getInstanceNumber());
		json.put("vendorName", getProperty(PropertyIdentifier.vendorName));
		json.put("name", getProperty(PropertyIdentifier.objectName));
		json.put("modelName", getProperty(PropertyIdentifier.modelName));
		json.put("description", getProperty(PropertyIdentifier.description));
		json.put("endpointCount", endpoints.size());
		json.put("discovered", discovered);
		json.put("endpointsDiscovered", endpointsDiscovered);
		json.put("pendingEndpointDiscoveries", endpointDiscoverQueue.getPendingRequests());
		return json;
	}
	
	public JSONObject toJSONWithEndpoints() throws JSONException {
		JSONObject json = this.toJSON();
		JSONArray list = new JSONArray();
		for(DeviceEndpoint endpoint : getEndpoints(discoverTypes)){
			list.put(endpoint.toJSON());
		}
		json.put("endpoints", list);
		return json;
	}

	@Override
	public void queueDidFinishTask(BacNetTaskQueue queue, BacNetTask task) {
		
	}

	@Override
	public void queueDrained(BacNetTaskQueue queue) {
		if(queue == endpointDiscoverQueue){
			this.endpointsDiscovered = true;
			delegate.deviceDidFinishDiscoveringAllEndpoints(this);
		}else if(queue == selfDiscoverQueue){
			this.discovered = true;
			delegate.deviceDidDiscoverSelf(this);
		}
	}
}
