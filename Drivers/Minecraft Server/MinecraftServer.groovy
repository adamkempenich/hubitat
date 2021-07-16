import hubitat.helper.HexUtils
import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.helper.ColorUtils

metadata {
    definition (name: "Minecraft Server", namespace: "adamkempenich", author: "AdamKempenich", importUrl: "") {
        capability "PresenceSensor"
        capability "Initialize"
        capability "Refresh"
        

        attribute "currentPlayers", "number"
        attribute "maxPlayers", "number"
        attribute "mostPlayersOnlineAtOnce", "number"
        attribute "serverName", "string"
    }

    preferences {  
        input "deviceIP", "text", title: "Device IP", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 25565)", required: true, defaultValue: 25565

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)

        input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
              description: "Number of failed pings before reconnecting device.", defaultValue: 3,
              required: true, displayDuringSetup: true)		
        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
              description: "Interval between refreshing a device for its current value. Default: 10. Use number between 0-60", defaultValue: 10,
              required: true, displayDuringSetup: true)
        // State After Power Restored
        input(name: "stateAfterPowerRestored", type: "enum", title: "Plug state after power is restored", options: [1: "Power on", 2: "Power off", 3: "Restore prior state"], defaultValue: 3, required: true)

    }


}

void parse( data ){
        log.debug "Parse: ${data}"

    if(data != ""){

        def dataBytes = HexUtils.hexStringToByteArray(data)

        // Remove All 00 pairs
        // FF 28   4F6666696369616C2052616D2052616E6368204D696E65637261667420536572766572 A7 30    A7 32 30
        // Header  Name                                                                   Current  Max

        def serverName
        def currentPlayers
        def maxPlayers

        def firstNameByte
        def firstCurrentPlayersByte
        def firstMaxPlayersByte


        def cleanOutput = [] 

        for(i=0; i < dataBytes.length; i++){
            if(dataBytes[i] != 0x00){
                cleanOutput << dataBytes[i]
               // log.trace "databytes[i] ${dataBytes[i]}"
            }
        }

        log.trace cleanOutput

        for(i = 0; i < cleanOutput.size; i++){
            if(cleanOutput[i] == 40 /*0x66*/ && firstNameByte == null){ // beginning of name location
                firstNameByte = i + 1
                log.trace "firstNameByte - ${i}"
            }
            if(cleanOutput[i] == -89 /*0xA7*/ && firstCurrentPlayersByte == null){ // beginning of currentTotalPlayers
                firstCurrentPlayersByte = i + 1
                log.trace "firstCurrentPlayersByte - ${i}"
            }
            else if(cleanOutput[i] == -89 /*0xA7*/ && firstMaxPlayersByte == null){ // beginning of maxPlayers
                firstMaxPlayersByte = i + 1
                log.trace "firstMaxPlayersByte - ${i}"
            }
        }

        // Name Buffer
        def deviceName = []
        for(i=firstNameByte; i < firstCurrentPlayersByte - 1; i++){
            // if we read an end of line, break 
            deviceName << cleanOutput[i]
        }
        def deviceNameToBytes = HexUtils.intArrayToHexString(*deviceName)
        serverName = new String(HexUtils.hexStringToByteArray(deviceNameToBytes), "UTF-8")
        log.trace "Name - ${serverName}"

        
        
        
        // Current Players Buffer
        def currentPlayersData = []
        for(i = firstCurrentPlayersByte; i < firstMaxPlayersByte - 1; i++){
            // if we read an end of line, break 
            currentPlayersData << cleanOutput[i]
        }
        def currentPlayersDataToBytes = HexUtils.intArrayToHexString(*currentPlayersData)
        currentPlayers = new String(HexUtils.hexStringToByteArray(currentPlayersDataToBytes), "UTF-8").toInteger()
        log.trace "current Players - ${currentPlayers}"

        
        
        
        // Max Players Buffer
        def maxPlayersData = []
        for(i = firstMaxPlayersByte; i <= cleanOutput.size; i++){
            // if we read an end of line, break 
            maxPlayersData << cleanOutput[i]
        }
        def maxPlayersDataToBytes = HexUtils.intArrayToHexString(*maxPlayersData)
        maxPlayers = new String(HexUtils.hexStringToByteArray(maxPlayersDataToBytes), "UTF-8").toInteger()

        log.trace "max players ${maxPlayers}"
        
        if(currentPlayers > 0){
            sendEvent(name: "presence", value: "present")   
        } else {
            sendEvent(name: "presence", value: "not present")
        }
        
        sendEvent(name: "currentPlayers", value: currentPlayers)
        sendEvent(name: "maxPlayers", value: maxPlayers)
        sendEvent(name: "serverName", value: serverName)
        
        if(currentPlayers > mostPlayersOnlineAtOnce || mostPlayersOnlineAtOnce == null){
            mostPlayersOnlineAtOnce == currentPlayers
        }
    }
}


def refresh( ) {

    logDebug "Number of failed responses: ${state.noResponse}"
    state.noResponse++
        state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    //byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    byte[] data = [0xFE]
    sendCommand(data)
}

private logDebug( text ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) { 
        log.debug "${device.name} (${settings.deviceIP}): ${text}"
    }
}

private logDescriptionText( text ){
    if( settings.logDescriptionText ) { 
        log.info "${device.name} (${settings.deviceIP}): ${text}"
    }
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

def sendCommand( data ) {
    // Sends commands to the device

    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    interfaces.rawSocket.sendMessage(stringBytes)
}

def socketStatus( status ) { 
    logDebug "socketStatus: ${status}"
}

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/${clamp(settings.refreshTime, 1, 59)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }

    interfaces.rawSocket.close()

    pauseExecution(1000)

    def tryWasGood = false
    try {
        logDebug "Opening Socket Connection."
        interfaces.rawSocket.connect(settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
        pauseExecution(1000)
        logDescriptionText "Connection successfully established"
        tryWasGood = true

    } catch(e) {
        logDebug("Error attempting to establish socket connection to device.")
        logDebug("Next initialization attempt in ${settings.refreshTime} seconds.")
        settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
        tryWasGood = false
    }

    if(tryWasGood){
        unschedule()
        logDebug "Stopping connectDevice loop. Starting refresh loop"
        schedule("0/${clamp(settings.refreshTime, 1, 59)} * * * * ? *", refresh)
        state.noResponse = 0
    }

}

def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

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

def initialize() {
    // Establish a connection to the device
    device.currentValue("switch") == null ? sendEvent(name: "switch", value: "off") : null

    logDebug "Initializing device."
    state.lastConnectionAttempt = now()
    connectDevice([firstRun: true])
}

def installed(){
    state.noResponse = 0
    state.lastConnectionAttempt = now()
}
