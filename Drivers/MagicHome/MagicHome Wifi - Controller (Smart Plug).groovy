/**
*  MagicHome Wifi - Controller (Smart Plug) 0.89
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*    0.89 (Feb 1, 2019)
*        - Initial Creation
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
import hubitat.helper.ColorUtils

metadata {
    definition (
        name: "MagicHome Wifi — Controller (Smart Plug)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Controller%20(Smart%20Plug).groovy") {
        
        capability "Actuator"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
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

def checkInstantiation( map, setName ){
    // Description    

    if(map[setName]){ 
        sendEvent(name: setName, value: clamp(map[setName].value))
        return clamp(map[setName].value)
    } else { 
        return device.currentValue("${setName}")
    }
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)
		
        settings.enablePreStaging ? null : ( device.currentValue("switch") != "on" ? on() : null )
}
def clamp( value, lowerBound = 0, upperBound = 100 ){
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
        log.info "MagicHome (${settings.deviceIP}): ${descriptionText}"
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
    logDescriptionText "A connection issue occurred."
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${clamp(settings.reconnectPings, 0, 10)-state.noResponse} more failed attempt(s)."
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
        schedule("0/${clamp(settings.refreshTime, 1, 60)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    try{
    InterfaceUtils.socketClose(device)
    } catch(e){
        logDebug "Attempted to close a socket when there is none to close."
    }
    
    pauseExecution(1000)
    
    if( data.firstRun || ( now() - state.lastConnectionAttempt) > clamp(settings.refreshTime, 1, 60) * 500 /* Breaks infinite loops */ ) {
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
	    	schedule("0/${clamp(settings.refreshTime, 1, 60)} * * * * ? *", refresh)
	    	state.noResponse = 0
	    }
        log.debug "Proper time has passed, or it is the device's first run."
        log.debug "${(now() - state.lastConnectionAttempt)} >= ${clamp(settings.refreshTime, 1, 60) * 500}. First run: ${data.firstRun}"
        state.lastConnectionAttempt = now()
    }
    else{
        log.debug "Tried to connect too soon. Skipping this round."
        log.debug "X ${(now() - state.lastConnectionAttempt)} >= ${clamp(settings.refreshTime, 1, 60) * 500}"
        state.lastConnectionAttempt = now()
    }
}

def initialize() {
    // Establish a connection to the device
    device.currentValue("switch") == null ? sendEvent(name: "switch", value: "off") : null

    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
    state.noResponse = 0
    state.lastConnectionAttempt = now()
}
