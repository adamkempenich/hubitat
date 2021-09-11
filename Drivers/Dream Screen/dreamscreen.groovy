/**
* DreamScreen - 
* 
*	Change History:
*		0.7 (Sep 10, 2021):
*			- Added Clamp methods
*			- Restructured methods
*			- Added color capability
*			- Added CT capability
*			- Added colormode capability
*			- Added lightEffects capability
*			- Added switch capability
*			- Added switchLevel capability
*           - added inputSource
**/




import hubitat.device.HubAction
import hubitat.device.Protocol
import hubitat.helper.HexUtils
import hubitat.helper.ColorUtils



def driverVer() {
    return "0.1" // Initial build
}


static def getCRCTable(){


    final byte[] CRCTABLE = [0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15, 0x38, 0x3F, 0x36, 0x31, 0x24,
                             0x23, 0x2A, 0x2D, 0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46, 0x41, 0x54, 0x53, 
                             0x5A, 0x5D, (byte) 0xE0, (byte) 0xE7, (byte) 0xEE, (byte) 0xE9, (byte) 0xFC, (byte) 0xFB,
                             (byte) 0xF2, (byte) 0xF5, (byte) 0xD8, (byte) 0xDF, (byte) 0xD6, (byte) 0xD1, 
                             (byte) 0xC4, (byte) 0xC3, (byte) 0xCA, (byte) 0xCD, (byte) 0x90, (byte) 0x97,
                             (byte) 0x9E, (byte) 0x99, (byte) 0x8C, (byte) 0x8B, (byte) 0x82, (byte) 0x85,
                             (byte) 0xA8, (byte) 0xAF, (byte) 0xA6, (byte) 0xA1, (byte) 0xB4, (byte) 0xB3,
                             (byte) 0xBA, (byte) 0xBD, (byte) 0xC7, (byte) 0xC0, (byte) 0xC9, (byte) 0xCE,
                             (byte) 0xDB, (byte) 0xDC, (byte) 0xD5, (byte) 0xD2, (byte) 0xFF, (byte) 0xF8,
                             (byte) 0xF1, (byte) 0xF6, (byte) 0xE3, (byte) 0xE4, (byte) 0xED, (byte) 0xEA,
                             (byte) 0xB7, (byte) 0xB0, (byte) 0xB9, (byte) 0xBE, (byte) 0xAB, (byte) 0xAC,
                             (byte) 0xA5, (byte) 0xA2, (byte) 0x8F, (byte) 0x88, (byte) 0x81, (byte) 0x86,
                             (byte) 0x93, (byte) 0x94, (byte) 0x9D, (byte) 0x9A, 0x27, 0x20, 0x29, 0x2E,
                             0x3B, 0x3C, 0x35, 0x32, 0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A, 0x57,
                             0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42, 0x6F, 0x68, 0x61, 0x66, 0x73, 0x74,
                             0x7D, 0x7A, (byte) 0x89, (byte) 0x8E, (byte) 0x87, (byte) 0x80, (byte) 0x95,
                             (byte) 0x92, (byte) 0x9B, (byte) 0x9C, (byte) 0xB1, (byte) 0xB6, (byte) 0xBF,
                             (byte) 0xB8, (byte) 0xAD, (byte) 0xAA, (byte) 0xA3, (byte) 0xA4, (byte) 0xF9,
                             (byte) 0xFE, (byte) 0xF7, (byte) 0xF0, (byte) 0xE5, (byte) 0xE2, (byte) 0xEB,
                             (byte) 0xEC, (byte) 0xC1, (byte) 0xC6, (byte) 0xCF, (byte) 0xC8, (byte) 0xDD,
                             (byte) 0xDA, (byte) 0xD3, (byte) 0xD4, 0x69, 0x6E, 0x67, 0x60, 0x75, 0x72,
                             0x7B, 0x7C, 0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44, 0x19, 0x1E, 0x17,
                             0x10, 0x05, 0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34,
                             0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78, 0x7F, 0x6A,
                             0x6D, 0x64, 0x63, 0x3E, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B, 0x06, 0x01,
                             0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13, (byte) 0xAE, (byte) 0xA9, (byte) 0xA0,
                             (byte) 0xA7, (byte) 0xB2, (byte) 0xB5, (byte) 0xBC, (byte) 0xBB, (byte) 0x96,
                             (byte) 0x91, (byte) 0x98, (byte) 0x9F, (byte) 0x8A, (byte) 0x8D, (byte) 0x84,
                             (byte) 0x83, (byte) 0xDE, (byte) 0xD9, (byte) 0xD0, (byte) 0xD7, (byte) 0xC2,
                             (byte) 0xC5, (byte) 0xCC, (byte) 0xCB, (byte) 0xE6, (byte) 0xE1, (byte) 0xE8,
                             (byte) 0xEF, (byte) 0xFA, (byte) 0xFD, (byte) 0xF4, (byte) 0xF3]


    //def CRCTABLE = [
    //    0, 7, 4, 9, 28, 27, 18, 21,
    //    56, 63, 54, 49, 36, 35, 42,
    //    45, 112, 119, 126, 121, 108,
    //    107, 98, 101, 72, 79, 70, 65,
    //    84, 83, 90, 93, 224, 231, 238,
    //    233, 252, 251, 242, 245, 216,
    //    223, 214, 209, 196, 195, 202,
    //    205, 144, 151, 158, 153, 140,
    //    139, 130, 133, 168, 175, 166,
    //    161, 180, 179, 186, 189, 199,
    //    192, 201, 206, 219, 220, 213,
    //    210, 255, 248, 241, 246, 227,
    //    228, 237, 234, 183, 176, 185,
    //    190, 171, 172, 165, 162, 143,
    //    136, 129, 134, 147, 148, 157,
    //    154, 39, 32, 41, 46, 59, 60,
    //    53, 50, 31, 24, 17, 22, 3,
    //    4, 13, 10, 87, 80, 89, 94,
    //    75, 76, 69, 66, 111, 104,
    //    97, 102, 115, 116, 125, 122,
    //    137, 142, 135, 0, 149, 146,
    //    155, 156, 177, 182, 191, 184,
    //    173, 170, 163, 164, 249, 254,
    //    247, 240, 229, 226, 235, 236,
    //    193, 198, 207, 200, 221, 218,
    //    211, 212, 105, 110, 103, 96,
    //    117, 114, 123, 124, 81, 86,
    //    95, 88, 77, 74, 67, 68, 25,
    //    30, 23, 16, 5, 2, 11, 12, 33,
    //    38, 47, 40, 61, 58, 51, 52, 78,
    //    73, 64, 71, 82, 85, 92, 91, 118,
    //    113, 120, 255, 106, 109, 100,
    //    99, 62, 57, 48, 55, 34, 37,
    //    44, 43, 6, 1, 8, 15, 26,
    //    29, 20, 19, 174, 169, 160,
    //    167, 178, 181, 188, 187, 150,
    //    145, 152, 159, 138, 141, 132,
    //    131, 222, 217, 208, 215, 194,
    //    197, 204, 203, 230, 225, 232,
    //    239, 250, 253, 244, 243
    //] // crcTable is grabbed from a PDF from DreamScreen that describes UDP process



    return CRCTABLE

}

