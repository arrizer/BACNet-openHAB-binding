package org.openhab.binding.bacnet.driver;

import java.util.Arrays;
import java.util.LinkedList;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;

public class PropertyReadTask extends Task {
	
	private PropertyIdentifier[] properties;
	
	public PropertyReadTask(BacNetObject bacNetObject, PropertyIdentifier[] properties){
		super(bacNetObject);
		this.properties = properties;
	}
	
	@Override
	public void run() { 
			LinkedList<ReadAccessSpecification> accessSpecifications = new LinkedList<ReadAccessSpecification>();
			for(PropertyIdentifier property : properties){
				accessSpecifications.add(new ReadAccessSpecification(bacNetObject.objectID, property));
			}
			ReadPropertyMultipleRequest request = new ReadPropertyMultipleRequest(new SequenceOf<ReadAccessSpecification>(accessSpecifications));
    		ReadPropertyMultipleAck ack = null;
			try {
				ack = (ReadPropertyMultipleAck)bacNetObject.manager.localDevice.send(bacNetObject.device, request);
			} catch (BACnetException e) {
				System.err.println(e);
			}
			if(ack != null){
				for(ReadAccessResult readResult : ack.getListOfReadAccessResults()){
					for(Result result : readResult.getListOfResults()){
						if(!result.isError()){
							bacNetObject.setProperty(result.getPropertyIdentifier(), result.getReadResult().getDatum());
						}
					}
				}
			}
			super.finish();
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj) 
				&& obj instanceof PropertyReadTask 
				&& Arrays.deepEquals(((PropertyReadTask)obj).properties, this.properties);
	}
}