package org.openhab.binding.bacnet.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

import org.openhab.binding.bacnet.BacNetBindingProvider;
import org.openhab.binding.bacnet.driver.Device;
import org.openhab.binding.bacnet.driver.DeviceDelegate;
import org.openhab.binding.bacnet.driver.Endpoint;
import org.openhab.binding.bacnet.driver.Manager;
import org.openhab.binding.bacnet.driver.ManagerDelegate;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;

public class BacNetBinding extends AbstractActiveBinding<BacNetBindingProvider> implements ManagedService, ManagerDelegate, DeviceDelegate, Observer {
	static final Logger logger = LoggerFactory.getLogger(BacNetBinding.class);
	private Manager manager;
	private HashMap<BacNetBindingConfig, Encodable> lastUpdate = new HashMap<BacNetBindingConfig, Encodable>();
	
	
	@Override
	protected String getName() {
		return "BacNet Service";
	}
	
	@Override
	public void activate() {
		manager = new Manager(this);
		manager.discover();
		super.activate();
		setProperlyConfigured(true);
	}
	
	@Override
	public void deactivate() {
		manager.terminate();
		manager = null;
	}
	
	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		lastUpdate = new HashMap<BacNetBindingConfig, Encodable>();
	}

	
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		performUpdate(itemName, newState);
	}
	
	@Override
	public void internalReceiveCommand(String itemName, Command command) {
		performUpdate(itemName, command);
	}
	
	private void performUpdate(String itemName, Type newValue){
		BacNetBindingConfig config = configForItemName(itemName);
		if(config != null){
			Endpoint endpoint = deviceEndpointForConfig(config);
			if(endpoint != null){
				if(!endpoint.getDevice().endpointWriteInProgress()){
					Encodable value = BacNetValueConverter.openHabTypeToBacNetValue(endpoint.objectID.getObjectType(), newValue);
					endpoint.writeThrottle = config.writeThrottle;
					endpoint.writePresentValue(value);
				}else{
					logger.warn("Dropping write " + endpoint + " -> " + newValue + ", because previous write to same BacNet object is still in progress");
				}
				lastUpdate.remove(config);
			}
		}
	}

	@Override
	public void addBindingProvider(BacNetBindingProvider provider) {
		super.addBindingProvider(provider);
	}
	
	@Override
	protected void execute() {
		for(BacNetBindingProvider provider : providers){
			for(BacNetBindingConfig config : provider.allConfigs()){
				Endpoint endpoint = deviceEndpointForConfig(config);
				if(endpoint != null){
					endpoint.readThrottle = config.readThrottle;
					endpoint.readPresentValue();
				}
			}
		}
	}

	@Override
	protected long getRefreshInterval() {
		return 1000;
	}

	@Override
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException {

	}

	@Override
	public void update(Observable object, Object arg) {
		Endpoint endpoint = (Endpoint)object;
		Encodable value = endpoint.getPresentValue();
		State state = UnDefType.UNDEF;
		BacNetBindingConfig config = configForEndpoint(endpoint);
		if(config == null || value == null){
			return;
		}
		Encodable oldValue = lastUpdate.get(config);
		if(oldValue == null || !oldValue.equals(value)){
			state = this.createState(config.itemType, value);
			eventPublisher.postUpdate(config.itemName, state);			
			lastUpdate.put(config, value);	
		}
	}
	
	private State createState(Class<? extends Item> type, Encodable value) {
		try {
			return BacNetValueConverter.bacNetValueToOpenHabState(type, value);
		} catch (Exception e) {
			logger.debug("Couldn't create state of type '{}' for value '{}'", type, value);
			return StringType.valueOf(value.toString());
		}
	}
		
	private Endpoint deviceEndpointForConfig(BacNetBindingConfig config){
		Device device = manager.devices.get(config.deviceID);
		if(device != null){
			return device.getEndpoint(new ObjectType(config.endpointTypeID), config.endpointID);
		}
		return null;
	}
	
	@Override
	public void managerDidDiscoverRemoteDevice(Manager localDevice, Device device){
		for(Endpoint endpoint : device.getEndpoints(Device.discoverTypes)){
			endpoint.addObserver(this);
		}
	}

	@Override
	public void deviceDidDiscoverSelf(Device device) {
		System.out.println(device);
	}

	@Override
	public void deviceDidDiscoverEndpoint(Device device, Endpoint endpoint) {
		
	}

	@Override
	public void deviceDidFinishDiscoveringAllEndpoints(Device device) {
		
	}

	private BacNetBindingConfig configForItemName(String itemName) {
		for(BacNetBindingProvider provider : providers){
			BacNetBindingConfig config = provider.configForItemName(itemName);
			if(config != null){
				return config;
			}
		}
		return null;
	}

	private BacNetBindingConfig configForEndpoint(Endpoint endpoint) {
		for(BacNetBindingProvider provider : providers){
			BacNetBindingConfig config = provider.configForEndpoint(
				endpoint.getDevice().objectID.getInstanceNumber(), 
				endpoint.objectID.getObjectType().intValue(), 
				endpoint.objectID.getInstanceNumber()
			);
			if(config != null){
				return config;
			}
		}
		return null;
	}
}
