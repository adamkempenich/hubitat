/**
 *  MagicHome Wifi - Bulb (Dimmable) 0.8
 *
 *  Author: 
 *    Adam Kempenich 
 *
 *  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
 *
 *  Changelog:
 *
 *    0.8 (Feb 11 2019)
 *      - Initial Release
 *      - Cannot set device time, yet.
 *      - Auto-discovery not yet implemented
 *      - Custom functions not yet implemented
 *      - On-device scheduling not yet implemented
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
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
    definition (
        name: "MagicHome Wifi — Bulb (Dimmable)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Initialize"

    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: false, displayDuringSetup: true)
        
        input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when values change?",
              defaultValue: true, required: true, displayDuringSetup: true)
    }
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    logDebug( "Switch set to on" )
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug( "Switch set to off" )
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    sendEvent(name: "level", value: level)
    logDebug( "Level set to ${level}")
    
    if( !transmit ) return level
    byte[] data = powerOnWithChanges(true) + appendChecksum( [ 0x31, level * 2.55, 0, 0, 0x03, 0x01, 0x0f ] )
    sendCommand( data ) 
}

def getLevel(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "level" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "level" ) )
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( append=false ){
    // If the device is off and light settings change, turn it on (if user settings apply)
    if(append){
        return settings.powerOnBrightnessChange ? ( [0x71, 0x23, 0x0F, 0xA3] ) : ([])
    }
    else{
        settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
    }
}

def calculateChecksum( data ){
    // Totals an array of bytes
    
    int sum = 0;
    for(int d : data)
        sum += d;
    return sum & 255
}

def appendChecksum( data ){
    // Adds a checksum to an array
    
    data += calculateChecksum(data)
    return data 
}

def parse( response ) {
    // Parse data received back from this device

    def responseArray = HexUtils.hexStringToIntArray(response)
    if( responseArray.length == 4 ) {
        // Does the device say it's on?
        
        responseArray[ 2 ] == 35 ? sendEvent(name: "switch", value: "on") : sendEvent(name: "switch", value: "off")
    }
    else if( responseArray.length == 14 ) {
        // Does the device say it's on?
        
        responseArray[ 2 ] == 35 ? ( sendEvent(name: "switch", value: "on") ) : ( sendEvent(name: "switch", value: "off") )
         
    }
    else if( response == null ){
        log.debug "${settings.deviceIP}: No response received from device" 
    }
    else{
        log.debug "${settings.deviceIP}: Received a response with an unexpected length of " + responseArray.length
    }
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs
    
    if( settings.logDebug ) { 
        log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}
def refresh( ) {
    
    byte[] data =  [ 0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand( data )
}

def telnetStatus( status ) { logDebug "telnetStatus: ${status}" }
def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    if(status == "send error: Broken pipe (Write failed)") {
        // Cannot reach device
        logDebug "Cannot reach device. Attempting to reconnect."
        runIn(10, initialize)
    }   
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}
def initialize() {
    // Establish a connection to the device
    
    logDebug "Initializing device."
    InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
    unschedule()

    runIn(20, keepAlive)
}

def keepAlive(){
    // Poll the device every 250 seconds, or it will lose connection.
    
    refresh()
    runIn(150, keepAlive)
}
