/**
*  Rootin' Tootin' Self-Rebootin' Hub
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*    
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

import hubitat.device.HubAction

metadata {
    definition (
        name: "Rootin' Tootin' Self-Rebootin' Hub", 
        namespace: "adamkempenich", 
        author: "Adam Kempenich") {
        
		capability "Initialize"
        command "stopIt"
    }
    
    preferences {  
		
        input(name:"isReadable", type:"bool", title: '<img alt="" src="data:image/png;base64,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" /><h1>It is yeehaw  time my dudes</h1>',
              defaultValue: false,
              required: true, displayDuringSetup: true)
    }
}

def stopIt(){
    log.info "Pullin' the reigns to stop self-healing loops."
    unschedule()
}

def connectDevice( data ){

    if(data.firstRun){
        log.info "Scheduling self-healing routine."
        unschedule() // remove any existing loops
        schedule("0/30 * * * * ? *", connectDevice, [data: [firstRun: false]])
    }
    log.info "Checking hub DB for readability..."
    
    pauseExecution(1000)
    
    try {
        if(settings.isReadable == true || settings.isReadable == false || settings.isReadable == null){
            // DB is still readable
            log.info "Hub DB is readable."
        }
    } catch(e) {
        log.error "Hub is unreachable. Rebooting now."
            httpPost([uri: "http://INSERT_IP_HERE:8080/hub/reboot"]){
    }
    }
}

def initialize() {
    // Establish a connection to the device
    log.info "Initializing self-healing routine."
    connectDevice([firstRun: true])
}

def installed(){
    initialize()
}
