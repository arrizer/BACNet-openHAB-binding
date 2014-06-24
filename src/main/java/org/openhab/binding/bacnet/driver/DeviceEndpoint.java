package org.openhab.binding.bacnet.driver;
import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class DeviceEndpoint extends BacNetObject implements JSONable {
	private WeakReference<Device> parentDevice;
	
	public DeviceEndpoint(RemoteDevice device, ObjectIdentifier objectID, LocalDevice localDevice, Device parentDevice) {
		super(device, objectID, localDevice);
		PropertyIdentifier[] p = {
			PropertyIdentifier.objectName,
			PropertyIdentifier.presentValue,
			PropertyIdentifier.units,
			PropertyIdentifier.maxPresValue,
			PropertyIdentifier.minPresValue,
			PropertyIdentifier.description
		};
		commonProperties = p;
		this.parentDevice = new WeakReference<Device>(parentDevice);
	}
	
	public Device getDevice(){
		return this.parentDevice.get();
	}
	
	@Override
	public String toString() {
		Encodable units = this.getProperty(PropertyIdentifier.units);
		String unitString = "";
		if(units != null && !(units.toString().indexOf("Unknown") == 0)){
			unitString = units.toString();
		}
		String rangeString = "";
		Encodable min = this.getProperty(PropertyIdentifier.minPresValue);
		Encodable max = this.getProperty(PropertyIdentifier.maxPresValue);
//		String valueClassName = "null";
//		if(this.getPresentValue() != null){
//			valueClassName = this.getPresentValue().getClass().getName();
//		}
		if(min != null || max != null){
			rangeString = String.format(" [%s-%s] ", min, max);
		}
		return String.format("<%s> %s = %s %s%s %s",
			super.toString(),
			this.getProperty(PropertyIdentifier.objectName),
			this.getProperty(PropertyIdentifier.presentValue),
			unitString,
			rangeString,
			this.getProperty(PropertyIdentifier.description)
		);
	}
	
	public Encodable getPresentValue(){
		return this.getProperty(PropertyIdentifier.presentValue);
	}
	
	public void writePresentValue(Encodable value){
		this.getDevice().writeEndpointValue(this, value);
	}
	
	public void readPresentValue(){
		this.getDevice().readEndpointValue(this);
	}
	
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("id", objectID.getInstanceNumber());
		json.put("type", objectID.getObjectType().toString());
		json.put("typeID", objectID.getObjectType().intValue());
		json.put("name", getProperty(PropertyIdentifier.objectName));
		json.put("value", getProperty(PropertyIdentifier.presentValue));
		json.put("units", getProperty(PropertyIdentifier.units));
		json.put("maxValue", getProperty(PropertyIdentifier.maxPresValue));
		json.put("minValue", getProperty(PropertyIdentifier.maxPresValue));
		json.put("description", getProperty(PropertyIdentifier.description));
		return json;
	}
}
