import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
	definition (name: "MagicHome Wifi — Controller (RGB)", namespace: "MagicHome", author: "Adam Kempenich") {
        capability "Switch Level"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Color Temperature"
        capability "Color Control"
	capability "Initialize"
		
        command "on"
        command "off" 
        command "sendPreset", ["number", "number"]       // 0 (off), 1-20 (other presets)
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
        
    	attribute "currentPreset", "string" // 0 (off), 1-20 (other presets)
        attribute "presetSpeed", "string" 
	}
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

		
	    input(name:"powerOnWithChanges", type:"bool", title: "Turn on this light when settings change?",
              description: "Makes devices behave like other switches.", defaultValue: true,
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
		input(name:"wwSaturationLowPoint", type:"number", title: "Warm White Saturation closest 4000k (or the neutral white point).",
			description: "Saturation: (0-100) Default: 0", defaultValue: 0)
		input(name:"wwSaturationHighPoint", type:"number", title: "Warm White Saturation at ~2700k.",
			description: "Saturation: (0-100) Default: 50", defaultValue: 50)
	}
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
    // Set the hue of a device (0-99)

    hue = normalizePercent( hue, 0, 99) 
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
    log.debug "MagicHome - Level set to " + level

    if( transmit ) {
        setColor([level:level])
    }
    else{
        return device.currentValue( "level" )   
    }
}

def setAdjustedColor( parameters ){
	// This is an old function from the SmartThings days. Pass its values to setColor

	setColor( parameters )
}

def setColor(parameters){
	// Set the color of a device. Hue (0 - 100), Saturation (0 - 100), Level (0 - 100).

	byte[] msg
	byte[] data

	// ------------ Device Hue ------------ //
    if(parameters.hue == null){
        if( device.currentValue( "hue" ) == null ){
            sendEvent( name: "hue", value: 99 )
            parameters.hue = 99
        }
        else{
            parameters.hue = device.currentValue( "hue" )
        }
    }
    else{
        normalizedHue = normalizePercent( parameters.hue, 0, 99 )
        sendEvent( name: "hue", value: normalizedHue)
        parameters.hue = normalizedHue
    }
    // ------------ Device Saturation ------------ //
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
        normalizedSaturation = normalizePercent( parameters.saturation )
        sendEvent( name: "saturation", value: normalizedSaturation)
        parameters.saturation = normalizedSaturation
    }
    // ------------ Device Level ------------ //
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
		if( parameters.level != -1 ) {
			normalizedLevel = normalizePercent( parameters.level )
			sendEvent( name: "level", value: normalizedLevel)
			parameters.level = normalizedLevel
		}
    }


	powerOnWithChanges()

    // Register that presets are disabled
    sendEvent( name: "currentPreset", value: 0 )


	rgbColors = hsvToRGB( parameters.hue, parameters.saturation, parameters.level )

	msg =  [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x0f ]
	data = [ 0x31, rgbColors.red, rgbColors.green, rgbColors.blue, 0x00, 0x0f, calculateChecksum( msg ) ]

    sendCommand( data )
}

