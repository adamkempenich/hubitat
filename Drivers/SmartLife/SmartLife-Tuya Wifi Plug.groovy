/**
*  SmartLife/Tuya Smart Plug - PROOF OF CONCEPT
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  [TBA]
*
*  Changelog:
*    Mar 17: 
*        - First Public Release
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
        name: "SmartLife/Tuya Wifi Plug", 
        namespace: "Tuya", 
        author: "Adam Kempenich") {
        
        capability "Actuator"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"

    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 6668)", required: true, defaultValue: 6668
		
        input "onArray1", "text", title: "On Array (1 of 4)", required: true, defaultValue: "AABBCC..."
        input "onArray2", "text", title: "On Array (2 of 4)", required: false, defaultValue: "AABBCC..."
        input "onArray3", "text", title: "On Array (3 of 4)", required: false, defaultValue: "AABBCC..."
        input "onArray4", "text", title: "On Array (4 of 4)", required: false, defaultValue: "AABBCC..."

        input "offArray1", "text", title: "Off Array (1 of 4)", required: true, defaultValue: "AABBCC..."
        input "offArray2", "text", title: "Off Array (2 of 4)", required: false, defaultValue: "AABBCC..."
        input "offArray3", "text", title: "Off Array (3 of 4)", required: false, defaultValue: "AABBCC..."
        input "offArray4", "text", title: "Off Array (4 of 4)", required: false, defaultValue: "AABBCC..."

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
            description: "Number of failed pings before reconnecting device.", defaultValue: 7,
            required: true, displayDuringSetup: true)

        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 10. Use number between 0-60", defaultValue: 10,
            required: true, displayDuringSetup: true)
    }
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    logDebug "Switch set to on" 
    def byteArray = settings.onArray1 + settings.onArray2 + settings.onArray3 + settings.onArray4
    byte[] data = HexUtils.hexStringToByteArray(byteArray)

    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug "Switch set to off" 
    def byteArray = settings.offArray1 + settings.offArray2 + settings.offArray3 + settings.offArray4
    byte[] data = HexUtils.hexStringToByteArray(byteArray)

    sendCommand(data)
}

// ------------------- Helper Functions ------------------------- //

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
        case null:
            logDebug "Null response received from device but hey, at least it's responding!"
            break;
        
        default:
            logDebug "Received a response with an length of ${responseArray.length} containing ${response}"
            break;
    }
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

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs
    
    if( settings.logDebug ) { 
        log.debug "Tuya (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    if( settings.logDescriptionText ) { 
        log.info "Tuya (${settings.deviceIP}): ${descriptionText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
	
	logDebug "refresh() called. Number of failed responses: ${state.noResponse}"
	state.noResponse++
    state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data = null
    if(device.currentValue("switch") == "on"){
        off()
    }
    else{
       on()
    }
}

def socketStatus( status ) { 
    logDescriptionText "A connection issue occurred."
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${limit(settings.reconnectPings, 0, 10)-state.noResponse} more failed attempt(s)."
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
        schedule("0/${limit(settings.refreshTime, 1, 60)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    
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
	    	schedule("0/${limit(settings.refreshTime, 1, 60)} * * * * ? *", refresh)
	    	state.noResponse = 0
	    }
        logDebug "Proper time has passed, or this is the first time this device has attempted to reconnect."
        state.lastConnectionAttempt = now()
    }
    else{
        logDebug "This device has tried to reconnect too quickly. Skipping this round."
        state.lastConnectionAttempt = now()
    }
}

def initialize() {
    // Establish a connection to the device
    
    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
	sendEvent(name: "switch", value: "off")
    state.noResponse = 0
    
    
    state.lastConnectionAttempt = now()
}
