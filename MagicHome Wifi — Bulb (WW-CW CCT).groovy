/**
 *  MagicHome Wifi - Bulb (WW/CW CCT) 0.8
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
        name: "MagicHome Wifi — Bulb (WW/CW CCT)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Color Temperature"
        capability "Initialize"

        command "on"
        command "off" 

        command "setWarmWhiteLevel", [ "number" ] // 0 - 100
        command "setColdWhiteLevel", [ "number" ] // 0 - 100


        attribute "warmWhiteLevel", "number"
        attribute "coldWhiteLevel", "number"
    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: false, displayDuringSetup: true)
        
        input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when values change?",
              defaultValue: true, required: true, displayDuringSetup: true)

        input(name:"deviceWWTemperature", type:"number", title: "Warm White rating of this light",
            description: "Temp in K (default 2700)", defaultValue: 2700,
            required: false, displayDuringSetup: true)

        input(name:"deviceCWTemperature", type:"number", title: "Cold White Rating of this light",
            description: "Temp in K (default 6500)", defaultValue: 6500,
            required: false, displayDuringSetup: true)
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

    level = normalizePercent( level )
    sendEvent(name: "level", value: level)
    logDebug( "Level set to ${level}")
    
    if( !transmit ) return level
    setColor( level: level )
}

def getLevel(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "level" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "level" ) )
}

def setWarmWhiteLevel(warmWhiteLevel, transmit=true){
    // Set the warm white level of a device (0-100)

    normalizePercent(warmWhiteLevel)
    sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
    log.debug "${settings.deviceIP}: MagicHome - Warm White Level set to " + warmWhiteLevel
    
    if( !transmit ) return getWarmWhiteLevel()
    setColorTemperature(null)
}

def getWarmWhiteLevel(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "warmWhiteLevel" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "warmWhiteLevel" ) )
}

def setColdWhiteLevel(coldWhiteLevel, transmit=true){
    // Set the cold white level of a device (0-100)

    normalizePercent(coldWhiteLevel)
    sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
    log.debug "${settings.deviceIP}: MagicHome - Cold White Level set to " + coldWhiteLevel
    if( !transmit ) return getColdWhiteLevel()
    setColorTemperature(null)
}

def getColdWhiteLevel(){
    // Get the warm white level of a device (0 - 100)

    return device.currentValue( "coldWhiteLevel" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "coldWhiteLevel" ) )
}

def setColorTemperature( setTemp = getColorTemperature(), transmit=true ){
    // Adjust the color temperature of a device  
    
    deviceLevel = roundUpBetweenZeroAndOne( normalizePercent( getLevel( ) ) )
    setTemp = normalizePercent( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature, getColorTemperature() )
    def newSaturation
    def newHue
    
    sendEvent(name: "colorTemperature", value: setTemp)
    logDebug "Color Temperature set to ${setTemp}"

    brightnessWW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature ) )
    brightnessCW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceCWTemperature, settings.deviceWWTemperature ) )

    if( brightnessWW + brightnessCW > 100 ){
        brightnessWW = brightnessWW / (( brightnessWW + brightnessCW ) / 100 )
        brightnessCW = brightnessCW / (( brightnessWW + brightnessCW ) / 100 )
    }

    setWarmWhiteLevel( brightnessWW, false )
    setColdWhiteLevel( brightnessCW, false )

    if( !transmit ) return getColorTemperature()
    powerOnWithChanges()
    byte[] data =  appendChecksum( [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ] )
    sendCommand( data )
}

def getColorTemperature(){
    // Get the color temperature of a device ( ~2000-7000 )

    return device.currentValue( "colorTemperature" ) == null ? ( sendEvent( name: "colorTemperature", value: 2700 ) ) : ( device.currentValue( "colorTemperature" ) )
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( append=false ){
    // If the device is off and light settings change, turn it on (if user settings apply)
    if(append){
        return settings.powerOnBrightnessChange ? ( [0x71, 0x23, 0x0F, 0xA3] ) : null
    }
    else{
        settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
    }
}

def invertLinearValue( neutralValue, value1, value2 ){
    // Determines how far from a point two values are 

    return (( 100 )/( value1 - value2 )) * neutralValue + ( 100 - ( 100 /( value1 - value2 )) * value1 )
}

def proportionalToDeviceLevel( value ){
    // Returns the value of a number proportionally to the device's brightness
    
    return roundUpBetweenZeroAndOne( normalizePercent( value * getLevel( ) / 100 ) )
}

def roundUpBetweenZeroAndOne(number){
    // Rounds up a number between two points
    
    return number > 0 && number < 1 ? ( 1 ) : ( number )
}


def calculateCTSaturation( coldWhite = true, offset ) {
        
    def CURVE
    def lowPoint
    def highPoint
    
    if( coldWhite ) {
        lowPoint = settings.cwSaturationLowPoint < settings.cwSaturationHighPoint ? settings.cwSaturationLowPoint : settings.cwSaturationHighPoint
        highPoint = settings.cwSaturationHighPoint > settings.cwSaturationLowPoint ? settings.cwSaturationHighPoint : settings.cwSaturationLowPoint
        CURVE = 1.8
    }
    else{ 
        lowPoint = settings.wwSaturationLowPoint < settings.wwSaturationHighPoint ? settings.wwSaturationLowPoint : settings.wwSaturationHighPoint
        highPoint = settings.wwSaturationHighPoint > settings.wwSaturationLowPoint ? settings.wwSaturationHighPoint : settings.wwSaturationLowPoint
        CURVE = 2.16666
    }
    
    return (((( 100 - lowPoint  ) / 100 ) * ( CURVE * Math.sqrt( offset ))) + lowPoint  ) * highPoint / 100
}      

def hslToCT(){

}

def normalizePercent(value, lowerBound=0, upperBound=100 ){
    // Takes a value and ensures it's between two defined thresholds
    
    // If the value doesn't exist, create it
    value = value == null ? upperBound : value

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound}
        if(value > upperBound){ value = upperBound}
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound}
        if(value > lowerBound ){ value = lowerBound}
    }
    
    return value
}

def checkIfInMap( parameterValue, valueName) {
    // Check if a value is in a map, and return (or set and return) a value

    if( parameterValue == null ){ 
        if( device.currentValue( valueName ) == null ){
            sendEvent( name: valueName, value: maxValue )
            return device.currentValue( "${valueName}" )
        }
        else{
            return device.currentValue( "${valueName}" )
        }
    }
    else {
        sendEvent( name: valueName, value: parameterValue )
        return parameterValue
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
        
        // Convert integers to percentages
        double warmWhite = responseArray[ 6 ] / 2.55
        double coldWhite = responseArray[ 7 ] / 2.55
        
        // If values differ from HE, change them
        device.currentValue( "warmWhiteLevel" ) != warmWhite ? ( setWarmWhiteLevel( warmWhite, false ) ) : null
        device.currentValue( "coldWhiteLevel" ) != coldWhite ? ( setColdWhiteLevel( coldWhite, false ) ) : null
        device.currentValue( "level" ) != ( warmWhite + coldWhite ) ? ( setLevel( ( warmWhite + coldWhite ), false ) ) : null
        
        // Calculate the color temperature, based on what data was received
        setTemp = settings.deviceCWTemperature - (( settings.deviceCWTemperature - settings.deviceWWTemperature ) * ( warmWhite / 100 ))
        // If value differs from HE, change it
        setTemp != device.currentValue( "colorTemperature" ) ? ( sendEvent(name: "colorTemperature", value: setTemp.toInteger()) ) : null
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
        runin(10, initialize)
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
