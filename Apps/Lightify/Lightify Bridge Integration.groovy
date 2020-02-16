/**
* Lightify Bridge Integration — Parent App (0.22) 
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    0.23 (Feb 17, 2020) - Tasks due
*        — Add Scenes
*        — Add proper discovery/update/deletion
*        — Add more refresh times
*        — Add transition speeds
*        - Add device Rename
*        - Fix group name parsing
*        - Update group status somehow
*        - Finish group moving when deleted in app
*        - Conjoin data packets option
*
*    0.22 (Feb 15, 2020)
*        - Added Groups
*        — Continued to build out device types
*        — Fixed child naming schema
*        - Updated settings pages a lot
*        - Beefed up initialization routine/error checking
*        - Added color prestaging and child description text
*
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
*
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
*        X add hsl
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
    page name: "mainPage", install: true, uninstall: true
    page name: "manageDevices"
    page name: "manageGroups"
    page name: "manageScenes"
}



def mainPage(){
    dynamicPage(name: "mainPage") {
        
        state.discoveredDevices == null ? state.discoveredDevices = [:] : null
        state.discoveredGroups == null ? state.discoveredGroups = [:] : null
        state.initialSetupComplete == null ?  state.initialSetupComplete = false : null
        
        section("Credits:", hideable: true, hidden: true) {
            paragraph "<b>Developed by:</b>"
            paragraph "- Adam Kempenich (@AdamKempenich)."
        }
        
        section("<h2>App Name</h2>"){
	    		label title: "Enter a name for this app (optional)", required: false
        }   
	    section("Lightify Details") {
                        
            input "deviceIP", "text", title: "Lightify Gateway IP Address (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
            input "devicePort", "number", title: "Device Port (Default: 4000)", required: true, defaultValue: 4000
            input "deviceMAC", "text", title: "Gateway MAC Address e.g. E.G. 010203A4. <b>DO NOT CHANGE THIS VALUE AFTER YOUR INPUT IT!</b>", required: true, defaultValue: "010203A4" 
            
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
        if(state.initialSetupComplete){ // Don't allow editing groups if the setup isn't completed.
            section(){
                href(name: "toManageDevices",
                        title: "<b>Manage Devices</b>",
                        page: "manageDevices",
                        description: "Add or rename devices."
                )
            }
            section(){
                href(name: "toManageGroups",
                        title: "<b>Manage Groups</b>",
                        page: "manageGroups",
                        description: "Add or rename groups."
                )
            }
        }
    }
}

def manageDevices(){
    dynamicPage(name: "manageDevices"){
        
        refresh()
        
        //section("<h2 id='propogating'>Please wait while propogating devices (10 seconds..)</h2>"){
        //    pauseExecution(10000) // Wait for devices to propogate
        //    paragraph "<style>#propogating{display:none;</style>"
        //}
        //
        // cron add devices - 15s
        //def childDevice = getChildDevice(deviceMAC)
        //def devices = childDevice.refresh(true)
        // end cron        
        //settings.checkedDevices = ""
        
        //settings.renameDevices = false
        //addChildDevice("Lightify", "Lightify Bulb - RGBW", "${macString}", null, [label: "${friendlyDeviceName}"])
        //state.discoveredDevices = [:]
        //log.debug "${state.discoveredDevices}"
        section("<h2>Select Devices</h2>") {
            input(name: "checkedDevices", type: "enum", title: "Add:", multiple: true, options: state.discoveredDevices)
            //input(name: "renameDevices", type: "bool", title: "Update names of checked devices?", default: false)
            
            paragraph "To rename or delete devices, remove them from their device page in Hubitat"
        } 
    }
}
def manageGroups(){
    dynamicPage(name: "managegroups"){
        
        getGroups()
        
        pauseExecution(5000)
        
        //section("<h2 id='propogating'>Please wait while propogating devices (10 seconds..)</h2>"){
        //    pauseExecution(10000) // Wait for devices to propogate
        //    paragraph "<style>#propogating{display:none;</style>"
        //}
        //
        // cron add devices - 15s
        //def childDevice = getChildDevice(deviceMAC)
        //def devices = childDevice.refresh(true)
        // end cron        
        //settings.checkedDevices = ""
        
        //settings.renameDevices = false
        //addChildDevice("Lightify", "Lightify Bulb - RGBW", "${macString}", null, [label: "${friendlyDeviceName}"])
        //state.discoveredDevices = [:]
        //log.debug "${state.discoveredDevices}"

        //    state.discoveredGroups.put("${groupID}", "${friendlyGroupName}")

        section("<h2>Select Groups</h2>") {
            input(name: "checkedGroups", type: "enum", title: "Add:", multiple: true, options: state.discoveredGroups)
            //input(name: "renamegroups", type: "bool", title: "Update names of checked devices?", default: false)
            
            paragraph "To rename or delete devices, remove them from their device page in Hubitat"
        } 
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

/* ——————————————————————————————————— GROUPS ——————————————————————————————————— */
def groupOn(childID){
    // Turns on a group 
    
    def deviceID = childID.toInteger()

    def byte[] data = [0x0F, 0x00, 0x02, 0x32, 0x04, 0x00, 0x00, 0x00, deviceID, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01]
    sendCommand(data)
}

