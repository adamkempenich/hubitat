/**
*    Hubitat port maintainer: Adam Kempenich
*    Initial release: Dec 1, 2019
*
*  SmartThings Device Handler: Russound Zone
*
*  Author: redloro@gmail.com
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*/
metadata {
  definition (name: "Russound Zone", namespace: "redloro-smartthings", author: "redloro@gmail.com") {

    /**
     * List our capabilties. Doing so adds predefined command(s) which
     * belong to the capability.
     */
    capability "Music Player"
    capability "Switch"
    capability "Switch Level"
    capability "Refresh"
    capability "Polling"
    capability "Sensor"
    capability "Actuator"

    /**
     * Define all commands, ie, if you have a custom action not
     * covered by a capability, you NEED to define it here or
     * the call will not be made.
     *
     * To call a capability function, just prefix it with the name
     * of the capability, for example, refresh would be "refresh.refresh"
     */
    command "source0"
    command "source1"
    command "source2"
    command "source3"
    command "source4"
    command "source5"
    command "loudnessOn"
    command "loudnessOff"
    command "bassLevel"
    command "trebleLevel"
    command "partyModeOn"
    command "partyModeOff"
    command "allOff"
    command "zone"
  }

}

/**************************************************************************
 * The following section simply maps the actions as defined in
 * the metadata into onAction() calls.
 *
 * This is preferred since some actions can be dealt with more
 * efficiently this way. Also keeps all user interaction code in
 * one place.
 *
 */
def on() { sendCommand(["state": 1], false) }
def off() { sendCommand(["state": 0], false) }
def source0() { sendCommand(["source": 0], true) }
def source1() { sendCommand(["source": 1], true) }
def source2() { sendCommand(["source": 2], true) }
def source3() { sendCommand(["source": 3], true) }
def source4() { sendCommand(["source": 4], true) }
def source5() { sendCommand(["source": 5], true) }
def setLevel(value) { sendCommand(["volume": (value/2).intValue()], true) }
def loudnessOn() { sendCommand(["loudness": 1], false) }
def loudnessOff() { sendCommand(["loudness": 0], false) }
def partyModeOn() { parent.partyMode(["state": 1, "master": getZone(), "source": getSource(), "volume": getVolume()]) }
def partyModeOff() { partyMode(["state": 0]) }
def bassLevel(value) { sendCommand(["bass": value+10], false) }
def trebleLevel(value) { sendCommand(["treble": value+10], false) }
def allOff() { sendCommand(["all": 0], false) }
def refresh() { sendCommand([], false) }
/**************************************************************************/

/**
 * Called every so often (every 5 minutes actually) to refresh the
 * tiles so the user gets the correct information.
 */
def poll() {
  refresh()
}

def zone(evt) {
  //log.debug "ZONE${getZone()} zone(${evt})"

  /*
  * Zone On/Off state (0x00 = OFF or 0x01 = ON)
  */
  if (evt.containsKey("state")) {
    //log.debug "setting state to ${result.state}"
    sendEvent(name: "switch", value: (evt.state == 1) ? "on" : "off")

    //turn off party mode
    if (evt.state == 0 || !device.currentState("partyMode")) {
      partyMode(["state": 0])
    }
  }

  /*
  * Zone Volume level (0x00 - 0x32, 0x00 = 0 ... 0x32 = 100 Displayed)
  */
  if (evt.containsKey("volume")) {
    //log.debug "setting volume to ${result.volume * 2}"
    sendEvent(name: "volume", value: evt.volume * 2)
  }

  /*
  * Zone Loudness (0x00 = OFF, 0x01 = ON )
  */
  if (evt.containsKey("loudness")) {
    //log.debug "setting loudness to ${result.loudness}"
    sendEvent(name: "loudness", value: (evt.loudness == 1) ? "on" : "off")
  }

  /*
  * Zone Bass level (0x00 = -10 ... 0x0A = Flat ... 0x14 = +10)
  */
  if (evt.containsKey("bass")) {
    //log.debug "setting bass to ${result.bass - 10}"
    sendEvent(name: "bassLevel", value: evt.bass - 10)
  }

  /*
  * Zone Treble level (0x00 = -10 ... 0x0A = Flat ... 0x14 = +10)
  */
  if (evt.containsKey("treble")) {
    //log.debug "setting treble to ${result.treble - 10}"
    sendEvent(name: "trebleLevel", value: evt.treble - 10)
  }

  /*
  * Zone Source selected (0-5)
  */
  if (evt.containsKey("source")) {
    //log.debug "setting source to ${result.source}"
    for (def i = 0; i < 6; i++) {
      if (i == evt.source) {
        state.source = i
        sendEvent(name: "source${i}", value: "on")
        sendEvent(name: "source", value: "Source ${i+1}: ${evt.sourceName}")
      }
      else {
        sendEvent(name: "source${i}", value: "off")
      }
    }
  }
}

def partyMode(evt) {
  // ["state": "", "master": "", "source": "", "volume": ""]
  //log.debug "ZONE${getZone()} partyMode(${evt})"
  if (evt.containsKey("state")) {
    sendEvent(name: "partyMode", value: (evt.state == 1) ? "on" : "off")
    if (evt.state == 1) {
      sendCommand(["state": 1], false)
    }
  } else {
    // exit if partyMode is off
    if (getPartyMode() == 0) {
      return
    }
  }

  if (evt.containsKey("volume")) {
    sendCommand(["volume": evt.volume], false)
  }

  if (evt.containsKey("source")) {
    sendCommand(["source": evt.source], false)
  }
}

private sendCommand(evt, broadcast) {
  //log.debug "ZONE${getZone()} sendCommand(${evt}, ${broadcast})"

  // send command to partyMode
  if (broadcast && getPartyMode()) {
    parent.partyMode(evt)
    return
  }

  // send command to Russound
  def part = ""
  if (evt.size() == 1) {
    part = "/${evt.keySet()[0]}/${evt.values()[0]}"
  }

  //log.debug "ZONE${getZone()} calling parent.sendCommand"
  parent.sendCommand("/plugins/rnet/controllers/${getController()}/zones/${getZone()}${part}")
}

private getTrebelLevel() {
  return device.currentState("trebleLevel").getValue().toInteger()
}

private getBassLevel() {
  return device.currentState("bassLevel").getValue().toInteger()
}

private getPartyMode() {
  return (device.currentState("partyMode").getValue() == "on") ? 1 : 0;
}

private getVolume() {
  return (device.currentState("volume").getValue().toInteger())/2
}

private getSource() {
    for (def i = 0; i < 6; i++) {
      if (device.currentState("source${i}").getValue()  == "on") {
        return i
      }
    }
}

private getController() {
  return new String(device.deviceNetworkId).tokenize('|')[1]
}

private getZone() {
  return new String(device.deviceNetworkId).tokenize('|')[2]
}
