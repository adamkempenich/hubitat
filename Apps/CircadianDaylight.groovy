/**
*	Hubitat Circadian Daylight 0.80
*
*	Author:
*		Adam Kempenich
*
*	Documentation:  https://community.hubitat.com/t/release-app-circadian-daylight-port/
*
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*                                                                             *
*	Forked from:                                                              *
*  		SmartThings Circadian Daylight v. 2.6                                 *
*		https://github.com/KristopherKubicki/smartapp-circadian-daylight/     *
*                                                                             *
* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*
*  Changelog:
*	0.81 (October 1 2019)
*		- Fixed default color temp outside of sunrise/set times when CT overridden
*	0.80 (May 13 2019)
*		- Added brightness-per-mode
*		- Added Min/Max brightness
*		- Fixed zip-code
*		- Added disable brightness changes when manually pressed
*		- Added disable when switch off option
*		- Added LogDebug
*		- Added DescriptionText
*		- Updated settings language
*		- Removed 'sleep modes' since they were confusing. Functionality is still there with manual overrides
*		- Began reworking settings
*		- Added custom fade-in/fade-out periods, thanks to Jeff Byrom (@talz13)
*
*	0.72 (Apr 01 2019)
*		- Added fix for sunset offset issues
*		- Added zip code override
*
*	0.71 (Mar 29 2019)
*		- Added fix for modes and switches not overriding
*
*	0.70 (Mar 28 2019)
*		- Initial (official) release
*
* 	To-Do:
*		- Add number verification
*		- Clean Up Code
*		- Use Child Apps
*		- Disable disableBrightnessWhenManuallyChanged with switch on/off
*		- Allow for CT to keep processing when disableBrightnessWhenManuallyChanged on/off
*		- Match light to luminosity (or average) from sensor
*		- Begin/End CT for sunrise/sunset
*		- Add switch to reset disableWhenManuallyChanged
*		- Add CRON adjustment
*		- Adding minimum/maximum sunrise/sunset times
*/

definition(
        name: "Circadian Daylight",
        namespace: "circadianDaylight",
        author: "Adam Kempenich",
        importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/CircadianDaylight.groovy",
        description: "Sync your color temperature, color changing, and dimmable lights to natural daylight hues to improve your cognitive functions and restfulness.",
        category: "Green Living",
        iconUrl: "",
        iconX2Url: ""
)

preferences {
    page name: "mainPage", install: true, uninstall: true
    page name: "sunsetSunriseOptions"
    page name: "colorTemperatureOverrides"
    page name: "advancedBrightnessOptions"
    page name: "disableOptions"

}

def mainPage(){
    dynamicPage(name: "mainPage") {

        section("Credits:", hideable: true, hidden: true) {
            paragraph "<b>Hubitat Port Maintainer:</b>"
            paragraph "- Adam Kempenich (@AdamKempenich)."

            paragraph "<b>Original SmartThings Project:</b>"
            paragraph "- ClaytonJN and Kristopher Kubicki."
            paragraph "https://github.com/KristopherKubicki/smartapp-circadian-daylight/"
			
			paragraph "<b>Custom fade-in/fade-out time:</b>"
            paragraph "- Jeff Byrom (@talz13)."
        }

		section("<h2>App Name</h2>"){
			label title: "Enter a name for this app (optional)", required: false
		}
		
        section("Thanks for installing Circadian Daylight! This application dims and adjusts the color temperature of your lights to match the state of the sun, which has been proven to aid in cognitive functions and restfulness. The default options are well suited for most users, but feel free to tweak accordingly!") {}
        section("Control these devices: (Only check each device once)") {
            input "colorTemperatureDevices", "capability.colorTemperature", title: "Which Color Temperature capable devices?", multiple:true, required: false
            input "colorDevices", "capability.colorControl", title: "Which Color-Changing devices?", multiple:true, required: false
            input "dimmableDevices", "capability.switchLevel", title: "Which dimmable devices?", multiple:true, required: false
        }

        section("<h2>Enable Dynamic Brightness?</h2>") {
            input "dynamicBrightness","bool", title: "Dims/brightens your lights based off of sunset.", required: false
        }

        section("<h2>Advanced Preferences:</h2>") {
        }

        section(){
            href(name: "toSunsetSunriseOptions",
                    title: "<b>Sunset/Sunrise Options</b>",
                    page: "sunsetSunriseOptions",
                    description: "Set Advanced Sunset/Sunrise Options"
            )
        }

        section(){
            href(name: "toColorTemperatureOverrides",
                    title: "<b>Color Temperature Options</b>",
                    page: "colorTemperatureOverrides",
                    description: "Set Advanced Sunset/Sunrise Options"
            )
        }

        section(){
            href(name: "toAdvancedBrightnessOptions",
                    title: "<b>Advanced Brightness Options</b>",
                    page: "advancedBrightnessOptions",
                    description: "Set Advanced Brightness and mode Options"
            )
        }

        section(){
            href(name: "toDisableOptions",
                    title: "<b>Disable Circadian Daylight Options</b>",
                    page: "disableOptions",
                    description: "Disable when..."
            )
        }
	
        section("Debugging:") {
            input(name:"logDescriptionText", type:"bool", title: "Log description text?",
                    description: "Logs useful information when things change. (Default: On)", defaultValue: true,
                    required: true, displayDuringSetup: true)
            input(name:"logDebug", type:"bool", title: "Log debug information?",
                    description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
                    required: true, displayDuringSetup: true)
        }
    }



}

