/**
*  MagicHome Wifi - Bulb (WW/CW CCT) 0.85
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
        name: "MagicHome Wifi — Bulb (WW/CW CCT)", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
		importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/MagicHome/MagicHome%20Wifi%20—%20Bulb%20(RGBWW).groovy") {
        
        capability "Actuator"
        capability "Color Temperature"
		capability "Initialize"
        capability "Polling"
        capability "Refresh"
		capability "Sensor"
		capability "Switch"
		capability "Switch Level"
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

def setLevel(level) {
    // Set the brightness of a device (0-100)
	level > 100 ? (level = 100) : null
    sendEvent(name: "level", value: level)
    logDebug "Level set to ${level}"
    
    setColorTemperature(device.currentValue('colorTemperature'), level)
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
    byte[] data =  appendChecksum( [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ] )
	sendCommand( data )
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
	sendEvent(name: "switch", value: "off")
	sendEvent(name: "colorTemperature", value: 4000)
	sendEvent(name: "colorMode", value: "RGB")
}
