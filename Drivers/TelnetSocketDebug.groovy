import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.helper.InterfaceUtils
import hubitat.device.Protocol

metadata {
    definition (
        name: "Telnet/Socket Debug", 
        namespace: "Development", 
        author: "Adam Kempenich") {
        
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
	
	log.debug "Parsed ${response}"
}


def sendCommand( data ) {
    // Sends commands to the device
    
    String stringBytes = HexUtils.byteArrayToHexString(data)
    InterfaceUtils.sendSocketMessage(device, stringBytes)
}

def refresh( ) {
    
    byte[] data =  [ 0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand( data )
}

def telnetStatus( status ) { 
	log.debug "telnetStatus" 					   
}

def socketStatus( status ) { 
    log.debug "socketStatus" 
}

def updated(){
    initialize()
}

def initialize() {
    // Establish a connection to the device
    
    log.debug "Initializing device."
    telnetClose()
    try {
        log.debug "Opening TCP-Telnet Connection."
        InterfaceUtils.socketConnect(device, settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
        
        pauseExecution(1000)
        log.debug "Connection successfully established"
    } catch(e) {
        log.debug("Error attempting to establish TCP-Telnet connection to device.")
    }
    unschedule()
}