def groupOff(childID){
    // Turns on a group 

    def deviceID = childID.toInteger()

    def byte[] data = [0x0F, 0x00, 0x02, 0x32, 0x04, 0x00, 0x00, 0x00, deviceID, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]
    sendCommand(data)
}

def setGroupLevel(childID, brightness, duration=0) {
    // Set the brightness of a group (0-100)
    
    def deviceID = childID.toInteger()

    clamp(brightness)
    def byte[] data = [0x12, 0x00, 0x02, 0x31, 0x06, 0x00, 0x00, 0x00, deviceID, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, brightness, 0x00, 0x00, 0x00]
    sendCommand(data)
}

def setGroupColor(childID, parameters ){
    // Change the color of a group

    def deviceID = childID.toInteger()

    
    def rgbColors = ColorUtils.hsvToRGB([parameters.hue.toFloat(), parameters.saturation.toFloat(), parameters.level.toFloat()])
    //                 14      00    02    36    05   00    00    00   04             00             00         00             00             00         00             00         ff         0d             01         ff  00     00
    //def byte[] data = [0x14, 0x00, 0x02, 0x36, 0x05, 0x00, 0x00, 0x00, deviceID[0], deviceID[1], deviceID[2], deviceID[3], deviceID[4], deviceID[5], deviceID[6], deviceID[7], rgbColors[0], rgbColors[1], rgbColors[2], 0x00, 0x00, 0x00]

    def byte[] data = [0x14, 0x00, 0x02, 0x36, 0x05, 0x00, 0x00, 0x00, deviceID, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, rgbColors[0], rgbColors[1], rgbColors[2], 0x00, 0x00, 0x00]

    sendCommand(data)
}

def setGroupColorTemperature(childID, setTemp, deviceLevel ){
    // Change the color temperature of a group

    def deviceID = childID.toInteger()
    
    setTemp = setTemp.toInteger()
    def byte[] setTempByte = [(setTemp & 0xFF), ((setTemp >> 8) & 0xFF)]
    def byte[] data = [0x12, 0x00, 0x02, 0x33, 0x07, 0x00, 0x00, 0x00, deviceID, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, setTempByte[0], setTempByte[1], 0x00, 0x00]
    sendCommand(data)
}

/* ——————————————————————————————————— END GROUPS ——————————————————————————————————— */


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

