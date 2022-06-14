definition(
    name: "Thermostat Synchronizer",
    namespace: "AdamKempenich",
    author: "Adam Kempenich",
    importURL: "",
    description: "",
    category: "",
    iconUrl: "",
    iconX2Url: ""
)

preferences {
    section("Options:") {

        //Master
        input "thermostats", "capability.thermostat", title: "Synchronize these devices", multiple:true, required: true

        input "masterDevice", "capability.thermostat", title: "This is the master device that others will occasionally poll", multiple:false, required: false

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

def initialize() {
    unsubscribe()
    unschedule()
    log.trace "init"

    subscribe(thermostats, "coolingSetpoint", thermostatChanged)
    subscribe(thermostats, "heatingSetpoint", thermostatChanged)
    subscribe(thermostats, "thermostatFanMode", thermostatChanged)
    subscribe(thermostats, "thermostatMode", thermostatChanged)
    subscribe(thermostats, "thermostatSetpoint", thermostatChanged)

    if(masterDevice != null){
        syncFromMaster()
        runEvery15Minutes(syncFromMaster)
    }

}

def thermostatChanged(event){

    log.trace "Event: ${event}"
    log.trace "Device Name: ${event.device.deviceNetworkId}"
    log.trace "Event Name: ${event.name}"
    log.trace "Event Value: ${event.value}"

    if(event.name == "thermostatSetpoint"){

        for(comparisonDevice in thermostats){
            log.trace "Checking ${comparisonDevice.deviceNetworkId}"

            if(event.device.deviceNetworkId != comparisonDevice.deviceNetworkId){
                log.trace "${event.device.deviceNetworkId} != ${comparisonDevice.deviceNetworkId}"

                if(comparisonDevice.hasCommand("setThermostatSetpoint")){

                    log.trace "Device has attribute: thermostatSetpoint"

                    if(event.value != comparisonDevice.thermostatSetpoint){
                        log.trace "Setting thermostatSetpoint to ${event.value}"

                        comparisonDevice.setThermostatSetpoint(event.value.toFloat())

                    }
                }
            }
        }
    }

    if(event.name == "coolingSetpoint"){

        for(comparisonDevice in thermostats){
            log.trace "Checking ${comparisonDevice.deviceNetworkId}"

            if(event.device.deviceNetworkId != comparisonDevice.deviceNetworkId){
                log.trace "${event.device.deviceNetworkId} != ${comparisonDevice.deviceNetworkId}"

                if(comparisonDevice.hasCommand("setCoolingSetpoint")){

                    log.trace "Device has attribute: coolingSetpoint"

                    if(event.value != comparisonDevice.coolingSetpoint){
                        log.trace "Setting cooling setpoint to ${event.value}"

                        comparisonDevice.setCoolingSetpoint(event.value.toFloat())

                    }
                }
            }
        }
    }


    if(event.name == "heatingSetpoint"){

        for(comparisonDevice in thermostats){
            log.trace "Checking ${comparisonDevice.deviceNetworkId}"

            if(event.device.deviceNetworkId != comparisonDevice.deviceNetworkId){
                log.trace "${event.device.deviceNetworkId} != ${comparisonDevice.deviceNetworkId}"

                if(comparisonDevice.hasCommand("setHeatingSetpoint")){

                    log.trace "Device has attribute: heatingSetpoint and received value of ${event.value}"

                    if(event.value != comparisonDevice.heatingSetpoint){
                        log.trace "Setting heating setpoint to ${event.value}"

                        comparisonDevice.setHeatingSetpoint(event.value.toFloat())

                    }
                }
            }
        }
    }


    if(event.name == "thermostatFanMode"){

        for(comparisonDevice in thermostats){
            log.trace "Checking ${comparisonDevice.deviceNetworkId}"

            if(event.device.deviceNetworkId != comparisonDevice.deviceNetworkId){
                log.trace "${event.device.deviceNetworkId} != ${comparisonDevice.deviceNetworkId}"

                if(comparisonDevice.hasCommand("setThermostatFanMode")){

                    log.trace "Device has attribute: thermostatFanMode"

                    if(event.value != comparisonDevice.thermostatFanMode){
                        log.trace "Setting thermostatFanMode to ${event.value}"

                        comparisonDevice.setThermostatFanMode(event.value)

                    }
                }
            }
        }
    }
    if(event.name == "thermostatMode"){

        for(comparisonDevice in thermostats){
            log.trace "Checking ${comparisonDevice.deviceNetworkId}"

            if(event.device.deviceNetworkId != comparisonDevice.deviceNetworkId){
                log.trace "${event.device.deviceNetworkId} != ${comparisonDevice.deviceNetworkId}"

                if(comparisonDevice.hasCommand("setThermostatMode")){

                    log.trace "Device has attribute: thermostatMode"

                    if(event.value != comparisonDevice.thermostatMode){
                        log.trace "Setting thermostatMode to ${event.value}"

                        comparisonDevice.setThermostatMode(event.value)

                    }
                }
            }
        }
    }

}

def syncFromMaster(){

    log.trace "sync from master"

    for(comparisonDevice in thermostats){
        log.trace "Checking ${comparisonDevice}"

        if(masterDevice.deviceNetworkId != comparisonDevice.deviceNetworkId){
            log.trace "Names are different"

            if(comparisonDevice.hasCommand("setThermostatSetpoint")){      
                
                log.trace masterDevice.currentValue('thermostatSetpoint')
                log.trace comparisonDevice.currentValue('thermostatSetpoint')
                
                if(masterDevice.currentValue('thermostatSetpoint') != comparisonDevice.currentValue('thermostatSetpoint')){

                    comparisonDevice.setThermostatSetpoint(masterDevice.currentValue('thermostatSetpoint').toFloat())
                }
            }

            if(comparisonDevice.hasCommand("setCoolingSetpoint")){                
                if(masterDevice.currentValue('coolingSetpoint') != comparisonDevice.currentValue('coolingSetpoint')){

                    comparisonDevice.setCoolingSetpoint(masterDevice.currentValue('coolingSetpoint').toFloat())
                }
            }

            if(comparisonDevice.hasCommand("setHeatingSetpoint")){                
                if(masterDevice.currentValue('heatingSetpoint') != comparisonDevice.currentValue('heatingSetpoint')){

                    comparisonDevice.setHeatingSetpoint(masterDevice.currentValue('heatingSetpoint').toFloat())
                }
            }
            if(comparisonDevice.hasCommand("setThermostatFanMode")){                
                if(masterDevice.currentValue('thermostatFanMode') != comparisonDevice.currentValue('thermostatFanMode')){

                    comparisonDevice.serThermostatFanMode(masterDevice.currentValue('thermostatFanMode'))
                }
            }

            if(comparisonDevice.hasCommand("setThermostatMode")){                
                if(masterDevice.currentValue('thermostatMode') != comparisonDevice.currentValue('thermostatMode')){

                    comparisonDevice.setThermostatMode(masterDevice.currentValue('thermostatMode'))
                }
            }

        }
    }



}


def parse(String description) {

}

def logDebug(text){
    log.debug text
}
