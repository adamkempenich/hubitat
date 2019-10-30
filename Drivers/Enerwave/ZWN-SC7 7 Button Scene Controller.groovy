/**
*  Enerwave ZWN-SC7 7-Button Scene Controller 0.70
*
*    Author: 
*    Adam Kempenich 
*
*    Documentation: ***TBA***
*
*    Changelog:
*        0.70 (Oct 29, 2019)
*            - Initial release
*
*  --------- BASED OFF OF THE FOLLOWING ORIGINAL CODEBASE ---------
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*  Author: Matt Frank based on VRCS Button Controller by Brian Dahlem, based on SmartThings Button Controller *
*  Date Created: 2014-12-18                                                                                   *
*  Last Updated: 2017-05-10 update DT to support pushed as @ady624                                            *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
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
*  2016-09-30 Modified to allow for correct event value "pushed" and added numberOfButtons as per ST documentation
*
*/

 metadata {
  // Automatically generated. Make future change here.
    definition (
        name: "ZWN-SC7 Enerwave 7 Button Scene Controller", 
        namespace: "enerwave", 
        author: "Adam Kempenich",
        importUrl: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Enerwave/ZWN-SC7%207%20Button%20Scene%20Controller.groovy") {
        
        capability "Actuator"
        capability "PushableButton"
        capability "Configuration"
        capability "Indicator"
        capability "Sensor"
        
        attribute "currentButton", "STRING"
        attribute "numButtons", "STRING"
        attribute "numberOfButtons", "NUMBER"

        fingerprint deviceId: "0x0202", inClusters:"0x21, 0x2D, 0x85, 0x86, 0x72"
        fingerprint deviceId: "0x0202", inClusters:"0x2D, 0x85, 0x86, 0x72"
    }
}

// parse events into attributes
def parse(description) {
  log.debug "Parsing '${description}'"

    def result = null
  def cmd = zwave.parse(description)
  if (cmd) {
    result = zwaveEvent(cmd)
  }
  return result

}

// Handle a button being pressed
def buttonEvent(button) {
  button = button as Integer
  def result = []

    log.debug "Button event fired"

    updateState("currentButton", "$button")

    if (button > 0) {
        // update the device state, recording the button press
        //result << createEvent(name: "pushableButton", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
       
        result << createEvent(name: "pushed", value: button, isStateChange: true)
        // turn off the button LED
        result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 255, level: 255, sceneId: 0))
  }
    else {
        // update the device state, recording the button press
        result << createEvent(name: "pushed", value: button, isStateChange: true)//, descriptionText: "$device.displayName button was released", isStateChange: true)

        result << response(zwave.sceneActuatorConfV1.sceneActuatorConfReport(dimmingDuration: 255, level: 255, sceneId: 0))
    }

    result
}

