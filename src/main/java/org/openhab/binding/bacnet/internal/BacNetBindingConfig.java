package org.openhab.binding.bacnet.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.model.item.binding.BindingConfigParseException;

import com.serotonin.bacnet4j.type.enumerated.ObjectType;

public class BacNetBindingConfig implements BindingConfig {
	public Class<? extends Item> itemType;
	public String itemName;
	public int deviceID;
	public int endpointTypeID;
	public int endpointID;
	public int readThrottle = 0;
	public int writeThrottle = 0;
	private static final Pattern CONFIG_PATTERN = Pattern.compile("^([A-z]+=[^,]+(,|$))+");
	private static final String[] mandatoryKeys = {"device","object","type"};
	
	static BacNetBindingConfig parseBindingConfig(String itemName, Class<? extends Item> itemType, String configString) throws BindingConfigParseException{
		Matcher matcher = CONFIG_PATTERN.matcher(configString);
		if (!matcher.matches()) {
			throw new BindingConfigParseException("Invalid BacNet config: '" + configString + "'. Expected key1=value1,key2=value2");
		}
		HashMap<String, String> values = new HashMap<String, String>(); 
		for(String item : configString.split(",")){
			String[] parts = item.split("=");
			if(parts.length != 2){
				throw new BindingConfigParseException("Expected key=value in BacNet config");
			}
			values.put(parts[0], parts[1]);
		}
		return new BacNetBindingConfig(itemName, itemType, values);
	}
	
	public BacNetBindingConfig(String itemName, Class<? extends Item> itemType, Map<String,String> values) throws BindingConfigParseException{
		this.itemName = itemName;
		this.itemType = itemType;
		for(String key : values.keySet()){
			if(key.equals("device")){
				this.deviceID = Integer.parseInt(values.get("device"));
			}else if(key.equals("type")){
				if(!parseObjectTypeName(values.get("type"))){
					throw new BindingConfigParseException("Unknown object BacNet object type" + values.get("type"));
				}
			}else if(key.equals("object")){
				this.endpointID = Integer.parseInt(values.get("object"));
			}else if(key.equals("readThrottle")){
				this.readThrottle = Integer.parseInt(values.get("readThrottle"));
			}else if(key.equals("writeThrottle")){
				this.writeThrottle = Integer.parseInt(values.get("writeThrottle"));
			}else{
				throw new BindingConfigParseException("Invalid key in BacNet config: " + key);
			}
		}
		for(String key : mandatoryKeys){
			if(!values.containsKey(key)){
				throw new BindingConfigParseException("Mandatory key in BacNet config missing: " + key);
			}
		}
	}
	
	private boolean parseObjectTypeName(String name){
		if(name.equals("binaryValue")){
			this.endpointTypeID = ObjectType.binaryValue.intValue();
		}else if(name.equals("binaryInput")){
			this.endpointTypeID = ObjectType.binaryInput.intValue();
		}else if(name.equals("binaryOutput")){
			this.endpointTypeID = ObjectType.binaryOutput.intValue();
		}else if(name.equals("analogValue")){
			this.endpointTypeID = ObjectType.analogValue.intValue();
		}else if(name.equals("analogInput")){
			this.endpointTypeID = ObjectType.analogInput.intValue();
		}else if(name.equals("analogOutput")){
			this.endpointTypeID = ObjectType.analogOutput.intValue();
		}else{
			return false;
		}
		return true;
	}
	
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
