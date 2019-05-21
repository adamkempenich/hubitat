/**
*  MagicHome Wifi - Controller (Dimmable) 0.86
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*
*	0.87 (May 16, 2019)
*		- Added an option for telnet/socket
*		- If you are on firmware before 2.1, use TELNET (does not support parse)
*		- ---> Otherwise, use SOCKET (supports parse) <----
*		- Greatly improved scheduling
*		- Started adding some features back in. Fully tested before release.
*		- Changed powerOnWithChanges to enablePreStaging — this matches Hubitat's vernacular
*
*	0.86 (April 14 2019)
*		- Fixed parse()
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
        name: "MagicHome Wifi — Controller (Dimmable)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Controller%20(Dimmable).groovy") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Initialize"
	}
    
    preferences {  
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
		input "useTelnet", "bool", title: "Use Telnet?", description: "Telnet - On, Socket - Off", required: true, defaultValue: false
		
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
         input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)
		
		input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"enablePreStaging", type:"bool", title: "Enable Brightness Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)

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

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug "Switch set to off" 
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def setLevel(level) {
    // Set the brightness of a device (0-100)
	limit( level )
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
	
	byte[] data = appendChecksum([0x31, level*2.55, 0, 0, 0x03, 0x01, 0x0f])
    sendCommand( data ) 
    powerOnWithChanges()
}

// ------------------- Helper Functions ------------------------- //


def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)
		
		pauseExecution(300)
        settings.enablePreStaging ? null : ( device.currentValue("status") != "on" ? on() : null )
}

def limit( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound}
        if(value > upperBound){ value = upperBound}
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound}
        if(value > lowerBound ){ value = lowerBound}
    }

    return value
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
    
    state.noResponse = 0    
    
    def responseArray = HexUtils.hexStringToIntArray(response)  
    switch(responseArray.length) {
        case 4:
            logDebug( "Received power-status packet of ${response}" )
            if( responseArray[2] == 35 ){
                if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
                }
            }
            else{
                if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
                }
            }
            break;
        
        case 14:
            logDebug( "Received general-status packet of ${response}" )
        
            if( responseArray[2] == 35 ){
                if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
                }
            }
            else{
                if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
                }
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
        log.debug "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    if( settings.logDescriptionText ) { 
        log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    if(settings.useTelnet == false || settings.useTelnet == null){
        InterfaceUtils.sendSocketMessage(device, stringBytes)
    }
    else{
        def transmission = new HubAction(stringBytes, Protocol.TELNET)
        sendHubCommand(transmission)
    }
}

def refresh( ) {
	
	state.noResponse++
    state.noResponse >= 2 ? ( initialize() ) : null // if a device hasn't responded twice, reconnect
	
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect..."
    initialize()
    }

def telnetStatus( status ) { 
    logDebug "telnetStatus: ${status}"
    logDebug "Attempting to reconnect..."
    initialize()
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/10 * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    telnetClose()
    def tryWasGood = false
    if(settings.useTelnet == false || settings.useTelnet == null){
        try {
            logDebug "Opening Socket Connection."
            InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
            pauseExecution(1000)
            logDebug "Connection successfully established"
			tryWasGood = true
            
        } catch(e) {
            logDebug("Error attempting to establish TCP connection to device.")
            logDebug("Next initialization attempt in 10 seconds.")
			settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
			tryWasGood = false
        }
    }
    else{
        try {
            logDebug "Opening Telnet Connection."
            telnetConnect([byteInterface: true, termChars:[129]], "${settings.deviceIP}", settings.devicePort.toInteger(), null, null)
            pauseExecution(1000)
            logDebug "Connection successfully established" 
			tryWasGood = true
        } catch(e) {
            logDebug("Error attempting to establish TCP connection to device.")
            logDebug("Next initialization attempt in 10 seconds.")
			settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
			tryWasGood = false
        }
    }
	
	if(tryWasGood){
		unschedule()
		logDebug "Starting refresh cron"
		schedule("0/10 * * * * ? *", refresh)
		state.noResponse = 0
	}
}

def initialize() {
    // Establish a connection to the device
    
    logDebug "Initializing device."
    connectDevice([firstRun: true])
}

def installed(){
	sendEvent(name: "level", value: 100)
	sendEvent(name: "switch", value: "off")
}

