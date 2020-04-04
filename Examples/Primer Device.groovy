/ ** Primer Device **/ 

metadata{
    definition(
        name: "Primer device type",
        namespace: "development",
        author: "Adam Kempenich",
        importURL: "") {
        
        capability "Actuator"
        capability "Initialize"
        capability "Refresh"
        capability "Switch"
        
        command "toggle"
    }
}

def initialize(){
    log.debug "Device initialized."
     
    // Do nothing, since nothing needs to be intialized
}

def updated(){
    log.debug "Device update"
    
    initialize()   
}

def on(){
    log.debug "Device was turned on"
    
    sendEvent(name: "switch", value: "on")
}

def off(){
    log.debug "Device was turned off"
    
    sendEvent(name: "switch", value: "off")
    
}

def toggle(){
    log.debug "Toggle Called"
    
    if(device.currentValue("switch") == "on") {
        off()
    } else {
        on()
    }

}