def getGroups(){
    // Gets a list of groups
    
    byte[] data = [0x0f, 0x00, 0x00, 0x1e, 0x01, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x00] // all off

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
    def groupTotals = []
    for(i = 1; i++; i < 16){ 
        groupTotals += (11 + (18 * i))
    }
    //log.trace "${response}"
    logDebug "responseArray[0] is ${responseArray[0]}"
    switch(responseArray.length) {
        //case {responseArray[0] == 18}:
        case 20:
            logDebug "Response Length: 20. Data: ${responseArray.length} : ${response}"
            logDebug "Data is: ${responseArray}. array[0] is ${responseArray[0]}"
        break;
        
        case {groupTotals.contains(responseArray.length)}: //0x51 packet for my groups
            logDebug "Parsing groups from response ${response}"
            
            def totalGroups = responseArray[9]
            logDebug "${totalGroups} Groups"
        
            for(thisGroup = 0; thisGroup < totalGroups; thisGroup++){
                def location = 11 + (thisGroup * 18) // Groups start at byte 11 (from zero. 0-10 are gateway data) Byte 0 of each group is its id, 1-16 are name, 17 is termination
                
                def groupID = responseArray[location]
                logDebug "${groupID}"
                
                def groupName = []
                for(i=2; i < 18; i++){
                   //// if we read an end of line, break
                   //if(responseArray[location + i] == 0 && responseArray[location + i + 1] == 0 && responseArray[location + i + 2] == 0 ){
                   //    break
                   //} 
                    groupName += responseArray[location + i]
                    
                }
                
                def groupNameToBytes = HexUtils.intArrayToHexString(*groupName)
                def String friendlyGroupName = new String(HexUtils.hexStringToByteArray(groupNameToBytes), "UTF-8")
                
                logDebug "${friendlyGroupName}"
                
                state.discoveredGroups.put("${groupID}", "${friendlyGroupName}")
                
                // check if device ${settings.deviceMAC}-${groupID}${friendlyGroupName}
            }
        
            break;
        

        case {it > 20}:
            // 91 is entire status packet
            
            //responselength > 861 ? logDebug("<h2>Response Length: >20. Data: ${responseArray.length}. array[0] is ${responseArray[0]}</h2>") : null

            def totalDevices = (responseArray.length - 11)/50
            def deviceTypes = [1: "Switch", 2: "CCT", 4: "Dimmable", 8: "RGB", 10: "RGBW"]
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
                
                //log.trace "R: ${responseArray[location+22]} G: ${responseArray[location+23]} B: ${responseArray[location+24]}"
                def deviceHSV = ColorUtils.rgbToHSV([responseArray[location+22], responseArray[location+23], responseArray[location+24]])
                
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
                def String friendlyDeviceType

                try{ friendlyDeviceType = deviceTypes[deviceType]
                } catch(deviceTypeError){
                    friendlyDeviceType = "Generic"
                }

                state.discoveredDevices.put("${macString}", "${friendlyDeviceName} - ${friendlyDeviceType}")
                
                
                //test group creation
                //addChildDevice("Lightify", "Lightify Child - Group", "0400000000000000", null, [label: "${friendlyDeviceName}"])
                
                //logDebug "Device name: ${friendlyDeviceName} has type ${deviceType}, its switch status is ${deviceSwitchStatus} and its online status ${deviceOnline}"

                /* --------------------------- Switch/Plug Devices --------------------------- */
                if(deviceType == 1){
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
                        //logDebug "This device has not been created. Skipping it."

                    }
                }
                /* --------------------------- CCT Devices --------------------------- */
                else if(deviceType == 2){
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
                        //logDebug "This device has not been created. Skipping it."
                    }
                }
                /* --------------------------- Dimmable Devices --------------------------- */
                else if(deviceType == 4){
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
                        //logDebug "This device has not been created. Skipping it."

                    }
                } 
                /* --------------------------- RGB Devices --------------------------- */
                else if(deviceType == 8){ 
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
                        //logDebug "This device has not been created. Skipping it."

                    }
                }
                else if(deviceType == 10){
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
                        //logDebug "This device has not been created. Skipping it."

                    }
                }
                /* --------------------------- Generic Devices --------------------------- */
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
                        //logDebug "This device has not been created. Skipping it."

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
    
    logDebug "Initializing with MAC ${settings.deviceMAC}, IP ${settings.deviceIP}, Port ${settings.devicePort}, refreshTime ${settings.refreshTime}, and reconnect pings${settings.reconnectPings}"
    
    if(settings.deviceMAC != null && settings.deviceMAC != "" && settings.deviceIP != null && settings.deviceIP != "" && settings.devicePort != null && settings.devicePort != ""){
        try{
            def childDevice = getChildDevice(settings.deviceMAC)
            childDevice.setIPState(settings.deviceIP)
            childDevice.setPortState(settings.devicePort)
            childDevice.setMACState(settings.deviceMAC)
            childDevice.setRefreshTimeState(settings.refreshTime)
            childDevice.setReconnectPingsState(settings.reconnectPings)
            pauseExecution(1000)

            childDevice.initialize()
        } catch(e){ 
            // Device does not exist

            logDebug "Gateway child device probably doesn't exist. Error: ${e}"
            
            logDebug "Creating child gateway"
            try{
                addChildDevice("Lightify", "Lightify Bridge - Gateway", "${settings.deviceMAC}")
            } catch(gatewayCreationError){
                logDebug "Something went wrong when creating a child gateway."
            }
            
            pauseExecution(1000)
            
            try{
                def childDevice = getChildDevice(settings.deviceMAC)
                childDevice.setIPState(settings.deviceIP)
                childDevice.setPortState(settings.devicePort)
                childDevice.setMACState(settings.deviceMAC)
                childDevice.setRefreshTimeState(settings.refreshTime)
                childDevice.setReconnectPingsState(settings.reconnectPings)
                pauseExecution(1000)

                childDevice.initialize()
            }
            catch(applyChildSettingsError){
                logDebug "A new device was created successfully, but there was an issue attributing settings to it."
                logDebug "Failed with error ${applyChildSettingsError}"
            }    
        }
        
        state.initialSetupComplete = true
        
    }
    else{ // do nothing 
        log.info "App settings updated. Please update your device's IP and MAC addresses."
        log.warn "After you update your MAC address, <b>Do not change the value again!</b>"
    }
    
    // Add child devices to hubitat
    for (device in checkedDevices) {

        def nomenclature = state.discoveredDevices.get(device).split(" - ") // Mac string is device | nomenclature[0] is name | nomenclature[1] is type
        
        //if(renameDevices){
        //    try{
        //        addChildDevice("Lightify", "Lightify Child - ${nomenclature[1]}", "${device}", null, [label: "${nomenclature[0]}"])
        //    }
        //    catch(renameError){
        //        logDebug "Device exists already. Renaming"
        //        logDebug "(renaming coming in a future version?)"
        //        //def childDevice = getChildDevice(device)
	    //        //childDevice.updateSetting("settingName",[type:"text", value:value])
        //    }
        //} else{
            try{
                logDebug "Creating ${nomenclature[1]}-type Lightify device with the name ${nomenclature[0]}"
                addChildDevice("Lightify", "Lightify Child - ${nomenclature[1]}", "${device}", null, [label: "${nomenclature[0]}"])
            } catch(creationError){
                logDebug "Device already exists. Ignoring."
            }
        //}
    }
    for (group in checkedGroups) {
//                state.discoveredGroups.put("${groupID}", "${friendlyGroupName}")
        
        //if(renameGroups){
        //    try{
        //        addChildDevice("Lightify", "Lightify Child - Group", "${settings.deviceMAC} - ${group}", null, [label: "${state.discoveredGroups.get(group)}"])
        //    }
        //    catch(renameError){
        //        logDebug "Device exists already. Renaming"
        //        logDebug "(renaming coming in a future version?)"
        //        //def childDevice = getChildDevice(device)
	    //        //childDevice.updateSetting("settingName",[type:"text", value:value])
        //    }
        //} else{
            try{
                logDebug "Creating Group Lightify device with the name ${state.discoveredGroups.get(group)} and ID ${settings.deviceMAC} - ${group}"
                addChildDevice("Lightify", "Lightify Child - Group", "${settings.deviceMAC} - ${group}", null, [label: "${state.discoveredGroups.get(group)}"])
            } catch(creationError){
                logDebug "Device already exists. Ignoring."
            }
        //}
    }
    
    
    
    // Remove old devices from discovery map
    state.discoveredDevices = [:]

    //checkedDevices = [] //reset checked selection of devices
    //renameDevices = false //reset renamedevices setting
}

def installed(){
    initialize()    
}
