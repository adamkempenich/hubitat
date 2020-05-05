/**
*   Simple Sprinklers by Zooz - Child - Hubitat
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Simple Sprinklers Setup",
    namespace: "Zooz",
    author: "Adam Kempenich",
    description: "Tap on the parent app, not this one! Water different sections of your lawn, one at a time.",
    category: "Convenience",
    
    parent: "Zooz:Simple Sprinklers",
    
    iconUrl: "https://community.hubitat.com/uploads/default/original/3X/5/f/5fb9352e4d800171699001ae71f70154ab179c4c.png",
    iconX2Url: "https://community.hubitat.com/uploads/default/original/3X/d/5/d5404eae2db65fe5b20da656805e62158f0f3558.png",
    iconX3Url: "https://community.hubitat.com/uploads/default/original/3X/9/e/9ebb8c08409adbcebbbdf7446986bf14b22f8e77.png",
)

preferences {
    page(name: "childSetup")
}

def childSetup() {
    dynamicPage(name: "childSetup", title: "Simple Sprinklers Setup", nextPage: null, install: true, uninstall: true, refreshInterval:0) {
                
        section(){
            paragraph("<h2>Tap to select schedule or trigger:</h2>")
        }
            
        section("Schedule sprinklers to run", hideable: true, hidden: true) {
                paragraph("<h2>On these days of the week...</h2>")
                input(name: "days", type: "enum", title: "Days", options: [1: "Sunday", 2: "Monday", 3: "Tuesday", 4: "Wednesday", 5: "Thursday", 6: "Friday", 7: "Saturday"], multiple: true, required: false)
                input("startTime", "time", title: "Start Time...", required: false)
        }
                 
        section("Start sprinklers when this switch turns on", hideable: true, hidden: true) {
                input "startSwitch", "capability.switch", title: "Which switch?", multiple:false, required: false
        }
            
        
        section() {   
            paragraph "<hr><h2>Select Sprinklers</h2>"
            input "sprinkler1", "capability.switch", title: "Turn on these switches...", multiple:true, required: false
            input "sprinkler1valve", "capability.valve", title: "Open these valves...", multiple:true, required: false
            
            paragraph "<h2>For this many minutes...</h2>"
            input "sprinklerDuration", "number", title: "Duration (minutes)", required: true, defaultValue: 30
            
            paragraph "<h2>Then, wait for this many minutes...</h2>"
            input "sprinklerPause", "number", title: "Duration (minutes)", required: true, defaultValue: 5
            
            
            paragraph "<h2>After waiting, repeat for these sprinkler(s)</h2>"
            input "sprinkler2", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
            input "sprinkler2valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            
            if(settings.sprinkler2 || settings.sprinkler2valve){
                input "sprinkler3", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler3valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler3 || settings.sprinkler3valve){
                input "sprinkler4", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler4valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler4 || settings.sprinkler4valve){
                input "sprinkler5", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler5valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler5 || settings.sprinkler5valve){
                input "sprinkler6", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler6valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler6 || settings.sprinkler6valve){
                input "sprinkler7", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler7valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler7 || settings.sprinkler7valve){
                input "sprinkler8", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler8valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler8 || settings.sprinkler8valve){
                input "sprinkler9", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler9valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler9 || settings.sprinkler9valve){
                input "sprinkler10", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler10valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler10 || settings.sprinkler10valve){
                input "sprinkler11", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler11valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler11 || settings.sprinkler11valve){
                input "sprinkler12", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler12valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler12 || settings.sprinkler12valve){
                input "sprinkler13", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler13valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler13 || settings.sprinkler13valve){
                input "sprinkler14", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler14valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler14 || settings.sprinkler14valve){
                input "sprinkler15", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler15valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
             if(settings.sprinkler15 || settings.sprinkler15valve){
                input "sprinkler16", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler16valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            }
            
            
            paragraph "<hr><h2>Emergency shutoff</h2>"
            input "stopSwitch", "capability.switch", title: "Disable a running automation when this switch turns on", multiple:false, required: false
            input(name:"logDebug", type:"bool", title: "Log debug information?",
                    description: "Logs data for debugging. (Default: On)", defaultValue: true,
                    required: true, displayDuringSetup: true)

            label title: "<h2>Enter a name for this setup (optional)</h2>", required: false
            
        }


    }

}

def initialize(){
    log.info("Initializing with settings: ${settings}")
    state.currentSprinkler = 0
    state.finishedRunning = false
    
    unsubscribe()
    unschedule()
    allOff()
    
    if(settings.startTime != null && settings.startTime != "" && settings.days != null && settings.days != ""){
        // Schedule-based automation
        def formattedTime = startTime.split(":")
        def hours = formattedTime[0].substring(formattedTime[0].length() - 2)
        def minutes = formattedTime[1].substring(0, 2)
        def days = "${days.toString().replaceAll("(\\s+)|(\\[)|(\\])","")}"

        schedule("0 ${minutes} ${hours} ? * ${days} *", beginSprinklerProcess)
        logDebug "scheduling schedule(0 ${minutes} ${hours} ? * ${days} *, startSprinkler)"
    }
    
    // A switch enables automation
    subscribe(startSwitch, "switch.on", startHandler)

    // A switch stops the sprinklers from running
    subscribe(stopSwitch, "switch.on", stopHandler)
}

def beginSprinklerProcess(){
    // Starts the sprinkler process from the beginning
    
    logDebug "Beginning scheduled sprinklers"
    
    allOff()
    state.currentSprinkler = 1
    state.finishedRunning = false
    startSprinkler()
}

def installed() {
    log.debug "installed"
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    logDebug "Updated"
    initialize()
}


def startHandler(evt){
    // The switch to turn on the sprinklers was enabled
    
    evt.value == "on" ? beginSprinklerProcess() : null

}

def stopHandler(evt){
    
    if(evt.value == "on"){
        state.finishedRunning = true
        endSprinkler()
    }


}

def allOff(){
    // Turns off all sprinklers
    
    logDebug "Turning off all sprinklers."
    
    for(int i = 1; i<=16; i++){
        if(this."getSprinkler${i}"().toString() != '[null, null]'){
            this."setSprinkler${i}"('off')
        } else { break }
    }
    

}

def startSprinkler(){
    // Start sprinkler
    
    logDebug "Starting sprinkler set ${state.currentSprinkler} for ${sprinklerDuration} minutes."
    
    this."setSprinkler${state.currentSprinkler}"('on')
    runIn(60 * sprinklerDuration, endSprinkler)
}

def endSprinkler(){
    // Stops a sprinkler, and schedules the next set
    
    logDebug "Ending sprinkler set ${state.currentSprinkler}. Finished running? ${state.finishedRunning}"
    
    this."setSprinkler${state.currentSprinkler}"("off")
    checkIfDone( state.currentSprinkler + 1 )
    
    if( state.currentSprinkler <= 16 && state.finishedRunning == false){
        logDebug "Starting ${state.currentSprinkler + 1} in ${sprinklerPause} minutes."
        state.currentSprinkler++
        
        sprinklerPause > 0 ?  runIn(60 * sprinklerPause, startSprinkler) : startSprinkler()
    } else {
        logDebug "<b>Today's sprinkler routine has completed.</b>"
    }
}

def checkIfDone( sprinklerSet ){
    // Checks if the sprinklerSet has any devices attached to it

    logDebug "Checking if next sprinkler set has any devices set to run..."
    
    log.trace "checking if done"
    this."getSprinkler${sprinklerSet}"().toString() == 'null' ? state.finishedRunning = true : null
}

private logDebug( text ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) {
        log.debug "Simple Sprinklers: (${app.name}): ${text}"
    }
}

// Dynamic methods to set/get state of devices //

def setSprinkler1( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler1){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler1valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler1(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler1] + [settings.sprinkler1valve]
    
    return devices
}

def setSprinkler2( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler2){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler2valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler2(){
    // Returns list of sprinklers in sprinkler2

    def devices = [settings.sprinkler2] + [settings.sprinkler2valve]
    
    return devices
}

def setSprinkler3( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler3){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler3valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler3(){
    // Returns list of sprinklers in sprinkler3

    def devices = [settings.sprinkler3] + [settings.sprinkler3valve]
    
    return devices}

def setSprinkler4( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler4){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler4valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler4(){
    // Returns list of sprinklers in sprinkler4

    def devices = [settings.sprinkler4] + [settings.sprinkler4valve]
    
    return devices
}

def setSprinkler5( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler5){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler5valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler5(){
    // Returns list of sprinklers in sprinkler5
    
    def devices = [settings.sprinkler5] + [settings.sprinkler5valve]
    
    return devices
}

def setSprinkler6( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler6){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler6valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler6(){
    // Returns list of sprinklers in sprinkler1

    def devices = [settings.sprinkler6] + [settings.sprinkler6valve]
    
    return devices
}

def setSprinkler7( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler7){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler7valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler7(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler7] + [settings.sprinkler7valve]
    
    return devices
}

def setSprinkler8( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler8){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler8valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler8(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler8] + [settings.sprinkler8valve]
    
    return devices
}

def setSprinkler9( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler9){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler9valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler9(){
    // Returns list of sprinklers in sprinkler9
    
    def devices = [settings.sprinkler9] + [settings.sprinkler9valve]
    
    return devices
}

def setSprinkler10( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler10){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler10valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler10(){
    // Returns list of sprinklers in sprinkler10
    
    def devices = [settings.sprinkler10] + [settings.sprinkler10valve]
    
    return devices
}

def setSprinkler11( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler11){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler11valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler11(){
    // Returns list of sprinklers in sprinkler setting

    def devices = [settings.sprinkler11] + [settings.sprinkler11valve]
    
    return devices
}

def setSprinkler12( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler12){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler12valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler12(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler12] + [settings.sprinkler12valve]
    
    return devices
}

def setSprinkler13( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler13){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler13valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler13(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler13] + [settings.sprinkler13valve]
    
    return devices
}

def setSprinkler14( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler14){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler14valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler14(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler14] + [settings.sprinkler14valve]
    
    return devices
}

def setSprinkler15( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler15){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler15valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler15(){
    // Returns list of sprinklers in sprinkler1
    
    def devices = [settings.sprinkler15] + [settings.sprinkler15valve]
    
    return devices
}

def setSprinkler16( switchStatus = "off" ){
 
    for(sprinkler in settings.sprinkler16){    
        switchStatus == "off" ? sprinkler.off() : sprinkler.on()
    }
    for(sprinkler in settings.sprinkler16valve){    
        switchStatus == "off" ? sprinkler.close() : sprinkler.open()
    }
}
def getSprinkler16(){
    // Returns list of sprinklers in sprinkler16

    def devices = [settings.sprinkler16] + [settings.sprinkler16valve]
    
    return devices
}
