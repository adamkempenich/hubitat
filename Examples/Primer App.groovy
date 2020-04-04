/** primer app **/

definition(
    name: "Primer app",
    namespace: "demonstration",
    author: "Adam Kempenich",
    description: "Let's make an app!",
    category: "Convenience",
    
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "" )

preferences{ 
    page( name: "appSetup")
}

def appSetup(){ 
    dynamicPage(name: "appSetup", title: "Primer App", nextPage: null, install: true, uninstall: true, refreshInterval: 0) {
        
        section("Section 1"){
            
            input "triggerSwitch", "capability.switch", title: "Which switch will trigger other lights?", multiple: false, required: true
            input "triggerContact", "capability.contactSensor", title: "Which contact sensor will trigger other lights?", multiple: false, required: true

            input "actionSwitch", "capability.switch", title: "Mirror the behavior from above to these devices.", multiple: true, required: true
            
            label title: "Enter a name for this app", required: false
            
        }
    }
}

def installed() {
    log.debug "Installed application"
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    log.debug "Updated application"
    unsubscribe()
    unschedule()
    initialize()
}

def initialize(){
    log.info("Initializing with settings: ${settings}")
    
    subscribe(settings.triggerSwitch, "switch", switchHandler)
    subscribe(settings.triggerContact, "contact", contactHandler)
}

def switchHandler( evt ){
    log.debug "Switch was turned ${evt.value}"
    
    if(evt.value == "on"){
        for(device in settings.actionSwitch){
            device.on()
        }
    } else{
        for(device in settings.actionSwitch){
            device.off()
        }
    }
}
              
def contactHandler( evt ){
    log.debug "Contact was turned ${evt.value}"
    
    if(evt.value == "open"){
        for(device in settings.actionSwitch){
            device.on()
        }
    } else{ // "closed"
        for(device in settings.actionSwitch){
            device.off()
        }
    }
}
