/**
*  Unifi Access Lock driver for Hubitat
*
*  Copyright © 2024 Adam Kempenich
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
* Hubitat is the Trademark and Intellectual Property of Hubitat, Inc.
* Unifi is the Trademark and Intellectual Property of Ubiquiti Networks, Inc.
*/

metadata {
    definition(
        name: "Unifi Access Doorbell",
        namespace: "adamkempenich",
        author: "Adam Kempenich",
        importUrl: ""
    ) {
        capability "Polling"
        capability "PushableButton"
        capability "HoldableButton"
        capability "ReleasableButton"
        capability "PresenceSensor"
        //  ImageCapture
        capability "Initialize"

        attribute "healthStatus", "string"
    }

    preferences {
        input name: "host",
            type: "string",
            title: "The API host address of the Unifi Access Controller (e.g. 192.168.0.1)",
            required: true,
            defaultValue: ""
        input name: "port",
            type: "number",
            title: "The API host port of the Unifi Access Controller (Default 12445, you probably don't need to change this)",
            required: true,
            defaultValue: "12445"
        input name: "token",
            type: "string",
            title: "The API token for the Unifi Access Controller",
            required: true,
            defaultValue: ""
        input name: "pingInterval",
            type: "number",
            title: "Ping every __ seconds",
            required: true,
            defaultValue: "60"


        command "requestNotifications"
    }
}


def updated() {
    initialize()
}


def lock() {
    log.debug "Unifi API does not support locking"
}

def push(buttonNumber){
    log.trace "push(${buttonNumber})"
    sendEvent(name: "pushed", value: buttonNumber, isStateChange: true)

}

def hold(buttonNumber){
    log.trace "hold(${buttonNumber})"
    sendEvent(name: "held", value: buttonNumber, isStateChange: true)

}

def release(buttonNumber){
    log.trace "release(${buttonNumber})"
    sendEvent(name: "released", value: 1, isStateChange: true)
}

def initialize(){
    requestNotifications()
}

def requestNotifications(){
    try {
        interfaces.webSocket.connect("wss://${host}:${port}/api/v1/developer/devices/notifications", pingInterval: 0, ignoreSSLIssues: true, headers: ["Authorization": "Bearer ${token}", "Upgrade": "websocket", "Connection": "Upgrade"]) 

    } catch (e) {
        log.error "Error retrieving door info: $e"
    }
}


def parse(response){
    //log.trace "parse(${response})"

    // log.trace response.size()
    //try{
    //    log.trace decodeBase64(response)
    //} catch(error){
    //    log.trace "could not convert to base64"
    //}


    if(response.size() == 8){
      unschedule(webSocketStatus)
        // connection alive and open
        // I'm not sure why checking this against its literal string doesn't work and I don't have energy to see what characters are really being sent
        // ""Hello" " << This is the string. Notice the trailing space 
        sendEvent(name: "healthStatus", value: "online")
        log.trace "online"
    } else {
        Map doorbellResponse = parseJson(response)
        log.trace doorbellResponse.event

        if(doorbellResponse.event == "access.remote_view"){
            //doorbell is ringing
            log.debug "doorbell is ringing"
            hold(1)
            presence("present")
        } else if( doorbellResponse.event == "access.remote_view.change"){
            log.debug "ringing cancelled"
            release(1)
            presence("not present")
        } else if(doorbellResponse.event == "access.hw.door_bell"){
            // doorbell button pressed
            log.debug "doorbell button pressed"
            hold(1)
            presence("present")
            runInMillis(11000, doorbellPressed, [overwrite: true])
        }

    }

}

def doorbellPressed(){
    release(1)
    presence("not present")
}

def webSocketStatus(status = "closed"){
    log.trace "webSocketStatus(${status})"

    if(status == "open"){
        // connection alive and open
    }
    else if(status == "closed"){
        // re-open connection
        sendEvent(name: "healthStatus", value: "offline")

        runIn(60, requestNotifications)
        runIn(90, webSocketStatus)
    }
}

def presence(presenceValue){

    sendEvent(name: "presence", value: presenceValue, isStateChange: true)


}
def poll() {
    refresh()
}
