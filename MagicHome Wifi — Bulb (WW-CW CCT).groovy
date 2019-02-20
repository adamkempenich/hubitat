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
 *    0.81 (Feb 19 2019)
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
    definition (name: "MagicHome Wifi — Bulb (WW/CW CCT)", namespace: "MagicHome", author: "Adam Kempenich") {
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
        input "deviceIP", "text", title: "Server", description: "Device IP", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port", required: true, defaultValue: 5577

        input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when settings change?",
              description: "Makes devices behave like other switches.", defaultValue: true,
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
    logDebug "Switch set to on"
    byte[] data = [0x71, 0x23,  0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug "Switch set to off"
    byte[] data = [0x71, 0x24,  0x0F, 0xA4]
    sendCommand(data)
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    normalizePercent(level)
    sendEvent(name: "level", value: level)
	logDebug "Level set to ${level}"
	
	if( !transmit ) return level
    setColorTemperature( device.currentValue('colorTemperature'), level )
}

def setWarmWhiteLevel(warmWhiteLevel, transmit=true){
    // Set the warm white level of a device (0-100)

    normalizePercent(warmWhiteLevel)
    sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
	logDebug "Warm White Level set to ${warmWhiteLevel}"
	
	if( !transmit ) return warmWhiteLevel
    setColorTemperature(null)
}

def setColdWhiteLevel(coldWhiteLevel, transmit=true){
    // Set the cold white level of a device (0-100)

    normalizePercent(coldWhiteLevel)
    sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
	logDebug "Cold White Level set to ${coldWhiteLevel}"
    if( !transmit ) return coldWhiteLevel
    setColorTemperature(null)
}

def setColorTemperature(setTemp = device.currentValue('colorTemperature'), deviceLevel = device.currentValue('level'), transmit=true){
    // Adjust the color temperature of a device
	
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

    // Update Device
	if( !transmit ) return setTemp
	
	byte[] data =  powerOnWithChanges(true) + appendChecksum([ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ] ) 
    sendCommand( data )
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( append=false ){
    // If the device is off and light settings change, turn it on (if user settings apply)
    if(append){
        return settings.powerOnWithChanges ? ( [0x71, 0x23, 0x0F, 0xA3] ) : ([])
    }
    else{
        settings.powerOnWithChanges ? ( device.currentValue("status") != "on" ? on() : null ) : null
    }
}

def appendChecksum( data ){
    // Adds a checksum to an array
    
    data += calculateChecksum(data)
    return data 
}

def normalizePercent( value, lowerBound = 0, upperBound = 100, fallBack = upperBound ){
    // Takes a value and ensures it's between two defined thresholds
    
    if( value == null ) return fallBack
    lowerBound <= upperBound ? ( null ) : ( ( lowerBound, upperBound ) = [ upperBound, lowerBound] )
    return value < upperBound ? ( value > lowerBound ? ( value ) : ( lowerBound ) ) : ( upperBound ) 
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

def calculateChecksum(bytes){
    // Totals an array of bytes
    
    int sum = 0
    for(int d : bytes)
        sum += d;
    return sum & 255
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

    runIn(20, keepAlive)
}

def keepAlive(){
    // Poll the device every 250 seconds, or it will lose connection.
    
    refresh()
	unschedule()
    runIn(10, keepAlive)
}