metadata {
    definition (
        name: "DreamScreen",
        namespace: "DreamScreen",
        author: "Brock Ondayko, Adam Kempenich",
        importURL: "",
        documentationLink: "",
        videoLink: ""

    ) {

        capability "Actuator"
        capability "Color Control"
        capability "Color Temperature"
        capability "Configuration"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Light"
        capability "Initialize"
        capability "Refresh"
        capability "MediaInputSource"

        capability "ColorMode"
        capability "LightEffects"

        command "setColorMode"
        attribute "Effect", "MAP"

        command "setDeviceName", ["string"]
        command "setGroupName", ["string"]
        command "setGroupNumber", ["number"]
        command "setHDMIName", ["number", "string"]

        command "setSource", [[
            name: "setSource",
            title: "Set Source",
            constraints: ["HDMI1", "HDMI2", "HDMI3"],
            type: "ENUM"]]
    }
    preferences {

        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 8888)", required: true, defaultValue: 8888

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)

        input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"enableHueInDegrees", type:"bool", title: "Enable Hue in degrees (0-360)",
              defaultValue: false, required: true, displayDuringSetup: true)

        input(name: "enableHDR", type: "bool", title: "Enable HDR on device?",
              description: "Allows you to turn on your DreamScreen to read data in HDR format", defaultValue: false, 
              required: true, displayDuringSetup: true)

        input(name:"groupAddress", type:"number", title: "Device's Group Address",
              description: "If your device is linked to a group in the DreamScreen app, this needs to match that number. Default: 0.", defaultValue: 0,
              required: true, displayDuringSetup: true)


        // indicator light auto off
        // minimum luminosity
        // music mode type
        // music mode colors
        // music mode weights
        // usb power enable
        // fade rate
        // cec passthrough
        // cec switching
        // hdp enabled
        // video frame delay
        // letterboxing enable
        // hdmi active channels
        // color boost
        //cec power enable
        // pillarboxing enable
        // sku setup
        // flex setup
        // hdr tone remapping
        // rgb saturation levels


        // Settigns that I'm not sure work

        input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
              description: "Number of failed pings before reconnecting device.", defaultValue: 3,
              required: true, displayDuringSetup: true)

        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)

        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
              description: "Interval between refreshing a device for its current value. Default: 59. Use number between 0-59", defaultValue: 59,
              required: true, displayDuringSetup: true)

    }
}

