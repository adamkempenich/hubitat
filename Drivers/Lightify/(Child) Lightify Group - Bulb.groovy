/**
* Lightify Child - Group (0.22)
*
*  Author: 
*    Adam Kempenich
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*
*    0.22 (Feb 11, 2020)
*        - Initial Commit
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
    name: "Lightify Child - Group", 
    namespace: "Lightify", 
    author: "Adam Kempenich",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Lightify/(Child) Lightify Group - Bulb.groovy") {
    
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
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    sendEvent(name: "switch", value: "on")
    parent.groupOn(nomenclature[1])
}
def off(){
    // Turn the device off
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format
    
    sendEvent(name: "switch", value: "on")
    parent.groupOff(nomenclature[1])
}
def setLevel(levelValue, duration=0){
    // Update the brightness of  adevice 
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    sendEvent(name: "level", value: levelValue.toInteger())
    parent.setGroupLevel(nomenclature[1], levelValue.toInteger())   
}

def setHue(hueValue){
    // Update only the hue of a device
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    clamp(hueValue)
    
    logDescriptionText("Hue set to ${hueValue}")


    sendEvent(name: "saturation", value: hueValue.toFloat())
    def parameters = [hue: device.currentValue('hue'), saturation: hueValue, level: device.currentValue('level')]
    parent.setGroupColor(nomenclature[1], parameters)
    preStage()
}

def setSaturation(saturationValue){
    // Update only the saturation of a device
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    clamp(saturationValue)
    
    logDescriptionText("Saturation set to ${saturationValue}")


    sendEvent(name: "saturation", value: saturationValue.toFloat())
    def parameters = [hue: device.currentValue('hue'), saturation: saturationValue, level: device.currentValue('level')]
    parent.setGroupColor(nomenclature[1], parameters) 
    preStage()
}

def setColor(parameters){
    // Update the color of a device 
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    setParameters = [:]
    
    if(parameters.hue){setParameters.hue = clamp(parameters.hue)
    } else{ setParameters.hue = device.currentValue("hue")}
    
    if(parameters.saturation){setParameters.saturation = clamp(parameters.saturation)
    } else{ setParameters.saturation = device.currentValue("saturation") }
    
    if(parameters.level){setParameters.level = clamp(parameters.level)
    } else{ setParameters.level = device.currentValue("level") }

    sendEvent(name: "hue", value: setParameters.hue.toFloat())
    sendEvent(name: "saturation", value: setParameters.saturation.toFloat())
    sendEvent(name: "level", value: setParameters.level.toInteger())

    parent.setGroupColor(nomenclature[1], setParameters)
    preStage()
}


def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), levelValue = device.currentValue('level')){
    // Update the colorTemperature of a device
    def nomenclature = device.deviceNetworkId.split(" - ") // "${settings.deviceMAC} - ${group}" format

    clamp(colorTemperature.toInteger(), 1000, 8000)
    clamp(levelValue)
    
    sendEvent(name: "colorTemperature", value: colorTemperature)
    parent.setGroupColorTemperature(nomenclature[1], colorTemperature, levelValue)
    preStage()
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
