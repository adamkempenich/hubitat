/**
*  MagicHome Wifi - Bulb (RGB + WW/CW CCT) 0.85
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
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

metadata {
    definition (
        name: "MagicHome Wifi — Bulb (RGB + WW/CW CCT)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Bulb%20(RGBWW).groovy") {
        
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
    // Set the hue of a device ( 0-100) 

    hue > 99 ? (hue = 99) : null
    sendEvent(name: "hue", value: hue )
	logDebug "Hue set to ${hue}"
	    
    setColor(hue: hue, level: device.currentValue("level"), saturation: device.currentValue("saturation"))
}

def setSaturation(saturation){
    // Set the saturation of a device (0-100)

	saturation > 100 ? (saturation = 100) : null
    sendEvent(name: "saturation", value: saturation)
    logDebug "Saturation set to ${saturation}"
    
    setColor(hue: device.currentValue("hue"), saturation: saturation, level: device.currentValue("level"))
}

def setLevel(level) {
    // Set the brightness of a device (0-100)
	level > 100 ? (level = 100) : null
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    
    device.currentValue("colorMode") == "RGB" ? setColor(hue: device.currentValue("hue"), saturation: device.currentValue("saturation"), level: level) : setColorTemperature(device.currentValue('colorTemperature'), level)
}

def setColor( parameters ){
   
    // Register that presets are disabled
    sendEvent(name: "currentPreset", value: 0)
	sendEvent(name: "hue", value: parameters.hue)
	sendEvent(name: "saturation", value: parameters.saturation)
	sendEvent(name: "level", value: parameters.level)
	powerOnWithChanges()

	rgbColors = hsvToRGB( parameters.hue, parameters.saturation, parameters.level )
	byte[] data = appendChecksum( [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x00, 0xf0, 0x0f ] )
	sendCommand( data ) 
	
}

def setColorTemperature( setTemp = device.currentValue("colorTemperature"), deviceLevel = device.currentValue("level") ){
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
	sendEvent(name: "coldWhiteLevel", valur: brightnessCW)
	sendEvent(name: "colorMode", value: "CT")
   
	powerOnWithChanges()
	byte[] data = appendChecksum( [0x31, 0x00, 0x00, 0x00, brightnessWW * 2.55, brightnessCW * 2.55, 0x0f, 0x0f] )
	sendCommand( data )
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

        settings.powerOnWithChanges ? ( device.currentValue("status") != "on" ? on() : null ) : null
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
	
	unschedule()
	//settings.refreshTime == null ? runIn(20, refresh) : runIn(settings.refreshTime, refresh)
	runIn(20, refresh)
	
    def responseArray = HexUtils.hexStringToIntArray(response)	
	switch(responseArray.length) {
		case 4:
			logDebug( "Received power-status packet of ${response}" )
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
			break;
		
		case 14:
			logDebug( "Received general-status packet of ${response}" )
		
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
			//def warmWhite = ( responseArray[ 9 ].toDouble() / 2.55 ).round()
			//def coldWhite = ( responseArray[ 11 ].toDouble() / 2.55 ).round()
			//hsvMap = rgbToHSV( responseArray[ 6 ], responseArray[ 7 ], responseArray[ 8 ] )
//
		//
			//if( (warmWhite + coldWhite) > 0) {
			//	if((warmWhite + coldWhite) >= (device.currentValue("level") + 0.4) && (warmWhite + coldWhite) <= (device.currentValue("level") - 0.4)){
			//		sendEvent(name: "level", value: warmWhite + coldWhite )
			//	}
//
			//	// Only change the CT if it's not close to the returned value
			//	// and only change it if the device's value isn't going to lose the data.
			//	// Since going below 5 won't retain the CT accurately
			//	if(device.currentValue('warmWhiteLevel' ) >= (warmWhite + 0.4) && device.currentValue('warmWhiteLevel' ) <= (warmWhite - 0.4)  && device.currentValue('coldWhiteLevel' ) >= (coldWhite + 0.4) && device.currentValue('coldWhiteLevel' ) <= (coldWhite - 0.4) && (warmWhite + coldWhite) > 5){
			//		setTemp = settings.deviceCWTemperature - (( settings.deviceCWTemperature - settings.deviceWWTemperature ) * ( warmWhite / 100 ))
			//		device.currentValue( 'colorTemperature' ) != setTemp.toInteger() ? sendEvent(name: "colorTemperature", value: setTemp.toInteger()) : null
			//	}
			//	if(device.currentValue('warmWhiteLevel' ) >= (warmWhite + 0.4) && device.currentValue('warmWhiteLevel' ) <= (warmWhite - 0.4)){
			//		sendEvent(name: "warmWhiteLevel", value: warmWhite)
			//	}
			//	if(device.currentValue('coldWhiteLevel' ) >= (coldWhite + 0.4) && device.currentValue('coldWhiteLevel' ) <= (coldWhite - 0.4)){
			//		sendEvent(name: "coldWhiteLevel", value: coldWhite)
			//	}
			//} 
			//else{
			//	// Or, set the color
			//	device.currentValue( 'colorMode' ) != 'RGB' ? sendEvent(name: "colorMode", value: "RGB") : null
			//	device.currentValue( 'warmWhiteLevel' ) != 0 ? sendEvent(name: "warmWhiteLevel", value: 0) : null
			//	device.currentValue( 'coldWhiteLevel' ) != 0 ? sendEvent(name: "coldWhiteLevel", value: 0) : null
			//	if(level > 5){
			//	device.currentValue( 'hue' ) >= (hsvMap.hue + 0.4) && device.currentValue( 'hue' ) <= (hsvMap.hue - 0.4) ? sendEvent(name: "hue", value: hsvMap.hue) : null
			//	device.currentValue( 'saturation' ) >= (hsvMap.saturation + 0.4) && device.currentValue( 'saturation' ) <= (hsvMap.saturation - 0.4) ? sendEvent(name: "saturation", value: hsvMap.saturation) : null
			//	}
			//device.currentValue( 'level' ) >= (hsvMap.value + 0.4) && device.currentValue( 'level' ) <= (hsvMap.value - 0.4) ? sendEvent(name: "level", value: hsvMap.value) : null
			//}
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
	unschedule()
	
	String stringBytes = HexUtils.byteArrayToHexString(data)
	logDebug "${data} was converted. Transmitting: ${stringBytes}"
	InterfaceUtils.sendSocketMessage(device, stringBytes)
	runIn(60, initialize)
}

def refresh( ) {
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
	logDebug "socketStatus: ${status}"
	logDebug "Attempting to reconnect in 10 seconds."
	runIn(10, initialize)
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
	
	InterfaceUtils.socketClose(device)
	unschedule()
	try {
		logDebug "Opening Socket Connection."
		InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
		pauseExecution(1000)
		logDebug "Connection successfully established"
	    runIn(1, refresh)
	} catch(e) {
		logDebug("Error attempting to establish TCP connection to device.")
		logDebug("Next initialization attempt in 20 seconds.")
		sendEvent(name: "switch", value: "off") // If we didn't hear back, the device is likely physically powered off
		runIn(20, initialize)
	}
}
def installed(){
	sendEvent(name: "hue", value: 0)
	sendEvent(name: "saturation", value: 100)
	sendEvent(name: "level", value: 99)
	sendEvent(name: "switch", value: "off")
	sendEvent(name: "colorMode", value: "RGB")
}
