/**
* Lightify Bridge - Local Control (0.1) 
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    0.10 (Jan 27, 2020)
*        X Create parent device
*        X send a test command to the bridge
*        - create structure for parent/children
*        - send command to individual devices
*        - send command to groups // need to research if it is possible
*        - add separate device types
*        X add power
*        X add brightness
*        - add color
*        - add hsl
*        - add color temperature - reverse engineer
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
        name: "Lightify Gateway", 
        namespace: "Lightify", 
        author: "Adam Kempenich",
		importUrl: "https://github.com/adamkempenich/hubitat/raw/master/Drivers/Lightify/lightify%20gateway.groovy") {
        
        capability "Actuator"
        capability "Color Control"
		capability "Color Mode"
        capability "Color Temperature"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
        
        command "allOn"
        command "allOff"

    }
    
    preferences {  
        input "deviceIP", "text", title: "Gateway IP", description: "Ligtify Gateway IP Address (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 4000)", required: true, defaultValue: 4000
		
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
        
         input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 10. Use number between 0-60", defaultValue: 10,
            required: true, displayDuringSetup: true)
	
        input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
            description: "Number of failed pings before reconnecting Lightify gateway.", defaultValue: 3,
            required: true, displayDuringSetup: true)
    }
}




def on(childID){
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    
    //def byte[] header = [0x0F, 0x00, 0x00, 0x32]
    //def byte[] randomData = [(Math.random() * 255).toInteger(),(Math.random() * 255).toInteger(),(Math.random() * 255).toInteger(),(Math.random() * 255).toInteger()]
    def byte[] data = [0x0F, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], 0x01]
    sendCommand(data)
}

def off(childID){
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    
    def byte[] data = [0x0F, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], 0x00]
    sendCommand(data)
}

def allOn() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    logDebug "Switch set to on" 
    
    byte[] data = [0x0f, 0x00, 0x00, 0x32, 0x01, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01] // all on
    sendCommand(data)
}

def allOff() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug "Switch set to off" 
    byte[] data = [0x0f, 0x00, 0x00, 0x32, 0x01, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00] // all off
    sendCommand(data)
}

def setHue(childID, hue){
    // Set the hue of a device ( 0 - 101 ) 

}

def setSaturation(childID, saturation){
    // Set the saturation of a device (0-100)

}

def setLevel(childID, brightness, duration=0) {
    // Set the brightness of a device (0-100)
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    clamp(brightness)
    
    def byte[] data = [0x11, 0x00, 0x00, 0x31, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], brightness, 0x00, 0x00]
    sendCommand(data)
}

def setColor(childID, parameters ){
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    
    def rgbColors = ColorUtils.hsvToRGB([parameters.hue.toFloat(), parameters.saturation.toFloat(), parameters.level.toFloat()])
    
    def byte[] data = [0x14, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], rgbColors[0], rgbColors[1], rgbColors[2], 0, 0x00, 0x00]
    sendCommand(data)


}

def setColorTemperature(childID, setTemp = device.currentValue("colorTemperature"), deviceLevel = device.currentValue("level") ){
    
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

def sendPreset(setPreset = 1, speed = 100){
    
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
        if(value < lowerBound ){ value = lowerBound }
        if(value > upperBound){ value = upperBound }
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound }
        if(value > lowerBound ){ value = lowerBound }
    }

    return value
}

def parse( response ) {
    // Parse data received back from this device
    
    state.noResponse = 0    
    def responseArray = HexUtils.hexStringToIntArray(response)  
    def devices = [:]
    
    switch(responseArray.length) {
        case 20:
        logDebug "Response Length: 20. Data: ${responseArray.length}"
            break;
        case {it > 20}:
            
            logDebug "${responseArray[9]} devices."
            def totalDevices = responseArray[9]
        
            for(thisDevice = 0; thisDevice < totalDevices; thisDevice++){
                def location = 11 + (thisDevice * 50) // Devices start at byte 11 (from zero. 0-10 are gateway data) Each device's data is 50 bytes long
                // Create a locator 

                def deviceID = [responseArray[location], responseArray[location+1]]
                // Not sure if we need this
                //log.debug "deviceID: ${HexUtils.intArrayToHexString(*deviceID)}"
               
                def deviceMAC = []
                for( i=2; i < 10; i++ ){
                    deviceMAC += responseArray[location + i]
                }
                def macString = HexUtils.intArrayToHexString(*deviceMAC)
                //log.trace "MAC: ${macString}"
                // Store as child device's DNI
                
                def deviceType = [responseArray[location+10] ]
                // Create different child device depending on this
                
                def deviceFirmware = []
                for( i=11; i < 15; i++ ){
                    deviceFirmware += responseArray[location + i]
                }
                // Not super important but maybe worth storing in child device
                
                def deviceOnline = [responseArray[location+15]]
                // Not sure what to do with this --- check Mike's drivers then ask Mike/Chuck
                
                def deviceGroupID = [responseArray[location+16], responseArray[location+17]]
                // Not quite sure what to do with this
                
                def deviceSwitchStatus = [responseArray[location+18]] // 0 is off, 1 is on
                // Add to child device status 
                
                def deviceBrightness = [responseArray[location+19]]
                // Add to child device level
                
                def deviceTemperature = [responseArray[location+20], responseArray[location+21]]
                // Figure out how this is stored
                
                def deviceRed = [responseArray[location+22]]
                def deviceGreen = [responseArray[location+23]]
                def deviceBlue = [responseArray[location+24]]
                // Convert RGB to HSL
                
                def deviceWhite = [responseArray[location+25]]
                // Learn what this is
                
                def deviceName = []
                for(i=26; i < 50; i++){
                    deviceName += responseArray[location + i]
                }
                
                def deviceNameToBytes = HexUtils.intArrayToHexString(*deviceName)
                def String friendlyDeviceName = new String(HexUtils.hexStringToByteArray(deviceNameToBytes), "UTF-8")
                

                try{
                    def childDevice = getChildDevice(macString)
                    childDevice.sendEvent(name: "switch", value: "${deviceSwitchStatus == 0 ? 'off' : 'on'}")
                } catch(e){ // Device does not exist
                    addChildDevice("Lightify Gateway — Child", "${macString}", [isComponent: false, label: "${friendlyDeviceName}"])
                }
                
                
                
            }
        //logDebug "Device table: ${devices}"
               
        case null:
            //logDebug "Null response received from device" // Apparently these get sent a lot
            break;
        
        default:
            logDebug "Received a response with a length of ${responseArray.length} containing ${response}"
            break;
    }
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
    state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
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

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/${clamp(settings.refreshTime, 1, 60)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    
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

    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
    state.noResponse = 0
    state.lastConnectionAttempt = now()
}
