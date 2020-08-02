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

        section("Skip watering if this switch is on", hideable: true, hidden: true) {
                input "cancelSwitch", "capability.switch", title: "Which switch?", multiple:false, required: false
        }
        
        section() {   
            paragraph "<hr><h2>Select Sprinklers</h2>"
            input "sprinkler1", "capability.switch", title: "Turn on these switches...", multiple:true, required: false
            input "sprinkler1valve", "capability.valve", title: "Open these valves...", multiple:true, required: false
            input "sprinkler1humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
            input "sprinkler1moisureLevel", "number", title: "Maximum moisture"
            
            paragraph "<h2>For this many minutes...</h2>"
            input "sprinklerDuration", "number", title: "Duration (minutes)", required: true, defaultValue: 30
            
            paragraph "<h2>Then, wait for this many minutes...</h2>"
            input "sprinklerPause", "number", title: "Duration (minutes)", required: true, defaultValue: 5
            
            
            paragraph "<h2>After waiting, repeat for these sprinkler(s)</h2>"
            input "sprinkler2", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
            input "sprinkler2valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
            input "sprinkler2humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
            input "sprinkler2moisureLevel", "number", title: "Maximum moisture"
            
            if(settings.sprinkler2 || settings.sprinkler2valve){
                input "sprinkler3", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler3valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler3humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler3moisureLevel", "number", title: "Maximum moisture"
            }
             if(settings.sprinkler3 || settings.sprinkler3valve){
                input "sprinkler4", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler4valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler4humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler4moisureLevel", "number", title: "Maximum moisture"
            }
             if(settings.sprinkler4 || settings.sprinkler4valve){
                input "sprinkler5", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler5valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler5humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler5moisureLevel", "number", title: "Maximum moisture"
            }
             if(settings.sprinkler5 || settings.sprinkler5valve){
                input "sprinkler6", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler6valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler6humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler6moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler6 || settings.sprinkler6valve){
                input "sprinkler7", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler7valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler7humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler7moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler7 || settings.sprinkler7valve){
                input "sprinkler8", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler8valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler8humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler8moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler8 || settings.sprinkler8valve){
                input "sprinkler9", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler9valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler9humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler9moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler9 || settings.sprinkler9valve){
                input "sprinkler10", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler10valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler10humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler10moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler10 || settings.sprinkler10valve){
                input "sprinkler11", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler11valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler11humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler11moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler11 || settings.sprinkler11valve){
                input "sprinkler12", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler12valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler12humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler12moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler12 || settings.sprinkler12valve){
                input "sprinkler13", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler13valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler13humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler13moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler13 || settings.sprinkler13valve){
                input "sprinkler14", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler14valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler14humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler14moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler14 || settings.sprinkler14valve){
                input "sprinkler15", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler15valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler15humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler15moisureLevel", "number", title: "Maximum moisture"                
            }
             if(settings.sprinkler15 || settings.sprinkler15valve){
                input "sprinkler16", "capability.switch", title: "Turn on these switches...", multiple:true, required: false, submitOnChange: true
                input "sprinkler16valve", "capability.valve", title: "Open these valves...", multiple:true, required: false, submitOnChange: true
                input "sprinkler16humidity", "capability.relativeHumidityMeasurement", title: "Disable when ALL moisture sensors are above threshold", multiple: true
                input "sprinkler16moisureLevel", "number", title: "Maximum moisture"                
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
    
    if (cancelSwitch == null || cancelSwitch.currentValue("switch") == "off") {
        logDebug "Beginning scheduled sprinklers"
        
        allOff()
        state.currentSprinkler = 1
        state.finishedRunning = false
        startSprinkler()
    }
    else
        logDebug "Skipping watering because override switch is on"
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
    if (evt.value == "on")
        beginSprinklerProcess()
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
        if (settings."sprinkler${i}" != null || settings."sprinkler${i}valve" != null) {
            this.setSprinkler(i, 'off')
        } else { break }
    }
}

def startSprinkler(){
    // Start sprinkler
    def moistureSensors = this.getProperty("sprinkler${state.currentSprinkler}humidity")
    if (moistureSensors != null) {
        def moistureLevel = this.getProperty("sprinkler${state.currentSprinkler}moisureLevel")
        if (!moistureSensors.find { it.currentValue("humidity") <= moistureLevel }) {
            logDebug "Skipping watering because of moisture level above ${moistureLevel}"
            checkIfDone( state.currentSprinkler + 1 )
            if( state.currentSprinkler <= 16 && state.finishedRunning == false){
                state.currentSprinkler++
                startSprinkler()
            }
            return
        }
    }
    logDebug "Starting sprinkler set ${state.currentSprinkler} for ${sprinklerDuration} minutes."
    
    this.setSprinkler(state.currentSprinkler, 'on')
    runIn(60 * sprinklerDuration, endSprinkler)
}

def endSprinkler(){
    // Stops a sprinkler, and schedules the next set
    
    logDebug "Ending sprinkler set ${state.currentSprinkler}. Finished running? ${state.finishedRunning}"
    
    this.setSprinkler(state.currentSprinkler, 'off')
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

    if (settings."sprinkler${sprinklerSet}" == null && settings."sprinkler${sprinklerSet}valve" == null)
        state.finishedRunning = true
}

private logDebug( text ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) {
        log.debug "Simple Sprinklers: (${app.name}): ${text}"
    }
}

def setSprinkler(idx, switchStatus = "off") {
    if (settings?."sprinkler${idx}" != null) {
        if (switchStatus == "off")
            this.getProperty("sprinkler${idx}")*.off()
        else
            this.getProperty("sprinkler${idx}")*.on()
    }
    if (settings?."sprinkler${idx}valve" != null) {
        if (switchStatus == "off")
            this.getProperty("sprinkler${idx}")*.close()
        else
            this.getProperty("sprinkler${idx}")*.open()
    }
}