import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.device.Protocol

metadata {
	definition (name: "MagicHome Wifi — Controller (RGBWW)", namespace: "MagicHome", author: "Adam Kempenich") {
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Color Temperature"
        capability "Color Control"
		
        command "on"
        command "off" 

        command "setLevel", [ "number" ] 		// 0 - 100
        command "setHue", [ "number" ] 			// 0 - 100
        command "setSaturation", [ "number" ] 	// 0 - 100
    	command "setAdjustedColor"
		command "setColor", ["number", "number", "number"] // Hue (0-100), Saturation (0-100), Value (0-100)
        command "setColorTemperature", [ "number" ] // Kelvin ( Light Minimum Color Temperature - Light Maximum Color Temperature )
        command "setWarmWhiteLevel", [ "number" ] // 0 - 100
        command "setColdWhiteLevel", [ "number" ] // 0 - 100

        command "sendPreset", ["number", "number"] 		 // 0 (off), 1-20 (other presets)
        command "presetRedFade", 			[ "number" ] // 0 - 100 (speed)
		command "presetGreenFade", 			[ "number" ] // 0 - 100 (speed)
		command "presetBlueFade", 			[ "number" ] // 0 - 100 (speed)
		command "presetYellowFade", 		[ "number" ] // 0 - 100 (speed)
		command "presetCyanFade", 			[ "number" ] // 0 - 100 (speed)
		command "presetPurpleFade", 		[ "number" ] // 0 - 100 (speed)
		command "presetWhiteFade", 			[ "number" ] // 0 - 100 (speed)
		command "presetRedGreenDissolve", 	[ "number" ] // 0 - 100 (speed)
		command "presetRedBlueDissolve", 	[ "number" ] // 0 - 100 (speed)
		command "presetGreenBlueDissolve", 	[ "number" ] // 0 - 100 (speed)
		command "presetSevenColorStrobe", 	[ "number" ] // 0 - 100 (speed)
		command "presetRedStrobe", 			[ "number" ] // 0 - 100 (speed)
		command "presetGreenStrobe", 		[ "number" ] // 0 - 100 (speed)
		command "presetBlueStrobe", 		[ "number" ] // 0 - 100 (speed)
		command "presetYellowStrobe", 		[ "number" ] // 0 - 100 (speed)
		command "presetCyanStrobe", 		[ "number" ] // 0 - 100 (speed)
		command "presetPurpleStrobe", 		[ "number" ] // 0 - 100 (speed)
		command "presetSevenColorJump",	 	[ "number" ] // 0 - 100 (speed)
        
    	attribute "currentPreset", "string" // 0 (off), 1-20 (other presets)
        attribute "presetSpeed", "string" 
        attribute "warmWhiteLevel", "number"
		attribute "coldWhiteLevel", "number"

	}
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

		
	    input(name:"powerOnBrightnessChange", type:"bool", title: "Turn on this light when brightness changes?",
       		  description: "Makes devices behave like other switches. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)

	    input(name:"enableRGBCT", type:"bool", title: "Use RGB in Color Temperature calculations?",
			description: "For use with color temperature calculation.", defaultValue: true,
			required: true, displayDuringSetup: true)

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
			description: "Hue (0 - 100). Default 100 (Bulb's White LEDs)", defaultValue: 100)
		input(name:"wwSaturationLowPoint", type:"number", title: "Cold White Saturation closest 4000k (or the neutral white point).",
			description: "Saturation: (0-100) Default: 0", defaultValue: 0)
		input(name:"wwSaturationHighPoint", type:"number", title: "Cold White Saturation at ~2700k.",
			description: "Saturation: (0-100) Default: 50", defaultValue: 50)


		input(name:"enableWW", type:"bool", title: "Use Warm White in Color Temperature calculations?",
			description: "For use with color temperature calculation.", defaultValue: true,
			required: true, displayDuringSetup: true)
        input(name:"deviceWarmWhiteTemperature", type:"number", title: "Warm white channel's color temperature .",
            description: "Temp in K (default 2700)", defaultValue: 2700,
            required: false, displayDuringSetup: true)

        input(name:"ww_low_cutoff_temperature", type:"number", title: "Low-End CW point?",
			description: "Temp in K", defaultValue: 2300,
			required: true, displayDuringSetup: true)
        input(name:"ww_high_cutoff_temperature", type:"number", title: "High-end CW point?",
			description: "Temp in K", defaultValue: 6200,
			required: false, displayDuringSetup: true)

		input(name:"enableCW", type:"bool", title: "Use Cold White in Color Temperature calculations?",
			description: "For use with color temperature calculation.", defaultValue: true,
			required: true, displayDuringSetup: true)
        input(name:"deviceColdWhiteTemperature", type:"number", title: "Cold white channel's color temperature .",
            description: "Temp in K (default 6000)", defaultValue: 6000,
            required: false, displayDuringSetup: true)

		input(name:"cw_low_cutoff_temperature", type:"number", title: "Low-End CW point?",
			description: "Temp in K", defaultValue: 2800,
			required: true, displayDuringSetup: true)
        input(name:"cw_high_cutoff_temperature", type:"number", title: "High-end CW point?",
			description: "Temp in K", defaultValue: 7000,
			required: true, displayDuringSetup: true)
            

        input(name:"whiteFollowsBrightness", type:"bool", title: "White channel follows device brightness for color temperature?",
            description: "Default: On", defaultValue: true,
            required: false, displayDuringSetup: true)

        input(name:"high_cri_mode", type:"bool", title: "Enable high-CRI color temperature mode",
       		  description: "Blast WW & CW channels at full, use color range to adjust temperature.", defaultValue: false,
              required: true, displayDuringSetup: true)
	}
}

