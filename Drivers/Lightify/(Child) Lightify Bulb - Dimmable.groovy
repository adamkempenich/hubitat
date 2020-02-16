/**
* Lightify Bridge - Local Control (Child) (0.1) 
*
*  Author: 
*    Adam Kempenich
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    0.22 (Feb 25, 2020)
*	- Updated naming schema
*
*    0.21 - No changes
*
*    0.20 (Feb 04, 2020)
*        - Added parent/child structure
*        - Actually holds data now
*        - Does not have individual H/S setting
*
*    0.10 (Jan 27, 2020)
*        - Create parent app
*        - send a command to the bridge
*        - create structure for parent/children
*        - send command to individual devices
*        - send command to groups
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

metadata {
definition (
    name: "Lightify Child - Dimmable", 
    namespace: "Lightify", 
    author: "Adam Kempenich",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Lightify/(Child)%20Lightify%20Bulb%20-%20Dimmable.groovy") {
    
        capability "Actuator"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
	}
    preferences {  
         input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)
  
        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)
    }
}

def on(){
    // Turn the device on
    
    sendEvent(name: "switch", value: "on")
    parent.on(device.deviceNetworkId)
}
def off(){
    // Turn the device off
    
    sendEvent(name: "switch", value: "on")
    parent.off(device.deviceNetworkId)
}
def setLevel(levelValue, duration=0){
    // Update the brightness of  adevice 
    
    sendEvent(name: "level", value: levelValue.toInteger())
    parent.setLevel(device.deviceNetworkId, levelValue.toInteger())   
}

def initialize(){
    // Do nothing
    
}

def updated(){
    // Do nothing
    
}

def refresh(){
    // Request an info packet from the gateway
    
    parent.refresh()
}

def preStage(){
    // Turn on a light when values change if this setting is false
    
    settings.enableColorPrestaging ? null : ( device.currentValue("switch") == "off" ? on() : null )
}

def logDescriptionText(text){
    // Log device changes if set to
    
    settings.logDescriptionText == true ? log.info("${device.deviceNetworkId}: ${text}") : null   
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
