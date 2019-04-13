/**
*  MagicHome Wifi - Bulb (Dimmable) 0.85
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*
*	0.85 (April 12 2019)
*		- Simplified most of the code
*		- Eliminated Telnet method
*		- Removed most of parse() while I simplify it further. Only power reports.
*		- Continued to enhance self-healing. Devices that are physically powered off report as off.
*		- Reworked powerOnWithChanges.
* 		- Added import URL
*
*	0.84 (Mar 28 2019) 
* 		- Completely reworked Telnet and Socket methods... Waiting on some bugfixes
* 		- Greatly enhanced self-healing capabilities
*
*	0.83 (Feb 27 2019) 
*	  - Un-did the parse() removal. Added Data checking for parse()
*	  - Added a setting for time-to-refresh
*
*  	0.82 (Feb 25 2019)
*	  - Commented out parse() contents, since I think they are causing slowdown...
*
*    0.81 (Feb 19 2019) 
* 		- Added try/catch to initialize method
* 		- Removed a bunch of extraneous code
* 		- Fixed an issue with data loss for CCT in parse method 
*
*    0.8 (Feb 11 2019)
*      - Initial Release
*      - Cannot set device time, yet.
*      - Auto-discovery not yet implemented
*      - Custom functions not yet implemented
*      - On-device scheduling not yet implemented
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*/

import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
    definition (
        name: "MagicHome Wifi — Bulb (Dimmable)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Bulb%20(Dimmable).groovy") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Initialize"
	}
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: false, displayDuringSetup: true)
        
        input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when values change?",
              defaultValue: true, required: true, displayDuringSetup: true)

		input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 60. Recommended: Under 200", defaultValue: 60,
            required: true, displayDuringSetup: true)
    }
}
def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    logDebug "Switch set to on" 
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def setLevel(level) {
    // Set the brightness of a device (0-100)
	level > 100 ? (level = 100) : null
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    powerOnWithChanges()
	
	byte[] data = appendChecksum([0x31, level*2.55, 0, 0, 0x03, 0x01, 0x0f])
    sendCommand( data ) 
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)

	settings.powerOnWithChanges ? ( device.currentValue("status") != "on" ? on() : null ) : null
}

def calculateChecksum( data ){
    // Totals an array of bytes
    
    int sum = 0;
    for(int d : data)
        sum += d;
    return sum & 255
}

def appendChecksum( data ){
    // Adds a checksum to an array
    
    data += calculateChecksum(data)
    return data 
}

def parse( response ) {
    // Parse data received back from this device
	
	unschedule()
	runIn(20, refresh)
	
    def responseArray = HexUtils.hexStringToIntArray(response)	
	switch(responseArray.length) {
		case 4:
			logDebug( "Received power-status packet of ${response}" )
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
			break;
		
		case 14:
			logDebug( "Received general-status packet of ${response}" )
		
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
		break;	
	case null:
			logDebug "No response received from device"
			initialize()
			break;	
	default:
		logDebug "Received a response with an unexpected length of ${responseArray.length} containing ${response}"
		break;
	}
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs
    
    if( settings.logDebug ) { 
        log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
	unschedule()
	runIn(60, initialize)


	String stringBytes = HexUtils.byteArrayToHexString(data)
	logDebug "${data} was converted. Transmitting: ${stringBytes}"
	InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
	logDebug "socketStatus: ${status}"
	logDebug "Attempting to reconnect in 10 seconds."
	runIn(10, initialize)
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}

def initialize() {
    // Establish a connection to the device
    
    logDebug "Initializing device."
	
	InterfaceUtils.socketClose(device)
	unschedule()
	try {
		logDebug "Opening Socket Connection."
		InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
		pauseExecution(1000)
		logDebug "Connection successfully established"
	    runIn(1, refresh)
	} catch(e) {
		logDebug("Error attempting to establish TCP connection to device.")
		logDebug("Next initialization attempt in 20 seconds.")
		sendEvent(name: "switch", value: "off") // If we didn't hear back, the device is likely physically powered off
		runIn(20, initialize)
	}
}

def installed(){
	sendEvent(name: "level", value: 100)
	sendEvent(name: "switch", value: "off")
}
