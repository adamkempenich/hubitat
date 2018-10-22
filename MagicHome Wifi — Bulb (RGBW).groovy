import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.device.Protocol

metadata {
    definition (name: "MagicHome Wifi — Bulb (RGBW)", namespace: "MagicHome", author: "Adam Kempenich") {
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

        command "setLevel" // 0 - 100
        command "setHue", [ "number" ]          // 0 - 100
        command "setSaturation", [ "number" ]   // 0 - 100
        command "setColor" // Hue (0-100), Saturation (0-100), Value (0-100)
        command "setColorTemperature", [ "number" ] // Kelvin ( Light Minimum Color Temperature - Light Maximum Color Temperature )
        command "setWhiteLevel", [ "number" ] // 0 - 100

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
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Device Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        
        input(name:"powerOnBrightnessChange", type:"bool", title: "Turn on this light when brightness changes?",
              description: "Makes devices behave like other switches. (Default: Off)", defaultValue: false,
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

def poll() {
    parent.poll(this)
}

def parse( response ) {
    log.debug "Device responded with " + response    
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    log.debug "MagicHome - Switch set to " + device.currentValue("switch")
    byte[] data = [ 0x71, 0x23,  0x0F, 0xA3 ]
    sendCommand( data )
}

def powerOnWithChanges(){
    // If the device is off and light settings change, turn it on (if user settings apply)
    
    settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    log.debug "MagicHome - Switch set to " + device.currentValue("switch")
    byte[] data = [ 0x71, 0x24,  0x0F, 0xA4 ]
    sendCommand( data )
}

def setHue(hue, transmit=true){
    // Set the hue of a device (0-100)

    normalizePercent(hue) 
    sendEvent(name: "hue", value: hue)
    log.debug "MagicHome - Hue set to " + device.currentValue("hue")
    
    if( !transmit ) return getHue()
    setColor(null)
}

def getHue(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "hue" ) == null ? ( setHue( 100, false ) ) : ( device.currentValue( "hue" ) )
}

def setSaturation(saturation, transmit=true){
        // Set the saturation of a device (0-100)

    normalizePercent(saturation)
    sendEvent(name: "saturation", value: saturation)    
    log.debug "MagicHome - Saturation set to " + device.currentValue("saturation")
    
    if( !transmit ) return getSaturation()
    setColor(null)
}
def getSaturation(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "saturation" ) == null ? ( setSaturation( 100, false ) ) : ( device.currentValue( "saturation" ) )
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    normalizePercent(level)
    sendEvent(name: "level", value: level)
    log.debug "MagicHome - Level set to " + level
    
    if( !transmit ) return getLevel()
    setColor(null)
}
def getLevel(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "level" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "level" ) )
}


def setWhiteLevel(whiteLevel, transmit=true){
    // Set the warm white brightness of a device (0-100)

    normalizePercent(whiteLevel) 
    setHue( 100, false )
    setLevel( whiteLevel, false )
    log.debug "MagicHome - light set to white mode with level " + device.currentValue("level")
    
    if( !transmit ) return getwhiteLevel()
    setColor(null)
}
def getWhiteLevel(){
    // Get the brightness of a device (0 - 100)

    return device.currentValue( "level" ) == null ? ( setWhiteLevel( 100, false ) ) : ( device.currentValue( "level" ) )
}

def setColor( parameters, transmit = true ){
    // Set the color of a device. Hue (0 - 100), Saturation (0 - 100), Level (0 - 100). If Hue is 16.6, use the white LEDs.

    byte[] msg
    byte[] data

    parameters.hue == null ? ( parameters.hue = getHue() ) : ( setHue( parameters.hue, false ) )
    parameters.saturation == null ? ( parameters.saturation = getSaturation() ) : ( setSaturation( parameters.saturation, false ) )
    parameters.level == null ? ( parameters.level = getLevel() ) : ( setLevel( parameters.level, false ) )

    if( parameters.hue == 100 ){
        // Update bulb's white level if the hue is set to 100

        msg =  [ 0x31, 0x00, 0x00, 0x00, device.currentValue( "level" ) * 2.55, 0x0f, 0x0f ]
        data = [ *msg, calculateChecksum( msg ) ]
    }
    else{
        // Update bulb's color

        rgbColors = hslToRGB( parameters.hue, parameters.saturation, parameters.level )

        msg =  [ 0x31, *hslToRGB( parameters.hue, parameters.saturation, parameters.level ), 0x00, 0xf0, 0x0f ]
        data = [ *msg, calculateChecksum( msg ) ]
    }

    setPreset(false)
    powerOnWithChanges()
    sendCommand( data )
}

