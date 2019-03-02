/**
 *  MagicHome Wifi - Bulb (WW/CW CCT) 0.83
 *
 *  Author: 
 *    Adam Kempenich 
 *
 *  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
 *
 *
 *	0.83 (Feb 28 2019) 
 *	  - Un-did the parse() removal. Added Data checking for parse()
 *	  - Added a setting for time-to-re
 *
 *  Changelog:
 *  	0.82 (Feb 25 2019)
 *	  - Commented out parse() contents, since I think they are causing slowdown...
 *
 *     0.81 (Feb 19 2019)
 *      - Added try/catch to intialize() method
 *      - Cleaned up a bunch of extraneous code
 *      - Fixed a rounding error in parse
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

	       input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 60. Recommended: Under 200", defaultValue: 60,
            required: true, displayDuringSetup: true)
        
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
    setColorTemperature(device.currentValue( 'colorTemperature'), level )
}

def setWarmWhiteLevel(warmWhiteLevel, transmit=true){
    // Set the warm white level of a device (0-100)

    normalizePercent(warmWhiteLevel)
    sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
    logDebug( "Warm White Level set to ${warmWhiteLevel}")
    
    if( !transmit ) return warmWhiteLevel
    setColorTemperature()
}

def setColdWhiteLevel(coldWhiteLevel, transmit=true){
    // Set the cold white level of a device (0-100)

    normalizePercent(coldWhiteLevel)
    sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
    logDebug("Cold White Level set to ${coldWhiteLevel}")
    if( !transmit ) return coldWhiteLevel
    setColorTemperature()
}



def setColorTemperature( setTemp = device.currentValue('colorTemperature'), deviceLevel = device.currentValue('level'), transmit=true ){
    // Adjust the color temperature of a device  
    
    sendEvent(name: "colorTemperature", value: setTemp)
    logDebug "Color Temperature set to ${setTemp}"

    brightnessWW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature ) )
    brightnessCW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceCWTemperature, settings.deviceWWTemperature ) )

    if( brightnessWW + brightnessCW > 100 ){
        brightnessWW = brightnessWW / (( brightnessWW + brightnessCW ) / 100 )
        brightnessCW = brightnessCW / (( brightnessWW + brightnessCW ) / 100 )
    }

    sendEvent(name: "warmWhiteLevel", value: brightnessWW)
    sendEvent(name: "coldWhiteLevel", value: brightnessCW)

    if( !transmit ) return setTemp	
    powerOnWithChanges()
    byte[] data =  appendChecksum( [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ] )
    sendCommand( data )
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

def invertLinearValue( neutralValue, value1, value2 ){
    // Determines how far from a point two values are 

    return (( 100 )/( value1 - value2 )) * neutralValue + ( 100 - ( 100 /( value1 - value2 )) * value1 )
}

def proportionalToDeviceLevel( value ){
    // Returns the value of a number proportionally to the device's brightness
    
    return roundUpBetweenZeroAndOne( normalizePercent( value * device.currentValue('level') / 100 ) )
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
//    // Parse data received back from this device
//
    def responseArray = HexUtils.hexStringToIntArray(response)	
	switch(responseArray.length) {
		case 4:
			logDebug( "Received power-status packet of ${response}" )
			if( responseArray[2] == 35 ){
				device.currentValue( 'status' ) != 'on' ? sendEvent(name: "switch", value: "on") : null
			}
			else{
				device.currentValue( 'status' ) != 'off' ? sendEvent(name: "switch", value: "off") : null
			}
			break;
		
		case 14:
			logDebug( "Received general-status packet of ${response}" )
		
			if( responseArray[2] == 35 ){
				device.currentValue( 'status' ) != 'on' ? sendEvent(name: "switch", value: "on") : null
			}
			else{
				device.currentValue( 'status' ) != 'off' ? sendEvent(name: "switch", value: "off") : null
			}
            def warmWhite = ( responseArray[ 6 ].toDouble() / 2.55 ).round()
            def coldWhite = ( responseArray[ 7 ].toDouble() / 2.55 ).round()
			
			if( (warmWhite + coldWhite) > 0) {
				// Calculate the color temperature, based on what data was received
				device.currentValue( 'level' ) != normalizePercent( warmWhite + coldWhite ) ? sendEvent(name: "level", value: normalizePercent( warmWhite + coldWhite )) : null
				if(device.currentValue('warmWhiteLevel' ) != warmWhite && device.currentValue('coldWhiteLevel') != coldWhite ){
					setTemp = settings.deviceCWTemperature - (( settings.deviceCWTemperature - settings.deviceWWTemperature ) * ( warmWhite / 100 ))
					device.currentValue( 'colorTemperature' ) != setTemp.toInteger() ? sendEvent(name: "colorTemperature", value: setTemp.toInteger()) : null
				}
				device.currentValue( 'warmWhiteLevel' ) != warmWhite ? sendEvent(name: "warmWhiteLevel", value: warmWhite) : null
				device.currentValue( 'coldWhiteLevel' ) != coldWhite ? sendEvent(name: "coldWhiteLevel", value: coldWhite) : null
			}
			break;
		
		case null:
			logDebug "No response received from device"
			initialize()
			break;
		
		default:
			logDebug "Received a response with an unexpected length of ${responseArray.length} containing ${response}"
			break;
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
        runIn(2, initialize)
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
    telnetClose()
    try {
        logDebug("Opening TCP-Telnet Connection.")
        InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
        
        pauseExecution(1000)
        logDebug("Connection successfully established")
    } catch(e) {
        logDebug("Error attempting to establish TCP-Telnet connection to device.")
    }
    unschedule()

    runIn(5, keepAlive)
}

def keepAlive(){
    // Poll the device every 250 seconds, or it will lose connection.
    
    refresh()
	settings.refreshTime == null ? runIn(60, keepAlive) : runIn(settings.refreshTime, keepAlive)
}
