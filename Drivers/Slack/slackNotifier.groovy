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
        author: "Adam Kempenich") {
        
		capability "Notification"
    }
    
    preferences {  
		 input "apiURL", "text", title: "URL to Slack Hook", description: "URL to Slack Hook", defaultValue: "https://...", required: true, displayDuringSetup: true
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
				log.debug "${it.name} : ${it.value}"
			}
			log.debug "response contentType: ${resp.contentType}"
		}
	} catch (e) {
		log.debug "something went wrong: $e"
	}
}