def setSource(value){
     setInputSource(value)
}
def setInputSource(inputName){
    logDebug "setInputSource: ${source}"

    byte[] data
    switch (inputName) {
        case "HDMI1" || 1:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x20, 0x00], 1))
        sendEvent(name: "mediaInputSource", value: "HDMI1")
        break

        case "HDMI2" || 2:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x20, 0x01], 1))

        sendEvent(name: "mediaInputSource", value: "HDMI2")
        break
        case "HDMI3" || 3:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x20, 0x02], 1))

        sendEvent(name: "mediaInputSource", value: "HDMI3")
        break
        
        default:
        sendEvent(name: "mediaInputSource", value: "HDMI1")
            data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x20, 0x00], 1))

    }

    sendCommand(data)
}


def on(){
    // set to last known input
    // set light mode to last known mode

    logDebug "Sending on command" 
    sendEvent(name: "switch", value: "on")

    if(device.currentValue("colorMode") == "EFFECTS"){
        log.trace "Sending FX of ${device.currentValue("Effect")[1]}"
        setEffect(device.currentValue("Effect")[1].toInteger())

    } else if(device.currentValue("colorMode") == "RGB"){
        setColor(null)
    } else if(device.currentValue("colorMode") == "CT"){
        setColorTemperature()
    }
}

def off(){
    // set mode off

    logDebug "Sending off command" 
    sendEvent(name: "switch", value: "on")
    byte[] data = [0xfc, 0x06, settings.groupAddress.toInteger(), 0x11, 0x03, 0x01, 0x00, 0x0d]
    sendCommand(data)
}

def setHue(value) {
    // Set hue of device, then fire setColor

    settings.enableHueInDegrees ? (value = clamp(value, 0, 360)) : (value = clamp(value))

    setColor(hue: value, level: device.currentValue("level"), saturation: device.currentValue("saturation"))
}

def setSaturation(value) {
    // Set saturation of device, then fire setColor

    clamp(value)

    setColor(hue: device.currentValue("hue"), level: device.currentValue("level"), saturation: value)

}

def setLevel(value, duration = 0) {
    // Set level of device, natively supported

    value = clamp(value)
    sendEvent(name: "level", value: value)

    byte[] data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x02, value.toInteger()], 1))

    sendCommand(data)
}

def setColor(colorMap){

    if(colorMap == null){
        colorMap = [hue: device.currentValue('hue'), saturation: device.currentValue('saturation'), level: device.currentValue('level')]   
    } else {
        sendEvent(name: "hue", value: colorMap.hue)
        sendEvent(name: "saturation", value: colorMap.saturation)
    }

    if(colorMap.containsKey('setFromCT')){
        setColorMode("CT")   
    } else {
        setColorMode("RGB")
    }

    def rgbColors = ColorUtils.hsvToRGB([colorMap.hue.toFloat(), colorMap.saturation.toFloat(), colorMap.level.toFloat()])

    byte[] setToAmbient = prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x01, 0x03, 0x04 ])
    byte[] setAmbientToColorMode = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x08, 0x00], 1))
    byte[] data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x05, rgbColors[0].toInteger(), rgbColors[1].toInteger(), rgbColors[2].toInteger()], 1))

    sendCommand(setToAmbient)
    sendCommand(setAmbientToColorMode)
    sendCommand(data)

}

