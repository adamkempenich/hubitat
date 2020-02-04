/**
* Lightify Bridge - Gateway (0.2) 
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    0.20 (Feb 04, 2020)
*        - Added parent/child structure
*        - Actually holds data now
*        - Does not have individual H/S setting
*        - Device names propogate correctly
*        - Lots of instantiation/parsing changes
*    0.10 (Jan 27, 2020)
*        X Create parent device
*        X send a test command to the bridge
*        X create structure for parent/children
*        X send command to individual devices
*        - send command to groups 
*        / add separate device types
*        X add power
*        X add brightness
*        X add color
*        / add hsl
*        / add color temperature - reverse engineer > Transmits only
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
        name: "Lightify Bridge - Gateway", 
        namespace: "Lightify", 
        author: "Adam Kempenich",
		importUrl: "!!!") {
        
        capability "Actuator"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
    }
    
    preferences {  
        
        //state.deviceIP
        //state.devicePort
        //state.deviceMAC
        //state.refreshTime
        //state.reconnectPings
        
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
        
         input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)

    }
}




def on(childID){
    parent.allOn()
    sendEvent(name: "switch", value: "on")
}

def off(childID){
    parent.allOff()
    sendEvent(name: "switch", value: "off")
}

def setIPState( value ){
    state.deviceIP = value
}
def setPortState( value ){
        state.devicePort = value
}
def setMACState( value ){
        state.deviceMac = value
}
 def setRefreshTimeState( value ){
        state.refreshTime = value
 }
def setReconnectPingsState( value ){
        state.reconnectPings = value
}

def parse( response ) {
    // Parse data received back from this device
    state.noResponse = 0
    parent.parse( response )
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs
    
    if( settings.logDebug ) { 
        log.debug "Lightify (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    if( settings.logDescriptionText ) { 
        log.info "Lightify (${settings.deviceIP}): ${descriptionText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "Transmitting: ${stringBytes}"
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
	
	logDebug "Number of failed responses: ${state.noResponse}"
	state.noResponse++
    state.noResponse >= state.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data =  [0x0B, 0x00, 0x00, 0x13, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00]
    sendCommand(data)
}	

def socketStatus( status ) { 
    logDescriptionText "A connection issue occurred."
    logDebug "socketStatus: ${status}"
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}

def uninstalled(){
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound }
        if(value > upperBound){ value = upperBound }
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound }
        if(value > lowerBound ){ value = lowerBound }
    }

    return value
}

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/${clamp(state.refreshTime, 1, 60)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    //InterfaceUtils.socketClose(device)
    
    pauseExecution(1000)
    
    if( data.firstRun || ( now() - state.lastConnectionAttempt) > clamp(state.refreshTime, 1, 60) * 500 /* Breaks infinite loops */ ) {
        def tryWasGood = false
        try {
            logDebug "Opening Socket Connection."
            InterfaceUtils.socketConnect(device, state.deviceIP, state.devicePort.toInteger(), byteInterface: true)
            pauseExecution(1000)
            logDescriptionText "Connection successfully established"
            tryWasGood = true
    
        } catch(e) {
            logDebug("Error attempting to establish socket connection to device.")
            logDebug("Next initialization attempt in ${state.refreshTime} seconds.")
            state.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
            tryWasGood = false
        }
	    
	    if(tryWasGood){
	    	unschedule()
	    	logDebug "Stopping connectDevice loop. Starting refresh loop"
	    	schedule("0/${clamp(state.refreshTime, 1, 60)} * * * * ? *", refresh)
	    	state.noResponse = 0
	    }
        log.debug "Proper time has passed, or it is the device's first run."
        log.debug "${(now() - state.lastConnectionAttempt)} >= ${clamp(state.refreshTime, 1, 60) * 500}. First run: ${data.firstRun}"
        state.lastConnectionAttempt = now()
    }
    else{
        log.debug "Tried to connect too soon. Skipping this round."
        log.debug "X ${(now() - state.lastConnectionAttempt)} >= ${clamp(state.refreshTime, 1, 60) * 500}"
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
    state.noResponse = 0
    state.lastConnectionAttempt = now()
}
