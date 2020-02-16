/**
* Lightify Child - CCT (0.22)
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
    name: "Lightify Child - CCT", 
    namespace: "Lightify", 
    author: "Adam Kempenich",
    importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Lightify/(Child)%20Lightify%20Child%20-%20CCT.groovy") {
    
        capability "Actuator"
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

def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), level = device.currentValue('level')){
    sendEvent(name: "colorTemperature", value: colorTemperature.toFloat())
    parent.setColorTemperature(device.deviceNetworkId, colorTemperature.toInteger(), level)
}

def initialize(){
    // Do nothing
}

def updated(){
    // Do nothing
}