def setColorTemperature(colorTemperature = device.currentValue('colorTemperature'), level = device.currentValue('level'), duration = 0){

    sendEvent(name: "colorTemperature", value: colorTemperature)
    sendEvent(name: "level", value: level)
    setColorMode("CT")

    def temp = colorTemperature / 100;
    def red
    def green
    def blue

    if( temp <= 66 ){ 
        red = 255 
        green = temp
        green = 99.4708025861 * Math.log(green) - 161.1195681661

        if( temp <= 19){
            blue = 0
        } else {
            blue = temp-10
            blue = 138.5177312231 * Math.log(blue) - 305.0447927307
        }
    } else {
        red = temp - 60
        red = 329.698727446 * Math.pow(red, -0.1332047592)

        green = temp - 60
        green = 288.1221695283 * Math.pow(green, -0.0755148492 )

        blue = 255
    }

    def hsvColors = ColorUtils.rgbToHSV([red/2.55, green/2.55, blue/2.55])
    setColor([hue: hsvColors[0], saturation: hsvColors[1], level: level.toInteger(), setFromCT: true])

}

def setColorMode(value) {
    // Set colormode of device, then fire setColor, setColorTemperature, or setEffect

    if(value == "RGB"){
        sendEvent(name: "colorMode", value: "RGB")
    } 
    else if(value == "CT" || value == "CCT"){
        sendEvent(name: "colorMode", value: "CT")
    }
    else if(value == "FX" || value == "EFFECTS"){
        sendEvent(name: "colorMode", value: "EFFECTS")
    }
}


def setEffect(value) {
    // Set lightEffect of device, shows us which mode the device is in
    // 0: Off, 1: Video, 2: Audio, 3: Ambient, 4+: Actual Effect no. + 4

    if(value != 2){
        setColorMode('FX')
    } 

    logDebug "setLightEffect: ${value}"

    byte[] setToAmbient = prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x01, 0x03, 0x04 ])
    byte[] setAmbientToSceneMode = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x08, 0x01], 1))
    byte[] data

    switch (value) {

        case 0: //case "Video":
        log.trace "Video"
        data = prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x01, 0x01, 0x0a])
        sendEvent(name: "Effect", value: [0, "Video"])
        break

        case 1: //case "Music":
        data = prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x01, 0x02, 0x03 ])
        sendEvent(name: "Effect", value: [1, "Music"])
        break

        case 2:
        data =  appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x00], 1))
        sendEvent(name: "Effect", value: [2, "Random Color"])
        break

        case 3:
        data =  appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x01], 1))
        sendEvent(name: "Effect", value: [3, "Fireside"])
        break

        case 4:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x02], 1))
        sendEvent(name: "Effect", value: [4, "Twinkle"])
        break 

        case 5:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x03], 1))
        sendEvent(name: "Effect", value: [5, "Ocean"])        
        break

        case 6:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x04], 1))
        sendEvent(name: "Effect", value: [6, "Rainbow"])        
        break

        case 7:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x05], 1))
        sendEvent(name: "Effect", value: [7, "July 4th"])        
        break

        case 8:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x06], 1))
        sendEvent(name: "Effect", value: [8, "Holiday"])        
        break

        case 9:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x07], 1))
        sendEvent(name: "Effect", value: [9, "Pop"])        
        break

        case 10:
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x08], 1))
        sendEvent(name: "Effect", value: [10, "Enchanted Forest"])        
        break 

        case 11: 
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x09], 1))
        sendEvent(name: "Effect", value: [11, "Orange Crawling"])        
        break

        case 12: 
        data = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x0d, 0x0a], 1))
        sendEvent(name: "Effect", value: [12, "White Crawling"])        
        break

        default:
            payload = 0
    }



    if(value >= 2){
        // Scenes require ambient mode to be set, and also ambient mode to be set to scene mode
        log.trace "sending ambient scene"
        sendCommand(setToAmbient)
        sendCommand(setAmbientToSceneMode)

    } else {


    }
    sendCommand(data)

}


def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound }
        if(value > upperBound){ value = upperBound }
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound }
        if(value > lowerBound ){ value = lowerBound }
    } else if(lowerBound == upperBound){
        value = upperBound
    }

    return value
}

def parse( response ) {
    // Parse response from device

    log.trace "Received ${response} back"
    state.noResponse = 0    

}


def refresh(value) {
    // Set hue of device, then fire setColor

    //logDebug "Number of failed responses: ${state.noResponse}"
    //state.noResponse++
    //state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data =  [0xFC, 0x05, 0xFF, 0x30, 0x01, 0x0A, 0x2A]
    //                    FC:   05:   FF: 30:   01:   0A:   2A

    sendCommand(data)
}

