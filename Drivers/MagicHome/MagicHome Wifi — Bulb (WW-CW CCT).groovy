/**
*  MagicHome Wifi - Bulb (WW/CW CCT) 0.89
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*    Changelog:
*  0.90 (Oct. 12, 2021)
*	- Changed log.debug to logdebug on lines 384, 385, 389, 3909 so Log message follows Debug switch state 
*
*    0.89 (Oct 15, 2019)
*        - Removed unused state value (only state.noResponse is necessary)
*        - Removed telnet
*        - Fixed powerOnWithChanges
*        - Added null 2nd option to setLevel for duration
*
*    0.88 (June 12, 2019)
*        - Added option for failed pings threshold 
*        - Resolved issue with recursive loops and initializing devices
*		
*    0.87 (May 16, 2019)
*        - Added an option for telnet/socket
*        - If you are on firmware before 2.1, use TELNET (does not support parse)
*        - ---> Otherwise, use SOCKET (supports parse) <----
*        - Greatly improved scheduling
*        - Started adding some features back in. Fully tested before release.
*        - Changed powerOnWithChanges to enablePreStaging — this matches Hubitat's vernacular
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
*		- Corrected all function parameters except for setColorTemperature() which is mostly corrected
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
        name: "MagicHome Wifi — Bulb (WW/CW CCT)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Bulb%20(WW-CW%20CCT).groovy") {
        
        capability "Actuator"
        capability "Color Temperature"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
    }
    
    preferences {  
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
		
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
        input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)
		
		input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)
		
		input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
            description: "Number of failed pings before reconnecting device.", defaultValue: 3,
            required: true, displayDuringSetup: true)


        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)
		
		input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 60. Recommended: Under 200", defaultValue: 60,
            required: true, displayDuringSetup: true)
		
		input(name:"deviceWWTemperature", type:"number", title: "Warm White rating of this light",
            description: "Temp in K (default 2700)", defaultValue: 2700,
            required: false, displayDuringSetup: true)

        input(name:"deviceCWTemperature", type:"number", title: "Cold White Rating of this light",
            description: "Temp in K (default 6500)", defaultValue: 6500,
            required: false, displayDuringSetup: true)
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

def setLevel(level, duration=0) {
    // Set the brightness of a device (0-100)
	level > 100 ? (level = 100) : null
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    
    setColorTemperature(device.currentValue('colorTemperature'), level)
}
	
def setColorTemperature( setTemp = device.currentValue("colorTemperature"), deviceLevel = device.currentValue("level") ){
    // Adjust the color temperature of a device  
    
	sendEvent(name: "colorTemperature", value: setTemp)
	logDebug "Color Temperature set to ${setTemp}"

	brightnessWW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature ) )
	brightnessCW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceCWTemperature, settings.deviceWWTemperature ) )

	if( brightnessWW + brightnessCW > 100 ){
	   brightnessWW = brightnessWW / (( brightnessWW + brightnessCW ) / 100 )
	   brightnessCW = brightnessCW / (( brightnessWW + brightnessCW ) / 100 )
	}

	sendEvent(name: "warmWhiteLevel", value: brightnessWW)
	sendEvent(name: "coldWhiteLevel", value: brightnessCW)
   
	powerOnWithChanges()
    byte[] data =  appendChecksum( [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ] )
	sendCommand( data )
}

// ------------------- Helper Functions ------------------------- //



def invertLinearValue( neutralValue, value1, value2 ){
    // Determines how far from a point two values are 

    return (( 100 )/( value1 - value2 )) * neutralValue + ( 100 - ( 100 /( value1 - value2 )) * value1 )
}

def proportionalToDeviceLevel( value ){
    // Returns the value of a number proportionally to the device's brightness
    
    return value * device.currentValue('level') / 100
}

def calculateCTSaturation( coldWhite = true, offset ) {
        
    def CURVE
    def lowPoint
    def highPoint
    
    if( coldWhite ) {
        lowPoint = settings.cwSaturationLowPoint < settings.cwSaturationHighPoint ? settings.cwSaturationLowPoint : settings.cwSaturationHighPoint
        highPoint = settings.cwSaturationHighPoint > settings.cwSaturationLowPoint ? settings.cwSaturationHighPoint : settings.cwSaturationLowPoint
        CURVE = 1.8
    }
    else{ 
        lowPoint = settings.wwSaturationLowPoint < settings.wwSaturationHighPoint ? settings.wwSaturationLowPoint : settings.wwSaturationHighPoint
        highPoint = settings.wwSaturationHighPoint > settings.wwSaturationLowPoint ? settings.wwSaturationHighPoint : settings.wwSaturationLowPoint
        CURVE = 2.16666
    }
    
    return (((( 100 - lowPoint  ) / 100 ) * ( CURVE * Math.sqrt( offset ))) + lowPoint  ) * highPoint / 100
}      

def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)

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
                //if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
                //}
            }
            else{
                //if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
               // }
            }
            break;
        
        case 14:
            logDebug( "Received general-status packet of ${response}" )
        
            if( responseArray[2] == 35 ){
                //if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
               // }
            }
            else{
               //if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
               // }
            }
            break;
        
        case null:
            logDebug "Null response received from device"
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
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
	
	logDebug "Number of failed responses: ${state.noResponse}"
	state.noResponse++
    state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}
def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${settings.reconnectPings-state.noResponse} more failed attempt(s)."
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
        schedule("0/${limit(settings.refreshTime, 1, 59)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    telnetClose()
    
    pauseExecution(1000)
    
    if( data.firstRun || ( now() - state.lastConnectionAttempt) > limit(settings.refreshTime, 1, 60) * 500 /* Breaks infinite loops */ ) {
        def tryWasGood = false
        try {
            logDebug "Opening Socket Connection."
            InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
            pauseExecution(1000)
            logDescriptionText "Connection successfully established"
            tryWasGood = true
    
        } catch(e) {
            logDebug("Error attempting to establish socket connection to device.")
            logDebug("Next initialization attempt in ${settings.refreshTime} seconds.")
            settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
            tryWasGood = false
        }
	    
	    if(tryWasGood){
	    	unschedule()
	    	logDebug "Stopping connectDevice loop. Starting refresh loop"
	    	schedule("0/${limit(settings.refreshTime, 1, 59)} * * * * ? *", refresh)
	    	state.noResponse = 0
	    }
        logdebug "Proper time has passed, or it is the device's first run." //jshimota changed log.debug to logdebug 10/12/21
        logdebug "${(now() - state.lastConnectionAttempt)} >= ${limit(settings.refreshTime, 1, 60) * 500}. First run: ${data.firstRun}" //jshimota changed log.debug to logdebug 10/12/21
        state.lastConnectionAttempt = now()
    }
    else{
        logdebug "Tried to connect too soon. Skipping this round." //jshimota changed log.debug to logdebug 10/12/21
        logdebug "X ${(now() - state.lastConnectionAttempt)} >= ${limit(settings.refreshTime, 1, 60) * 500}" //jshimota changed log.debug to logdebug 10/12/21
        state.lastConnectionAttempt = now()
    }
}

def initialize() {
    // Establish a connection to the device
    state.remove("initializeLoopRunning")
    state.remove("refreshRunning")
    state.remove("initializeLoop")
    state.remove("oldvariablename")
    
    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
	sendEvent(name: "switch", value: "off")
	sendEvent(name: "level", value: 100)
	sendEvent(name: "colorTemperature", value: 4000)
	state.noResponse = 0
}


