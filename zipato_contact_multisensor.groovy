/**
 *  Zipato Z-Wave Contact Multi Sensor 0.9
 *
 *  Author: 
 *    Adam Kempenich
 *
 *  Documentation:  [Does not exist, yet]
 *  This driver is derived from Mike Maxwell's z-wave configuration tool
 *  Thank you, Mike!
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
		input "debugOutput", "bool", 
			title: "Enable debug logging?", 
			defaultValue: true, 
			displayDuringSetup: true, 
			required: false
	}
}

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
	if ( cmd.value == 0 ) {
		logDebug("Contact closed")
		sendEvent(name: "contact", value: "closed")
	}
	else if( cmd.value == 255 ) {
		logDebug("Contact opened")
		sendEvent(name: "contact", value: "open")
	}
	else{
		logDebug("Unexpected result of ${cmd.value} returned")
	}
}

def zwaveEvent(hubitat.zwave.commands.notificationv3.NotificationReport cmd){
	if( cmd.v1AlarmType == 3 ) {
		logDebug("Alarm Type is 3")
	}
	else {
		logDebug("Alarm type is ${cmd.v1AlarmType}")
	}

	if ( cmd.v1AlarmLevel == 0 ) {
		logDebug("Alarm tamper is clear")
		sendEvent(name: "tamper", value: "clear")
	}
	else if( cmd.v1AlarmLevel == 255 ) {
		logDebug("Alarm tamper is detected")
		sendEvent(name: "tamper", value: "detected")
	}
	else{
		logDebug("Unexpected result of ${cmd.v1AlarmLevel} returned")
	}
}

def zwaveEvent(hubitat.zwave.Command cmd) {
    log.debug "skip: ${cmd}"
}

//cmds
def getVersionReport(){
	return secureCmd(zwave.versionV1.versionGet())		
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
