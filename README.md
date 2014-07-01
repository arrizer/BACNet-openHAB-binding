# BACNet Binding for openHAB

This binding provides basic access to BACNet devices for openHAB. It currently supports binary and analog value objects on BACNet devices.

## Installation

Drop the .jar file from the plugins/ folder in your openHAB `addons` directory and restart your server.

## Configure items

Each BACNet endpoint is identified by a device instance ID, an object type and an object instance ID. The configuration your items file looks as follows:

`... {bacnet="key1=value1,key2=value2,..."}`

e.g.

`Switch reflectors_west_2 "Reflectors West Center" <reflector> (Reflector) {bacnet="device=701105,type=binaryValue,object=3"}`

The following _mandatory_ keys are available:

- `device` is the instance ID (integer) of the device as configured on your local network (it is *not* the IP address of the device)
- `object`is the instance ID (integer) of the object you want to tie to this openHAB item
- `type` is the object type. Available types are  `analogInput`, `analogOutput`, `analogValue`, `binaryInput`, `binaryOutput`, `binaryValue`

The following _optional_ keys are available:

- `readThrrottle` is the minimum number of time (in milliseconds) that the binding should wait between two property read requests to the device of this configuration.
- `writeThrrottle` is the minimum number of time (in milliseconds) that the binding should wait between two property write requests to the device of this configuration.

## How does it work?

The binding uses BACNet/IP and sends out a broadcast discovery command on startup. All devices on the local network responding to the broadcast become available to the binding. The binding will continuously update endpoint values for all objects that are configured in your item files by issuing read property commands via BACNet. Sending commands/status updates will result in write property commands.

### Development

The item types "dimmer", "number" and "switch" have been tested. This binding is still in it's early development stage and might need many tweaks to work reliably in all cases!