def sunsetSunriseOptions(){
    dynamicPage(name: "sunsetSunriseOptions") {
        section("Sunset/Sunrise Options") {
            input "useSunOverrides", "bool", title: "Use Sunset/Sunrise Overrides?"
            input "sunriseOverride", "time", title: "Sunrise Override", required: false, hideWhenEmpty: "useSunOverrides"
            input "sunsetOverride", "time", title: "Sunset Override", required: false, hideWhenEmpty: "useSunOverrides"

            input "useSunOffsets", "bool", title: "Use Sunset/Sunrise Offsets (+/-)?"
            input "sunriseOffset", "decimal", title: "Sunrise Offset (+/-)", required: false, hideWhenEmpty: "useSunOffsets"
            input "sunsetOffset", "decimal", title: "Sunset Offset (+/-)", required: false, hideWhenEmpty: "useSunOffsets"
			
            // input "zipCodeOverride", "number", title: "Zip Code Override", required: false
        }

    }
}

def colorTemperatureOverrides(){
    dynamicPage(name: "colorTemperatureOverrides") {
        section("Color Temperature Overrides"){
            input "useCTOverrides", "bool", title: "Use Color Temperature Overrides?"
            input "coldCTOverride", "number", title: "Cold White Temperature", required: false, hideWhenEmpty: "useCTOverrides"
            input "warmCTOverride", "number", title: "Warm White Temperature", required: false, hideWhenEmpty: "useCTOverrides"
        }
    }
}

