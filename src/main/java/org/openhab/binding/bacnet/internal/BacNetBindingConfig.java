package org.openhab.binding.bacnet.internal;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;

public class BacNetBindingConfig implements BindingConfig {
	public Class<? extends Item> itemType;
	public String itemName;
	public int deviceID;
	public int endpointTypeID;
	public int endpointID;
	
	@Override
	public int hashCode() {
		Object[] objects = {
			itemName,
			new Integer(deviceID),
			new Integer(endpointTypeID),
			new Integer(endpointID)
		};
		return hashCode(objects);
	}
	
	public int hashCode(Object[] composite) {
		int value = 0;
		for(Object object : composite){
			value += object.hashCode();
		}
		return 97 * value;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj.hashCode() == this.hashCode();
	}
}
