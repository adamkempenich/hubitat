/**
*	Hubitat Circadian Daylight 0.72
*
*	Author: 
*		Adam Kempenich 
*
*	Documentation:  https://community.hubitat.com/t/release-app-circadian-daylight-port/
*	
*	Forked from:
*  		SmartThings Circadian Daylight v. 2.6
*		https://github.com/KristopherKubicki/smartapp-circadian-daylight/
*
*  Changelog:
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
*		- Add logDebug method
*		- Add brightness max/min overrides
* 		- Add custom zip code
*/

definition(
	name: "Circadian Daylight",
	namespace: "circadianDaylight",
	author: "Adam Kempenich",
	description: "Sync your color changing lights and dimmers with natural daylight hues to improve your cognitive functions and restfulness.",
	category: "Green Living",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
    section("Thank you for installing Circadian Daylight! This application dims and adjusts the color temperature of your lights to match the state of the sun, which has been proven to aid in cognitive functions and restfulness. The default options are well suited for most users, but feel free to tweak accordingly!") {
    }
    section("Control these bulbs; Select each bulb only once") {
        input "ctbulbs", "capability.colorTemperature", title: "Which Temperature Changing Bulbs?", multiple:true, required: false
        input "bulbs", "capability.colorControl", title: "Which Color Changing Bulbs?", multiple:true, required: false
        input "dimmers", "capability.switchLevel", title: "Which Dimmers?", multiple:true, required: false
    }
    section("What are your 'Sleep' modes? The modes you pick here will dim your lights and filter light to a softer, yellower hue to help you fall asleep easier. Protip: You can pick 'Nap' modes as well!") {
        input "smodes", "mode", title: "What are your Sleep modes?", multiple:true, required: false
    }
    section("Override Constant Brightness (default) with Dynamic Brightness? If you'd like your lights to dim as the sun goes down, override this option. Most people don't like it, but it can look good in some settings.") {
        input "dbright","bool", title: "On or off?", required: false
    }
    section("Override night time Campfire (default) with Moonlight? Circadian Daylight by default is easier on your eyes with a yellower hue at night. However if you'd like a whiter light instead, override this option. Note: this will likely disrupt your circadian rhythm.") {
        input "dcamp","bool", title: "On or off?", required: false
    }
    section("Override night time Dimming (default) with Rhodopsin Bleaching? Override this option if you would not like Circadian Daylight to dim your lights during your Sleep modes. This is definitely not recommended!") {
        input "ddim","bool", title: "On or off?", required: false
    }
    section("Disable Circadian Daylight when the following switches are on:") {
        input "dswitches","capability.switch", title: "Switches", multiple:true, required: false
    }

    section("Sunset/sunrise Overrides") {
        input "sunriseOverride", "time", title: "Sunrise Override", required: false
        input "sunsetOverride", "time", title: "Sunset Override", required: false
    }
	section("Sunrise/Sunset Offsets"){
        input "sunriseOffset", "decimal", title: "Sunrise Offset (+/-)", required: false
        input "sunsetOffset", "decimal", title: "Sunset Offset (+/-)", required: false
    }
    section("Color Temperature Overrides"){
        input "coldCTOverride", "number", title: "Cold White Temperature", required: false
        input "warmCTOverride", "number", title: "Warm White Temperature", required: false
    }
    section("Brightness Overrides?") {
        input "maxBrightnessOverride","number", title: "Max Brightness Override", required: false
        input "minBrightnessOverride","number", title: "Min Brightness Override", required: false
    }
	section("Zipcode override") {
        input "zipCodeOverride","number", title: "Zip Code Override", required: false
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
    log.debug("initialize() with settings: ${settings}")
    if(ctbulbs) { subscribe(ctbulbs, "switch.on", modeHandler) }
    if(bulbs) { subscribe(bulbs, "switch.on", modeHandler) }
    if(dimmers) { subscribe(dimmers, "switch.on", modeHandler) }
    if(dswitches) { subscribe(dswitches, "switch.off", modeHandler) }
    subscribe(location, "mode", modeHandler)
    
    // revamped for sunset handling instead of motion events
    subscribe(location, "sunset", modeHandler)
    subscribe(location, "sunrise", modeHandler)
    schedule("0 */15 * * * ?", modeHandler)
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
	}
	else{
		sunRiseSet = getSunriseAndSunset(zipCode: $settings.zipCodeOverride)
	}
	if(settings.sunriseOverride != null && settings.sunriseOverride != ""){
		 sunriseTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunriseOverride)
	}
	else if(settings.sunriseOffset != null && settings.sunriseOffset != ""){
		sunriseTime = sunRiseSet.sunrise.plusMinutes(settings.sunriseOffset)
	}
	else{
	    sunriseTime = sunRiseSet.sunrise
	}
	return sunriseTime
}
private def getSunsetTime(){
	def sunRiseSet 
	def sunriseTime
	
	if(settings.zipCodeOverride == null || settings.zipCodeOverride == ""){
		sunRiseSet = getSunriseAndSunset()
	}
	else{
		sunRiseSet = getSunriseAndSunset(zipCode: $settings.zipCodeOverride)
	}
	if(settings.sunsetOverride != null && settings.sunsetOverride != ""){
		sunsetTime = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", settings.sunsetOverride)
	}
	else if(settings.sunsetOffset != null && settings.sunsetOffset != ""){
		sunsetTime = sunRiseSet.sunset.plusMinutes(settings.sunsetOffset)
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
    log.debug("sunrise time ${sunriseTime}")
	
    def sunsetTime = getSunsetTime()
    log.debug("sunset time ${sunsetTime}")
    
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
    
	log.debug "checking... ${runTime.time} : $runTime. state.nextTime is ${state.nextTime}"
    if(state.nextTime != runTime.time) {
        state.nextTime = runTime.time
        log.debug "Scheduling next step at: $runTime (sunset is $sunsetTime) :: ${state.nextTime}"
        runOnce(runTime, modeHandler)
    }
}


// Poll all bulbs, and modify the ones that differ from the expected state
def modeHandler(evt) {
    for (dswitch in dswitches) {
        if(dswitch.currentSwitch == "on") {
            return
        }
    }
    
    def ct = getCT()
    def hex = getHex()
    def hsv = getHSV()
    def bright = getBright()
    
    for(ctbulb in ctbulbs) {
        if(ctbulb.currentValue("switch") == "on") {
            if((settings.dbright == true || location.mode in settings.smodes) && ctbulb.currentValue("level") != bright) {
                ctbulb.setLevel(bright)
            }
            if(ctbulb.currentValue("colorTemperature") != ct) {
                ctbulb.setColorTemperature(ct)
            }
        }
    }
    def color = [hex: hex, hue: hsv.h, saturation: hsv.s, level: bright]
    for(bulb in bulbs) {
        if(bulb.currentValue("switch") == "on") {
			def tmp = bulb.currentValue("color")
            if(bulb.currentValue("color") != hex) {
            	if(settings.dbright == true || location.mode in settings.smodes) { 
	            	color.value = bright
                } else {
					color.value = bulb.currentValue("level")
				}
            	def ret = bulb.setColor(color)
			}
        }
    }
    for(dimmer in dimmers) {
        if(dimmer.currentValue("switch") == "on") {
        	if(dimmer.currentValue("level") != bright) {
            	dimmer.setLevel(bright)
            }
        }
    }
    
    scheduleTurnOn()
}

def getCTBright() {	
	def sunriseTime = getSunriseTime()
    def sunsetTime = getSunsetTime()
    def midDay = sunriseTime.time + ((sunsetTime.time - sunriseTime.time) / 2)
    
    def currentTime = now()
    def float brightness = 1
	
    def int colorTemp = settings.coldCTOverride == null || settings.coldCTOverride == "" ? 2700 : settings.coldCTOverride
	def int coldCT = settings.coldCTOverride == null || settings.coldCTOverride == "" ? 6500 : settings.coldCTOverride
	def int warmCT = settings.warmCTOverride == null || settings.warmCTOverride == "" ? 2700 : settings.warmCTOverride
	def int midCT = coldCT - warmCT
	
    if(currentTime > sunriseTime.time && currentTime < sunsetTime.time) {
        if(currentTime < midDay) {
    		
            colorTemp = warmCT + ((currentTime - sunriseTime.time) / (midDay - sunriseTime.time) * midCT)
            brightness = ((currentTime - sunriseTime.time) / (midDay - sunriseTime.time))
        }
        else {
            colorTemp = coldCT - ((currentTime - midDay) / (sunsetTime.time - midDay) * midCT)
            brightness = 1 - ((currentTime - midDay) / (sunsetTime.time - midDay))
            
        }
    }
    
    if(settings.dbright == false) {
        brightness = 1
    }
    
	if(location.mode in settings.smodes) {
		if(currentTime > sunsetTime.time) {
			if(settings.dcamp == true) {
				colorTemp = coldCT
			}
			else {
				colorTemp = warmCT
			}
		}
		if(settings.ddim == false) {
			brightness = 0.01
		}
	}
    
    def ct = [:]
    ct = [colorTemp: colorTemp, brightness: Math.round(brightness * 100)]
    ct
}

def getCT() {
	def ctb = getCTBright()
    //log.debug "Color Temperature: " + ctb.colorTemp
    return ctb.colorTemp
}

def getHex() {
	def ct = getCT()
    //log.debug "Hex: " + rgbToHex(ctToRGB(ct)).toUpperCase()
    return rgbToHex(ctToRGB(ct)).toUpperCase()
}

def getHSV() {
	def ct = getCT()
    //log.debug "HSV: " + rgbToHSV(ctToRGB(ct))
    return rgbToHSV(ctToRGB(ct))
}

def getBright() {
	def ctb = getCTBright()
    //log.debug "Brightness: " + ctb.brightness
    return ctb.brightness
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
