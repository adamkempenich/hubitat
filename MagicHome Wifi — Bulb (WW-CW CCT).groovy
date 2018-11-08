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
    log.debug "${settings.deviceIP}: MagicHome - Switch set to on"
    byte[] data = [0x71, 0x23,  0x0F, 0xA3]
    sendCommand(data)
}

def powerOnWithChanges(){
    // If the device is off and light settings change, turn it on (if user settings apply)
    
	settings.powerOnBrightnessChange ? ( device.currentValue("status") != "on" ? on() : null ) : null
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    log.debug "${settings.deviceIP}: MagicHome - Switch set to off"
    byte[] data = [0x71, 0x24,  0x0F, 0xA4]
    sendCommand(data)
}

def setLevel(level, transmit=true) {
    // Set the brightness of a device (0-100)

    normalizePercent(level)
    sendEvent(name: "level", value: level)
    log.debug "${settings.deviceIP}: MagicHome - Level set to " + level
	
	if( !transmit ) return getLevel()
    setColorTemperature(null)
}
def getLevel(){
	// Get the brightness of a device (0 - 100)

	return device.currentValue( "level" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "level" ) )
}

def setWarmWhiteLevel(warmWhiteLevel, transmit=true){
    // Set the warm white level of a device (0-100)

    normalizePercent(warmWhiteLevel)
    sendEvent(name: "warmWhiteLevel", value: warmWhiteLevel)
    log.debug "${settings.deviceIP}: MagicHome - Warm White Level set to " + warmWhiteLevel
	
	if( !transmit ) return getWarmWhiteLevel()
    setColorTemperature(null)
}

def getWarmWhiteLevel(){
	// Get the brightness of a device (0 - 100)

	return device.currentValue( "warmWhiteLevel" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "warmWhiteLevel" ) )
}

def setColdWhiteLevel(coldWhiteLevel, transmit=true){
    // Set the cold white level of a device (0-100)

    normalizePercent(coldWhiteLevel)
    sendEvent(name: "coldWhiteLevel", value: coldWhiteLevel)
    log.debug "${settings.deviceIP}: MagicHome - Cold White Level set to " + coldWhiteLevel
    if( !transmit ) return getColdWhiteLevel()
    setColorTemperature(null)
}

def getColdWhiteLevel(){
	// Get the warm white level of a device (0 - 100)

	return device.currentValue( "coldWhiteLevel" ) == null ? ( setLevel( 100, false ) ) : ( device.currentValue( "coldWhiteLevel" ) )
}

def setColorTemperature(setTemp, transmit=true){
    // Adjust the color temperature of a device

    // Ensure device has a level, and that a temperature was passed through
    deviceLevel = roundUpBetweenZeroAndOne( normalizePercent( getLevel( ) ) )
    setTemp = normalizePercent( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature, getColorTemperature() )
	
    sendEvent(name: "colorTemperature", value: setTemp)
    log.info "${settings.deviceIP}: MagicHome - Color Temperature set to " + setTemp
    
    brightnessWW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceWWTemperature, settings.deviceCWTemperature ) )
    brightnessCW = proportionalToDeviceLevel(invertLinearValue( setTemp, settings.deviceCWTemperature, settings.deviceWWTemperature ) )

    if( brightnessWW + brightnessCW > 100 ){
        brightnessWW = brightnessWW / (( brightnessWW + brightnessCW ) / 100 )
        brightnessCW = brightnessCW / (( brightnessWW + brightnessCW ) / 100 )
    }

    setWarmWhiteLevel( brightnessWW, false )
    setColdWhiteLevel( brightnessCW, false )

    // Update Device
	if( !transmit ) return getColorTemperature()
	
    powerOnWithChanges()
    byte[] msg =  [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f ]
    byte[] data = [ 0x31, brightnessWW * 2.55, brightnessCW * 2.55, 0x00, 0x03, 0x01, 0x0f, calculateChecksum( msg ) ]
    sendCommand( data )
}

def getColorTemperature(){
	// Get the color temperature of a device (~2700 - ~6000)

	return device.currentValue( "colorTemperature" ) == null ? ( setColorTemperature( 2700, false ) ) : ( device.currentValue( "colorTemperature" ))
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

def calculateChecksum(bytes){
    // Totals an array of bytes
    
    int sum = 0
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

def socketStatus(status) { log.debug "socketStatus:${status}" }
def telnetStatus(status) { log.debug "telnetStatus:${status}" }

def poll() {
	// Poll this device 
	
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
		// Does the device say it's on?
		
		responseArray[ 2 ] == 35 ? ( sendEvent(name: "switch", value: "on") ) : ( sendEvent(name: "switch", value: "off") )
		
		// Convert integers to percentages
		double warmWhite = responseArray[ 6 ] / 2.55
		double coldWhite = responseArray[ 7 ] / 2.55
		
		// If values differ from HE, change them
		device.currentValue( "warmWhite" ) != warmWhite ? ( setWarmWhiteLevel( warmWhite, false ) ) : null
		device.currentValue( "coldWhite" ) != coldWhite ? ( setColdWhiteLevel( coldWhite, false ) ) : null
		device.currentValue( "level" ) != ( warmWhite + coldWhite ) ? ( setLevel( ( warmWhite + coldWhite ), false ) ) : null
		
		// Calculate the color temperature, based on what data was received
		setTemp = settings.deviceCWTemperature - (( settings.deviceCWTemperature - settings.deviceWWTemperature ) * ( warmWhite / 100 ))
		// If value differs from HE, change it
		setTemp != device.currentValue( "colorTemperature" ) ? ( sendEvent(name: "colorTemperature", value: setTemp.toInteger()) ) : null
	}
	else if( response == null ){
		log.debug "${settings.deviceIP}: No response received from device" 
	}
	else{
		log.debug "${settings.deviceIP}: Received a response with an unexpected length of " + responseArray.length
	}
}

def updated(){
	// If any settings were changed, re-initialize the device in HE
	
	initialize()
}
def initialize() {
	// Set up the device on boot
	
	InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
	unschedule()
	runIn(20, keepAlive)
}

def keepAlive(){
	// Poll the device every 250 seconds, or it will lose connection.
	
	refresh()
	runIn(150, keepAlive)
}