def setColorTemperature(setTemp, transmit=true){
    // Using RGB and the WW/CW channels, adjust the color temperature of a device
    
    setTemp == null ? ( setTemp = getColorTemperature() ) : ( normalizePercent( sendEvent( name: "colorTemperature", value: setTemp ), settings.deviceWWTemperature, settings.deviceCWTemperature ) ) 
    float newSaturation = 0
    float newHue = 0
    int neutralWhite = normalizePercent( settings.neutralWhite, 2000, 8000 )

    log.info 'MagicHome - Color Temperature set to ' + setTemp

    if(setTemp >= neutralWhite){
        // Set cold white temperature if above the user's neutral white point

        cwSaturationLowPoint < cwSaturationHighPoint ? ( newSaturation = rgbColorTempCalculation( normalizePercent( settings.cwSaturationLowPoint ), normalizePercent( settings.cwSaturationHighPoint ), setTemp - neutralWhite ) ) : ( newSaturation = rgbColorTempCalculation( normalizePercent( settings.cwSaturationHighPoint ), normalizePercent( settings.cwSaturationLowPoint ), setTemp - neutralWhite ) )
        newHue =  normalizePercent( settings.cwHue )
    }
    else{
        // set warm white temperature if below the user's neutral white point

        wwSaturationLowPoint < wwSaturationHighPoint ? ( newSaturation = rgbColorTempCalculation( normalizePercent( settings.wwSaturationLowPoint ), normalizePercent( settings.wwSaturationHighPoint ), neutralWhite - setTemp ) ) : ( newSaturation = rgbColorTempCalculation( normalizePercent( settings.wwSaturationHighPoint ), normalizePercent( settings.wwSaturationLowPoint ), neutralWhite - setTemp ) )
        newHue =  normalizePercent( settings.wwHue )
    }
    
    setColor( [hue: newHue, saturation: newSaturation] )
}

def getColorTemperature(){
    // Get the color temperature of a device (~2700 - ~6000)

    return device.currentValue( "colorTemperature" ) == null ? ( setColorTemperature( 2700, false ) ) : ( device.currentValue( "colorTemperature" ))
}

def rgbColorTempCalculation( lowPoint, highPoint, offset ){
    return (((( 100 - lowPoint ) / 100 ) * ( 1.8 * Math.sqrt( offset ))) + lowPoint ) * highPoint / 100
}

def sendPreset( turnOn, preset = 1, speed = 100, transmit = true ){
    // Turn on preset mode (true), turn off preset mode (false). Preset (1 - 20), Speed (1 (slow) - 100 (fast)).

    byte[] msg
    byte[] data

    if(turnOn){
        normalizePercent( preset, 1, 20 )
        normalizePercent( speed )
        
        // Hex range of presets is (int) 37 - (int) 57. Add the preset number to get that range.
        preset += 36
        speed = (100 - speed)

        msg =  [ 0x61, preset, speed, 0x0F ]
        data = [ *msg, calculateChecksum(msg) ]

        powerOnWithChanges()

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
    
    return roundUpBetweenZeroAndOne( normalizePercent( value * getLevel( ) / 100 ) )
}

def roundUpBetweenZeroAndOne(number){
    // Rounds up a number between two points
    
    return number > 0 && number < 1 ? ( 1 ) : ( number )
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
    
    int sum = 0
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
    data = [*msg, calculateChecksum( msg )]

    sendCommand( data )
}
