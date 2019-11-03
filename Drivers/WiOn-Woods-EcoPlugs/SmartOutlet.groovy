/**
*  WiOn/Woods/EcoPlugs Smart Outlets 0.70
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-wion-woods-aka-ecoplugs-smart-outlets-0-70/26114
*
*  Changelog:
*    0.70 (Nov 03, 2019)
*        - Initial Release
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

import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.helper.HexUtils

metadata {
	definition(
        name: "Wion/Woods/EcoPlugs Smart Plugs",
        namespace: "ecoplugs", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/WiOn-Woods-EcoPlugs/SmartOutlet.groovy") {
 
		capability "Actuator"
		capability "Switch"
		capability "Polling"
		capability "Refresh"
        capability "Initialize"
	}
    
    preferences {  
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 80)", required: true, defaultValue: 80
       
        input(name:"ecoID", type:"string", title: "Eco ID (found in the EcoPlug App's Device's Settings):",
       		  description: "ECO-12345678", defaultValue: "${ecoID}",
              required: true, displayDuringSetup: true)
        
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
        input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)
        
      //  input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
      //        description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
      //        required: true, displayDuringSetup: true)
      //  
		//input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
      //      description: "Number of failed pings before reconnecting device.", defaultValue: 3,
      //      required: true, displayDuringSetup: true)
//
//
      //  input(name:"refreshTime", type:"number", title: "Time to refresh (0-60 seconds)",
      //      description: "Interval between refreshing a device for its current value. Default: 40. Use less than 60.", defaultValue: 40,
      //      required: true, displayDuringSetup: true)
	}
}

def poll() {
	refresh()
}

def refresh(){
    
}

def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${settings.reconnectPings-state.noResponse} failed attempts."
}

def parse( response ) {
    // Parse data received back from this device
    
    state.noResponse = 0    
    
    def responseArray = HexUtils.hexStringToIntArray(response)  
    switch(responseArray.length) {
        case 167:
            logDebug "${response}"
            break;
        case null:
            logDebug "Null response received from device"
            break;
        
        default:
            logDebug "Received a response with an unexpected length of ${responseArray.length} containing ${response}"
            break;
    }
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs
    
    if( settings.logDebug ) { 
        log.debug "WiOn/Woods/EcoPlugs (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    // If descriptionText is enabled in settings, pass text to the logs

    if( settings.logDescriptionText ) { 
        log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def on() {
	sendEvent(name: "switch", value: "on")
    byte[] id = "${settings.ecoID}".getBytes()
    byte[] hexData = [0x16, 0x00, 0x05, 0x00, 0x00, 0x00, 0xe6, 0x62, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, id[0], id[1], id[2], id[3], id[4], id[5], id[6], id[7], id[8], id[9], id[10], id[11], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4b, 0x65, 0x65, 0x7a, 0x65, 0x72, 0x20, 0x4c, 0x69, 0x67, 0x68, 0x74, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x37, 0x38, 0x30, 0x41, 0x39, 0x45, 0x42, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x25, 0x2f, 0x60, 0x5d, 0x00, 0x00, 0x00, 0x00, 0x6b, 0x20, 0x0b, 0x42, 0x01, 0x01]
    sendCommand(hexData)
}

def off() {
	sendEvent(name: "switch", value: "off")
    byte[] id = "${settings.ecoID}".getBytes()
    byte[] hexData = [0x16, 0x00, 0x05, 0x00, 0x00, 0x00, 0xff, 0x07, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, id[0], id[1], id[2], id[3], id[4], id[5], id[6], id[7], id[8], id[9], id[10], id[11], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x4b, 0x65, 0x65, 0x7a, 0x65, 0x72, 0x20, 0x4c, 0x69, 0x67, 0x68, 0x74, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x37, 0x38, 0x30, 0x41, 0x39, 0x45, 0x42, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x25, 0x2f, 0x60, 0x5d, 0x00, 0x00, 0x00, 0x00, 0x6b, 0x20, 0x0b, 0x42, 0x01, 0x00]
    sendCommand(hexData)
}

def sendCommand(data) {    
	  
    String stringBytes = HexUtils.byteArrayToHexString(data)
    new HubAction(stringBytes, Protocol.LAN, [type: HubAction.Type.LAN_TYPE_UDPCLIENT, encoding: HubAction.Encoding.HEX_STRING, destinationAddress: "${settings.deviceIP}", destinationPort: settings.devicePort])
}

def initialize(){
    // Add persistent connection
    // Add epoch time and other device parameters
    // Add remaining routines
    // Add support for energy meter devices
}

