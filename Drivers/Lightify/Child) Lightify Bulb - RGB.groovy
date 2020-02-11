/**
* Lightify Bulb - RGB (0.21)
*
*  Author: 
*    Adam Kempenich
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*
*    0.21 (Feb 10, 2020)
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
    name: "Lightify Bulb - RGB", 
    namespace: "Lightify", 
    author: "Adam Kempenich",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Lightify/(Child)%20Lightify%20Bulb%20-%20RGB.groovy") {
    
        capability "Actuator"
        capability "Color Control"
		capability "Color Mode"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
	}
}

def on(){
    sendEvent(name: "switch", value: "on")
    parent.on(device.deviceNetworkId)
}
def off(){
    sendEvent(name: "switch", value: "on")
    parent.off(device.deviceNetworkId)
}
def setLevel(brightness, duration=0){
    sendEvent(name: "level", value: brightness.toInteger())
    parent.setLevel(device.deviceNetworkId, brightness.toInteger())   
}

def setHue(hueValue){
    sendEvent(name: "saturation", value: hueValue.toFloat())
    def parameters = [hue: device.currentValue('hue'), saturation: hueValue, level: device.currentValue('level')]
    parent.setColor(device.deviceNetworkId, parameters)
}

def setSaturation(saturationValue){
    sendEvent(name: "saturation", value: saturationValue.toFloat())
    def parameters = [hue: device.currentValue('hue'), saturation: saturationValue, level: device.currentValue('level')]
    parent.setColor(device.deviceNetworkId, parameters)   
}

def setColor(parameters){
    sendEvent(name: "hue", value: parameters.hue.toFloat())
    sendEvent(name: "saturation", value: parameters.saturation.toFloat())
    sendEvent(name: "level", value: parameters.level.toInteger())

    parent.setColor(device.deviceNetworkId, parameters)
}
def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), level = device.currentValue('level')){
    // Add in my RGB <> CCT method from beta magichome drivers
}

def initialize(){
    // Do nothing
}

def updated(){
    // Do nothing
}