def advancedBrightnessOptions(){
    dynamicPage(name: "advancedBrightnessOptions") {
        section("<h2>Brightness Overrides</h2>") {

            input "useBrightnessOverrides", "bool", title: "Use Brightness Overrides?"

            input "maxBrightnessOverride","number", title: "Max Brightness Override", required: false, hideWhenEmpty: "useBrightnessOverrides"
            input "minBrightnessOverride","number", title: "Min Brightness Override", required: false, hideWhenEmpty: "useBrightnessOverrides"
        }
		
		section("<h2>Manual brighten/dim periods</h2><br><p>You must have dynamic brightness enabled for this to work</p>") { // Thanks, Jeff Byrom (@talz13)!
			input "brightenTimeStart", "time", title: "Start Brightening At", required: false
			input "brightenTimeEnd", "time", title: "End Brightening At", required: false

			input "dimTimeStart", "time", title: "Start Dimming At", required: false
			input "dimTimeEnd", "time", title: "End Dimming At", required: false
		}

        section() {
            section("<h2>Brightness Per Mode Options</h2>") {}

            section("<h3><b>Override 1</b></h3>") {
                input "mode1Override", "mode", title: "When this mode is enabled...", multiple: false, required: false
                input "mode1OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode1Override"
                input "mode1OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode1Override"
            }
            section("<h3><b>Override 2</b></h3>") {
                input "mode2Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode2OverrideValue"
                input "mode2OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode2Override"
                input "mode2OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode2Override"
            }
            section("<h3><b>Override 3</b></h3>") {
                input "mode3Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode2Override"
                input "mode3OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode3Override"
                input "mode3OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode3Override"
            }
            section("<h3><b>Override 4</b></h3>") {
                input "mode4Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode3OverrideValue"
                input "mode4OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode4Override"
                input "mode4OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode4Override"
            }
            section("<h3><b>Override 5</b></h3>") {
                input "mode5Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode4OverrideValue"
                input "mode5OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode5Override"
                input "mode5OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode5Override"
            }
            section("<h3><b>Override 6</b></h3>") {
                input "mode6Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode5OverrideValue"
                input "mode6OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode6Override"
                input "mode6OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode6Override"
            }
            section("<h3><b>Override 7</b></h3>") {
                input "mode7Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode6OverrideValue"
                input "mode7OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode7Override"
                input "mode7OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode7Override"
            }
            section("<h3><b>Override 8</b></h3>") {
                input "mode8Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode8OverrideValue"
                input "mode8OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode8Override"
                input "mode8OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode8Override"
            }
            section("<h3><b>Override 9</b></h3>") {
                input "mode9Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode9OverrideValue"
                input "mode9OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode9Override"
                input "mode9OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode9Override"
            }
            section("<h3><b>Override 10</b></h3>") {
                input "mode10Override", "mode", title: "When this mode is enabled...", multiple: false, required: false, hideWhenEmpty: "mode9Override"
                input "mode10OverrideValue","number", title: "Set brightness to...", required: false, hideWhenEmpty: "mode10Override"
                input "mode10OverrideColorTemperature","number", title: "Set color temperature to...", required: false, hideWhenEmpty: "mode10Override"
            }
        }
    }
}

def disableOptions(){
    dynamicPage(name: "disableOptions") {
        section("<h2>Disable When...</h2>") {
            input "disableWhenDimmed", "bool", title: "Disable CD when a selected device is dimmed?"
            input "reenableDimmingTime", "time", title: "Re-enable CD from dimmer override at this time each day", hideWhenEmpty: "disableWhenDimmed"
        }

        section("Disable Circadian Daylight when the following switches are on:") {
            input "disablingSwitches","capability.switch", title: "Switches", multiple:true, required: false
            input "disableWhenSwitchOff","bool", title: "Disable when off (normally disables when switch is on)", required: false
        }
    }
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

private def initialize() {
    logDebug("initialize() with settings: ${settings}")
    if(colorTemperatureDevices) {
        subscribe(colorTemperatureDevices, "switch.on", modeHandler)
        subscribe(colorTemperatureDevices, "level", modeHandler)
    }
    if(colorDevices) {
        subscribe(colorDevices, "switch.on", modeHandler)
        subscribe(colorDevices, "level", modeHandler)
    }
    if(dimmableDevices) {
        subscribe(dimmableDevices, "switch.on", modeHandler)
        subscribe(dimmableDevices, "level", modeHandler)
    }
    if(disablingSwitches) { subscribe(disablingSwitches, "switch.off", modeHandler) }
    subscribe(location, "mode", modeHandler)

    state.disabledFromDimmer = false
    state.lastAssignedBrightness = 100
    state.justInitialized = true

    if(settings.reenableDimmingTime != null && settings.reenableDimmingTime != ""){

        def scheduleEnable = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.reenableDimmingTime)
        schedule(scheduleEnable, disableDimmerOverride)
    }

    // revamped for sunset handling instead of motion events
    subscribe(location, "sunset", modeHandler)
    subscribe(location, "sunrise", modeHandler)
    schedule("0 */5 * * * ?", modeHandler)
    subscribe(app,modeHandler)
    subscribe(location, "sunsetTime", scheduleTurnOn)
    // rather than schedule a cron entry, fire a status update a little bit in the future recursively
    scheduleTurnOn()
}