// A zwave command for a button press was received
def zwaveEvent(hubitat.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {

  // The controller likes to repeat the command... ignore repeats
  if (state.lastScene == cmd.sceneId && (state.repeatCount < 4) && (now() - state.repeatStart < 2000)) {
      log.debug "Button ${cmd.sceneId} repeat ${state.repeatCount}x ${now()}"
        state.repeatCount = state.repeatCount + 1
        createEvent([:])
    }
    else {
      // If the button was really pressed, store the new scene and handle the button press
        state.lastScene = cmd.sceneId
        state.lastLevel = 0
        state.repeatCount = 0
        state.repeatStart = now()

        buttonEvent(cmd.sceneId)
    }
}

// A scene command was received -- it's probably scene 0, so treat it like a button release
def zwaveEvent(hubitat.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {

  buttonEvent(cmd.sceneId)

}

// The controller sent a scene activation report.  Log it, but it really shouldn't happen.
def zwaveEvent(hubitat.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
    log.debug "Scene activation report"
  log.debug "Scene ${cmd.sceneId} set to ${cmd.level}"

    createEvent([:])
}


// Configuration Reports are replys to configuration value requests... If we knew what configuration parameters
// to request this could be very helpful.
def zwaveEvent(hubitat.zwave.commands.associationcommandconfigurationv1.CommandConfigurationReport cmd) {
  createEvent([:])
}

// The VRC supports hail commands, but I haven't seen them.
def zwaveEvent(hubitat.zwave.commands.hailv1.Hail cmd) {
    createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}

// Update manufacturer information when it is reported
def zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
  if (state.manufacturer != cmd.manufacturerName) {
    updateDataValue("manufacturer", cmd.manufacturerName)
  }

    createEvent([:])
}

// Association Groupings Reports tell us how many groupings the device supports.  This equates to the number of
// buttons/scenes in the VRCS
def zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
  def response = []

    log.debug "${getDataByName("numButtons")} buttons stored"
  if ((getDataByName("numButtons") != "$cmd.supportedGroupings") || (getDataByName("numberOfButtons") != (int) cmd.supportedGroupings)) {
        updateState("numButtons", "$cmd.supportedGroupings")
        updateState("numberOfButtons", (int) cmd.supportedGroupings)
        log.debug "${cmd.supportedGroupings} groups available"
        response << createEvent(name: "numButtons", value: cmd.supportedGroupings, displayed: false)
        response << createEvent(name: "numberOfButtons", value: cmd.supportedGroupings, displayed: false)

        response << associateHub()
  }
    else {
      response << createEvent(name: "numButtons", value: cmd.supportedGroupings, displayed: false)
      response << createEvent(name: "numberOfButtons", value: cmd.supportedGroupings, displayed: false)
    }
    return response
}


// Handles all Z-Wave commands we don't know we are interested in
def zwaveEvent(hubitat.zwave.Command cmd) {
    createEvent([:])
}

// handle commands

// Create a list of the configuration commands to send to the device
def configurationCmds() {
  // Always check the manufacturer and the number of groupings allowed
  def commands = [
    zwave.manufacturerSpecificV1.manufacturerSpecificGet().format(),
    zwave.associationV1.associationGroupingsGet().format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:1, sceneId:1).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:2, sceneId:2).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:3, sceneId:3).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:4, sceneId:4).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:5, sceneId:5).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:6, sceneId:6).format(),
    zwave.sceneControllerConfV1.sceneControllerConfSet(dimmingDuration:0, groupId:7, sceneId:7).format()

    ]

   // commands << associateHub()
  //  log.debug "192"
//    delayBetween(commands)
    associateHub(commands)
}

// Configure the device
def configure(var=null) {
  def cmd=configurationCmds()
    log.debug("Sending configuration: ${cmd}")
    sendEvent(name:"numberOfButtons", value: 7)
    return cmd
   
}


//
// Associate the hub with the buttons on the device, so we will get status updates
def associateHub(commands) {
    //def commands = []

    // Loop through all the buttons on the controller
    for (def buttonNum = 1; buttonNum <= integer(getDataByName("numButtons")); buttonNum++) {
          pauseExecution(100)
          // Associate the hub with the button so we will get status updates
          commands << zwave.associationV1.associationSet(groupingIdentifier: buttonNum, nodeId: zwaveHubNodeId).format()

  }

    return commands
}

// Update State
// Store mode and settings
def updateState(String name, String value) {
  state[name] = value
  device.updateDataValue(name, value)
}

// Get Data By Name
// Given the name of a setting/attribute, lookup the setting's value
def getDataByName(String name) {
  state[name] ?: device.getDataValue(name)
}

//Stupid conversions

// convert a double to an integer
def integer(double v) {
  return v.toInteger()
}

// convert a hex string to integer
def integerhex(String v) {
  if (v == null) {
      return 0
    }

  return Integer.parseInt(v, 16)
}

// convert a hex string to integer
def integer(String v) {
  if (v == null) {
      return 0
    }

  return Integer.parseInt(v)
}
