/**
*	Salt and Pepper (1.0)
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Salt and Pepper",
    namespace: "Salt and Pepper",
    author: "Adam Kempenich",
    description: "Use two lights of different color temperatures to create a range of CT.",
    category: "Lighting",
	    
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
)


preferences {
     page name: "mainPage", title: "", install: true, uninstall: true
} 

def installed() {
    log.info "Installed with settings: ${settings}"
    initialize()
}


def updated() {
    log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    log.info "Salt and Pepper has ${childApps.size()} child apps"
    childApps.each { child ->
        log.info "Child app: ${child.label}"
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
                
		    section("<h2>Help/Information</h2>"){
                paragraph "<h3>Salt and Pepper is a film term used when two color temperatures are mixed to create an intermediate color temperature.</h3><hr><h3>This app lets you mix multiple devices to create a range of color temperatures from only a few bulbs</h3><br><a href='https://www.youtube.com/watch?v=-nM2xkejpZI'>It's also a wonderful phrase by Patrick Stewart</a>"
    
            }
		    section() {
		    	app(name: "anyOpenApp", appName: "Salt and Pepper - Child", namespace: "Salt and Pepper", title: "<b>Add a new Salt and Pepper setup</b>", multiple: true)
		    }		
	}
}