def poll() {
	parent.poll(this)
}

def parse(resp) {	    
	parseResponse(resp)    
}

private parseResponse(resp){
  log.debug resp
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    log.debug "MagicHome - Switch set to " + device.currentValue("switch")
    byte[] data = [0x71, 0x23,  0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

	sendEvent(name: "switch", value: "off")
    log.debug "MagicHome - Switch set to " + device.currentValue("switch")
    byte[] data = [0x71, 0x24,  0x0F, 0xA4]
    sendCommand(data)
}

def setHue(hue, transmit=true){
    // Set the hue of a device (0-100)

   	hue = normalizePercent(hue) 
	sendEvent(name: "hue", value: hue)
    log.debug "MagicHome - Hue set to " + device.currentValue("hue")
    if( transmit ) {
    	setColor([hue:hue])
    }
    else {
    	return device.currentValue( "hue" )
    }
}

def setSaturation(saturation, transmit=true){
    // Set the saturation of a device (0-100)

    saturation = normalizePercent(saturation)
	sendEvent(name: "saturation", value: saturation)    
	log.debug "MagicHome - Saturation set to " + device.currentValue("saturation")
    if( transmit ) {
    	setColor([saturation:saturation])
    }
    else{
    	return device.currentValue( "saturation" )
    }
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    level = normalizePercent(level)
    sendEvent(name: "level", value: level)
    log.debug "MagicHome - Level set to " + device.currentValue( "level" )

    if( transmit ) {
        setColor([level:level])
    }
    else{
    	return device.currentValue( "level" )   
    }
}

def setWarmWhiteLevel( warmWhiteLevel, transmit=true ){
    // Set the dedicated white channel's brightness of a device (0-100)

   	warmWhiteLevel = normalizePercent(warmWhiteLevel) 
	sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
    log.debug "MagicHome - Warm white level set to " + device.currentValue("warmWhiteLevel")
    if( transmit ) {
    	setColor( [ warmWhitelevel : warmWhiteLevel ] )
    }
    else {
    	return device.currentValue( "warmWhiteLevel" )
    }
}

def setColdWhiteLevel( coldWhiteLevel, transmit=true ){
    // Set the dedicated white channel's brightness of a device (0-100)

   	coldWhiteLevel = normalizePercent(coldWhiteLevel) 
	sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
    log.debug "MagicHome - Cold white level set to " + device.currentValue("coldWhiteLevel")
    if( transmit ) {
    	setColor( [ coldWhitelevel : coldWhiteLevel ] )
    }
    else {
    	return device.currentValue( "coldWhiteLevel" )
    }
}

def setAdjustedColor( parameters ){
	// This is an old function from the SmartThings days. Pass its values to setColor

	setColor( parameters )
}

def setColor(parameters){
	// Set the color of a device. Hue (0 - 100), Saturation (0 - 100), Level (0 - 100). If Hue is 16.6, use the white LEDs.

	byte[] msg
	byte[] data

	if(parameters.hue == null){
		if( device.currentValue( "hue" ) == null ){
			sendEvent( name: "hue", value: 100 )
			parameters.hue = 100
		}
		else{
			parameters.hue = device.currentValue( "hue" )
		}
	}
	else{
		sendEvent( name: "hue", value: normalizePercent(parameters.hue))
	}
	if(parameters.saturation == null){
		if( device.currentValue( "saturation" ) == null ){
			sendEvent( name: "saturation", value: 100 )
			parameters.saturation = 100
		}
		else{
			parameters.saturation = device.currentValue( "saturation" )
		}
	}
	else{
		sendEvent( name: "saturation", value: normalizePercent(parameters.saturation))
	}
	if(parameters.level == null){
        if( device.currentValue( "level" ) == null ){
            sendEvent( name: "level", value: 100 )
            parameters.level = 100
        }
        else{
            parameters.level = device.currentValue( "level" )
        }
    }
    else{
        sendEvent( name: "level", value: normalizePercent(parameters.level))
    }
    if(parameters.warmWhiteLevel == null){
        if( device.currentValue( "warmWhiteLevel" ) == null ){
            sendEvent( name: "warmWhiteLevel", value: 100 )
            parameters.warmWhiteLevel = 100
        }
        else{
            parameters.warmWhiteLevel = device.currentValue( "warmWhiteLevel" )
        }
    }
    else{
        sendEvent( name: "level", value: normalizePercent(parameters.warmWhiteLevel))
    }
    if(parameters.coldWhiteLevel == null){
        if( device.currentValue( "coldWhiteLevel" ) == null ){
            sendEvent( name: "coldWhiteLevel", value: 100 )
            parameters.coldWhiteLevel = 100
        }
        else{
            parameters.coldWhiteLevel = device.currentValue( "coldWhiteLevel" )
        }
    }
    else{
        sendEvent( name: "level", value: normalizePercent(parameters.coldWhiteLevel))
    }


	if(settings.powerOnBrightnessChange){
    	device.currentValue("status") == "on" ? on() : ( null )
    }
    // Register that presets are disabled
    sendEvent( name: "currentPreset", value: 0 )


    // Convert HSL (0-100, 0-100, 0-100) to an RGB list (0-255, 0-255, 0-255)
	rgbColors = hslToRGB( parameters.hue, parameters.saturation, parameters.level )

	// Update Color
	msg =  [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x00, 0xf0, 0x0f ]
	data = [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x00, 0xf0, 0x0f, calculateChecksum( msg )]
	sendCommand( data )

    // Update WW/CW
	msg =  [0x31, 0x00, 0x00, 0x00, parameters.warmWhiteLevel * 2.55, parameters.coldWhiteLevel * 2.55, 0x0f, 0x0f]
	data = [0x31, 0x00, 0x00, 0x00, parameters.warmWhiteLevel * 2.55, parameters.coldWhiteLevel * 2.55, 0x0f, 0x0f, calculateChecksum( msg )]
	sendCommand( data )
}

def setColorTemperature(setTemp, transmit=true){
    // Using RGB and the WW/CW channels, adjust the color temperature of a device

    // If a level isn't set, initialize it
    device.currentValue( "level" ) == null ? ( deviceLevel = setLevel( 100, false )): ( deviceLevel = device.currentValue( "level" ))
    device.currentValue( "warmWhiteLevel" ) == null ? ( deviceWarmWhiteLevel = setWarmWhiteLevel( 100, false )): ( deviceWarmWhiteLevel = device.currentValue( "warmWhiteLevel" ))
    device.currentValue( "coldWhiteLevel" ) == null ? ( deviceColdWhiteLevel = setColdWhiteLevel( 100, false )): ( deviceColdWhiteLevel = device.currentValue( "coldWhiteLevel" ))
	// If the percentage is above zero, but less than one (for some reason), set it to one
    roundUpIfBetweenTwoNumbers( deviceLevel )

    // If no color temperature was passed through, use the current device's color temperature, and check if it's in bounds
    if(setTemp == null){
        if(device.currentValue( "colorTemperature") != null){
            setTemp = device.currentValue( "colorTemperature" )
        }
        else{
            sendEvent( name: "colorTemperature", value: 2700 )
            setTemp = device.currentValue( "colorTemperature" )
        }
    }
    else{
		sendEvent( name: "colorTemperature", value: setTemp )
	}
    // Set the colorTemperature's value between the device's maximum range, if it's out of bounds
    setTemp = normalizePercent( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature )
    
    log.info 'MagicHome - Color Temperature set to ' + setTemp
    // ————————————————————————————————————————————— Start RGB CT Handling ————————————————————————————————————————————— //
    if(settings.enableRGBCT){
        // Initialize base variables
    	def ( float newSaturation, float newHue, int brightnessWarmWhite, int brightnessColdWhite ) = [ null, null, 0, 0 ]
       	def ( double setCoolWhiteHue, double setWarmWhiteHue, int neutralWhite ) = [normalizePercent( settings.cwHue ), normalizePercent( settings.wwHue ), normalizePercent( settings.neutralWhite, 2000, 8000 )]
       	def ( int cwSaturationLowPoint, int cwSaturationHighPoint, int wwSaturationLowPoint, int wwSaturationHighPoint ) = [ normalizePercent( settings.cwSaturationLowPoint ), normalizePercent( settings.cwSaturationHighPoint ), normalizePercent( settings.wwSaturationLowPoint ), normalizePercent( settings.wwSaturationHighPoint ) ]
       	def ( int deviceWarmWhiteTemperature, int deviceColdWhiteTemperature, int ww_high_cutoff_temp, int ww_low_cutoff_temp , int cw_low_cutoff_temp , int cw_high_cutoff_temp) = [settings.deviceWarmWhiteTemperature, settings.deviceColdWhiteTemperature, settings.ww_high_cutoff_temperature, settings.ww_low_cutoff_temperature, settings.cw_low_cutoff_temperature, settings.cw_high_cutoff_temperature]    
    
        if(setTemp >= neutralWhite){
        	// Set cold white temperature if above the user's neutral white point
    
        	int offset = setTemp - neutralWhite
            	if( cwSaturationLowPoint < cwSaturationHighPoint ){
        			newSaturation = (((( 100 - cwSaturationLowPoint)/100 ) * ( 1.8 * Math.sqrt( offset ))) + cwSaturationLowPoint ) * cwSaturationHighPoint / 100
            	}
                else{
        			newSaturation = (((( 100 - cwSaturationHighPoint)/100 ) * ( 1.8 * Math.sqrt( offset ))) + cwSaturationHighPoint ) * cwSaturationLowPoint / 100
                }
            newHue =  setCoolWhiteHue / 3.6 
        }
        else{
        	// set warm white temperature if below the user's neutral white point
    
        	int offset = neutralWhite - setTemp
            	if(wwSaturationLowPoint < wwSaturationHighPoint ){
        			newSaturation = (((( 100 - wwSaturationLowPoint ) / 100) * ( 2.166666 * Math.sqrt( offset ))) + wwSaturationLowPoint ) * wwSaturationHighPoint / 100
                }
                else{
        			newSaturation = (((( 100 - wwSaturationHighPoint ) / 100) * ( 2.166666 * Math.sqrt( offset ))) + wwSaturationHighPoint ) * wwSaturationLowPoint / 100
                }
            newHue =  setWarmWhiteHue / 3.6
        }
    }

    // ————————————————————————————————————————————— Start White Channel CT Handling ————————————————————————————————————————————— //



    // High CRI mode is an idea I had where we use both of WW/CW channels and blast their channels to max, using extreme shifts in the color to shift the hue
    // It only works with LED strips
    if(settings.high_cri_mode){
    	brightnessWarmWhite = 100
    	brightnessColdWhite = 100
    }
    else{
        // Set the warm white and cold white channel temperatures
        if(settings.enableWW){
            if(deviceWarmWhiteTemperature <= setTemp){
                brightnessWarmWhite = ((100)/(deviceWarmWhiteTemperature - ww_high_cutoff_temp))*setTemp + (100 - (100/(deviceWarmWhiteTemperature - ww_high_cutoff_temp))*deviceWarmWhiteTemperature)
            }
            else{
                brightnessWarmWhite = ((100)/(deviceWarmWhiteTemperature - ww_low_cutoff_temp))*setTemp + (100 - (100/(deviceWarmWhiteTemperature - ww_low_cutoff_temp))*deviceWarmWhiteTemperature)
            }
        }
        if(settings.enableCW){
            if(deviceColdWhiteTemperature >= setTemp){
                brightnessColdWhite = ((100)/(deviceColdWhiteTemperature - cw_low_cutoff_temp))*setTemp + (100 - (100/(deviceColdWhiteTemperature - cw_low_cutoff_temp))*deviceColdWhiteTemperature)
            }
            else{
                brightnessColdWhite = ((100)/(deviceColdWhiteTemperature - cw_high_cutoff_temp))*setTemp + (100 - (100/(deviceColdWhiteTemperature - cw_high_cutoff_temp))*deviceColdWhiteTemperature)
            }
        }
    }

    // Adjust the brightness by using device level as modifier
    if(settings.whiteFollowsBrightness){
        brightnessWarmWhite = roundUpBetweenZeroAndOne( normalizePercent( brightnessWarmWhite * deviceLevel / 100 ) )
        brightnessColdWhite = roundUpBetweenZeroAndOne( normalizePercent( brightnessColdWhite * deviceLevel / 100 ) )
    }
    else{
        normalizePercent( brightnessWarmWhite )
        normalizePercent( brightnessColdWhite)
    }


	def parameters = [ hue: newHue, saturation: newSaturation, warmWhitelevel: brightnessWarmWhite, coldWhitelevel: brightnessColdWhite ]
	setColor( parameters )
}


// ------------------- Begin Preset Handling ------------------------- //
def sendPreset( turnOn, preset = 1, speed = 100, transmit = true ){
	// Turn on preset mode (true), turn off preset mode (false). Preset (1 - 20), Speed (1 (slow) - 100 (fast)).

	// Presets:
	// 1 Seven Color Dissolve, 2 Red Fade, 3 Green Fade, 4 Blue Fade, 5 Yellow Fade, 6 Cyan Fade, 7 Purple Fade, 8 White Fade, 9 Red Green Dissolve
	// 10 Red Blue Dissolve, 11 Green Blue Dissolve, 12 Seven Color Strobe, 13 Red Strobe, 14 Green Strobe, 15 Blue Strobe, 16 Yellow Strobe
	// 17 Cyan Strobe, 18 Purple Strobe, 19 White Strobe, 20 Seven Color Jump

	byte[] msg
	byte[] data

    if(turnOn){
    	normalizePercent( preset, 1, 20 )
    	normalizePercent( speed )
        
        // Hex range of presets is (int) 37 - (int) 57. Add the preset number to get that range.
        preset += 36
        speed = (100 - speed)

        msg =  [ 0x61, preset, speed, 0x0F ]
        data = [ 0x61, preset, speed, 0x0F, calculateChecksum(msg) ]
        if(settings.powerOnBrightnessChange){
    		device.currentValue("status") == "on" ? on() : (null)
    	}

    	sendEvent( name: "currentPreset", value: preset )
    	sendEvent( name: "presetSpeed", value: speed )

    	sendCommand( data )
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
	sendPreset( true, 10, speed )
}
def presetRedBlueDissolve( speed = 100 ){
	sendPreset( true, 11, speed )
}
def presetGreenBlueDissolve( speed = 100 ){
	sendPreset( true, 12, speed )
}
def presetSevenColorStrobe( speed = 100 ){
	sendPreset( true, 13, speed )
}
def presetRedStrobe( speed = 100 ){
	sendPreset( true, 14, speed )
}
def presetGreenStrobe( speed = 100 ){
	sendPreset( true, 15, speed )
}
def presetBlueStrobe( speed = 100 ){
	sendPreset( true, 16, speed )
}
def presetYellowStrobe( speed = 100 ){
	sendPreset( true, 17, speed )
}
def presetCyanStrobe( speed = 100 ){
	sendPreset( true, 18, speed )
}
def presetPurpleStrobe( speed = 100 ){
	sendPreset( true, 19, speed )
}
def presetSevenColorJump( speed = 100 ){
	sendPreset( true, 20, speed )
}

// ------------------- End Preset Handling ------------------------- //


// ------------------- Begin Helper Functions ------------------------- //
def normalizePercent(value, lowerBound=0, upperBound=100){
    // Takes a value and ensures it's between two defined thresholds

    // If the value doesn't exist, create it
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

def roundUpIfBetweenTwoNumbers(number, lowPoint = 0, highPoint = 1){
    if(number > lowPoint && number < highPoint){
        return highPoint
    }
    else{
        return number
    }
}

def hslToRGB(float conversionHue, float conversionSaturation, float conversionValue){
	// Returns RGB between 0 - 255
	conversionHue = conversionHue / 100
	conversionSaturation = conversionSaturation / 100
	conversionValue = conversionValue / 100

    log.debug 'Adding RGB via HSV conversion. H: ' + conversionHue + ' S: ' + conversionSaturation + ' L: ' + conversionValue
        
        
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

def calculateChecksum(bytes){
    // Totals an array of bytes
    int sum = 0;
    for(int d : bytes)
	    sum += d;
    return sum & 255
}

// ------------------- End Helper Functions ------------------------- //

def sendCommand(data) {
    // Sends commands to the device
	
    telnetConnect([byteInterface: true], "${settings.deviceIP}", settings.devicePort.toInteger(), null, null)
    
	String stringBytes = HexUtils.byteArrayToHexString(data)
	log.debug "" +  data + " was converted. Transmitting: " + stringBytes

    def transmission = new HubAction(stringBytes, Protocol.TELNET)
    sendHubCommand(transmission)
}

def refresh(data) {
    msg =  [ 0x81, 0x8A, 0x8B ]
    data = [ 0x81, 0x8A, 0x8B, calculateChecksum( msg )]

    sendCommand( data )
}