def updated(value) {
    // Set hue of device, then fire setColor

    initialize()
}


def installed(value) {
    // Instantiate variables

    state.noResponse = 0
    initialize()
}

def logDescriptionText(text) {
    // Set hue of device, then fire setColor
    if( settings.logDescriptionText ) { 
        log.info "DreamScreen (${settings.deviceIP}): ${text}"
    }

}

def logDebug(text) {
    // Set hue of device, then fire setColor

    if( settings.logDebug ) { 
        log.debug "DreamScreen (${settings.deviceIP}): ${text}"
    }
}

def appendCRC(arrayToAppend){

    log.trace "Array to append ${arrayToAppend}"
    byte[] sendToCalculateCRC = arrayToAppend

    def crc = calculateCRC(sendToCalculateCRC)

    log.trace "received CRC: ${crc}"
    def data = []
    for(item in arrayToAppend){
        data << item   
    }
    data << crc

    byte[] convertedData = data

    log.trace "Data: ${convertedData}"

    return convertedData
}

def calculateCRC(byte[] data){ // Adam's version
    log.trace "calculateCRC ${data}"

    byte size = (byte) (data[1] + 0x01)
    byte cntr = 0x00
    byte crc = 0x00
    def crcTable = getCRCTable()

    while(cntr < size){
        crc = crcTable[(byte) (crc ^ (data[cntr])) & 0xFF]
        byte[] crcString = crc
        cntr++
            }
    return crc
}

def sendCommand( data ) {
    // Convert and transmit data

    logDebug "sendCommand: ${data}"

    String stringBytes = HexUtils.byteArrayToHexString(data)

    def myHubAction = new hubitat.device.HubAction(stringBytes, hubitat.device.Protocol.LAN,  
                                                   [type: hubitat.device.HubAction.Type.LAN_TYPE_UDPCLIENT,  
                                                    destinationAddress: "${settings.deviceIP}:${settings.devicePort}",
                                                    destinationPort: 8888,
                                                    encoding: hubitat.device.HubAction.Encoding.HEX_STRING,
                                                    callback: "parse",
                                                    parseWarning: true])
    sendHubCommand(myHubAction)

}

def initialize() {
    // Set hue of device, then fire setColor

    device.currentValue("hue") == null ? sendEvent(name: "hue", value: 0) : null
    device.currentValue("saturation") == null ? sendEvent(name: "saturation", value: 100) : null
    device.currentValue("level") == null ? sendEvent(name: "level", value: 100) : null
    device.currentValue("switch") == null ? sendEvent(name: "switch", value: "off") : null
    device.currentValue("colorMode") == null ? sendEvent(name: "colorMode", value: "FX") : null
    device.mediaInputSource.value = ["HDMI1", "HDMI2", "HDMI3"]
    logDebug "Initializing device."


    // Initialize Settings

    byte[] hdrData
    if(settings.enableHDR){
        hdrData = prefixPacketLength([settings.groupAddress.toInteger(), 0x41, 0x03, 0x60, 0x01, 0x16])
    } else{
        hdrData = prefixPacketLength([settings.groupAddress.toInteger(), 0x41, 0x03, 0x60, 0x00, 0x11])
    }
    sendCommand(hdrData)

    //byte[] subscribeRequest = appendCRC(prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x03, 0x02, value.toInteger()], 1))
    byte[] subscribeRequest = prefixPacketLength([settings.groupAddress.toInteger(), 0x11, 0x01, 0x0C, 0x01])
    sendCommand(subscribeRequest)


    unschedule()
    //connectDevice([firstRun: true])

}

def prefixPacketLength(data, addCRCLength = 0){
    def packetLength = 0

    try{
        packetLength = data.length // array
    } catch(listError){
        packetLength = data.size // list
    }

    def arrayBuilder = [0xFC, (packetLength + addCRCLength)] // prefix checksum and initial packet data for positions [0 and 1]
    for(element in data){
        arrayBuilder << element // add remaining elements now that they've been counted
    }

    byte[] prefixAttachedByteArray = arrayBuilder 
    return prefixAttachedByteArray
}

def connectDevice(data) {
    // Set hue of device, then fire setColor

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/${clamp(settings.refreshTime, 1, 59)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }

    interfaces.rawSocket.close()

    pauseExecution(1000)

    //if( data.firstRun || ( now() - state.lastConnectionAttempt) > clamp(settings.refreshTime, 1, 60) * 500 /* Breaks infinite loops */ ) {
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




