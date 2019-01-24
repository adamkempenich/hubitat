/**
 *  Zipato Z-Wave Contact Multi Sensor 0.9
 *
 *  Author: 
 *    Adam Kempenich
 *
 *  Documentation:  [Does not exist, yet]
 *    
 *
 *  Changelog:
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
		namespace: "adamkempenich", 
		author: "Adam Kempenich"
	) { 
		capability "Contact Sensor"
		capability "Tamper Alert"
		
    }
	preferences {
		input "externalContact", "bool", 
			title: "Use external contact instead of magnetic?", 
			defaultValue: false, 
			displayDuringSetup: true, 
			required: false
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: true, 
			required: false
	}
}

//@Field Map zwLibType = [
//	0:"N/A",1:"Static Controller",2:"Controller",3:"Enhanced Slave",4:"Slave",5:"Installer",
//	6:"Routing Slave",7:"Bridge Controller",8:"Device Under Test (DUT)",9:"N/A",10:"AV Remote",11:"AV Device"
//]

def parse(String description) {
	//log.debug description
    def cmd = zwave.parse(description,[0x85:1,0x86:1])
    if (cmd) {
        zwaveEvent(cmd)
    }
}

//Z-Wave responses
def zwaveEvent(hubitat.zwave.commands.versionv1.VersionReport cmd) {
	log.info "VersionReport- zWaveLibraryType:${zwLibType.find{ it.key == cmd.zWaveLibraryType }.value}"
	log.info "VersionReport- zWaveProtocolVersion:${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
	log.info "VersionReport- applicationVersion:${cmd.applicationVersion}.${cmd.applicationSubVersion}"
}

def zwaveEvent(hubitat.zwave.commands.associationv1.AssociationReport cmd) {
    log.info "AssociationReport- groupingIdentifier:${cmd.groupingIdentifier}, maxNodesSupported:${cmd.maxNodesSupported}, nodes:${cmd.nodeId}"
}

def zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    log.info "ConfigurationReport- parameterNumber:${cmd.parameterNumber}, size:${cmd.size}, value:${cmd.scaledConfigurationValue}"
}

def zwaveEvent(hubitat.zwave.commands.versionv1.VersionCommandClassReport cmd) {
    log.info "CommandClassReport- class:${ "0x${intToHexStr(cmd.requestedCommandClass)}" }, version:${cmd.commandClassVersion}"		
}	

def zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapCmd = cmd.encapsulatedCommand()
    def result = []
    if (encapCmd) {
		result += zwaveEvent(encapCmd)
    } else {
        log.warn "Unable to extract encapsulated cmd from ${cmd}"
    }
    return result
}

def zwaveEvent(hubitat.zwave.commands.basicv1.BasicSet cmd) {
	if ( !settings?.externalContact) {
		// Only assign contact if the magnetic contact is set to be used
		
		if ( cmd.value == 0 ) {
			logDebug("Magnetic contact closed")
			sendEvent(name: "contact", value: "closed")
		}
		else if( cmd.value == 255 ) {
			logDebug("Magnetic contact opened")
			sendEvent(name: "contact", value: "open")
		}
		else{
			logDebug("Unexpected result of ${cmd.value} returned")
		}
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
		// External Contact Report
		
		logDebug("Alarm Type is 2")
		if ( settings?.externalContact || settings?.externalContact == null ) {
			// Only assign contact if external contact is desired
			
			if ( cmd.v1AlarmLevel == 0 ) {
				logDebug("External contact closed")
				sendEvent(name: "contact", value: "closed")
			}
			else if( cmd.v1AlarmLevel == 255 ) {
				logDebug("External ontact opened")
				sendEvent(name: "contact", value: "open")
			}
			else {
				logDebug("Alarm type is ${cmd.v1AlarmType}")
			}	
		}
	}
	else {
		logDebug("Unexpected result of ${cmd.v1AlarmLevel} returned")
	}
	

	
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

def installed(){}

def configure() {}

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
