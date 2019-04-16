/**
 *  Zipato Z-Wave Contact Multi Sensor 0.91
 *
 *  Author: 
 *    Adam Kempenich
 *
 *  Documentation:  [Does not exist, yet]
 *    
 *
 *  Changelog:
 *	  0.91 (Mar 17 2019)
 * 		- Added child devices for external and magnetic contact
 *
 *    0.9 (Jan 24 2019)
 *      - Initial Release
 *      - Does not report temperature, yet
 *      - Does not report battery, yet
 *		- Does not report fingerprint, yet 
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
metadata {
	definition (
		name: "Zipato Z-Wave Contact Multi Sensor", 
		namespace: "zipato", 
		author: "Adam Kempenich",
		importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Zipato%20Door%20-%20Contact%20Multisensor/zipato_contact_multisensor.groovy"
	) { 
		capability "Tamper Alert"
		capability "Temperature Measurement"
		capability "Configuration"
		fingerprint deviceId: "0x4101", inClusters: "0x71,0x85,0x80,0x70,0x72,0x30,0x86,0x84,0x31", manufacturer: "015D", model: "F51C", prod: "0651", deviceJoinName: "Zipato Z-Wave Multi Sensor"
		
		command "closeExternal"
		command "openExternal"
		command "openMagnetic"
		command "closeMagnetic"
    }
	preferences {
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: true, 
			required: false
	}
}

def installed() {
        createChildDevices()
        //response(refresh() + configure())
}

def uninstalled(){
	deleteChildDevice("${device.deviceNetworkId}-external")
	deleteChildDevice("${device.deviceNetworkId}-magnetic")
}

def configure(){
	log.debug "Configuring..."
    createChildDevices()

}

private void createChildDevices() {
	// Add child devices
 
	addChildDevice("Zipato Z-Wave Contact Multi Sensor Child", "${device.deviceNetworkId}-external", [isComponent: true, label: "${device.displayName}-external"])
	addChildDevice("Zipato Z-Wave Contact Multi Sensor Child", "${device.deviceNetworkId}-magnetic", [isComponent: true, label: "${device.displayName}-magnetic"])
}

def parse(String description) {
	//log.debug description
    def cmd = zwave.parse(description,[0x85:1,0x86:1])
	log.debug "Received ${cmd}"
	if (cmd) {
        zwaveEvent(cmd)
    }
}

//Z-Wave responses
def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	// Assign child magnetic-contact

	if ( cmd.value == 0 ) {
		logDebug("Magnetic contact closed")
		closeMagnetic()
	}
	else if( cmd.value == 255 ) {
		logDebug("Magnetic contact opened")
		openMagnetic()
	}
	else{
		logDebug("Unexpected result of ${cmd.value} returned")
	}
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd){
	if( cmd.v1AlarmType == 3 ) {
		// Tamper Switch Report
		
		logDebug("Alarm Type is 3")
		if ( cmd.v1AlarmLevel == 0 ) {
			logDebug("Alarm tamper is clear")
			sendEvent(name: "tamper", value: "clear")
		}
		else if( cmd.v1AlarmLevel == 255 ) {
			logDebug("Alarm tamper is detected")
			sendEvent(name: "tamper", value: "detected")
		}
		else {
			logDebug("Alarm type is ${cmd.v1AlarmType}")
		}
	}
	else if ( cmd.v1AlarmType == 2 ) {
		// Assign child external-contact
		
		logDebug("Alarm Type is 2")

		if ( cmd.v1AlarmLevel == 0 ) {
			logDebug("External contact closed")
			closeExternal()
		}
		else if( cmd.v1AlarmLevel == 255 ) {
			logDebug("External ontact opened")
			openExternal()
		}
		else {
			logDebug("Alarm type is ${cmd.v1AlarmType}")
		}
	}
	else {
		logDebug("Unexpected result of ${cmd.v1AlarmLevel} returned")
	}
	

	
}

def openExternal(){
	def children = childDevices
	def childDevice = children.find{it.deviceNetworkId.endsWith("-external")}
	childDevice.sendEvent(name: "contact", value: "open")
}

def closeExternal(){
	def children = childDevices
	def childDevice = children.find{it.deviceNetworkId.endsWith("-external")}
	childDevice.sendEvent(name: "contact", value: "closed")
}

def openMagnetic(){
	def children = childDevices
	def childDevice = children.find{it.deviceNetworkId.endsWith("-magnetic")}
	childDevice.sendEvent(name: "contact", value: "open")
}

def closeMagnetic(){
	def children = childDevices
	def childDevice = children.find{it.deviceNetworkId.endsWith("-magnetic")}
	childDevice.sendEvent(name: "contact", value: "closed")
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip: ${cmd}"
	
	// Values ... (v1AlarmType:3, v1AlarmLevel:0, reserved:0, notificationStatus:0, notificationType:0, event:0, sequence:false, eventParametersLength:0, eventParameter:[])
	
}

//cmds
def getVersionReport(){
	return secureCmd(zwave.versionV1.versionGet())		
}

def setParameter(parameterNumber = null, size = null, value = null){
    if (parameterNumber == null || size == null || value == null) {
		log.warn "incomplete parameter list supplied..."
		log.info "syntax: setParameter(parameterNumber,size,value)"
    } else {
		return delayBetween([
	    	secureCmd(zwave.configurationV1.configurationSet(scaledConfigurationValue: value, parameterNumber: parameterNumber, size: size)),
	    	secureCmd(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber))
		],500)
    }
}

def getAssociationReport(){
	def cmds = []
	1.upto(5, {
		cmds.add(secureCmd(zwave.associationV1.associationGet(groupingIdentifier: it)))
    })
    return cmds	
}

def getParameterReport(param = null){
    def cmds = []
    if (param) {
		cmds = [secureCmd(zwave.configurationV1.configurationGet(parameterNumber: param))]
    } else {
		0.upto(255, {
	    	cmds.add(secureCmd(zwave.configurationV1.configurationGet(parameterNumber: it)))	
		})
    }
    return cmds
}	

def getCommandClassReport(){
    def cmds = []
    def ic = getDataValue("inClusters").split(",").collect{ hexStrToUnsignedInt(it) }
    ic.each {
		if (it) cmds.add(secureCmd(zwave.versionV1.versionCommandClassGet(requestedCommandClass:it)))
    }
    return delayBetween(cmds,500)
}

private handleTemperatureEvent(cmd) {
	def result = []
	//def cmdScale = cmd.scale == 1 ? "F" : "C"
	def cmdScale = "F"
	
	def val = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)	
	if ("$val".endsWith(".")) {
		val = safeToInt("${val}"[0..-2])
	}
			
	result << createEvent(createEventMap("temperature", val, null, "Temperature ${val}°${getTemperatureScale()}", getTemperatureScale()))
	return result
}

def zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	logTrace "SensorMultilevelReport: ${cmd}"
	sendLastCheckinEvent()
	
	def result = []
	if (cmd.sensorType == tempSensorType) {
		result += handleTemperatureEvent(cmd)
	}
	else {
		logDebug "Unknown Sensor Type: ${cmd}"
	} 
	return result
}

def updated() {}

private secureCmd(cmd) {
    if (getDataValue("zwaveSecurePairingComplete") == "true") {
		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
		return cmd.format()
    }	
}

private logDebug( msg ) {
	if ( settings?.debugOutput || settings?.debugOutput == null ) {
		log.debug "$msg"
	}
}
