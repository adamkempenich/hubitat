/**
*   Rootin' Tootin' Self-Rebootin' (0.95)
*
*   Author:
*       Adam Kempenich
*
*   Documentation: https://community.hubitat.com/t/release-rootin-tootin-self-rebootin-hub-0-9/27863
*
*  Changelog:
*    0.95 (Feb 11 2020) - Thanks to @CodaHQ for the updates! 
*        - Added ignore restart during maintenance window 
*        - Consolidated the options to the very top of file
*        - Added more targeted logging with configuration options
*        - Fixed login method to work by changing literal Strings (single-quote) to dynamic Strings (double-quote)
*        - Standardized formatting 
*
*    0.94 (Jan 03 2020)
*        - Updated timecheck
*        - Added variablity for initialization delay
*        - Added cookie/login support
*        - Boot loop limiter resets automatically after a successful boot
*
*    0.93 (Dec08 2019)
*        - Fixed the initialization routine
*
*    0.92 (Dec 03 2019)
*        - Fixed boot loop limiter
*        - Added startup delay
* 
*    0.91 (Nov 25 2019)
*        - Added a boot loop limiter. Chuck Schwer's beautiful idea.
* 
*    0.90 (Nov 25 2019)
*        - Initial Release
*/

//TODO: comment these better so people who don't read code can understand what to change
//OPTIONS
@Field String hubIP = "X.X.X.X"
@Field int maintenanceStart = 2 //mine always starts at 2:00 am so this means 2 am
@Field boolean hubRequiresPassword = true  //change to false if authentication is turned off
@Field String username = "YOUR_USERNAME"
@Field String password = "YOUR_PASSWORD"
//END OPTIONS



/*****  DO NOT MODIFY BELOW THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING *****/
import groovy.transform.Field
import hubitat.device.HubAction

definition (
  name: "Rootin' Tootin' Self-Rebootin'",
  namespace: "adamkempenich",
  author: "Adam Kempenich",
  importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Utilities/RootinTootinSelfRebootin.groovy",
  description: "Reboots your Hub if the slowdown is detected or the db is unreadable.",
  category: "Wild West",
  iconUrl: "",
  iconX2Url: ""
)

preferences {
  section('<img alt="" src="data:image/png;base64,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" /><h1>It is yeehaw  time my dudes</h1><style>.mdl-switch, h1{font-family:Comic Sans MS, Comic Sans, Cursive}</style>') { 
  }
  section("Options:") {
    input "startupDelayTime", "enum", title: "Delay this app upon boot for this many minutes:", options: [1,2,3,4,5,7,10], required: true
    input "bootLoopLimiter", "enum", title: "Number of times a hub can unsuccessfully reboot in a row before this app stops rebooting:", options: [1,2,3,4,5,7,10], required: true
    input "checkEveryNSeconds", "enum", title: "Run a test every N seconds:", options: [20,30,60], required: true
    input "rebootDuringMaintenance", "bool", title: "Allow Reboot During Maintenance?", Description: "Allow RTSR to reboot the hub during the nightly maintenance window?", default: false
    input "logLevel", "enum", title: "Logging Level", options: [1: "Error", 2: "Warn", 3: "Info", 4: "Debug", 5: "Trace"], required: false
  }
}


def installed() {
  unsubscribe()
  unschedule()
  initialize()
}

def updated() {
  log.warn "Logging level is ${logLevel}"
  getCookie()
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  unsubscribe()
  unschedule()
  runIn((60 * settings.startupDelayTime.toInteger()), startApp) // Have to do it this way or the app hangs on initialization for N minutes
  logInfo "Scheduled a Rootin' Tootin' Startup in ${60 * settings.startupDelayTime.toInteger()} seconds"
}


def getCookie() {

  def body = "username=${username}&password=${password}"

  def params = [
    uri: "http://${hubIP}:8080",
    path: '/login',
    requestContentType: 'application/x-www-form-urlencoded',
    contentType: 'application/x-www-form-urlencoded',
    body: "username=${username}&password=${password}"
  ]

  logDebug "Post params: $params"
  
  asynchttpPost("loginResponse", params)
}

