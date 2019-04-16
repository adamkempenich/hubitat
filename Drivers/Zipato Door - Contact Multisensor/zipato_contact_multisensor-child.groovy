/**
 *  Zipato Z-Wave Contact Multi Sensor 0.91
 *
 *  Author: 
 *    Adam Kempenich
 *
 *  Documentation:  [Does not exist, yet]
 *    
 *
 *  Changelog:
 *	  0.91 (Mar 17 2019)
 * 		- Added child devices for external and magnetic contact
 *		- 
 *
 *    0.9 (Jan 24 2019)
 *      - Initial Release
 *      - Does not report temperature, yet
 *      - Does not report battery, yet
 *		- Does not report fingerprint, yet 
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
metadata {
definition (name: "Zipato Z-Wave Contact Multi Sensor Child", namespace: "zipato", author: "Adam Kempenich", importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Drivers/Zipato%20Door%20-%20Contact%20Multisensor/zipato_contact_multisensor-child.groovy") {
        capability "Contact Sensor"
	}
}