private def getSunriseTime(){
    def sunRiseSet
    def sunriseTime

    if(settings.zipCodeOverride == null || settings.zipCodeOverride == ""){
        sunRiseSet = getSunriseAndSunset()
        logDebug "getSunriseTime - System Sunrise time: ${sunRiseSet}"
    }
    else{
	sunRiseSet = getSunriseAndSunset(zipCode: "${settings.zipCodeOverride}")
        logDebug "getSunrisetTime - Zipcode (${settings.zipCodeOverride}). Sunrise time: ${sunRiseSet}"
    }
	
    if(settings.sunriseOverride != null && settings.sunriseOverride != ""){
        sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunriseOverride)
        logDebug "Sunrise overridden to ${sunriseTime}"
    }
    else if(settings.sunriseOffset != null && settings.sunriseOffset != ""){
        sunriseTime = sunRiseSet.sunrise.plusMinutes(settings.sunriseOffset)
        logDebug "Sunrise offset to ${sunriseTime}"
    }
    else{
        sunriseTime = sunRiseSet.sunrise
    }
    return sunriseTime
}

private def getSunsetTime(){
    def sunRiseSet
    def sunsetTime

    if(settings.zipCodeOverride == null || settings.zipCodeOverride == ""){
        sunRiseSet = getSunriseAndSunset()
        logDebug "getSunsetTime - System Sunset time: ${sunRiseSet}"
    }
    else{
	sunRiseSet = getSunriseAndSunset(zipCode: "${settings.zipCodeOverride}")
        logDebug "getSunsetTime - Zipcode (${settings.zipCodeOverride}). Sunset time: ${sunRiseSet}"
    }
	
    if(settings.sunsetOverride != null && settings.sunsetOverride != ""){
        sunsetTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunsetOverride)
        logDebug "Sunset overridden to ${sunsetTime}"
    }
    else if(settings.sunsetOffset != null && settings.sunsetOffset != ""){
        sunsetTime = sunRiseSet.sunset.plusMinutes(settings.sunsetOffset)
        logDebug "Sunset offset to ${sunsetTime}"
    }
    else{
        sunsetTime = sunRiseSet.sunset
    }
    return sunsetTime
}

def scheduleTurnOn() {
    def int iterRate = 20

    // get sunrise and sunset times
    def sunriseTime = getSunriseTime()
    logDebug("sunrise time ${sunriseTime}")

    def sunsetTime = getSunsetTime()
    logDebug("sunset time ${sunsetTime}")

    if(sunriseTime > sunsetTime) {
        sunriseTime = new Date(sunriseTime - (24 * 60 * 60 * 1000))
    }

    def runTime = new Date(now() + 60*15*1000)
    for (def i = 0; i < iterRate; i++) {
        def long uts = sunriseTime.time + (i * ((sunsetTime.time - sunriseTime.time) / iterRate))
        def timeBeforeSunset = new Date(uts)
        if(timeBeforeSunset.time > now()) {
            runTime = timeBeforeSunset
            last
        }
    }

    logDebug "checking... ${runTime.time} : $runTime. state.nextTime is ${state.nextTime}"
    if(state.nextTime != runTime.time) {
        state.nextTime = runTime.time
        logDescriptionText "Scheduling next step at: $runTime (sunset is $sunsetTime) :: ${state.nextTime}"
        runOnce(runTime, modeHandler)
    }
}


