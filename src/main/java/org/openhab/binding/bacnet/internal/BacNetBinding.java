package org.openhab.binding.bacnet.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.openhab.binding.bacnet.BacNetBindingProvider;
import org.openhab.binding.bacnet.driver.Device;
import org.openhab.binding.bacnet.driver.DeviceDelegate;
import org.openhab.binding.bacnet.driver.DeviceEndpoint;
import org.openhab.binding.bacnet.driver.DeviceExplorer;
import org.openhab.binding.bacnet.driver.DeviceExplorerDelegate;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.openhab.core.types.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;

public class BacNetBinding extends AbstractActiveBinding<BacNetBindingProvider> implements ManagedService, DeviceExplorerDelegate, DeviceDelegate, Observer {
	static final Logger logger = LoggerFactory.getLogger(BacNetBinding.class);
	private DeviceExplorer explorer;
	private LocalDevice localDevice;
	private HashMap<BacNetBindingConfig, Encodable> lastUpdate = new HashMap<BacNetBindingConfig, Encodable>();
	public ConcurrentHashMap<Integer, Device> devices = new ConcurrentHashMap<Integer, Device>();
	
	@Override
	protected String getName() {
		return "BacNet Service";
	}
	
	@Override
	public void activate() {
        IpNetwork network = new IpNetwork();
        Transport transport = new Transport(network);
        //transport.setTimeout(15000);
        //transport.setSegTimeout(15000);
        localDevice = new LocalDevice(1234, transport);
        try {
			localDevice.initialize();
		} catch (Exception e) {
			e.printStackTrace();
		}
		explorer = new DeviceExplorer(localDevice, this);
		explorer.discover();
		super.activate();
    	setProperlyConfigured(true);
	}
	
	@Override
	public void deactivate() {
		localDevice.terminate();
		explorer = null;
	}
	
	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		lastUpdate = new HashMap<BacNetBindingConfig, Encodable>();
	}

	
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		logger.warn("So you want to set BacNet " + itemName + " to " + newState);
		performUpdate(itemName, newState);
	}
	
	@Override
	public void internalReceiveCommand(String itemName, Command command) {
		logger.warn("So you want to send BacNet " + itemName + " the command " + command);
		performUpdate(itemName, command);
	}
	
	private void performUpdate(String itemName, Type newValue){
		BacNetBindingConfig config = configForItemName(itemName);
		if(config != null){
			DeviceEndpoint endpoint = deviceEndpointForConfig(config);
			if(endpoint != null){
				Encodable value = BacNetValueConverter.openHabTypeToBacNetValue(endpoint.objectID.getObjectType(), newValue);
				endpoint.writePresentValue(value);
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
				DeviceEndpoint endpoint = deviceEndpointForConfig(config);
				if(endpoint != null){
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
		DeviceEndpoint endpoint = (DeviceEndpoint)object;
		Encodable value = endpoint.getPresentValue();
		//System.out.println("<BACNET UPDATE> " + endpoint + " -> " + value);
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
		
	private DeviceEndpoint deviceEndpointForConfig(BacNetBindingConfig config){
		Device device = devices.get(config.deviceID);
		if(device != null){
			return device.getEndpoint(new ObjectType(config.endpointTypeID), config.endpointID);
		}
		return null;
	}
	
	@Override
	public void explorerDidDiscoverDevice(DeviceExplorer explorer, Device device) {
		devices.put(device.objectID.getInstanceNumber(), device);
		for(DeviceEndpoint endpoint : device.getEndpoints(Device.discoverTypes)){
			endpoint.addObserver(this);
		}
		//device.discover();
	}

	@Override
	public void deviceDidDiscoverSelf(Device device) {
		System.out.println(device);
	}

	@Override
	public void deviceDidDiscoverEndpoint(Device device, DeviceEndpoint endpoint) {
		
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

	private BacNetBindingConfig configForEndpoint(DeviceEndpoint endpoint) {
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
