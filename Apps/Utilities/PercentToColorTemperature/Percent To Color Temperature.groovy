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
    name:"Percent to Color Temperature",
    namespace: "AdamKempenich",
    author: "Adam Kempenich",
    description: "Take the brightness of a virtual device on a dashboard and use it to control the color temperature of a different device!",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importURL: ""
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
    log.info "There are ${childApps.size()} child apps"
    childApps.each { child ->
        log.info "Child app: ${child.label}"
    }
}

def installCheck() {         
    state.appInstalled = app.getInstallationState()
    
    if (state.appInstalled != 'COMPLETE') {
        section{paragraph "Press <i>Done</i> to complete installation."}
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {

    	installCheck()

		if(state.appInstalled == 'COMPLETE'){
			section("<h2>Credits/App Name:</h2>", hideable: true, hidden: true){
                paragraph "Developed by @AdamKempenich"
                label title: "Enter a name for parent app (optional)", required: false
            }
			section() {
				app(name: "anyOpenApp", appName: "Percent to Color Temperature - Child", namespace: "AdamKempenich", title: "<h3>Add a new correlation</h3>", multiple: true)
			}
		}
	}
}
