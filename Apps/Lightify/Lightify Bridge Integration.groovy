/**
* Lightify Bridge Integration — Parent App (0.21) 
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    0.21 (Feb 10, 2020)
*        - Added switch device
*        - Added CCT device
*        - Figured out how CCT is parsed
*        - Fixed an integer conversion issue with level
*        - Fixed device adding --- verifies packets are the correct length before parsing
*        
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
*        X add separate device types
*        X add power
*        X add brightness
*        X add color
*        / add hsl
*        X add color temperature
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
import hubitat.device.Protocol
import hubitat.helper.ColorUtils


definition(
    name: "Lightify Bridge Integration",
    namespace: "Lightify",
    author: "Adam Kempenich",
    description: "Locally connect to your Lightify gateway and control devices",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Lightify/Lightify%20Bridge%20Integration.groovy",
    singleInstance: false
    )


preferences {
	section("Lightify Details") {
        
        // Refresh bulbs
        // Refresh groups
        
        input "deviceIP", "text", title: "Lightify Gateway IP Address (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Device Port (Default: 4000)", required: true, defaultValue: 4000
        input "deviceMAC", "text", title: "Gateway MAC Address e.g. E.G. OSR010203A4. <b>DO NOT CHANGE THIS VALUE AFTER YOUR INPUT IT!</b>", required: true, defaultValue: "OSR010203A4" 

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
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "switch", value: "on")

    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    def byte[] data = [0x0F, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], 0x01]
    sendCommand(data)
}



def off(childID){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "switch", value: "off")

    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    def byte[] data = [0x0F, 0x00, 0x00, 0x32, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], 0x00]
    sendCommand(data)
}

def allOn() {
    // Turn on the device
    
    def childDevice = getChildDevice(settings.deviceMAC)
    childDevice.sendEvent(name: "switch", value: "on")
    
    byte[] data = [0x0f, 0x00, 0x00, 0x32, 0x01, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x01] // all on
    sendCommand(data)
}

def allOff() {
    // Turn off the device

    def childDevice = getChildDevice(settings.deviceMAC)
    childDevice.sendEvent(name: "switch", value: "off")
    
    byte[] data = [0x0f, 0x00, 0x00, 0x32, 0x01, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00] // all off
    sendCommand(data)
}

def setOn(childID){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "switch", value: "on")
}

def setOff(childID){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "switch", value: "off")
}

def setHue(childID, hue){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "hue", value: hue.toFloat())

}

def setSaturation(childID, saturation){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "saturation", value: saturation.toFloat())
}

def setLevel(childID, brightness, duration=0) {
    // Set the brightness of a device (0-100)
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    clamp(brightness)
    
    def byte[] data = [0x11, 0x00, 0x00, 0x31, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], brightness, 0x00, 0x00]
    sendCommand(data)
}

def updateChildLevel(childID, level){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "level", value: level.toFloat())
}

def setColor(childID, parameters ){
    
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    
    def rgbColors = ColorUtils.hsvToRGB([parameters.hue.toFloat(), parameters.saturation.toFloat(), parameters.level.toFloat()])
    
    def byte[] data = [0x14, 0x00, 0x00, 0x36, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], rgbColors[0], rgbColors[1], rgbColors[2], 0, 0x00, 0x00]
    sendCommand(data)
}
def updateChildColor(childID, parameters){
    def childDevice = getChildDevice(childID)
    if(parameters.hue){ childDevice.sendEvent(name: "hue", value: parameters.hue.toFloat()) }
    if(parameters.saturation){ childDevice.sendEvent(name: "saturation", value: parameters.saturation.toFloat()) }
    if(parameters.level){ childDevice.sendEvent(name: "level", value: parameters.level.toFloat()) }
}

def setColorTemperature(childID, setTemp, deviceLevel ){
    def byte[] deviceID = HexUtils.hexStringToByteArray(childID)
    
    setTemp = setTemp.toInteger()
    def byte[] setTempByte = [(setTemp & 0xFF), ((setTemp >> 8) & 0xFF)]
    
    def byte[] data = [0x12, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], setTempByte[0], setTempByte[1], 0x00, 0x00]
    sendCommand(data)
}

def updateChildColorTemperature(childID, colorTemperature){
    def childDevice = getChildDevice(childID)
    childDevice.sendEvent(name: "colorTemperature", value: colorTemperature.toInteger())

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
    
    def responseArray = HexUtils.hexStringToIntArray(response)  
    
    def devices = [:]
    
    switch(responseArray.length) {
        case 20:
        logDebug "Response Length: 20. Data: ${responseArray.length}"
        logDebug "Data is: ${responseArray}"
            break;
        case {it > 20}:
            logDebug "Response Length: 20. Data: ${responseArray.length}"

        
            def totalDevices = (responseArray.length - 11)/50
           // logDebug "${responseArray[9]} devices compared to ${totalDevices}. Byte comparison: ${(responseArray[9] * 50) + 11} - ${responseArray.length}."

            // def totalDevices = responseArray[9]
        if((responseArray[9] * 50) + 11 == responseArray.length){
            for(thisDevice = 0; thisDevice < totalDevices; thisDevice++){
                def location = 11 + (thisDevice * 50) // Devices start at byte 11 (from zero. 0-10 are gateway data) Each device's data is 50 bytes long
                // Create a locator 

                def deviceID = [responseArray[location], responseArray[location+1]] // Not sure if we need this
                //log.debug "deviceID: ${HexUtils.intArrayToHexString(*deviceID)}" // is this little endian, too?
               
                def deviceMAC = []
                for( i=2; i < 10; i++ ){
                    deviceMAC += responseArray[location + i]
                }
                def macString = HexUtils.intArrayToHexString(*deviceMAC) // Store as child device's DNI
                
                def deviceType = responseArray[location+10] // Create different child device depending on this
                
                def deviceFirmware = []
                for( i=11; i < 15; i++ ){
                    deviceFirmware += responseArray[location + i]
                } // Not super important but maybe worth storing in child device
                
                def deviceOnline = responseArray[location+15] // Not sure what to do with this --- check Mike's drivers then ask Mike/Chuck
                
                def deviceGroupID = [responseArray[location+16], responseArray[location+17]] // Not quite sure what to do with this
                
                def deviceSwitchStatus = responseArray[location+18] // 0 is off, 1 is on // Add to child device status 
                
                def deviceLevel = responseArray[location+19] // Add to child device level
                
                def byte[] temperatureArray = [responseArray[location+21], responseArray[location+20]]
                def deviceTemperature = Integer.parseInt(HexUtils.byteArrayToHexString(temperatureArray), 16)
                
                def deviceHSV = ColorUtils.rgbToHSV([responseArray[location+22]/2.55.toDouble(), responseArray[location+23]/2.55.toDouble(), responseArray[location+24]/2.55.toDouble()])
                
                def deviceWhite = responseArray[location+25] // Learn what this is
                
                def deviceName = []
                for(i=26; i < 50; i++){
                    // if we read an end of line, break
                    if(responseArray[location + i] == 0 && responseArray[location + i + 1] == 0 && responseArray[location + i + 2] == 0 ){
                        break
                    } 
                    deviceName += responseArray[location + i]
                    
                }
                
                def deviceNameToBytes = HexUtils.intArrayToHexString(*deviceName)
                def String friendlyDeviceName = new String(HexUtils.hexStringToByteArray(deviceNameToBytes), "UTF-8")
                
                log.debug "Device name: ${friendlyDeviceName} has type ${deviceType}, its switch status is ${deviceSwitchStatus} and its online status ${deviceOnline}"
                if(deviceType == 10){ // RGBW = 10
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline  == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "hue", value: deviceHSV[0].toFloat())
                        childDevice.sendEvent(name: "saturation", value: deviceHSV[1].toFloat())
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                        childDevice.sendEvent(name: "colorTemperature", value: deviceTemperature.toInteger())
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                        log.debug "${e}"
                        addChildDevice("Lightify", "Lightify Bulb - RGBW", "${macString}", null, [label: "${friendlyDeviceName}"])

                    }
                }
                else if(deviceType == 2){ // CCT = 2
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline  == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                        childDevice.sendEvent(name: "colorTemperature", value: deviceTemperature.toInteger())
                        
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                        
                        log.debug "${e}"
                        addChildDevice("Lightify", "Lightify Bulb - CCT", "${macString}", null, [label: "${friendlyDeviceName}"])

                    }
                }
                else if(deviceType == 1){ // Switch/Plug = 2
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline  == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                        childDevice.sendEvent(name: "colorTemperature", value: deviceTemperature.toInteger())
                        
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                        
                        log.debug "${e}"
                        addChildDevice("Lightify", "Lightify Bulb - Switch", "${macString}", null, [label: "${friendlyDeviceName}"])

                    }
                }
                else if(deviceType == 4){ //Dimmable = 4
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                        log.debug "${e}"
                        try{
                        addChildDevice("Lightify", "Lightify Bulb - Dimmable", "${macString}", null, [label: "${friendlyDeviceName}"])
                        } catch(creationError){
                            "That device already exists"
                        }
                    }
                } 
                else if(deviceType == 8){ // RGB = 8
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline  == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "hue", value: deviceHSV[0].toFloat())
                        childDevice.sendEvent(name: "saturation", value: deviceHSV[1].toFloat())
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                        log.debug "${e}"
                        addChildDevice("Lightify", "Lightify Bulb - RGB", "${macString}", null, [label: "${friendlyDeviceName}"])

                    }
                }
                else{
                    try{
                        def childDevice = getChildDevice(macString)
                        if(deviceSwitchStatus == 0 || deviceOnline == 0){
                            childDevice.sendEvent(name: "switch", value: "off")
                        }
                        else{
                            childDevice.sendEvent(name: "switch", value: "on")
                        }
                        childDevice.sendEvent(name: "hue", value: deviceHSV[0].toFloat())
                        childDevice.sendEvent(name: "saturation", value: deviceHSV[1].toFloat())
                        childDevice.sendEvent(name: "level", value: deviceLevel)
                        childDevice.sendEvent(name: "colorTemperature", value: deviceTemperature.toInteger())
                    } catch(e){ // Device does not exist
                        // addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:]) 
                       log.debug "${e}"
                        try{
                            addChildDevice("Lightify", "Lightify Bulb - RGBW", "${macString}", null, [label: "${friendlyDeviceName}"])
                        } catch(creationError){
                            "That device already exists"
                        }
                    }
                }
            }
        }
        
        //logDebug "Device table: ${devices}"
        break;
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
    
    def childDevice = getChildDevice(settings.deviceMAC)
    //childDevice.sendEvent(name: "switch", value: "${deviceSwitchStatus == 0 ? 'off' : 'on'}")
    childDevice.sendCommand( data )
}

def refresh( ) {
	
    def childDevice = getChildDevice(settings.deviceMAC)
    childDevice.refresh()
}	

def updated(){
    // check if child device exists with MAC (if mac exists), and make one if it doesn't
    
        initialize()
    
}

def uninstalled(){
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def initialize() {
    // Establish a connection to the device
    
    if(settings.deviceMAC != null && settings.deviceMAC != "" && settings.deviceMAC != "OSR010203A4"){
        try{
            def childDevice = getChildDevice(settings.deviceMAC)
            childDevice.setIPState(settings.deviceIP)
            childDevice.setPortState(settings.devicePort)
            childDevice.setMACState(settings.deviceMAC)
            childDevice.setRefreshTimeState(settings.refreshTime)
            childDevice.setReconnectPingsState(settings.reconnectPings)

            //childDevice.state.deviceIP = settings.deviceIP
            //childDevice.state.devicePort = settings.devicePort
            //childDevice.state.deviceMAC = settings.deviceMAC
            //childDevice.state.refreshTime = settings.refreshTime
            //childDevice.state.reconnectPings = settings.reconnectPings
            childDevice.connectDevice([firstRun: true])
        } catch(e){ // Device does not exist
            //addChildDevice(String namespace, String typeName, String deviceNetworkId, Map properties = [:])
            addChildDevice("Lightify", "Lightify Bridge - Gateway", "${settings.deviceMAC}")
        }
    }
    else{ // do nothing 
        log.info "App settings updated. Please update your device's IP and MAC addresses."
        log.warn "After you update your MAC address, <b>Do not change the value again!</b>"
    }
}

def installed(){
    // Need to initialize anything?
}

