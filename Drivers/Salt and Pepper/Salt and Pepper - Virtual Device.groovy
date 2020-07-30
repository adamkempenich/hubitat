/**
* Salt and Pepper - Virtual Device
*
*  Author: 
*    Adam Kempenich
*
*  Documentation:  [Does not exist, yet]
*
*  Changelog:
*    1.0 - Initial Release
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
    name: "Salt and Pepper - Virtual Device", 
    namespace: "Salt and Pepper", 
    author: "Adam Kempenich",
    importUrl: "N/A") {
    
        capability "Actuator"
        capability "Color Temperature"
		capability "Initialize"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
	}
}

def on(){
    sendEvent(name: "switch", value: "on")
}
def off(){
    sendEvent(name: "switch", value: "off")
}
def setLevel(brightness, duration=0){
    sendEvent(name: "level", value: brightness.toInteger())
}

def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), level = device.currentValue('level')){
    sendEvent(name: "colorTemperature", value: colorTemperature.toInteger())
}

def installed(){
    sendEvent(name: "switch", value: "off")
    sendEvent(name: "level", value: 100)
    sendEvent(name: "colorTemperature", value: 4000)
    
}

def initialize(){
    // Do nothing
    
}

def updated(){
    // Do nothing
    
}



