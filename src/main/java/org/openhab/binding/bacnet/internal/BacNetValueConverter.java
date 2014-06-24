package org.openhab.binding.bacnet.internal;

import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Double;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import org.openhab.core.types.Type;
import org.openhab.core.types.State;

public class BacNetValueConverter {
	public static State bacNetValueToOpenHabState(Class<? extends Item> type, Encodable value){
		try {
			if (type.isAssignableFrom(ContactItem.class)) {
				return (decodeBoolean(value) ? OpenClosedType.CLOSED : OpenClosedType.OPEN);
			} else if (type.isAssignableFrom(SwitchItem.class) && value instanceof BinaryPV) {
				return (decodeBoolean(value) ? OnOffType.ON : OnOffType.OFF);
			} else if (type.isAssignableFrom(DimmerItem.class) && value instanceof Real) {
				return new PercentType(decodeInt(value));
			} else if (type.isAssignableFrom(RollershutterItem.class)) {
				return new PercentType(decodeInt(value));
			} else if (type.isAssignableFrom(NumberItem.class)) {
				return new DecimalType(decodeFloat(value));
			} else {
				return StringType.valueOf(value.toString());
			}
		} catch (Exception e) {
			System.err.println(e);
			return StringType.valueOf(value.toString());
		}
	}
	
	private static boolean decodeBoolean(Encodable value){
		if(value instanceof BinaryPV){
			return (value.equals(BinaryPV.active));
		}else if(value instanceof Boolean){
			return ((Boolean)value).booleanValue();
		}
		throw new IllegalArgumentException("Cannot convert BacNet value " + value + " to boolean");
	}
	
	private static float decodeFloat(Encodable value){
		if(value instanceof Real){
			return ((Real)value).floatValue();
		}else if(value instanceof Double){
			return (float)((Double)value).doubleValue();
		}else if(value instanceof UnsignedInteger){
			return (float)((UnsignedInteger)value).intValue();
		}else if(value instanceof SignedInteger){
			return (float)((SignedInteger)value).intValue();
		}
		throw new IllegalArgumentException("Cannot convert BacNet value " + value + " to float");
	}
	
	private static int decodeInt(Encodable value){
		if(value instanceof Real){
			return (int)((Real)value).floatValue();
		}else if(value instanceof Double){
			return (int)((Double)value).doubleValue();
		}else if(value instanceof UnsignedInteger){
			return ((UnsignedInteger)value).intValue();
		}else if(value instanceof SignedInteger){
			return ((SignedInteger)value).intValue();
		}
		throw new IllegalArgumentException("Cannot convert BacNet value " + value + " to int");
	}
	
	public static Encodable openHabTypeToBacNetValue(ObjectType type, Type value){
		if(type.equals(ObjectType.binaryValue) || type.equals(ObjectType.binaryOutput) || type.equals(ObjectType.binaryInput)){
			return encodeBoolean(value);
		}else if(type.equals(ObjectType.analogValue) || type.equals(ObjectType.analogOutput) || type.equals(ObjectType.analogInput)){
			return encodeFloat(value);
		}
		throw new IllegalArgumentException("BacNet object type " + type + " is not implemented");
	}
	
	private static Encodable encodeBoolean(Type type){
		if(type instanceof OnOffType){
			return (((OnOffType)type).equals(OnOffType.ON) ? BinaryPV.active : BinaryPV.inactive); 
		}
		throw new IllegalArgumentException("Cannot convert openHAB type " + type + " to boolean");
	}
	
	private static Encodable encodeFloat(Type type){
		if(type instanceof DecimalType){
			return new Real(((PercentType)type).floatValue());
		}
		throw new IllegalArgumentException("Cannot convert openHAB type " + type + " to integer");
	}
}
