/**
*	Percentage to Color Temperature (Child) 0.7
*
*    Author:
*        Adam Kempenich
*
*    Documentation:  
*        [TBA]
*
*    Changelog:
*        0.70 (Feb 24, 2020)
*            - Initial Commit
*
*/

definition(
    name:"Percent to Color Temperature - Child",
    namespace: "AdamKempenich",
    author: "Adam Kempenich",
    description: "Takes a value (0-100) from a dimmer and sets a color temperature. Useful for dashboards.",
    category: "Convenience",
	
    parent: "AdamKempenich:Percent to Color Temperature",
    
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Utilities/PercentToColorTemperature/Percent%20To%20Color%20Temperature%20(Child).groovy"
)

preferences {
    page(name: "childSetup")
}

def childSetup() {
    dynamicPage(name: "pageConfig", title: "Percent to Color Temperature - Child", nextPage: null, install: true, uninstall: true, refreshInterval:0) {
		section("Settings") {
		        label title: "<h2>Enter a name for this setup (optional)</h2>", required: false
			
			paragraph "When this dimmer changes..."
            input "dimmingDevice", "capability.switchLevel", title: "Which dimmable device?", multiple:false, required: true
			paragraph "Proportionally change the Color Temperature of this device..."
            input "colorTemperatureDevice", "capability.colorTemperature", title: "Which Color Temperature device?", multiple:false, required: true

			paragraph "When the dimmer is at 0%, the color temperature will be set to ..."
            input "warmWhiteValue", "number", title: "Warm White Temperature", required: true, defaultValue: 2700

			paragraph "When the dimmer is at 100%, the color temperature will be set to..."
            input "coldWhiteValue", "number", title: "Cold White Temperature", required: true, defaultValue: 6000
		}
		
	}
}

def initialize(){
    log.info("Initializing with settings: ${settings}")
    subscribe(dimmingDevice, "level", modeHandler)
}

def installed() {
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def modeHandler(evt){
    // Converts the level of the dimmer to the range of CT
    
    colorTemperatureDevice.setColorTemperature( warmWhiteValue + ( (( coldWhiteValue - warmWhiteValue ) / 100 ) * dimmingDevice.currentValue("level").toInteger()))
}