def setColorTemperature(setTemp, transmit=true){
    // Using RGB and the WW/CW channels, adjust the color temperature of a device

    // If a level isn't set, create it
    device.currentValue( "level" ) == null ? ( deviceLevel = setLevel( 100, false )): ( deviceLevel = device.currentValue( "level" ))
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
    // Initialize base variables
	def ( float newSaturation, float newHue ) = [ 0, 0 ]
   	def ( double setCoolWhiteHue, double setWarmWhiteHue, int neutralWhite ) = [normalizePercent( settings.cwHue ), normalizePercent( settings.wwHue ), normalizePercent( settings.neutralWhite, 2000, 8000 )]
   	def ( int cwSaturationLowPoint, int cwSaturationHighPoint, int wwSaturationLowPoint, int wwSaturationHighPoint ) = [ normalizePercent( settings.cwSaturationLowPoint ), normalizePercent( settings.cwSaturationHighPoint ), normalizePercent( settings.wwSaturationLowPoint ), normalizePercent( settings.wwSaturationHighPoint ) ]

    if(setTemp >= neutralWhite){
    	// Set cold white temperature if above the user's neutral white point

    	int offset = setTemp - neutralWhite
        	if( cwSaturationLowPoint < cwSaturationHighPoint ){
    			newSaturation = (((( 100 - cwSaturationLowPoint)/100 ) * ( 1.8 * Math.sqrt( offset ))) + cwSaturationLowPoint ) * cwSaturationHighPoint / 100
        	}
            else{
    			newSaturation = (((( 100 - cwSaturationHighPoint)/100 ) * ( 1.8 * Math.sqrt( offset ))) + cwSaturationHighPoint ) * cwSaturationLowPoint / 100
            }
        newHue =  setCoolWhiteHue
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
        newHue =  setWarmWhiteHue
    }

    // If powerOnBrightnessChange is enabled, ensure the device is on
    settings.powerOnBrightnessChange ? ( device.currentValue("status") == "on" ? on() : null ) : null
    
    // Update WW/CW
	def parameters = [hue: newHue, saturation: newSaturation]
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
def presetWhiteStrobe( speed = 75 ){
    sendPreset( true, 19, speed )
}
def presetSevenColorJump( speed = 100 ){
    sendPreset( true, 20, speed )
}

def powerOnWithChanges(){
    // If the device is off and light settings change, turn it on (if user settings apply)
    
	settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
}

def normalizePercent(value, lowerBound=0, upperBound=100){
    // Takes a value and ensures it's between two defined thresholds

    // If the value doesn't exist, create it
    value == null ? value = upperBound : null
    
    // If the boundary parameters were backwards (for some reason) flip them around
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
    // Round up everything between two numbers when necessary
    
    if(number > lowPoint && number < highPoint){
        return highPoint
    }
    else{
        return number
    }
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

def calculateChecksum(bytes){
    // Totals an array of bytes
    int sum = 0;
    for(int d : bytes)
	    sum += d;
    return sum & 255
}

def sendCommand(data) {
    // Sends commands to the device
    String stringBytes = HexUtils.byteArrayToHexString(data)

	InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( parameters ) {
    byte[] msg =  [ 0x81, 0x8A, 0x8B ]
    byte[] data = [ 0x81, 0x8A, 0x8B, calculateChecksum( msg )]

    sendCommand( data )
}

def telnetStatus(status) { log.debug "telnetStatus:${status}" }
def socketStatus(status) { 
	log.debug "socketStatus:${status}"
	if(status == "send error: Broken pipe (Write failed)") {
		// Cannot reach device
		log.debug "Cannot reach ${settings.deviceIP}, attempting to reconnect in 10s..."
		runIn( 10, initialize )
	}
	
}

def poll() {
    parent.poll(this)
}

def parse( response ) {
    // Parse data received back from this device

    def responseArray = HexUtils.hexStringToIntArray(response)
    if( responseArray.length == 4 ) {
        // Does the device say it's on?
        
        responseArray[ 2 ] == 35 ? sendEvent(name: "switch", value: "on") : sendEvent(name: "switch", value: "off")
    }
    else if( responseArray.length == 14 ) {
        // Full set received. Check received colors
        
        responseArray[ 2 ] == 35 ? ( sendEvent(name: "switch", value: "on") ) : ( sendEvent(name: "switch", value: "off") )
        
        // Convert integers to percentages
        hsvMap = rgbToHSV( responseArray[ 6 ], responseArray[ 7 ], responseArray[ 8 ] )

        // If values differ from HE, change them
        device.currentValue( "level" ).toDouble()           != ( hsvMap.value ).toDouble()          ? setLevel( hsvMap.value, false ) : null
        device.currentValue( "saturation" ).toDouble()      != ( hsvMap.saturation ).toDouble()     ? setSaturation( hsvMap.saturation, false ) : null
        device.currentValue( "hue" ).toDouble()             != ( hsvMap.hue ).toDouble()            ? setHue( hsvMap.hue, false ) : null
        
        // Calculate the color temperature, based on what data was received
    }
    else if( response == null ){
        log.debug "${settings.deviceIP}: No response received from device"
        initialize()
    }
    else{
        log.debug "${settings.deviceIP}: Received a response with an unexpected length of " + responseArray.length
    }
}

def updated(){
	initialize()
}
def initialize() {
	InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
	unschedule()
	runIn(20, keepAlive)
}

def keepAlive(){
	// Poll the device every 250 seconds, or it will lose connection.
	
	refresh()
	runIn(150, keepAlive)
}
