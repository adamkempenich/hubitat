import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
    definition (
        name: "MagicHome Universal Debug", 
        namespace: "Development", 
        author: "Adam Kempenich",
        importURL: "https://github.com/adamkempenich/hubitat/raw/master/Drivers/MagicHome/MagicHome-Universal-Debug.groovy") {
        
		capability "Switch"
		capability "Initialize"
        capability "Refresh"
    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
    }
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    log.debug( "Switch set to on" )
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    log.debug( "Switch set to off" )
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}


def parse( response ) {
    // Parse data received back from this device
	
	unschedule() // remove the initialize() call
	runIn(20, refresh) // refresh again in 20
	
	def responseArray = HexUtils.hexStringToIntArray(response)	
	log.debug "Parsed ${response}, length is ${responseArray.length}"
	switch(responseArray.length) {
		case 4:
			log.debug( "Received power-status packet of ${response}" )
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
			break;
		
		case 14:
			log.debug( "Received general-status packet of ${response}" )
		
			if( responseArray[2] == 35 ){
				sendEvent(name: "switch", value: "on")
			}
			else{
				sendEvent(name: "switch", value: "off")
			}
			break;
		
		case null:
			log.debug "No response received from device"
			initialize()
			break;
		
		default:
			log.debug "Received a response with an unexpected length of ${responseArray.length} containing ${response}"
			break;
	}
}


def sendCommand( data ) {
    // Sends commands to the device
    
	String stringBytes = HexUtils.byteArrayToHexString(data)
	log.debug "${data} was converted. Transmitting: ${stringBytes}"
	InterfaceUtils.sendSocketMessage(device, stringBytes)
	runIn(60, initialize)
}

def refresh( ) {
	unschedule() // remove the initialize() call
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
	log.debug "socketStatus: ${status}"
	log.debug "Attempting to reconnect."
	runIn(1, initialize)
}

def updated(){
    runIn(1, initialize)
}

def initialize() {
    // Establish a connection to the device
    
    log.debug "Initializing device."
	
	InterfaceUtils.socketClose(device)
	telnetClose()
	unschedule()
	try {
		log.debug "Opening Socket Connection."
		InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
		pauseExecution(1000)
		log.debug "Connection successfully established"
	    runIn(1, refresh)
	} catch(e) {
		log.debug("Error attempting to establish TCP connection to device.")
		log.debug("Next initialization attempt in 20 seconds.")
		sendEvent(name: "switch", value: "off") // If we didn't hear back, the device is likely physically powered off
		runIn(20, initialize)
	}
}

