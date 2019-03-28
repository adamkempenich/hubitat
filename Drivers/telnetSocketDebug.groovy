import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
    definition (
        name: "MagicHome Debug", 
        namespace: "Development", 
        author: "Adam Kempenich") {
        
		capability "Switch"
		capability "Initialize"
        capability "Refresh"
    }
    
    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577
		input "telnet", "bool", title: "Telnet - true, socket - false", description: "Use Telnet, or Socket?", required: true, defaultValue: true
    }
}

def on() {
    // Turn on the device

    sendEvent(name: "switch", value: "on")
    log.debug( "Switch set to on" )
	// data varies depending on the device. MagicHome uses 	[0x71, 0x23, 0x0F, 0xA3]
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    sendEvent(name: "switch", value: "off")
    log.debug( "Switch set to off" )
	// data varies depending on the device. MagicHome uses [0x71, 0x24, 0x0F, 0xA4]
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def parse( response ) {
    // Parse data received back from this device
	
	unschedule()
	runIn(60, refresh)
	
	def responseArray = HexUtils.hexStringToIntArray(response)	
	logDebug "Parsed ${response}, length is ${responseArray.length}"

}


def sendCommand( data ) {
    // Sends commands to the device
    
	String stringBytes = HexUtils.byteArrayToHexString(data)
        log.debug "${data} was converted. Transmitting: ${stringBytes}"
	if(settings.telnet == false || settings.telnet == null){
		InterfaceUtils.sendSocketMessage(device, stringBytes)
	}
	else{
		def transmission = new HubAction(stringBytes, Protocol.TELNET)
     	sendHubCommand(transmission)
	}
	runIn(60, initialize)
}

def refresh( ) {
    // For magichome:  0x81, 0x8A, 0x8B, 0x96 
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand( data )
	
	unschedule()
	runIn(20, refresh)
}

def telnetStatus( status ) { 
	log.debug "telnetStatus: ${status}" 
	log.debug "Attempting to reconnect."
	runIn(2, initialize)
}

def socketStatus( status ) { 
	log.debug "socketStatus: ${status}"
	log.debug "Attempting to reconnect."
	runIn(2, initialize) 
}

def updated(){
    initialize()
}

def initialize() {
    // Establish a connection to the device
    
    log.debug "Initializing device."
	
	try {
	InterfaceUtils.socketClose(device)
	}
	catch(e) {
		log.debug "No socket to close"
	}
	
	telnetClose()
	unschedule()
	try {
		if(settings.telnet == false || settings.telnet == null){
			log.debug "Opening Socket Connection."
			InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
		}
		else{
			log.debug "Opening TCP-Telnet Connection."
			telnetConnect([byteInterface: true, termChars:[170,85]], "${settings.deviceIP}", settings.devicePort.toInteger(), null, null)
		}
		
		pauseExecution(1000)
		log.debug "Connection successfully established"
	    runIn(2, refresh)
	} catch(e) {
		log.debug("Error attempting to establish TCP-Telnet connection to device.")
		log.debug("Next initialization attempt in 60 seconds.")
		sendEvent(name: "switch", value: "off")
		runIn(60, initialize)
	}
}
