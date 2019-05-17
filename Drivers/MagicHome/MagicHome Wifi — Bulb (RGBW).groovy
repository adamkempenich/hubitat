/**
*  MagicHome Wifi - Bulb (RGB + W) 0.87
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*	0.87 (May 16, 2019)
*		- Added an option for telnet/socket
*		- If you are on firmware before 1.110, use TELNET (does not support parse)
*		- ---> Otherwise, use SOCKET (supports parse) <----
*		- Greatly improved scheduling
*		- Started adding some features back in. Fully tested before release.
*		- Changed powerOnWithChanges to enablePreStaging — this matches Hubitat's vernacular
*
*	0.86 (Supplemental)
*		- Hotfix
*
*	0.85 (April 12 2019)
*		- Simplified most of the code
*		- Eliminated Telnet method
*		- Removed most of parse() while I simplify it further. Only power reports.
*		- Continued to enhance self-healing. Devices that are physically powered off report as off.
*		- Reworked powerOnWithChanges.
*		- Corrected all function parameters
*		- Removed setColorTemperature()
* 		- Added import URL
*
*	0.84 (Mar 28 2019) 
* 		- Completely reworked Telnet and Socket methods... Waiting on some bugfixes
* 		- Greatly enhanced self-healing capabilities
*
*	0.83 (Feb 27 2019) 
*	  - Un-did the parse() removal. Added Data checking for parse()
*	  - Added a setting for time-to-refresh
*
*  	0.82 (Feb 25 2019)
*	  - Commented out parse() contents, since I think they are causing slowdown...
*
*    0.81 (Feb 19 2019) 
* 		- Added try/catch to initialize method
* 		- Removed a bunch of extraneous code
* 		- Fixed an issue with data loss for CCT in parse method 
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
        name: "MagicHome Wifi — Bulb (RGBW)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Controller%20(RGBW).groovy") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Color Control"
        capability "Initialize"
        
        command "sendPreset",               ["number", "number"]       // 0 (off), 1-20 (other presets)
        command "presetSevenColorDissolve", [ "number" ] // 0 - 100 (speed)
        command "presetRedFade",            [ "number" ] // 0 - 100 (speed)
        command "presetGreenFade",          [ "number" ] // 0 - 100 (speed)
        command "presetBlueFade",           [ "number" ] // 0 - 100 (speed)
        command "presetYellowFade",         [ "number" ] // 0 - 100 (speed)
        command "presetCyanFade",           [ "number" ] // 0 - 100 (speed)
        command "presetPurpleFade",         [ "number" ] // 0 - 100 (speed)
        command "presetWhiteFade",          [ "number" ] // 0 - 100 (speed)
        command "presetRedGreenDissolve",   [ "number" ] // 0 - 100 (speed)
        command "presetRedBlueDissolve",    [ "number" ] // 0 - 100 (speed)
        command "presetGreenBlueDissolve",  [ "number" ] // 0 - 100 (speed)
        command "presetSevenColorStrobe",   [ "number" ] // 0 - 100 (speed)
        command "presetRedStrobe",          [ "number" ] // 0 - 100 (speed)
        command "presetGreenStrobe",        [ "number" ] // 0 - 100 (speed)
        command "presetBlueStrobe",         [ "number" ] // 0 - 100 (speed)
        command "presetYellowStrobe",       [ "number" ] // 0 - 100 (speed)
        command "presetCyanStrobe",         [ "number" ] // 0 - 100 (speed)
        command "presetPurpleStrobe",       [ "number" ] // 0 - 100 (speed)
        command "presetWhiteStrobe",        [ "number" ] // 0 - 100 (speed)
        command "presetSevenColorJump",     [ "number" ] // 0 - 100 (speed)
        
        attribute "currentPreset", "number" // 0 (off), 1-20 (other presets)
        attribute "presetSpeed", "number" // 0 - 100
    }
    
    preferences {  
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
		input "useTelnet", "bool", title: "Use Telnet?", description: "Telnet - On, Socket - Off", required: true, defaultValue: false
		
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: false, displayDuringSetup: true)
        
        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)

		input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 60. Recommended: Under 200", defaultValue: 60,
            required: true, displayDuringSetup: true)
    }
}
def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    logDebug "Switch set to on" 
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    logDebug "Switch set to off" 
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def setHue(hue){
    // Set the hue of a device ( 0-100) 

	hue = limit( hue )
    sendEvent(name: "hue", value: hue )
	logDebug "Hue set to ${hue}"
	    
    setColor(hue: hue, level: device.currentValue("level"), saturation: device.currentValue("saturation"))
}

def setSaturation(saturation){
    // Set the saturation of a device (0-100)

	saturation = limit( saturation )
    sendEvent(name: "saturation", value: saturation)
    logDebug "Saturation set to ${saturation}"
    
    setColor(hue: device.currentValue("hue"), saturation: saturation, level: device.currentValue("level"))
}

def setLevel(level) {
    // Set the brightness of a device (0-100)
	level = limit( level, 0, 99 )
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    
    setColor(hue: device.currentValue("hue"), saturation: device.currentValue("saturation"), level: level)
}

def setColor( parameters ){
   
    // Register that presets are disabled
    sendEvent(name: "currentPreset", value: 0)
	sendEvent(name: "hue", value: limit( parameters.hue ))
	sendEvent(name: "saturation", value: limit( parameters.saturation ))
	sendEvent(name: "level", value: limit(parameters.level, 0, 99 ))
	powerOnWithChanges()

    if( parameters.hue == 100 ) {
        byte[] data = appendChecksum([0x31, 0, 0, 0, parameters.level * 2.55, 0x0f, 0x0f])
        sendCommand( data ) 
	}
    else{
		rgbColors = hsvToRGB( parameters.hue, parameters.saturation, parameters.level )
        byte[] data = appendChecksum([0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0, 0xf0, 0x0f])
        sendCommand( data ) 
	}
}

def sendPreset(preset = 1, speed = 100){
    // Turn on preset mode (true), turn off preset mode (false). Preset (1 - 20), Speed (1 (slow) - 100 (fast)).

    // Presets:
    // 1 Seven Color Dissolve, 2 Red Fade, 3 Green Fade, 4 Blue Fade, 5 Yellow Fade, 6 Cyan Fade, 7 Purple Fade, 8 White Fade, 9 Red Green Dissolve
    // 10 Red Blue Dissolve, 11 Green Blue Dissolve, 12 Seven Color Strobe, 13 Red Strobe, 14 Green Strobe, 15 Blue Strobe, 16 Yellow Strobe
    // 17 Cyan Strobe, 18 Purple Strobe, 19 White Strobe, 20 Seven Color Jump

	powerOnWithChanges()

	preset > 20 ? (preset = 20) : null
	speed > 100 ? (speed = 100) : null

	// Hex range of presets is (int) 37 - (int) 57. Add the preset number to get that range.
	preset += 36
	speed = (100 - speed)

	sendEvent( name: "currentPreset", value: preset )
	sendEvent( name: "presetSpeed", value: speed )

	byte[] data = appendChecksum(  [ 0x61, preset, speed, 0x0F ] )
	sendCommand( data ) 
}

def presetSevenColorDissolve( speed = 100 ){
    sendPreset( 1, speed )
}
def presetRedFade( speed = 100 ){
    sendPreset( 2, speed )
}
def presetGreenFade( speed = 100 ){
    sendPreset( 3, speed )
}
def presetBlueFade( speed = 100 ){
    sendPreset( 4, speed )
}
def presetYellowFade( speed = 100 ){
    sendPreset( 5, speed )
}
def presetCyanFade( speed = 100 ){
    sendPreset( 6, speed )
}
def presetPurpleFade( speed = 100 ){
    sendPreset( 7, speed )
}
def presetWhiteFade( speed = 100 ){
    sendPreset( 8, speed )
}
def presetRedGreenDissolve( speed = 100 ){
    sendPreset( 9, speed )
}
def presetRedBlueDissolve( speed = 100 ){
    sendPreset( 10, speed )
}
def presetGreenBlueDissolve( speed = 100 ){
    sendPreset( 11, speed )
}
def presetSevenColorStrobe( speed = 100 ){
    sendPreset( 12, speed )
}
def presetRedStrobe( speed = 100 ){
    sendPreset( 13, speed )
}
def presetGreenStrobe( speed = 100 ){
    sendPreset( 14, speed )
}
def presetBlueStrobe( speed = 100 ){
    sendPreset( 15, speed )
}
def presetYellowStrobe( speed = 100 ){
    sendPreset( 16, speed )
}
def presetCyanStrobe( speed = 100 ){
    sendPreset( 17, speed )
}
def presetPurpleStrobe( speed = 100 ){
    sendPreset( 18, speed )
}
def presetWhiteStrobe( speed = 100 ){
    sendPreset( 19, speed )
}
def presetSevenColorJump( speed = 100 ){
    sendPreset( 20, speed )
}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)

        settings.enablePreStaging ? null : ( device.currentValue("status") != "on" ? on() : null )
}

def hsvToRGB(float conversionHue = 0, float conversionSaturation = 100, float conversionValue = 100, resolution = "low"){
    // Accepts conversionHue (0-100 or 0-360), conversionSaturation (0-100), and converstionValue (0-100), resolution ("low", "high")
    // If resolution is low, conversionHue accepts 0-100. If resolution is high, conversionHue accepts 0-360
    // Returns RGB map ([ red: 0-255, green: 0-255, blue: 0-255 ])
    
    // Check HSV limits
    resolution == "low" ? ( hueMax = 100 ) : ( hueMax = 360 ) 
    conversionHue > hueMax ? ( conversionHue = 1 ) : ( conversionHue < 0 ? ( conversionHue = 0 ) : ( conversionHue /= hueMax ) )
    conversionSaturation > 100 ? ( conversionSaturation = 1 ) : ( conversionSaturation < 0 ? ( conversionSaturation = 0 ) : ( conversionSaturation /= 100 ) )
    conversionValue > 100 ? ( conversionValue = 1 ) : ( conversionValue < 0 ? ( conversionValue = 0 ) : ( conversionValue /= 100 ) ) 
        
    int h = (int)(conversionHue * 6);
    float f = conversionHue * 6 - h;
    float p = conversionValue * (1 - conversionSaturation);
    float q = conversionValue * (1 - f * conversionSaturation);
    float t = conversionValue * (1 - (1 - f) * conversionSaturation);
    
    conversionValue *= 255
    f *= 255
    p *= 255
    q *= 255
    t *= 255
            
    if      (h==0) { rgbMap = [red: conversionValue, green: t, blue: p] }
    else if (h==1) { rgbMap = [red: q, green: conversionValue, blue: p] }
    else if (h==2) { rgbMap = [red: p, green: conversionValue, blue: t] }
    else if (h==3) { rgbMap = [red: p, green: q, blue: conversionValue] }
    else if (h==4) { rgbMap = [red: t, green: p, blue: conversionValue] }
    else if (h==5) { rgbMap = [red: conversionValue, green: p,blue: q]  }
    else           { rgbMap = [red: 0, green: 0, blue: 0] }

    return rgbMap
}

def rgbToHSV( r = 255, g = 255, b = 255, resolution = "low" ) {
    // Takes RGB (0-255) and returns HSV in 0-360, 0-100, 0-100
    // resolution ("low", "high") will return a hue between 0-100, or 0-360, respectively.
  
    r /= 255
    g /= 255
    b /= 255

    float h
    float s
    
    float max =   Math.max( Math.max( r, g ), b )
    float min = Math.min( Math.min( r, g ), b )
    float delta = ( max - min )
    float v = ( max * 100.0 )

    max != 0.0 ? ( s = delta / max * 100.0 ) : ( s = 0 )

    if (s == 0.0) {
        h = 0.0
    }
    else{
        if (r == max){
                h = ((g - b) / delta)
        }
        else if(g == max) {
                h = (2 + (b - r) / delta)
        }
        else if (b == max) {
                h = (4 + (r - g) / delta)
        }
    }

    h *= 60.0
        h < 0 ? ( h += 360 ) : null
  
    resolution == "low" ? h /= 3.6 : null
    return [ hue: h, saturation: s, value: v ]
}

def limit( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

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
    
    state.noResponse = 0    
    
    def responseArray = HexUtils.hexStringToIntArray(response)  
    switch(responseArray.length) {
        case 4:
            logDebug( "Received power-status packet of ${response}" )
            if( responseArray[2] == 35 ){
                if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
                }
            }
            else{
                if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
                }
            }
            break;
        
        case 14:
            logDebug( "Received general-status packet of ${response}" )
        
            if( responseArray[2] == 35 ){
                if(device.currentValue("switch") != "on"){
                    sendEvent(name: "switch", value: "on")
                }
            }
            else{
                if(device.currentValue("switch") != "off"){
                    sendEvent(name: "switch", value: "off")
                }
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
        log.debug "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    if( settings.logDescriptionText ) { 
        log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    if(settings.useTelnet == false || settings.useTelnet == null){
        InterfaceUtils.sendSocketMessage(device, stringBytes)
    }
    else{
        def transmission = new HubAction(stringBytes, Protocol.TELNET)
        sendHubCommand(transmission)
    }
}

def refresh( ) {
	
	state.noResponse++
    state.noResponse >= 2 ? ( initialize() ) : null // if a device hasn't responded twice, reconnect
	
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect..."
    initialize()
    }

def telnetStatus( status ) { 
    logDebug "telnetStatus: ${status}"
    logDebug "Attempting to reconnect..."
    initialize()
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/10 * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    telnetClose()
    def tryWasGood = false
    if(settings.useTelnet == false || settings.useTelnet == null){
        try {
            logDebug "Opening Socket Connection."
            InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
            pauseExecution(1000)
            logDebug "Connection successfully established"
			tryWasGood = true
            
        } catch(e) {
            logDebug("Error attempting to establish TCP connection to device.")
            logDebug("Next initialization attempt in 10 seconds.")
            sendEvent(name: "switch", value: "off") // If we didn't hear back, the device is likely physically powered off
			tryWasGood = false
        }
    }
    else{
        try {
            logDebug "Opening Telnet Connection."
            telnetConnect([byteInterface: true, termChars:[129]], "${settings.deviceIP}", settings.devicePort.toInteger(), null, null)
            pauseExecution(1000)
            logDebug "Connection successfully established" 
			tryWasGood = true
        } catch(e) {
            logDebug("Error attempting to establish TCP connection to device.")
            logDebug("Next initialization attempt in 10 seconds.")
            sendEvent(name: "switch", value: "off") // If we didn't hear back, the device is likely physically powered off
			tryWasGood = false
        }
    }
	
	if(tryWasGood){
		unschedule()
		logDebug "Starting refresh cron"
		schedule("0/10 * * * * ? *", refresh)
		state.noResponse = 0
	}
}

def initialize() {
    // Establish a connection to the device
    
    logDebug "Initializing device."
    connectDevice([firstRun: true])
}

def installed(){
	sendEvent(name: "hue", value: 0)
	sendEvent(name: "saturation", value: 100)
	sendEvent(name: "level", value: 99)
	sendEvent(name: "switch", value: "off")
    state.noResponse = 0
}
