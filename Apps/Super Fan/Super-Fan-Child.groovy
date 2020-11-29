/**
*   SUPER FAN
*
*    - Add Light Color Change option
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Super Fan - Child",
    namespace: "Super Fan",
    author: "Adam Kempenich",
    description: "",
    category: "Convenience",

    parent: "Super Fan:Super Fan",

    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Super%20Fan/Super-Fan-Child.groovy"
)

preferences {
    page(name: "childSetup")
}

def childSetup() {
    dynamicPage(name: "childSetup", title: "Super Fan Device Setup and Overrides", nextPage: null, install: true, uninstall: true, refreshInterval:0) {

        section() {   
            paragraph """<style>.mdl-switch__track, .mdl-switch__thumb, .mdl-switch__ripple-container{display:none;}
                .left-selected{
                    background:#3498db; 
                    border-top-left-radius:25px; 
                    border-bottom-left-radius:25px; 
                    padding: 1em; color:#ecf0f1
                }
                .left{
                    background:#ecf0f1; 
                    border-top-left-radius:25px; 
                    border-bottom-left-radius:25px; 
                    padding: 1em; 
                    color: #95a5a6;
                }
                .right-selected{
                    background:#3498db; 
                    border-top-right-radius:25px; 
                    border-bottom-right-radius:25px; 
                    padding: 1em; color:#ecf0f1;
                }
                .right{
                    background:#ecf0f1; 
                    border-top-right-radius:25px; 
                    border-bottom-right-radius:25px; 
                    padding: 1em;
                    color: #95a5a6;
                }
                .enclosure{
                    margin: 1.5em -44px !important;
                }
                .ignoreRequired + span{
                    display:none;
                }
                </style>"""
            label title: "<h2>Enter a name for this app (optional)</h2>", required: false, submitOnChange: true
        }

        section("<h2>Basic Settings</h2>", hideable: true){
            input "baseHumidity", "capability.relativeHumidityMeasurement", title: "This device represents the normal humidity in my house: <b>Currently: ${baseHumidity.currentValue('humidity') == null ? '(Select a device)' : baseHumidity.currentValue('humidity')}</b>", multiple:false, required: true, submitOnChange: true
            input "checkHumidity", "capability.relativeHumidityMeasurement", title: "This device represents the monitored room's humidity: <b>Currently: ${checkHumidity.currentValue('humidity') == null ? '(Select a device)' : checkHumidity.currentValue('humidity')}</b>", multiple:false, required: true, submitOnChange: true

            paragraph "My bathroom fan's device type is a..."

            def fanOrDimmerTrue = "<div class='ignoreRequired enclosure'><span class='left'>Fan</span><span class='right-selected'><b>Dimmer</b></span></div>"
            def fanOrDimmerFalse = "<div class='ignoreRequired enclosure'><span class='left-selected'><b>Fan</b></span><span class='right'>Dimmer</span></div>"

            input "fanOrDimmer", "bool", title: "${fanOrDimmer && fanOrDimmer != null ? fanOrDimmerTrue : fanOrDimmerFalse}", required: true, submitOnChange: true, defaultValue: false
            if(!fanOrDimmer || fanOrDimmer == null){
                input "fan", "capability.fanControl", title: "This is my bathroom fan controller", multiple: false, required: true, submitOnChange: true
            } else{
                input "fan", "capability.switchLevel", title: "This is my bathroom fan dimmer controller", multiple: false, required: true, submitOnChange: true
            }

            input "humidityMaximumValue", "number", title: "When the difference in humidity is this high or greater, my fan should run at full: <b>Current Difference: ${baseHumidity.currentValue('humidity') == null || checkHumidity.currentValue('humidity') == null ? '(Select devices)' : checkHumidity.currentValue('humidity')-baseHumidity.currentValue('humidity')}</b>", defaultValue: 50, required: true
            input "humidityMinimumValue", "number", title: "When the difference in humidity is this low or less, my fan should be off", defaultValue: 10, required: true

            input "fanFull", "number", title: "My fan's full speed should run at:", defaultValue: 100, required: true
            input "fanLow", "number", title: "This is the lowest speed my fan should ever run at", defaultValue: 50, required: true

            input "dimmingRate", "number", title: "Change the fan's speed over this many seconds: (Optional)", defaultValue: 2, required: false

            input "doNotResume", "number", title: "If I turn my fan off or on manually, don't run this app again for this many hours:", defaultValue: 2, required: true

        }

        section("Debug settings", hideable: true, hidden: true){

            input(name:"logDebug", type:"bool", title: "${logDebug ? '<div class="enclosure ignoreRequired"><input type="checkbox" class="mdl-checkbox__input" checked><span class="mdl-checkbox__label"><b style="padding-left:10px;">Log debug information</b></span></div>' : '<div class="enclosure ignoreRequired"> <input type="checkbox" class="mdl-checkbox__input"><span class="mdl-checkbox__label"><b style="padding-left:10px;">Log debug information</b></span></div>'}",
                  description: "Logs data for debugging. (Default: Off)", defaultValue: parent.logDebug,
                  required: true, displayDuringSetup: true, submitOnChange: true)
            input(name:"logDescriptionText", type:"bool", title: "${logDescriptionText ? '<div class="enclosure ignoreRequired"><input type="checkbox" class="mdl-checkbox__input" checked><span class="mdl-checkbox__label"><b style="padding-left:10px;">Log description text</b></span></div>' : '<div class="enclosure ignoreRequired"> <input type="checkbox" class="mdl-checkbox__input"><span class="mdl-checkbox__label"><b style="padding-left:10px;">Log description text</b></span></div>'}",
                  description: "Logs when useful things happen. (Default: On)", defaultValue: parent.logDescriptionText,
                  required: true, displayDuringSetup: true, submitOnChange: true)
        }
        section("<h2>Help/Information</h2>"){
            paragraph "Thanks for installing Super Fan!"    
        }

        logDebug("Parent settings: ${parent.settings}")
        logDebug("Child settings: ${settings}")

    }
}

def initialize(){

    // reset all state variables

    logDebug("Initializing with settings: ${settings}")

    unsubscribe()
    unschedule()    

    subscribe(baseHumidity, "humidity", humidityHandler)
    subscribe(checkHumidity, "humidity", humidityHandler)

    if(doNotResume != 0 && doNotResume != null){
        subscribe(fan, "switch", overridePowerHandler)
        if(fanOrDimmer){
            subscribe(fan, "switch.level", overrideLevelHandler)
        } else {
            subscribe(fan, "fanControl", overrideFanHandler)
        }


    }
    state.override = false   
    state.allowOffReset = false
    humidityHandler()
}

def overridePowerHandler( evt ){

    if(evt.value == "on" && state.lastSetPower == "off"){
        state.allowOffReset = true
        state.override = true
    }
    if(evt.value == "off" && state.allowOffReset){
        state.override = false
        state.allowOffReset = false
    }
    if(!state.override){
        if(evt.value != state.lastSetPower){

            state.override = true
            runIn(doNotResume * 3600, resetOverride)

        }
    }
}

def overrideLevelHandler( evt ){

    if(!state.override){
        if(evt.value != withinThreshold(state.lastSetLevel, evt.value, 1)){
            state.override = true
            runIn(doNotResume * 3600, resetOverride)
        }
    }
}

def overrideFanHandler( evt ){

    if(!state.override){
        if(evt.value != state.lastSetFan){
            state.override = true
            runIn(doNotResume * 3600, resetOverride)
        }
    }
}

void resetOverride(){
    state.override = false   
}

def humidityHandler( evt ){
    logDebug "Humidity reported a change."

    if(!state.override){
        def difference = checkHumidity.currentValue("humidity")-baseHumidity.currentValue("humidity")

        logDebug "Base humidity: ${baseHumidity.currentValue('humidity')}, Check Humidity: ${checkHumidity.currentValue('humidity')}. Difference ${difference}"

        if(difference > humidityMaximumValue){
            setFan(fanFull)
        } else if(difference < humidityMinimumValue){
            setFan(0)
        } else{
            def fanRate = calculatePercentDone(difference, humidityMinimumValue, humidityMaximumValue)
            log.trace "Rate: ${fanRate}"
            def speed = calculateFromPercentage(fanRate, fanLow, fanFull)
            setFan(speed)
        }
    } else{
        logDebug "Not adjusting: override is enabled"
    }
}

def setFan(speed){

    logDebug "Setting fan to ${speed}: Using ${fanOrDimmer ? 'Dimmer' : 'Fan'}"
    if(fanOrDimmer){ // Dimmer
        fan.setLevel(speed.toInteger(), dimmingRate)
        if(speed.toInteger() != 0){
            state.lastSetPower = "on"
        } 
        else{
            state.lastSetPower = "off"
        }
        state.lastSetLevel = speed.toInteger()
    } else{ // Fan
        if(speed == 0){
            fan.setSpeed("off")
            state.lastSetPower = "off"
        } else if(speed > 0 && speed < 20){
            fan.setSpeed("low")
            state.lastSetPower = "on"
            state.lastSetFanSpeed = "low"
        } else if (speed >= 20 && speed < 40){
            fan.setSpeed("medium-low")
            state.lastSetPower = "on"
            state.lastSetFanSpeed = "medium-low"
        } else if(speed >= 40 && speed < 60){
            fan.setSpeed("medium")
            state.lastSetPower = "on"
            state.lastSetFanSpeed = "medium"
        } else if(speed >= 60 && speed < 80){
            fan.setSpeed("medium-high")
            state.lastSetPower = "on"
            state.lastSetFanSpeed = "medium-high"
        } else {
            fan.setSpeed("high")
            state.lastSetPower = "on"
            state.lastSetFanSpeed = "high"
        }
    }
}

def installed() {
    logDebug "Installed"
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    logDebug "Updated"
    initialize()
}


def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound }
        if(value > upperBound){ value = upperBound }
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound }
        if(value > lowerBound ){ value = lowerBound }
    }

    return value
}

def calculateFromPercentage(percentage, lowValue, highValue){
    // Takes a range of numbers, and the percentage between them (between 0 and 1), and calculates what number the percentage is at
    lowValue = lowValue.toDouble()
    highValue = highValue.toDouble()
    percentage = percentage.toDouble()


    def value = ((highValue - lowValue) * percentage) + lowValue
    return value
}


def calculatePercentDone(currentPoint, startPoint, endPoint){

    def percentCompleted = (currentPoint - startPoint) / (endPoint - startPoint)
    return percentCompleted
}

private logDebug( text ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) {
        log.debug "(${app.name}): ${text}"
    }
}   

def withinThreshold(testValue, baseValue, range){
    // Returns a boolean if a test value is between a threshold range 
    // EG: Test if a CT is +/- 10k of the prior value set 

    testValue = testValue.toDouble()
    baseValue = baseValue.toDouble()
    range = range.toDouble()

    log.trace "withinThreshold(${testValue}, ${baseValue}, ${range})"
    def withinRange = false
    if(testValue > (baseValue - range) && testValue < (baseValue + range)){
        log.trace "within range"
        withinRange = true
    } else { 
        log.trace "not within range"
        withinRange = false 
    }

    return withinRange
}
