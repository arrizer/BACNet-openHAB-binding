# BACNet Binding for openHAB

This binding provides basic access to BACNet devices for openHAB. It currently supports binary and analog value objects on BACNet devices.

## Installation

Drop the .jar file from the plugins/ folder in your openHAB addons directory and restart your server.

## Configure items

Each BACNet endpoint is identified by a device instance ID, an object type and an object instance ID. The configuration your items file looks as follows:

`... {bacnet="<deviceID>:<objectType>:<objectID>"}

`deviceID` is the instance ID (integer) of the device as configured on your local network (it is *not* the IP address of the device)
`objectType` is one of the following:
		- analogInput = 0
		- analogOutput = 1
		- analogValue = 2
    	- binaryInput = 3
		- binaryOutput = 4
		- binaryValue = 5
`objectID`= Instance ID (integer) of the object you want to tie to this openHAB item

## How does it work?

The binding uses BACNet/IP and sends out a broadcast discovery command on startup. All devices on the local network responding to the broadcast become available to the binding. The binding will continuously update endpoint values for all objects that are configured in your item files by issuing read property commands via BACNet. Sending commands/status updates will result in write property commands.

### Development

The item types "dimmer", "number" and "switch" have been tested. This binding is still in it's early development stage and might need many tweaks to work reliably in all cases!