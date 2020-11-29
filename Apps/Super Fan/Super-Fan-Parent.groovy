/**
*	Super Fan
*
*    Author:
*        Adam Kempenich
*
*/
definition(
    name:"Super Fan",
    namespace: "Super Fan",
    author: "Adam Kempenich",
    description: "Your lights fade up and down. Why not let your fans do the same?",
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
    state.initializeCompleted = true
    initialize()
}


def updated() {
    log.info "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    childApps.each { child ->
        logInformationText "Child app: ${child.label}"
    }
}

def mainPage() {
    dynamicPage(name: "mainPage", refreshInterval:0){

        section("<h2>Help/Information</h2>"){
            paragraph "Bathroom fans are stupid! On? Off? Do they really have to be so loud all the time? Why can't they fade up and down nicely? Let's fix that!"    
            label title: "<h2>Want to give Super Fan a different name? (optional)</h2>", required: false, submitOnChange: true

        }
        if(state.initializeCompleted){
            section() {
                app(name: "Super Fan", appName: "Super Fan - Child", namespace: "Super Fan", title: "<style>.normal, .normal > .scaleOnHover, .normal > .hideOnHover{transform: scale(1); opacity: 1; transition-duration: 0.5s;} .normal:hover > .scaleOnHover{transition-duration: 0.5s; transform: scale(1.01)} .normal:hover > .hideOnHover, .normal:hover > hr{opacity: 0; transition-duration: 0.5s}</style><div class='normal'><div class='scaleOnHover' style='font-size: 2em !important'>Create New Super Fan Setup</div><hr><p class='normal hideOnHover'>Or, edit existing Super Fan setups:</p></div>", multiple: true)
            } 
        } else {
            section(){
                paragraph "<div style='position:relative; width: 100%; padding: 1em; text-align:left;' class='mdl-button mdl-js-button mdl-button--raised' disabled><div class='scaleOnHover' style='font-size: 2em !important'>Create New Super Fan Setup</div><hr><p class='normal hideOnHover'>Or, edit existing Super Fan setups:</p></div>"

                paragraph "<h2 style='color:#B22222;'>You must first confirm the settings below, and press <i>Done</i> before coming back to create Super Fan setups</h2>"
            }
        }



        section("Debug settings", hideable: true){

            input(name:"logDebug", type:"bool", title: "${logDebug || !logDescriptionText == null ? '<input type="checkbox" class="mdl-checkbox__input" checked><span class="mdl-checkbox__label"><b>Log debug information</b></span>' : '<input type="checkbox" class="mdl-checkbox__input"><span class="mdl-checkbox__label"><b>Log debug information</b></span>'}",
                  description: "Logs data for debugging. (Default: Off)", defaultValue: true,
                  required: false, displayDuringSetup: true, submitOnChange: true)
            input(name:"logDescriptionText", type:"bool", title: "${logDescriptionText || logDescriptionText == null ? '<input type="checkbox" class="mdl-checkbox__input" checked><span class="mdl-checkbox__label"><b>Log description text</b></span>' : '<input type="checkbox" class="mdl-checkbox__input"><span class="mdl-checkbox__label"><b>Log description text</b></span>'}",
                  description: "Logs when useful things happen. (Default: On)", defaultValue: true,
                  required: true, displayDuringSetup: true, submitOnChange: true)
        }
        section("<h2>Help/Information</h2>"){
            paragraph "Thanks for installing Super Fan!"
        }
    }
}