// Poll all devices, and modify the ones that differ from the expected state
def modeHandler(evt) {
	logDebug "modeHandler called"
	
    for (disableSwitch in disablingSwitches) {
        if(!settings.disableWhenSwitchOff){
            if(disableSwitch.currentSwitch == "on") {
				logDebug "Disabled from switch. Ignoring any brightness updates until reset."
                return
            }
        }
        else if(disableSwitch.currentSwitch == "off") {
            return
        }
    }

    if(!state.justInitialized){
		for(device in colorTemperatureDevices) {
			if(settings.disableWhenDimmed && device.currentValue("level") != state.lastAssignedBrightness){
				state.disabledFromDimmer = true
				logDescriptionText "Color temperature has changed outside of CD. Disabling until reset time runs"
			}
		}
		for(device in colorDevices) {
			if(settings.disableWhenDimmed && device.currentValue("level") != state.lastAssignedBrightness){
				state.disabledFromDimmer = true
				logDescriptionText "Color temperature has changed outside of CD. Disabling until reset time runs"
			}
		}
		for(device in dimmableDevices) {
			if(settings.disableWhenDimmed && device.currentValue("level") != state.lastAssignedBrightness){
				state.disabledFromDimmer = true
				logDescriptionText "Color temperature has changed outside of CD. Disabling until reset time runs"
			}
		}
    }
    else{
        state.justInitialized = false
    }
	
    def ct = getCT()
    def hex = getHex()
    def hsv = getHSV()
    def bright = getBright()

    if(!state.disabledFromDimmer){
        for(colorTemperatureDevice in colorTemperatureDevices) {
            if(colorTemperatureDevice.currentValue("switch") == "on") {
                if(colorTemperatureDevice.currentValue("colorTemperature") != ct) {
                    colorTemperatureDevice.setColorTemperature(ct)
                }
                if((checkCurrentMode() || settings.dynamicBrightness == true || settings.useBrightnessOverrides || settings.useBrightnessPerMode) && colorTemperatureDevice.currentValue("level") != bright) {
                    colorTemperatureDevice.setLevel(bright)
                }
            }
        }

        def color = [hex: hex, hue: hsv.h, saturation: hsv.s, level: bright]
        for(colorDevice in colorDevices) {
            if(colorDevice.currentValue("switch") == "on") {
                def tmp = colorDevice.currentValue("color")
                if(colorDevice.currentValue("color") != hex) {
                    if(checkCurrentMode() || settings.dynamicBrightness == true) {
                        color.value = bright
                    } else {
                        color.value = colorDevice.currentValue("level")
                    }
                    def ret = colorDevice.setColor(color)
                }
            }
        }
        for(dimmableDevice in dimmableDevices) {
            if(dimmableDevice.currentValue("switch") == "on") {
                if(dimmableDevice.currentValue("level") != bright) {
                    dimmableDevice.setLevel(bright)
                }
            }
        }
    }
    else{
        return
    }

    scheduleTurnOn()
}

def checkCurrentMode(){

    switch(location.mode) {
        case mode1Override:
            return true
            break
        case mode2Override:
            return true
            break
        case mode3Override:
            return true
            break
        case mode4Override:
            return true
            break
        case mode5Override:
            return true
            break
        case mode6Override:
            return true
            break
        case mode7Override:
            return true
            break
        case mode8Override:
            return true
            break
        case mode9Override:
            return true
            break
        case mode10Override:
            return true
            break
        default:
            return false
            break
    }

}

