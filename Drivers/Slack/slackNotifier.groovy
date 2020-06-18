/**
*	Hubitat Slack Notifier 0.10
*
*	Author: 
*		Adam Kempenich 
*
*	Documentation: Does not exist
*
*  Changelog:
*
*	0.10 (Apr 02 2019) 
*		- Initial (development internal) release
*
* 	To-Do:
*		- Everything
*
*/
metadata {
	definition (
        name: "Slack Notifier", 
        namespace: "slacknotifier", 
        author: "Adam Kempenich",
        importUrl: "https://github.com/adamkempenich/hubitat/raw/master/Drivers/Slack/slackNotifier.groovy") { capability "Notification" }
    
        preferences {
		    input "apiURL", "text", title: "URL to Slack Hook", defaultValue: "https://...", required: true, displayDuringSetup: true
		    input(name: "logLevel", title: "IDE logging level", multiple: false, required: true, type: "enum", options: getLogLevels(), submitOnChange : false, defaultValue : "1")
        }
}


def installed() {
    unsubscribe()
    unschedule()
    initialize()
}

def updated() {

}

private def initialize() {
    
}

def deviceNotification(text){
	def params = [
		uri: "${settings.apiURL}",
		body: [text: "${text}"]
	]
	try {
		httpPostJson(params) { resp ->
			resp.headers.each {
				debuglog("${it.name} : ${it.value}")
			}
			debuglog("response contentType: ${resp.contentType}")
		}
	} catch (e) {
		debuglog("something went wrong: $e")
	}
}

def debuglog(statement)
{
	def logL = 0
    if (logLevel) logL = logLevel.toInteger()
    if (logL == 0) {return}//bail
    else if (logL >= 2)
	{
		log.debug(statement)
	}
}
def infolog(statement)
{
	def logL = 0
    if (logLevel) logL = logLevel.toInteger()
    if (logL == 0) {return}//bail
    else if (logL >= 1)
	{
		log.info(statement)
	}
}
def getLogLevels(){
    return [["0":"None"],["1":"Running"],["2":"NeedHelp"]]
}
