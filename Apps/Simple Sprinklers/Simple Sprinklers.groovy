/**
*    Simple Sprinklers - Hubitat
*    Created for Zooz
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Simple Sprinklers",
    namespace: "Zooz",
    author: "Adam Kempenich",
    description: "Water different sections of your lawn, one at a time.",
    category: "Convenience",
    iconUrl: "https://community.hubitat.com/uploads/default/original/3X/5/f/5fb9352e4d800171699001ae71f70154ab179c4c.png",
    iconX2Url: "https://community.hubitat.com/uploads/default/original/3X/d/5/d5404eae2db65fe5b20da656805e62158f0f3558.png",
    iconX3Url: "https://community.hubitat.com/uploads/default/original/3X/9/e/9ebb8c08409adbcebbbdf7446986bf14b22f8e77.png",
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
    log.info "Simple Sprinklers has ${childApps.size()} child apps"
    childApps.each { child ->
        log.info "Child app: ${child.label}"
    }
}

def installCheck() {         
    state.appInstalled = app.getInstallationState()
    
    if (state.appInstalled != 'COMPLETE') {
        section{paragraph "Press <i>Done</i> to complete installation. Afterward, come back to this app to create a new watering schedule."}
    }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
                
            section("<h2>Help/Information</h2>"){
                paragraph "<b>Designed for use with the Multirelay ZEN16. Shop this device at <a href='https://www.thesmartesthouse.com/'>TheSmartestHouse.com</a></b>"
                paragraph "For help and support, visit <a href='https://www.support.getzooz.com/'>support.getzooz.com</a>"
    
            }
            section() {
                app(name: "anyOpenApp", appName: "Simple Sprinklers Setup", namespace: "Zooz", title: "<b>Add a new sprinkler setup</b>", multiple: true)
            }       
    }
}
