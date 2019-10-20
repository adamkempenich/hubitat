/**
*  MagicHome Wifi - Controller (RGB + WW/CW CCT) 0.89
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*    0.89 (Oct 17, 2019)
*        - Cleans up old, unused state variables
*        - Checks if too many initialize loops are being called at one time
*        - Removed RGB <> HSV methods
*        - Add recoverable HS option - in 0.90
*        - Remove unused state value (only state.noResponse is necessary)
*        - Removed telnet
*        - Added hue-in-degrees - Test
*        - Add CT parse back in
*        - Fixed powerOnWithChanges
*        - Added null 2nd option to setLevel for duration
*
*	0.88 (June 12, 2019)
*		- Added option for failed pings threshold 
*		- Resolved issue with recursive loops and initializing devices
*		
*	0.87 (May 16, 2019)
*		- Added an option for telnet/socket
*		- If you are on firmware before 2.1, use TELNET (does not support parse)
*		- ---> Otherwise, use SOCKET (supports parse) <----
*		- Greatly improved scheduling
*		- Started adding some features back in. Fully tested before release.
*		- Changed powerOnWithChanges to enablePreStaging — this matches Hubitat's vernacular
*
*	0.86 (April 14 2019)
*		- Fixed parse()
*
*	0.85 (April 12 2019)
*		- Simplified most of the code
*		- Eliminated Telnet method
*		- Removed most of parse() while I simplify it further. Only power reports.
*		- Continued to enhance self-healing. Devices that are physically powered off report as off.
*		- Reworked powerOnWithChanges.
*		- Corrected all function parameters except for setColorTemperature() which is mostly corrected
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
import hubitat.helper.ColorUtils

metadata {
    definition (
        name: "MagicHome Wifi — Controller (RGB + WW/CW CCT)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Controller%20(RGBWW).groovy") {
        
        capability "Actuator"
        capability "Color Control"
		capability "Color Mode"
        capability "Color Temperature"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"

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

        command "setWarmWhiteLevel", [ "number" ] // 0 - 100
        command "setColdWhiteLevel", [ "number" ] // 0 - 100

        attribute "currentPreset", "number" // 0 (off), 1-20 (other presets)
        attribute "presetSpeed", "number" // 0 - 100
        attribute "warmWhiteLevel", "number"
        attribute "coldWhiteLevel", "number"
    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
		input "useTelnet", "bool", title: "Use Telnet?", description: "Telnet - On, Socket - Off", required: true, defaultValue: false
		
        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
         input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)
		
		input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
            description: "Number of failed pings before reconnecting device.", defaultValue: 3,
            required: true, displayDuringSetup: true)

        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)
		
        input(name:"enableHueInDegrees", type:"bool", title: "Enable Hue in degrees (0-360)",
              defaultValue: false, required: true, displayDuringSetup: true)
        
        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
            description: "Interval between refreshing a device for its current value. Default: 10. Use number between 0-60", defaultValue: 10,
            required: true, displayDuringSetup: true)

        input(name:"deviceWWTemperature", type:"number", title: "Warm White rating of this light",
            description: "Temp in K (default 2700)", defaultValue: 2700,
            required: true, displayDuringSetup: true)

        input(name:"deviceCWTemperature", type:"number", title: "Cold White Rating of this light",
            description: "Temp in K (default 6500) <style>#tileContainter-presetBlueFade-4:hover{ animation: presetBlue-fade 3s infinite } @keyframes presetBlue-fade { 0% { color: blue } 50% { color: black } 100% { color: blue } } #tileContainter-presetBlueStrobe-5:hover{ animation: presetBlue-strobe 3s infinite } @keyframes presetBlue-strobe { 0% { color: blue } 49% { color: blue } 50% { color: black } 100% { color: black } } #tileContainter-presetCyanFade-6:hover{ animation: presetCyan-fade 3s infinite } @keyframes presetCyan-fade { 0% { color: cyan } 50% { color: black } 100% { color: cyan } } #tileContainter-presetCyanStrobe-7:hover{ animation: presetCyan-strobe 3s infinite } @keyframes presetCyan-strobe { 0% { color: cyan } 25% { color: cyan } 26% { color: black } 100% { color: black } } #tileContainter-presetGreenBlueDissolve-8:hover{ animation: presetGreenBlue-dissolve 3s infinite } @keyframes presetGreenBlue-dissolve { 0% { color: green } 50% { color: blue } 100% { color: green } } #tileContainter-presetGreenFade-9:hover{ animation: presetGreen-fade 3s infinite } @keyframes presetGreen-fade { 0% { color: green } 50% { color: black } 100% { color: green } } #tileContainter-presetGreenStrobe-10:hover{ animation: presetGreen-strobe 3s infinite } @keyframes presetGreen-strobe { 0% { color: green } 25% { color: green } 26% { color: black } 100% { color: black } } #tileContainter-presetPurpleFade-11:hover{ animation: presetPurple-fade 3s infinite } @keyframes presetPurple-fade { 0% { color: purple } 50% { color: black } 100% { color: purple } } #tileContainter-presetPurpleStrobe-12:hover{ animation: presetPurple-strobe 3s infinite } @keyframes presetPurple-strobe { 0% { color: purple } 25% { color: purple } 26% { color: black } 100% { color: black } } #tileContainter-presetRedBlueDissolve-13:hover{ animation: presetRedBlue-dissolve 3s infinite } @keyframes presetRedBlue-dissolve { 0% { color: red } 50% { color: blue } 100% { color: red } } #tileContainter-presetRedFade-14:hover{ animation: presetRed-fade 3s infinite } @keyframes presetRed-fade { 0% { color: red } 50% { color: black } 100% { color: red } } #tileContainter-presetRedGreenDissolve-15:hover{ animation: presetRedGreen-dissolve 3s infinite } @keyframes presetRedGreen-dissolve { 0% { color: red } 50% { color: green } 100% { color: red } } #tileContainter-presetRedStrobe-16:hover{ animation: presetRed-strobe 3s infinite } @keyframes presetRed-strobe { 0% { color: red } 25% { color: red } 26% { color: black } 100% { color: black } } #tileContainter-presetSevenColorDissolve-17:hover{ animation: presetSevenColor-dissolve 3s infinite } @keyframes presetSevenColor-dissolve { 0% { color: red } 12.5% { color: orange } 25% { color: yellow } 37.5% { color: green } 50% { color: blue } 62.5% { color: indigo } 75% { color: violet } 87.5% { color: white } 100% { color: red } } #tileContainter-presetSevenColorJump-18:hover{ animation: presetSevenColor-jump 3s infinite } @keyframes presetSevenColor-jump { 0% { color: red } 12% { color: red } 12.5% { color: orange } 24% { color: orange } 25% { color: yellow } 37% { color: yellow } 37.5% { color: green } 49% { color: green } 50% { color: blue } 62% { color: blue } 62.5% { color: indigo } 74% { color: indigo } 75% { color: violet } 87% { color: violet } 87.5% { color: white } 99% { color: white } 100% { color: red } } #tileContainter-presetSevenColorStrobe-19:hover{ animation: presetSevenColor-strobe 3s infinite } @keyframes presetSevenColor-strobe {  0% { color: black } 11% { color: black }  12% { color: red } 16% { color: red }  17% { color: black } 30% { color: black }  31% { color: orange } 38% { color: orange }  39% { color: black } 49% { color: black }  50% { color: yellow } 62% { color: yellow }  63% { color: black } 70% { color: black }  71% { color: green } 79% { color: green }  80% { color: black } 88% { color: black }  89% { color: blue } 99% { color: blue }  99% { color: black } } #tileContainter-presetWhiteFade-20:hover{ animation: presetWhite-fade 3s infinite } @keyframes presetWhite-fade { 0% { color: White } 50% { color: black } 100% { color: White } } #tileContainter-presetWhiteStrobe-21:hover{ animation: presetWhite-strobe 3s infinite } @keyframes presetWhite-strobe { 0% { color: White } 49% { color: White } 50% { color: black } 100% { color: black } } #tileContainter-presetYellowFade-22:hover{ animation: presetYellow-fade 3s infinite } @keyframes presetYellow-fade { 0% { color: Yellow } 50% { color: black } 100% { color: Yellow } } #tileContainter-presetYellowStrobe-23:hover{ animation: presetYellow-strobe 3s infinite } @keyframes presetYellow-strobe { 0% { color: Yellow } 49% { color: Yellow } 50% { color: black } 100% { color: black } } </style>", defaultValue: 6500,
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
    // Set the hue of a device ( 0 - 101 ) 

    settings.enableHueInDegrees ? hue /= 3.6 : null

	hue = limit( hue, 0, 101 )
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

def setLevel(level, duration=0) {
    // Set the brightness of a device (0-100)
	level = limit( level, 0, 99 )
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    
    device.currentValue("colorMode") == "RGB" ? setColor(hue: device.currentValue("hue"), saturation: device.currentValue("saturation"), level: level) : setColorTemperature(device.currentValue('colorTemperature'), level)
}

def setWarmWhiteLevel(warmWhiteLevel){
    // Set the warm white level of a device (0-100)

	warmWhiteLevel = limit( warmWhiteLevel )
	sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
	logDebug "Warm White Level set to ${warmWhiteLevel}"
    
    setColor(hue:100, saturation: device.currentValue("saturation"), level: device.currentValue("level"))
}


def setColdWhiteLevel(coldWhiteLevel){
    // Set the cold white level of a device (0-100)

	coldWhiteLevel = limit( coldWhiteLevel )
    sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
	logDebug "Cold White Level set to ${coldWhiteLevel}"
    setColor(hue:101, saturation: device.currentValue("saturation"), level: device.currentValue("level"))
}

def setColor( parameters ){
   
    // Register that presets are disabled
    
    sendEvent(name: "currentPreset", value: 0)
	sendEvent(name: "hue", value: settings.enableHueInDegrees ? parameters.hue/3.6 : parameters.hue)
	sendEvent(name: "saturation", value: limit( parameters.saturation ) )
	sendEvent(name: "level", value: limit( parameters.level, 0, 99 ) )

    if( parameters.hue == 100) {
        byte[] data1 =  appendChecksum( [ 0x31, 0, 0, 0, 0x00, 0x00, 0xf0, 0x0f ] ) 
        sendCommand( data1 ) 
		byte[] data2 = appendChecksum( [0x31, 0x00, 0x00, 0x00, parameters.level * 2.55, 0, 0x0f, 0x0f] )
		sendCommand( data2 ) 
	}
    else if( parameters.hue == 101 ) {
        byte[] data1 =  appendChecksum( [ 0x31, 0, 0, 0, 0x00, 0x00, 0xf0, 0x0f ] )
        sendCommand( data1 ) 
		byte[] data2 =  appendChecksum( [0x31, 0x00, 0x00, 0x00, 0, parameters.level * 2.55, 0x0f, 0x0f] )
		sendCommand( data2 ) 
	}
    else{
        rgbColors = ColorUtils.hsvToRGB( [parameters.hue.toFloat(), parameters.saturation.toFloat(), parameters.level.toFloat()] )
        byte[] data1 = appendChecksum( [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x00, 0xf0, 0x0f ] )  
        sendCommand( data1 ) 
		byte[] data2 = appendChecksum( [0x31, 0x00, 0x00, 0x00, 0, 0, 0x0f, 0x0f] )
		sendCommand( data2 ) 
	}
	powerOnWithChanges()
}

def setColorTemperature( setTemp = device.currentValue("colorTemperature"), deviceLevel = device.currentValue("level") ){
    // Adjust the color temperature of a device  
    
	sendEvent(name: "colorTemperature", value: setTemp)
	logDebug "Color Temperature set to ${setTemp}"
    setTemp = limit(setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature)
	brightnessWW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature ) )
	brightnessCW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceCWTemperature, settings.deviceWWTemperature ) )

	if( brightnessWW + brightnessCW > 100 ){
	   brightnessWW = brightnessWW / (( brightnessWW + brightnessCW ) / 100 )
	   brightnessCW = brightnessCW / (( brightnessWW + brightnessCW ) / 100 )
	}

	sendEvent(name: "warmWhiteLevel", value: brightnessWW)
	sendEvent(name: "coldWhiteLevel", value: brightnessCW)
	sendEvent(name: "colorMode", value: "CT")
   
	
	byte[] data1 = appendChecksum( [ 0x31, 0, 0, 0, 0x00, 0x00, 0xf0, 0x0f ] )
	sendCommand( data1 )
	byte[] data2 = appendChecksum( [0x31, 0x00, 0x00, 0x00, brightnessWW * 2.55, brightnessCW * 2.55, 0x0f, 0x0f] )
	sendCommand( data2 )
	powerOnWithChanges()
}

def sendPreset(preset = 1, speed = 100){
    // Turn on preset mode (true), turn off preset mode (false). Preset (1 - 20), Speed (1 (slow) - 100 (fast)).

    // Presets:
    // 1 Seven Color Dissolve, 2 Red Fade, 3 Green Fade, 4 Blue Fade, 5 Yellow Fade, 6 Cyan Fade, 7 Purple Fade, 8 White Fade, 9 Red Green Dissolve
    // 10 Red Blue Dissolve, 11 Green Blue Dissolve, 12 Seven Color Strobe, 13 Red Strobe, 14 Green Strobe, 15 Blue Strobe, 16 Yellow Strobe
    // 17 Cyan Strobe, 18 Purple Strobe, 19 White Strobe, 20 Seven Color Jump


	preset > 20 ? (preset = 20) : null
	speed > 100 ? (speed = 100) : null

	// Hex range of presets is (int) 37 - (int) 57. Add the preset number to get that range.
	preset += 36
	speed = (100 - speed)

	sendEvent( name: "currentPreset", value: preset )
	sendEvent( name: "presetSpeed", value: speed )

	byte[] data = appendChecksum(  [ 0x61, preset, speed, 0x0F ] )
	sendCommand( data ) 

    powerOnWithChanges()
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
		
        settings.enablePreStaging ? null : ( device.currentValue("switch") != "on" ? on() : null )
}

def invertLinearValue( neutralValue, value1, value2 ){
    // Determines how far from a point two values are 

    return (( 100 )/( value1 - value2 )) * neutralValue + ( 100 - ( 100 /( value1 - value2 )) * value1 )
}

def proportionalToDeviceLevel( value ){
    // Returns the value of a number proportionally to the device's brightness
    
    return value * device.currentValue('level') / 100
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
	// Need to add
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
            
            def warmWhite = ( responseArray[ 9 ].toDouble() / 2.55 ).round()
			def coldWhite = ( responseArray[ 11 ].toDouble() / 2.55 ).round()
            hsvMap = ColorUtils.rgbToHSV([responseArray[ 6 ], responseArray[ 7 ], responseArray[ 8 ]])


			//if( (warmWhite + coldWhite) > 0) {
			//	// Calculate the color temperature, based on what data was received
			//	sendEvent(name: "colorMode", value: "CT")
			//	device.currentValue( 'level' ) != normalizePercent( warmWhite + coldWhite ) ? sendEvent(name: "level", value: normalizePercent( warmWhite + coldWhite )) : null
			//	if(device.currentValue('warmWhiteLevel' ) != warmWhite && device.currentValue('coldWhiteLevel') != coldWhite ){
			//		setTemp = settings.deviceCWTemperature - (( settings.deviceCWTemperature - settings.deviceWWTemperature ) * ( warmWhite / 100 ))
			//		device.currentValue( 'colorTemperature' ) != setTemp.toInteger() ? sendEvent(name: "colorTemperature", value: setTemp.toInteger()) : null
			//	}
			//	sendEvent(name: "warmWhiteLevel", value: warmWhite)
			//	sendEvent(name: "coldWhiteLevel", value: coldWhite)
//
			//	
			//}
			//else{
			//	// Or, set the color
			//	sendEvent(name: "colorMode", value: "RGB")
			//	sendEvent(name: "warmWhiteLevel", value: 0)
			//	sendEvent(name: "coldWhiteLevel", value: 0)
			//	sendEvent(name: "level", value: hsvMap.value)
			//	sendEvent(name: "saturation", value: hsvMap.saturation)
			//	sendEvent(name: "hue", value: hsvMap.hue)
            //}
        
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
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
	
	logDebug "Number of failed responses: ${state.noResponse}"
	state.noResponse++
    state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
    logDescriptionText "A connection issue occurred."
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${limit(settings.reconnectPings, 0, 10)-state.noResponse} more failed attempt(s)."
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
        schedule("0/${limit(settings.refreshTime, 1, 60)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    
    InterfaceUtils.socketClose(device)
    telnetClose()
    
    pauseExecution(1000)
    
    if( data.firstRun || ( now() - state.lastConnectionAttempt) > limit(settings.refreshTime, 1, 60) * 500 /* Breaks infinite loops */ ) {
        def tryWasGood = false
        try {
            logDebug "Opening Socket Connection."
            InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
            pauseExecution(1000)
            logDescriptionText "Connection successfully established"
            tryWasGood = true
    
        } catch(e) {
            logDebug("Error attempting to establish socket connection to device.")
            logDebug("Next initialization attempt in ${settings.refreshTime} seconds.")
            settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
            tryWasGood = false
        }
	    
	    if(tryWasGood){
	    	unschedule()
	    	logDebug "Stopping connectDevice loop. Starting refresh loop"
	    	schedule("0/${limit(settings.refreshTime, 1, 60)} * * * * ? *", refresh)
	    	state.noResponse = 0
	    }
        log.debug "Proper time has passed, or it is the device's first run."
        log.debug "${(now() - state.lastConnectionAttempt)} >= ${limit(settings.refreshTime, 1, 60) * 500}. First run: ${data.firstRun}"
        state.lastConnectionAttempt = now()
    }
    else{
        log.debug "Tried to connect too soon. Skipping this round."
        log.debug "X ${(now() - state.lastConnectionAttempt)} >= ${limit(settings.refreshTime, 1, 60) * 500}"
        state.lastConnectionAttempt = now()
    }
}

def initialize() {
    // Establish a connection to the device
    state.remove("initializeLoopRunning")
    state.remove("refreshRunning")
    state.remove("initializeLoop")
    state.remove("oldvariablename")
    
    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
	sendEvent(name: "hue", value: 0)
	sendEvent(name: "saturation", value: 100)
	sendEvent(name: "level", value: 99)
	sendEvent(name: "switch", value: "off")
    state.noResponse = 0
    state.lastConnectionAttempt = now()
}