def getCTBright() {
		
    def brightenStart = settings.brightenTimeStart == null || settings.brightenTimeStart == "" ? null : Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.brightenTimeStart)
    def brightenEnd = settings.brightenTimeEnd == null || settings.brightenTimeEnd == "" ? null : Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.brightenTimeEnd)
    def dimStart = settings.dimTimeStart == null || settings.dimTimeStart == "" ? null : Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.dimTimeStart)
    def dimEnd = settings.dimTimeEnd == null || settings.dimTimeEnd == "" ? null : Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.dimTimeEnd)

	
    def sunriseTime = getSunriseTime()
    def sunsetTime = getSunsetTime()
	

    def midDay = sunriseTime.time + ((sunsetTime.time - sunriseTime.time) / 2)

    def currentTime = now()
    def float brightness = 1

    def int colorTemp = settings.warmCTOverride == null || settings.warmCTOverride == "" ? 2700 : settings.warmCTOverride
    def int coldCT = settings.coldCTOverride == null || settings.coldCTOverride == "" ? 6500 : settings.coldCTOverride
    def int warmCT = settings.warmCTOverride == null || settings.warmCTOverride == "" ? 2700 : settings.warmCTOverride
    def int midCT = coldCT - warmCT

    def highBrightness = 100
    def lowBrightness = 1
    def fullRange = highBrightness - lowBrightness

	if( settings.maxBrightnessOverride < 100 && settings.maxBrightnessOverride != null ){
		highBrightness = settings.maxBrightnessOverride
	}
	if( settings.minBrightnessOverride > 0 && settings.minBrightnessOverride != null){
		lowBrightness = settings.minBrightnessOverride
	}

	
    if( ( currentTime > sunriseTime.time && currentTime < sunsetTime.time ) || ( brightenStart != null && dimEnd != null && ( currentTime > brightenStart && currentTime < dimEnd ) ) ) { // time is between sunrise and sunset

        if(currentTime < midDay) {
            colorTemp = warmCT + ((currentTime - sunriseTime.time) / (midDay - sunriseTime.time) * midCT)

			// check if time is between brightenStart/end
			// if it is, calculate percent
			// if it isn't, set brightness to max
			
			if( brightenEnd != null && currentTime > brightenEnd.time ) {
				brightness == highBrightness
			}
			else if( brightenStart != null && currentTime < brightenStart.time ) {
				brightness = lowBrightness	
			}
			else if( brightenStart != null && brightenEnd != null && ( currentTime > brightenStart.time && currentTime < brightenEnd.time ) ) {
				brightnessPercentage = ( ( currentTime - brightenStart.time ) / ( brightenEnd.time - brightenStart.time))
				brightness = ( brightnessPercentage * fullRange + lowBrightness )/ 100
			}
			else{
				brightnessPercentage = ((currentTime - sunriseTime.time) / (midDay - sunriseTime.time))
				brightness = ( brightnessPercentage * fullRange + lowBrightness )/ 100
			}
        }
        else {
            colorTemp = coldCT - ((currentTime - midDay) / (sunsetTime.time - midDay) * midCT)

            if( dimStart != null && currentTime < dimStart.time ) {
				brightness == highBrightness
			}
			else if( dimEnd != null && currentTime > dimEnd.time ) {
				brightness = lowBrightness	
			}
			else if( dimStart != null && dimEnd != null && ( currentTime > dimStart.time && currentTime < dimEnd.time ) ) {
				brightnessPercentage = 1 - ((currentTime - dimStart.time) / (dimEnd.time - dimStart.time))
           	 	brightness = ( brightnessPercentage * fullRange + lowBrightness )/ 100
			}
			else{
				brightnessPercentage = 1 - ((currentTime - midDay) / (sunsetTime.time - midDay))
            	brightness = ( brightnessPercentage * fullRange + lowBrightness )/ 100
			}
            

        }
    }
	
    if(settings.dynamicBrightness == false) {
        if(settings.maxBrightnessOverride < 100 && settings.maxBrightnessOverride != null){
            brightness = settings.maxBrightnessOverride / 100
        }
        else{
            brightness = 1
        }
    }

    logDebug "Mode is ${location.mode} vs ${mode1Override}"
    switch(location.mode) {
        case mode1Override:
            brightness = mode1OverrideValue/100
            settings.mode1OverrideColorTemperature != "" && settings.mode1OverrideColorTemperature != null ? ( colorTemp = settings.mode1OverrideColorTemperature ) : null
            break
        case mode2Override:
            brightness = mode2OverrideValue/100
            settings.mode2OverrideColorTemperature != "" && settings.mode2OverrideColorTemperature != null ? ( colorTemp = settings.mode2OverrideColorTemperature ) : null
            break
        case mode3Override:
            brightness = mode3OverrideValue/100
            settings.mode3OverrideColorTemperature != "" && settings.mode3OverrideColorTemperature != null ? ( colorTemp = settings.mode3OverrideColorTemperature ) : null
            break
        case mode4Override:
            brightness = mode4OverrideValue/100
            settings.mode4OverrideColorTemperature != "" && settings.mode4OverrideColorTemperature != null ? ( colorTemp = settings.mode4OverrideColorTemperature ) : null
            break
        case mode5Override:
            brightness = mode5OverrideValue/100
            settings.mode5OverrideColorTemperature != "" && settings.mode5OverrideColorTemperature != null ? ( colorTemp = settings.mode5OverrideColorTemperature ) : null
            break
        case mode6Override:
            brightness = mode6OverrideValue/100
            settings.mode6OverrideColorTemperature != "" && settings.mode6OverrideColorTemperature != null ? ( colorTemp = settings.mode6OverrideColorTemperature ) : null
            break
        case mode7Override:
            brightness = mode7OverrideValue/100
            settings.mode7OverrideColorTemperature != "" && settings.mode7OverrideColorTemperature != null ? ( colorTemp = settings.mode7OverrideColorTemperature ) : null
            break
        case mode8Override:
            brightness = mode8OverrideValue/100
            settings.mode8OverrideColorTemperature != "" && settings.mode8OverrideColorTemperature != null ? ( colorTemp = settings.mode8OverrideColorTemperature ) : null
            break
        case mode9Override:
            brightness = mode9OverrideValue/100
            settings.mode9OverrideColorTemperature != "" && settings.mode9OverrideColorTemperature != null ? ( colorTemp = settings.mode9OverrideColorTemperature ) : null
            break
        case mode10Override:
            brightness = mode10OverrideValue/100
            settings.mode10OverrideColorTemperature != "" && settings.mode10OverrideColorTemperature != null ? ( colorTemp = settings.mode10OverrideColorTemperature ) : null
            break
        default:
            logDescriptionText "The current mode is not set for an override."
            break
    }

    state.lastAssignedBrightness = brightness

    def ct = [:]
    ct = [colorTemp: colorTemp, brightness: Math.round(brightness * 100)]
    ct
}

