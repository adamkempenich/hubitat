/**
*  Google Home Virtual Smoke Detector
*  Converts Hubitat Smoke events into events readable by the Community Google Home Integration
*
*  Author: 
*    Adam Kempenich 
*
*
*  Changelog:
*
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
    definition(
        name: "Google Home Smoke Detector",
        namespace: "adamkempenich", 
        author: "Adam Kempenich",
        importUrl: "") {

        capability "Smoke Detector"

        command "clear"
        command "detected"
        command "tested"

        attribute "SmokeLevel", "string"
        attribute "smokeLevelPPM", "number"
    }

    preferences {  

    }
}

def poll() {
    refresh()
}

def setSmokeLevel(){
}

def smoke(value){
    log.trace "smoke(${value})"}

def setSmoke(value){
    log.trace "setSmoke(${value})"
}

def detected() {
    sendSmokeAlarmEvent( 0 )
}

def clear() {
    sendSmokeAlarmEvent( 1 )
}

def tested() {
    sendSmokeAlarmEvent( 2 )
}

def sendSmokeAlarmEvent( value, isDigital=false ) {    // attributes: smoke ("detected","clear","tested")    ea.STATE, true, false).withDescription('Smoke alarm status'),  [dp=1] 
    
    def map = [:]
    
    map.value = value==0 ? "detected" : value==1 ? "clear" : value==2 ? "tested" : null

    if(value == 0) {
        sendEvent(name: "smokeLevel", value: "smoke detected")
        sendEvent(name: "smokeLevelPPM", value:  100000)

    }
    if(value == 1) {
        sendEvent(name: "smokeLevel", value: "no smoke detected")
        sendEvent(name: "smokeLevelPPM", value:  0)

    }
    
    map.name = "smoke"
    map.unit = ""
    map.type = isDigital == true ? "digital" : "physical"
    map.isStateChange = true
    map.descriptionText = "${map.name} is ${map.value}"
    sendEvent(map)
}