def sendReboot() {
  logError "An error occured. Hub is unreachable or has taken too long to respond. Rebooting now."

  if (!rebootDuringMaintenance && isMaintenanceWindow()) {
    logWarn "Maintenance happening.  Skipping reboot!"
    return
  }

  if (!hubRequiresPassword) {
    // NO-COOKIE METHOD
    asynchttpPost('rebootResponse', [uri: "http://${hubIP}:8080/hub/reboot"])
  } 
  else {
    // COOKIE METHOD
    asynchttpPost('rebootResponse', [uri: "http://${hubIP}:8080", path: '/hub/reboot', headers: ['Cookie': "${state.storedCookie}"]])
  }

  state.loopCounter++
}

def startApp() {
  // Check if the loop counter has been initialized yet
  state.loopCounter == null ? state.loopCounter = 0 : null
  state.checkInTime = now() // Reset the last check-in

  if (hubRequiresPassword) {
    getCookie()
  }

  logInfo "Initializing Rootin' Tootin' Self-Rebootin'"
  if (state.loopCounter >= settings.bootLoopLimiter.toInteger()) {
    logDebug "The hub has rebooted too many times in a row (${state.loopCounter} times). This app will now stop."
  }
  else {
    logInfo "Scheduling Rootin' Tootin' check-ins and readability tests"
    schedule("0/${settings.checkEveryNSeconds.toInteger() / 2} * * * * ?", checkIn)
    schedule("0/${settings.checkEveryNSeconds} * * * * ?", rebootTest)
  }

}


def parseResponse(response, data) {
  log.trace "What's this? //TODO: Make it so that errors are logged as errors but success is logged as info"
  log.trace "Received ${response}, ${data}"
}

def checkIn() {
  state.checkInTime = now()
}

def rebootTest() {

  logInfo "Checking hub for DB readability..."
  try {
    if ((now() - state.checkInTime) / 1000 > settings.checkEveryNSeconds.toInteger()) { 
      // If the current time minus the last time checked in is more seconds than the last scheduled check in, reboot
      sendReboot()
    }
    else {
      state.loopCounter = 0
      logInfo "DB read success. Boot Loop counter has been restarted"
    }
  }
  catch(e) {
    sendReboot()
  }
}


def loginResponse(response, data) {
  // Handle responses from sending username/password to the hub

  if (response.status == 200 || response.status == 302) {
    logInfo "Sucessfully logged in to hub"
    def cookie = response.headers["Set-Cookie"].split(";")
    state.storedCookie = cookie[0]
    logTrace "Stored cookie ${cookie[0]} from ${response.headers['Set-Cookie']}"
  }
  else {
    logError "Login failed with status ${response.status}.  Please check configuration!"
    response.properties.each {logDebug "resp prop: $it"}
  }
}

def rebootResponse(response, data) {
  // Handle resonses from a reboot with cookie

  logDebug "Response Status: {${response.status}"

  if (response.status == 200) {
    logInfo "Sucessfully sent reboot packet. Rebooting momentarily..."
  }
  else {
    logError "Something went wrong when trying to send a reboot packet."   
  }
}

def isMaintenanceWindow() {
  Calendar cal = Calendar.getInstance();
  def hour = cal.get(Calendar.HOUR_OF_DAY)
  logTrace "hour: $hour"
  //TODO: Get the maintenance window dynamically for all regions
  return hour == maintenanceStart
}

def logError(msg) {
  if (logLevel?.toInteger() >= 1) { log.error msg }
}

def logWarn(msg) {
  if (logLevel?.toInteger() >= 2) { log.warn msg }
}

def logInfo(msg) {
  if (logLevel?.toInteger() >= 3) { log.info msg }
}

def logDebug(msg) {
  if (logLevel?.toInteger() >= 4) { log.debug msg }
}

def logTrace(msg) {
  if (logLevel?.toInteger() >= 5) { log.trace msg }
}