def getCT() {
    def ctb = getCTBright()
    logDebug "Color Temperature: ${ctb.colorTemp}"
    return ctb.colorTemp
}

def getHex() {
    def ct = getCT()
    logDebug "Hex: ${rgbToHex(ctToRGB(ct)).toUpperCase()}"
    return rgbToHex(ctToRGB(ct)).toUpperCase()
}

def getHSV() {
    def ct = getCT()
    logDebug "HSV: ${rgbToHSV(ctToRGB(ct))}"
    return rgbToHSV(ctToRGB(ct))
}

def getBright() {
    def ctb = getCTBright()
    logDebug "Brightness: " + ctb.brightness
    return ctb.brightness
}

def disableDimmerOverride(){
	
	logDescriptionText "Resetting override from external dimming."
    state.disabledFromDimmer = false
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) {
        log.debug "Circadian Daylight (${app.name}): ${debugText}"
    }
}
private logDescriptionText( debugText ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDescriptionText ) {
        log.info "Circadian Daylight (${app.name}): ${debugText}"
    }
}


// Based on color temperature converter from
// http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
// This will not work for color temperatures below 1000 or above 40000
def ctToRGB(ct) {

    if(ct < 1000) { ct = 1000 }
    if(ct > 40000) { ct = 40000 }

    ct = ct / 100

    //red
    def r
    if(ct <= 66) { r = 255 }
    else { r = 329.698727446 * ((ct - 60) ** -0.1332047592) }
    if(r < 0) { r = 0 }
    if(r > 255) { r = 255 }

    //green
    def g
    if (ct <= 66) { g = 99.4708025861 * Math.log(ct) - 161.1195681661 }
    else { g = 288.1221695283 * ((ct - 60) ** -0.0755148492) }
    if(g < 0) { g = 0 }
    if(g > 255) { g = 255 }

    //blue
    def b
    if(ct >= 66) { b = 255 }
    else if(ct <= 19) { b = 0 }
    else { b = 138.5177312231 * Math.log(ct - 10) - 305.0447927307 }
    if(b < 0) { b = 0 }
    if(b > 255) { b = 255 }

    def rgb = [:]
    rgb = [r: r as Integer, g: g as Integer, b: b as Integer]
    rgb
}

def rgbToHex(rgb) {
    return "#" + Integer.toHexString(rgb.r).padLeft(2,'0') + Integer.toHexString(rgb.g).padLeft(2,'0') + Integer.toHexString(rgb.b).padLeft(2,'0')
}

//http://www.rapidtables.com/convert/color/rgb-to-hsv.htm
def rgbToHSV(rgb) {
    def h, s, v

    def r = rgb.r / 255
    def g = rgb.g / 255
    def b = rgb.b / 255

    def max = [r, g, b].max()
    def min = [r, g, b].min()

    def delta = max - min

    //hue
    if(delta == 0) { h = 0}
    else if(max == r) {
        double dub = (g - b) / delta
        h = 60 * (dub % 6)
    }
    else if(max == g) { h = 60 * (((b - r) / delta) + 2) }
    else if(max == b) { h = 60 * (((r - g) / delta) + 4) }

    //saturation
    if(max == 0) { s = 0 }
    else { s = (delta / max) * 100 }

    //value
    v = max * 100

    def degreesRange = (360 - 0)
    def percentRange = (100 - 0)

    return [h: ((h * percentRange) / degreesRange) as Integer, s: ((s * percentRange) / degreesRange) as Integer, v: v as Integer]
}
