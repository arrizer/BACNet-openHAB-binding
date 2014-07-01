package org.openhab.binding.bacnet.driver;


import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.confirmed.WritePropertyRequest;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class PropertyWriteTask extends Task {
	private PropertyIdentifier property;
	private Encodable value;
	
	public PropertyWriteTask(BacNetObject bacNetObject, PropertyIdentifier property, Encodable value){
		super(bacNetObject);
		this.property = property;
		this.value = value;
	}
	
	@Override
	public void run() {
		WritePropertyRequest request = new WritePropertyRequest(bacNetObject.objectID, property, null, value, new UnsignedInteger(8));
		try {
			bacNetObject.manager.localDevice.send(bacNetObject.device, request);
		} catch (BACnetException e) {
			System.err.println("Failed to write to BACNet device: " + e);
  			e.printStackTrace();
  		}
		super.finish();
	}

	@Override
	public boolean equals(Object obj) {
		return false;
//		return super.equals(obj) 
//				&& obj instanceof PropertyWriteTask 
//				&& ((PropertyWriteTask)obj).property.equals(this.property)
//				&& ((PropertyWriteTask)obj).value.equals(this.value);
	}
}