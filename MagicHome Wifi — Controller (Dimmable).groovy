/**
 *  MagicHome Wifi - Controller (Dimmable) 0.83
 *
 *  Author: 
 *    Adam Kempenich 
 *
 *  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
 *
 *  Changelog:
 *
 *	0.83 (Feb 28 2019) 
 *	  - Un-did the parse() removal. Added Data checking for parse()
 *	  - Added a setting for time-to-re
 *
 *  	0.82 (Feb 25 2019)
 *	  - Commented out parse() contents, since I think they are causing slowdown...
 *	  0.81 (Feb 19 2019) 
 *      - Added try/catch to initialize() method
 * 
 *    0.8 (Feb 11 2019)
 *      - Initial Release
 *      - Cannot set device time, yet.
 *      - Auto-discovery not yet implemented
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
        author: "Adam Kempenich") {
        
        capability "Actuator"
        capability "Initialize"
        capability "Polling"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"

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
    logDebug( "Switch set to on" )
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug( "Switch set to off" )
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    sendEvent(name: "level", value: level)
    logDebug( "Level set to ${level}")
    
    if( !transmit ) return level
    byte[] data = powerOnWithChanges(true) + appendChecksum( [ 0x31, level * 2.55, 0, 0, 0x03, 0x01, 0x0f ] )
    sendCommand( data ) 
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( append=false ){
    // If the device is off and light settings change, turn it on (if user settings apply)
    if(append){
        return settings.powerOnBrightnessChange ? ( [0x71, 0x23, 0x0F, 0xA3] ) : ([])
    }
    else{
        settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
    }
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
//    // Parse data received back from this device
//
	def responseArray = HexUtils.hexStringToIntArray(response)	
	switch(responseArray.length) {
		case 4:
			logDebug( "Received power-status packet of ${response}" )
			if( responseArray[2] == 35 ){
				device.currentValue( 'status' ) != 'on' ? sendEvent(name: "switch", value: "on") : null
			}
			else{
				device.currentValue( 'status' ) != 'off' ? sendEvent(name: "switch", value: "off") : null
			}
			break;
		
		case 14:
			logDebug( "Received general-status packet of ${response}" )
		
			if( responseArray[2] == 35 ){
				device.currentValue( 'status' ) != 'on' ? sendEvent(name: "switch", value: "on") : null
			}
			else{
				device.currentValue( 'status' ) != 'off' ? sendEvent(name: "switch", value: "off") : null
			}
		
			def level = ( responseArray[ 6 ].toDouble() / 2.55 ).round()
			device.currentValue( 'level' ) != level ? sendEvent( name: "level", value: level ) : null
			
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
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "Transmitting: ${stringBytes}"
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}
def refresh( ) {
    
    byte[] data =  [ 0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand( data )
}

def telnetStatus( status ) { logDebug "telnetStatus: ${status}" }
def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    if(status == "send error: Broken pipe (Write failed)" || status == "send error: Connection timed out (Write failed)") {
        // Cannot reach device
        logDebug "Cannot reach device. Attempting to reconnect."
        runIn(2, initialize)
    }   
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
	telnetClose()
	try {
		logDebug("Opening TCP-Telnet connection.")
	    InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
		
		pauseExecution(1000)
		logDebug("Connection successfully established")
	} catch(e) {
		logDebug("Error establishing TCP-Telnet connection.")
	}
    unschedule()

    runIn(5, keepAlive)
}

def keepAlive(){
    // Poll the device every 250 seconds, or it will lose connection.
    
    refresh()
	unschedule()
	settings.refreshTime == null ? runIn(60, keepAlive) : runIn(settings.refreshTime, keepAlive)
}
