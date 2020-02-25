/**
*    Percentage to Color Temperature (Child) 0.7
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
*    Remnant code probably exists somewhere in here from @Cobra or @BPTWorld, from whom I have learned incredibly much
*    Although, I'm not entirely sure, so I best credit them here regardless! :) 
*
*    ***********************************************************************************************************
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
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
    importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Utilities/PercentToColorTemperature/Percent%20To%20Color%20Temperature.groovy"
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
