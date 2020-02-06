/**
* Lightify Bulb - RGBW (0.2)
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
    name: "Lightify Bulb - RGBW", 
    namespace: "Lightify", 
    author: "Adam Kempenich",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Lightify/(Child)%20Lightify%20Bulb%20-%20RGBW.groovy") {
    
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
    sendEvent(name: "level", value: brightness.toFloat())
    parent.setLevel(device.deviceNetworkId, brightness.toFloat())   
}
def setColor(parameters){
    sendEvent(name: "hue", value: parameters.hue.toFloat())
    sendEvent(name: "saturation", value: parameters.saturation.toFloat())
    sendEvent(name: "level", value: parameters.level.toFloat())

    parent.setColor(device.deviceNetworkId, parameters)
}
def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), level = device.currentValue('level')){
    sendEvent(name: "colorTemperature", value: colorTemperature.toFloat())
    parent.setColorTemperature(device.deviceNetworkId, colorTemperature.toFloat(), level)
}

def initialize(){
    // Do nothing
}

def updated(){
    // Do nothing
}
