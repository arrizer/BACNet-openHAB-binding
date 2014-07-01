package org.openhab.binding.bacnet.driver;
import java.lang.ref.WeakReference;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class Endpoint extends BacNetObject {
	private WeakReference<Device> parentDevice;
	
	public Endpoint(Manager manager, ObjectIdentifier objectID, Device parentDevice) {
		super(manager, parentDevice.device, objectID);
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
}
