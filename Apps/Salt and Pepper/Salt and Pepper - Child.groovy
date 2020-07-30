/**
*	Salt and Pepper Child - Hubitat
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Salt and Pepper - Child",
    namespace: "Salt and Pepper",
    author: "Adam Kempenich",
    description: "Use two lights of different color temperatures to create a range of CT.",
    category: "Lighting",
	
    parent: "Salt and Pepper:Salt and Pepper",
    
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)

preferences {
    page(name: "childSetup")
}

def childSetup() {
    dynamicPage(name: "childSetup", title: "Salt and Pepper - Child", nextPage: null, install: true, uninstall: true, refreshInterval:0) {
                
        section() {   

            paragraph("<h2>Start ...</h2>")
            input "deviceName", "string", title: "<h3>Grouped light name:</h3>", required: true, defaultValue: "Salt & Pepper"

            input "coldDevices", "capability.switchLevel", title: "Use these Cold CT Devices...", multiple:true, required: false
            input "coldCTValue", "number", title: "The cold color temperature of these lights:", required: true, defaultValue: 6000

            input "warmDevices", "capability.switchLevel", title: "Use these Warm CT Devices...", multiple:true, required: false
            input "warmCTValue", "number", title: "The warm color temperature of these lights:", required: true, defaultValue: 2700

            input(name:"logDebug", type:"bool", title: "Log debug information?",
                    description: "Logs data for debugging. (Default: On)", defaultValue: true,
                    required: true, displayDuringSetup: true)

            label title: "<h2>Enter a name for this app (optional)</h2>", required: false
            
        }


	}

}

def initialize(){
    logDebug("Initializing with settings: ${settings}")
    
    
    def currentchild = getChildDevices()?.find { it.deviceNetworkId == "${settings.deviceName}"}
    
    if (currentchild == null) {
        logDebug "Device does not exist. Creating"

        addChildDevice("Salt and Pepper", "Salt and Pepper - Virtual Device", "${settings.deviceName}", null, [label: "${settings.deviceName}"])
        pauseExecution(1000)
    }

    childDevice = getChildDevice(settings.deviceName)

    unsubscribe()
    unschedule()
    
    subscribe(childDevice, "switch", powerHandler)
    subscribe(childDevice, "colorTemperature", colorTemperatureHandler)
    subscribe(childDevice, "level", levelHandler)

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

def powerHandler( evt ){
    
    log.trace "${evt}"
    
    def childDevice = getChildDevice(settings.deviceName)
    
    if(evt.value == "on"){
        if(childDevice.currentValue("colorTemperature") != settings.warmCTValue){
            for(device in settings.warmDevices){
                device.on()
            }
        }
        if(childDevice.currentValue("colorTemperature") != settings.coldCTValue){
            for(device in settings.coldDevices){
                device.on()
            }
        }
    } else {
        for(device in settings.warmDevices){
            device.off()
        }
        for(device in settings.coldDevices){
            device.off()
        }
    }
}

def colorTemperatureHandler( evt ){
    def childDevice = getChildDevice(settings.deviceName)
    
    setDevices(childDevice.currentValue("level"), evt.value.toInteger())

}

def levelHandler( evt ){
    def childDevice = getChildDevice(settings.deviceName)
    
    setDevices(evt.value.toInteger(), childDevice.currentValue("colorTemperature").toInteger())
 
}

def setDevices( level, colorTemperature ){
    def childDevice = getChildDevice(settings.deviceName)
    
    def percentage = getPercentage(settings.warmCTValue, settings.coldCTValue, clamp(colorTemperature, settings.warmCTValue, settings.coldCTValue))

    
    for(device in settings.warmDevices){
        device.setLevel(((100 - ( percentage * 100 )) * level / 100).toInteger())
    }

    for(device in settings.coldDevices){
        device.setLevel((percentage * level).toInteger())
    }

}


def calculateFromPercentage(lowValue, highValue, percentage){
    lowValue = lowValue.toDouble()
    highValue = highValue.toDouble()
    percentage = percentage.toDouble()
    
    value = ((highValue - lowValue) * percentage) + lowValue
    return value
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

def getPercentage(lowValue, highValue, median){
    
    percentCompleted = 1 - ((highValue - median) / (highValue - lowValue))
    
    return percentCompleted
}

private logDebug( text ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) {
        log.debug "Salt and Pepper: (${app.name}): ${text}"
    }
}

