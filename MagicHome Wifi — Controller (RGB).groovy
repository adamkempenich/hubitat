/**
 *  MagicHome Wifi - Controller (RGB) 0.8
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
        name: "MagicHome Wifi — Controller (RGB)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich") {
        
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Color Temperature"
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
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: false, displayDuringSetup: true)
		
	    input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when values change?",
              defaultValue: true, required: true, displayDuringSetup: true)

        input(name:"neutralWhite", type:"number", title: "Point where the light changes between cold and warm white hues",
            description: "Temp in K (Default: 4000)", defaultValue: 4000,
            required: false, displayDuringSetup: true)

		input(name:"cwHue", type:"number", title: "Hue that Cold White (bluish light) uses",
			description: "Hue (0 - 100). Default 55", defaultValue: 55)
		input(name:"cwSaturationLowPoint", type:"number", title: "Cold White Saturation closest 4000k (or the neutral white point).",
			description: "Saturation: (0-100) Default: 0", defaultValue: 0)
		input(name:"cwSaturationHighPoint", type:"number", title: "Cold White Saturation at ~6000k.",
			description: "Saturation: (0-100) Default: 50", defaultValue: 50)

		input(name:"wwHue", type:"number", title: "Hue that Warm White (orangeish light) uses",
			description: "Hue (0 - 100). Default 100 (Bulb's White LEDs)", defaultValue: 7.6)
		input(name:"wwSaturationLowPoint", type:"number", title: "Warm White Saturation closest 4000k (or the neutral white point).",
			description: "Saturation: (0-100) Default: 0", defaultValue: 0)
		input(name:"wwSaturationHighPoint", type:"number", title: "Warm White Saturation at ~2700k.",
			description: "Saturation: (0-100) Default: 80 <style>#tileContainter-presetBlueFade-4:hover{ animation: presetBlue-fade 3s infinite } @keyframes presetBlue-fade { 0% { color: blue } 50% { color: black } 100% { color: blue } } #tileContainter-presetBlueStrobe-5:hover{ animation: presetBlue-strobe 3s infinite } @keyframes presetBlue-strobe { 0% { color: blue } 49% { color: blue } 50% { color: black } 100% { color: black } } #tileContainter-presetCyanFade-6:hover{ animation: presetCyan-fade 3s infinite } @keyframes presetCyan-fade { 0% { color: cyan } 50% { color: black } 100% { color: cyan } } #tileContainter-presetCyanStrobe-7:hover{ animation: presetCyan-strobe 3s infinite } @keyframes presetCyan-strobe { 0% { color: cyan } 25% { color: cyan } 26% { color: black } 100% { color: black } } #tileContainter-presetGreenBlueDissolve-8:hover{ animation: presetGreenBlue-dissolve 3s infinite } @keyframes presetGreenBlue-dissolve { 0% { color: green } 50% { color: blue } 100% { color: green } } #tileContainter-presetGreenFade-9:hover{ animation: presetGreen-fade 3s infinite } @keyframes presetGreen-fade { 0% { color: green } 50% { color: black } 100% { color: green } } #tileContainter-presetGreenStrobe-10:hover{ animation: presetGreen-strobe 3s infinite } @keyframes presetGreen-strobe { 0% { color: green } 25% { color: green } 26% { color: black } 100% { color: black } } #tileContainter-presetPurpleFade-11:hover{ animation: presetPurple-fade 3s infinite } @keyframes presetPurple-fade { 0% { color: purple } 50% { color: black } 100% { color: purple } } #tileContainter-presetPurpleStrobe-12:hover{ animation: presetPurple-strobe 3s infinite } @keyframes presetPurple-strobe { 0% { color: purple } 25% { color: purple } 26% { color: black } 100% { color: black } } #tileContainter-presetRedBlueDissolve-13:hover{ animation: presetRedBlue-dissolve 3s infinite } @keyframes presetRedBlue-dissolve { 0% { color: red } 50% { color: blue } 100% { color: red } } #tileContainter-presetRedFade-14:hover{ animation: presetRed-fade 3s infinite } @keyframes presetRed-fade { 0% { color: red } 50% { color: black } 100% { color: red } } #tileContainter-presetRedGreenDissolve-15:hover{ animation: presetRedGreen-dissolve 3s infinite } @keyframes presetRedGreen-dissolve { 0% { color: red } 50% { color: green } 100% { color: red } } #tileContainter-presetRedStrobe-16:hover{ animation: presetRed-strobe 3s infinite } @keyframes presetRed-strobe { 0% { color: red } 25% { color: red } 26% { color: black } 100% { color: black } } #tileContainter-presetSevenColorDissolve-17:hover{ animation: presetSevenColor-dissolve 3s infinite } @keyframes presetSevenColor-dissolve { 0% { color: red } 12.5% { color: orange } 25% { color: yellow } 37.5% { color: green } 50% { color: blue } 62.5% { color: indigo } 75% { color: violet } 87.5% { color: white } 100% { color: red } } #tileContainter-presetSevenColorJump-18:hover{ animation: presetSevenColor-jump 3s infinite } @keyframes presetSevenColor-jump { 0% { color: red } 12% { color: red } 12.5% { color: orange } 24% { color: orange } 25% { color: yellow } 37% { color: yellow } 37.5% { color: green } 49% { color: green } 50% { color: blue } 62% { color: blue } 62.5% { color: indigo } 74% { color: indigo } 75% { color: violet } 87% { color: violet } 87.5% { color: white } 99% { color: white } 100% { color: red } } #tileContainter-presetSevenColorStrobe-19:hover{ animation: presetSevenColor-strobe 3s infinite } @keyframes presetSevenColor-strobe {  0% { color: black } 11% { color: black }  12% { color: red } 16% { color: red }  17% { color: black } 30% { color: black }  31% { color: orange } 38% { color: orange }  39% { color: black } 49% { color: black }  50% { color: yellow } 62% { color: yellow }  63% { color: black } 70% { color: black }  71% { color: green } 79% { color: green }  80% { color: black } 88% { color: black }  89% { color: blue } 99% { color: blue }  99% { color: black } } #tileContainter-presetWhiteFade-20:hover{ animation: presetWhite-fade 3s infinite } @keyframes presetWhite-fade { 0% { color: White } 50% { color: black } 100% { color: White } } #tileContainter-presetWhiteStrobe-21:hover{ animation: presetWhite-strobe 3s infinite } @keyframes presetWhite-strobe { 0% { color: White } 49% { color: White } 50% { color: black } 100% { color: black } } #tileContainter-presetYellowFade-22:hover{ animation: presetYellow-fade 3s infinite } @keyframes presetYellow-fade { 0% { color: Yellow } 50% { color: black } 100% { color: Yellow } } #tileContainter-presetYellowStrobe-23:hover{ animation: presetYellow-strobe 3s infinite } @keyframes presetYellow-strobe { 0% { color: Yellow } 49% { color: Yellow } 50% { color: black } 100% { color: black } } </style>", 
			  defaultValue: 80)
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


def setHue(hue, transmit=true){
    // Set the hue of a device (0-99)

    hue = normalizePercent( hue, 0, 99)
    sendEvent(name: "hue", value: hue )
	logDebug( "Hue set to " + device.currentValue('hue'))
	    
    if( !transmit ) return hue
    setColor( hue: hue )

}

def getHue(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "hue" ) == null ? ( setHue( 99, false ) ) : ( device.currentValue( "hue" ) )
}

def setSaturation(saturation, transmit=true){
    // Set the saturation of a device (0-100)

    saturation = normalizePercent( saturation )
    sendEvent(name: "saturation", value: saturation)
    logDebug( "Saturation set to ${saturation}")
    
    if( !transmit ) return saturation
    setColor( saturation: saturation )
}
def getSaturation(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "saturation" ) == null ? ( setSaturation( 100, false ) ) : ( device.currentValue( "saturation" ) )
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

def setColor( parameters ){
	// Set the color of a device. Hue (0 - 99), Saturation (0 - 100), Level (0 - 100). If Hue is 16.6, use the white LEDs.
    
  	// Register that presets are disabled
  	sendEvent( name: "currentPreset", value: 0 )

    powerOnWithChanges()
	
	rgbColors = hsvToRGB( checkIfInMap( parameters?.hue, "hue"), checkIfInMap( parameters?.saturation, "saturation"), checkIfInMap( parameters?.level, "level") )
	byte[] data = appendChecksum(  [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x00, 0x0f ] )
	sendCommand( data ) 
	
}

def setColorTemperature( setTemp = getColorTemperature(), transmit=true ){
	// Using RGB, adjust the color temperature of a device	
    
	// Set the colorTemperature's value between the device's maximum range, if it's out of bounds
	setTemp = normalizePercent( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature )
	def newSaturation
	def newHue
	
    if(setTemp >= neutralWhite){
		newSaturation = setSaturation( calculateCTSaturation( true, setTemp - settings.neutralWhite ), false ).toFloat()
		newHue = setHue( settings.cwHue.toFloat(), false ) 
	}
	else{
		newSaturation = setSaturation( calculateCTSaturation( false, settings.neutralWhite - setTemp ), false ).toFloat()
		newHue = setHue( settings.wwHue.toFloat(), false ) 
	}
	
    sendEvent( name: "colorTemperature", value: setTemp )
	setColor( [ hue:newHue, saturation:newSaturation ]  )
}

def getColorTemperature(){
    // Get the color temperature of a device ( ~2000-7000 )

    return device.currentValue( "colorTemperature" ) == null ? ( sendEvent( name: "colorTemperature", value: 2700 ) ) : ( device.currentValue( "colorTemperature" ) )
}

def sendPreset( turnOn, preset = 1, speed = 100, transmit = true ){
    // Turn on preset mode (true), turn off preset mode (false). Preset (1 - 20), Speed (1 (slow) - 100 (fast)).

    // Presets:
    // 1 Seven Color Dissolve, 2 Red Fade, 3 Green Fade, 4 Blue Fade, 5 Yellow Fade, 6 Cyan Fade, 7 Purple Fade, 8 White Fade, 9 Red Green Dissolve
    // 10 Red Blue Dissolve, 11 Green Blue Dissolve, 12 Seven Color Strobe, 13 Red Strobe, 14 Green Strobe, 15 Blue Strobe, 16 Yellow Strobe
    // 17 Cyan Strobe, 18 Purple Strobe, 19 White Strobe, 20 Seven Color Jump

    if(turnOn){
        normalizePercent( preset, 1, 20 )
        normalizePercent( speed )
        
        // Hex range of presets is (int) 37 - (int) 57. Add the preset number to get that range.
        preset += 36
        speed = (100 - speed)

        powerOnWithChanges()

        sendEvent( name: "currentPreset", value: preset )
        sendEvent( name: "presetSpeed", value: speed )

        sendCommand( appendChecksum(  [ 0x61, preset, speed, 0x0F ] ) ) 
    }
    else{
        // Return the color back to its normal state

        sendEvent( name: "currentPreset", value: 0 )
        setColor( null )
    }
}

def presetSevenColorDissolve( speed = 100 ){
    sendPreset( true, 1, speed )
}
def presetRedFade( speed = 100 ){
    sendPreset( true, 2, speed )
}
def presetGreenFade( speed = 100 ){
    sendPreset( true, 3, speed )
}
def presetBlueFade( speed = 100 ){
    sendPreset( true, 4, speed )
}
def presetYellowFade( speed = 100 ){
    sendPreset( true, 5, speed )
}
def presetCyanFade( speed = 100 ){
    sendPreset( true, 6, speed )
}
def presetPurpleFade( speed = 100 ){
    sendPreset( true, 7, speed )
}
def presetWhiteFade( speed = 100 ){
    sendPreset( true, 8, speed )
}
def presetRedGreenDissolve( speed = 100 ){
    sendPreset( true, 9, speed )
}
def presetRedBlueDissolve( speed = 100 ){
    sendPreset( true, 10, speed )
}
def presetGreenBlueDissolve( speed = 100 ){
    sendPreset( true, 11, speed )
}
def presetSevenColorStrobe( speed = 100 ){
    sendPreset( true, 12, speed )
}
def presetRedStrobe( speed = 100 ){
    sendPreset( true, 19, speed )
}
def presetGreenStrobe( speed = 100 ){
    sendPreset( true, 14, speed )
}
def presetBlueStrobe( speed = 100 ){
    sendPreset( true, 15, speed )
}
def presetYellowStrobe( speed = 100 ){
    sendPreset( true, 16, speed )
}
def presetCyanStrobe( speed = 100 ){
    sendPreset( true, 17, speed )
}
def presetPurpleStrobe( speed = 100 ){
    sendPreset( true, 18, speed )
}
def presetWhiteStrobe( speed = 100 ){
    sendPreset( true, 19, speed )
}
def presetSevenColorJump( speed = 100 ){
    sendPreset( true, 20, speed )
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

def hsvToRGB(float conversionHue = 0, float conversionSaturation = 100, float conversionValue = 100, resolution = "low"){
    // Accepts conversionHue (0-100 or 0-360), conversionSaturation (0-100), and converstionValue (0-100), resolution ("low", "high")
    // If resolution is low, conversionHue accepts 0-100. If resolution is high, conversionHue accepts 0-360
    // Returns RGB map ([ red: 0-255, green: 0-255, blue: 0-255 ])
    
    // Check HSV limits
	hue = hue > 100 ? normalizePercent( hue, 0, 99 ) : normalizePercent( hue, 0, 359 )
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

	logDebug "Device responded with ${response}"
	def responseArray = HexUtils.hexStringToIntArray(response)
	if( responseArray.length == 4 ) {
		// Device has responded with a power status packet
		
		responseArray[ 2 ] == 35 ? sendEvent(name: "switch", value: "on") : sendEvent(name: "switch", value: "off")
	}
	else if( responseArray.length == 14 ) {
		// Device responded with full color info set
		
		double white = responseArray[ 9 ] / 2.55
		hsvMap = rgbToHSV( responseArray[ 6 ], responseArray[ 7 ], responseArray[ 8 ] )
		
		// Assign Returned Power, Hue, Saturation, Level
		responseArray[ 2 ] == 35 ? ( sendEvent(name: "switch", value: "on") ) : ( sendEvent(name: "switch", value: "off") )
		setHue( hsvMap.hue, false )
		setSaturation( hsvMap.saturation, false )
		setLevel( hsvMap.value, false )
	    //hsvMap.hue == settings.wwHue || hsvMap.hue == settings.cwHue ? ( setColorTemperature( null, false ) ) : ( null )

	}
	else if( response == null ){
		logDebug "No response received from device."
		initialize()
	}
	else{
		logDebug "Received a response with an unexpected length of ${responseArray.length}"
	}
}

private logDebug( debugText ){
	// If debugging is enabled in settings, pass text to the logs
	
	if( settings.logDebug ) { 
		log.info "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

def sendCommand(data) {
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
