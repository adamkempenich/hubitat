/**
*  MagicHome Wifi - Controller (WS 28XX) 1.00
*
*  Author: 
*    Adam Kempenich 
*
*  Documentation:  https://community.hubitat.com/t/release-beta-0-7-magic-home-wifi-devices-initial-public-release/5197
*
*  Changelog:
*    1.00 - Initial Release
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

import hubitat.helper.HexUtils
import hubitat.helper.ColorUtils
import groovy.json.JsonSlurper
import groovy.transform.Field

metadata {
    definition (
        name: "MagicHome Wifi — Controller (Pixel Controller) - Dev", 
        namespace: "MagicHome", 
        author: "Adam Kempenich",
        importUrl: "") {

        capability "Actuator"
        capability "Color Control"
        capability "Color Mode"
        capability "Color Temperature"
        capability "Initialize"
        capability "Polling"
        capability "Refresh"
        capability "Sensor"
        capability "Switch"
        capability "Switch Level"
        capability "Light Effects"


        command "customEffectRun", [[name: "Program Name:", type: "STRING"]]
        command "customEffectAdd", [[name: "Program Name:", type: "STRING" ], [name: "Generated Code:", type: "STRING" ]]
        
        command "customEffectRemove", [[name: "Program Name:", type: "STRING"]]
        command "setEffectSpeed", [[name: "Speed", type: "NUMBER"]]
        attribute "effectSpeed", "number" // 0 - 100

    }

    preferences {  
        input "deviceIP", "text", title: "Server", description: "Device IP (e.g. 192.168.1.X)", required: true, defaultValue: "192.168.1.X"
        input "devicePort", "number", title: "Port", description: "Device Port (Default: 5577)", required: true, defaultValue: 5577

        input(name:"logDebug", type:"bool", title: "Log debug information?",
              description: "Logs raw data for debugging. (Default: Off)", defaultValue: false,
              required: true, displayDuringSetup: true)
        input(name:"logDescriptionText", type:"bool", title: "Log descriptionText?",
              description: "Logs when things happen. (Default: On)", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"turnOffWhenDisconnected", type:"bool", title: "Turn off when disconnected?",
              description: "When a device is unreachable, turn its state off. in Hubitat", defaultValue: true,
              required: true, displayDuringSetup: true)

        input(name:"reconnectPings", type:"number", title: "Reconnect after ...",
              description: "Number of failed pings before reconnecting device.", defaultValue: 3,
              required: true, displayDuringSetup: true)

        input(name:"enablePreStaging", type:"bool", title: "Enable Color Pre-Staging?",
              defaultValue: false, required: true, displayDuringSetup: true)

        input(name:"enableHueInDegrees", type:"bool", title: "Enable Hue in degrees (0-360)",
              defaultValue: false, required: true, displayDuringSetup: true)

        input(name:"refreshTime", type:"number", title: "Time to refresh (seconds)",
              description: "Interval between refreshing a device for its current value. Default: 10. Use number between 0-60", defaultValue: 10,
              required: true, displayDuringSetup: true)

    }
}

// Yes, I had to manually copy these from the app
@Field static Map lightEffects = [1:"Circulate all modes",
                                  2:"7 colors change gradually",
                                  3:"7 colors run in olivary",
                                  4:"7 colors change quickly",
                                  5:"7 colors strobe-flash",
                                  6:"7 colors running, 1 point from start to end and return back",
                                  7:"7 colors running, multi points from start to end and return back",
                                  8:"7 colors overlay, multi points from start to end and return back",
                                  9:"7 colors overlay, multi points from the middle to the both ends and return back",
                                  10:"7 colors flow gradually, from start to end and return back",
                                  11:"Fading out run, 7 colors from start to end and return back",
                                  12:"Runs in olivary, 7 colors from start to end and return back",
                                  13:"Fading out run, 7 colors start with white color from start to end and return back",
                                  14:"Run circularly, 7 colors with black background, 1point from start to end",
                                  15:"Run circularly, 7 colors with red background, 1point from start to end",
                                  16:"Run circularly, 7 colors with green background, 1point from start to end",
                                  17:"Run circularly, 7 colors with blue background, 1point from start to end",
                                  18:"Run circularly, 7 colors with yellow background, 1point from start to end",
                                  19:"Run circularly, 7 colors with purple background, 1point from start to end",
                                  20:"Run circularly, 7 colors with cyan background, 1point from start to end",
                                  21:"Run circularly, 7 colors with white background, 1point from start to end",
                                  22:"Run circularly, 7 colors with black background, 1point from end to start",
                                  23:"Run circularly, 7 colors with red background, 1point from end to start",
                                  24:"Run circularly, 7 colors with green background, 1point from end to start",
                                  25:"Run circularly, 7 colors with blue background, 1point from end to start",
                                  26:"Run circularly, 7 colors with yellow background, 1point from end to start",
                                  27:"Run circularly, 7 colors with purple background, 1point from end to start",
                                  28:"Run circularly, 7 colors with cyan background, 1point from end to start",
                                  29:"Run circularly, 7 colors with white background, 1point from end to start",
                                  30:"Run circularly, 7 colors with black background, 1point from start to end and return back",
                                  31:"Run circularly, 7 colors with red background, 1point from start to end and return back",
                                  32:"Run circularly, 7 colors With green background, 1point from start to end and return back",
                                  33:"Run circularly, 7 colors with blue background, 1point from start to end and return back",
                                  34:"Run circularly, 7 colors with yellow background, 1point from start to end and return back",
                                  35:"Run circularly, 7 colors with purple background, 1point from start to end and return back",
                                  36:"Run circularly, 7 colors with cyan background, 1point from start to end and return back",
                                  37:"Run circularly, 7 colors with white background, 1point from start to end and return back",
                                  38:"Run circularly, 7 colors with black background, 1point from middle to both ends",
                                  39:"Run circularly, 7 colors with red background, 1point from middle to both ends",
                                  40:"Run circularly, 7 colors with green background, 1point from middle to both ends",
                                  41:"Run circularly, 7 colors with blue background, 1point from middle to both ends",
                                  42:"Run circularly, 7 colors with yellow background, 1point from middle to both ends",
                                  43:"Run circularly, 7 colors with purple background, 1point from middle to both ends",
                                  44:"Run circularly, 7 colors with cyan background, 1point from middle to both ends",
                                  45:"Run circularly, 7 colors with white background, 1point from middle to both ends",
                                  46:"Run circularly, 7 colors with black background, 1point from both ends to middle",
                                  47:"Run circularly, 7 colors with red background, 1point from both ends to middle",
                                  48:"Run circularly, 7 colors with green background, 1point from both ends to middle",
                                  49:"Run circularly, 7 colors with blue background, 1point from both ends to middle",
                                  50:"Run circularly, 7 colors with yellow background, 1point from both ends to middle",
                                  51:"Run circularly, 7 colors with purple background, 1point from both ends to middle",
                                  52:"Run circularly, 7 colors with cyan background, 1point from both ends to middle",
                                  53:"Run circularly, 7 colors with white background, 1point from both ends to middle",
                                  54:"Run circularly, 7 colors with black background, 1point from middle to both ends and return back",
                                  55:"Run circularly, 7 colors with red background, 1point from middle to both ends and return back",
                                  56:"Run circularly, 7 colors with green background, 1point from middle to both ends and return back",
                                  57:"Run circularly, 7 colors with blue background, 1point from middle to both ends and return back",
                                  58:"Run circularly, 7 colors with yellow background, 1point from middle to both ends and return back",
                                  59:"Run circularly, 7 colors with purple background, 1point from middle to both ends and return back",
                                  60:"Run circularly, 7 colors with cyan background, 1point from middle to both ends and return back",
                                  61:"Run circularly, 7 colors with white background, 1point from middle to both ends and return back",
                                  62:"Overlay circularly, 7 colors with black background from start to end",
                                  63:"Overlay circularly, 7 colors with red background from start to end",
                                  64:"Overlay circularly, 7 colors with green background from start to end",
                                  65:"Overlay circularly, 7 colors with blue background from start to end",
                                  66:"Overlay circularly, 7 colors with yellow background from start to end",
                                  67:"Overlay circularly, 7 colors with purple background from start to end",
                                  68:"Overlay circularly, 7 colors with cyan background from start to end",
                                  69:"Overlay circularly, 7 colors with white background from start to end",
                                  70:"Overlay circularly, 7 colors with black background from end to start",
                                  71:"Overlay circularly, 7 colors with red background from end to start",
                                  72:"Overlay circularly, 7 colors with green background from end to start",
                                  73:"Overlay circularly, 7 colors with blue background from end to start",
                                  74:"Overlay circularly, 7 colors with yellow background from end to start",
                                  75:"Overlay circularly, 7 colors with purple background from end to start",
                                  76:"Overlay circularly, 7 colors with cyan background from end to start",
                                  77:"Overlay circularly, 7 colors with white background from end to start",
                                  78:"Overlay circularly, 7 colors with black background from start to end and return back",
                                  79:"Overlay circularly, 7 colors with red background from start to end and return back",
                                  80:"Overlay circularly, 7 colors with green backgroundfrom start to end and return back",
                                  81:"Overlay circularly, 7 colors with blue backgroundfrom start to end and return back",
                                  82:"Overlay circularly, 7 colors with yellow backgroundfrom start to end and return back",
                                  83:"Overlay circularly, 7 colors with purple backgroundfrom start to end and return back",
                                  84:"Overlay circularly, 7 colors with cyan backgroundfrom start to end and return back",
                                  85:"Overlay circularly, 7 colors with white backgroundfrom start to end and return back",
                                  86:"Overlay circularly, 7 colors with black backgroundfrom middle to both ends",
                                  87:"Overlay circularly, 7 colors with red backgroundfrom middle to both ends",
                                  88:"Overlay circularly, 7 colors with green backgroundfrom middle to both ends",
                                  89:"Overlay circularly, 7 colors with blue backgroundfrom middle to both ends",
                                  90:"Overlay circularly, 7 colors with yellow backgroundfrom middle to both ends",
                                  91:"Overlay circularly, 7 colors with purple backgroundfrom middle to both ends",
                                  92:"Overlay circularly, 7 colors with cyan backgroundfrom middle to both ends",
                                  93:"Overlay circularly, 7 colors with white backgroundfrom middle to both ends",
                                  94:"Overlay circularly, 7 colors with black backgroundfrom both ends to middle",
                                  95:"Overlay circularly, 7 colors with red backgroundfrom both ends to middle",
                                  96:"Overlay circularly, 7 colors with green backgroundfrom both ends to middle",
                                  97:"Overlay circularly, 7 colors with blue backgroundfrom both ends to middle",
                                  98:"Overlay circularly, 7 colors with yellow backgroundfrom both ends to middle",
                                  99:"Overlay circularly, 7 colors with purple backgroundfrom both ends to middle",
                                  100:"Overlay circularly, 7 colors with cyan backgroundfrom both ends to middle",
                                  101:"Overlay circularly, 7 colors with white backgroundfrom both ends to middle",
                                  102:"Overlay circularly, 7 colors with black backgroundfrom middle to both sides and return back",
                                  103:"Overlay circularly, 7 colors with red backgroundfrom middle to both sides and return back",
                                  104:"Overlay circularly, 7 colors with green backgroundfrom middle to both sides and return back",
                                  105:"Overlay circularly, 7 colors with blue backgroundfrom middle to both sides and return back",
                                  106:"Overlay circularly, 7 colors with yellow backgroundfrom middle to both sides and return back",
                                  107:"Overlay circularly, 7 colors with purple backgroundfrom middle to both sides and return back",
                                  108:"Overlay circularly, 7 colors with cyan backgroundfrom middle to both sides and return back",
                                  109:"Overlay circularly, 7 colors with white backgroundfrom middle to both sides and return back",
                                  110:"Fading out run circularly, 1point with blackbackground from start to end",
                                  111:"Fading out run circularly, 1point with redbackground from start to end",
                                  112:"Fading out run circularly, 1point with greenbackground from start to end",
                                  113:"Fading out run circularly, 1point with bluebackground from start to end",
                                  114:"Fading out run circularly, 1point with yellowbackground from start to end",
                                  115:"Fading out run circularly, 1point with purplebackground from start to end",
                                  116:"Fading out run circularly, 1point with cyanbackground from start to end",
                                  117:"Fading out run circularly, 1point with whitebackground from start to end",
                                  118:"Fading out run circularly, 1point with blackbackground from end to start",
                                  119:"Fading out run circularly, 1point with redbackground from end to start",
                                  120:"Fading out run circularly, 1point with greenbackground from end to start",
                                  121:"Fading out run circularly, 1point with bluebackground from end to start",
                                  122:"Fading out run circularly, 1point with yellowbackground from end to start",
                                  123:"Fading out run circularly, 1point with purplebackground from end to start",
                                  124:"Fading out run circularly, 1point with cyanbackground from end to start",
                                  125:"Fading out run circularly, 1point with whitebackground from end to start",
                                  126:"Fading out run circularly, 1point with blackbackground from start to end and return back",
                                  127:"Fading out run circularly, 1point with redbackground from start to end and return back",
                                  128:"Fading out run circularly, 1point with greenbackground from start to end and return back",
                                  129:"Fading out run circularly, 1point with bluebackground from start to end and return back",
                                  130:"Fading out run circularly, 1point with yellowbackground from start to end and return back",
                                  131:"Fading out run circularly, 1point with purplebackground from start to end and return back",
                                  132:"Fading out run circularly, 1point with cyanbackground from start to end and return back",
                                  133:"Fading out run circularly, 1point with whitebackground from start to end and return back",
                                  134:"Flows in olivary circularly, 7 colors with blackbackground from start to end",
                                  135:"Flows in olivary circularly, 7 colors with redbackground from start to end",
                                  136:"Flows in olivary circularly, 7 colors with greenbackground from start to end",
                                  137:"Flows in olivary circularly, 7 colors with bluebackground from start to end",
                                  138:"Flows in olivary circularly, 7 colors with yellowbackground from start to end",
                                  139:"Flows in olivary circularly, 7 colors with purplebackground from start to end",
                                  140:"Flows in olivary circularly, 7 colors with cyanbackground from start to end",
                                  141:"Flows in olivary circularly, 7 colors with whitebackground from start to end",
                                  142:"Flows in olivary circularly, 7 colors with blackbackground from end to start",
                                  143:"Flows in olivary circularly, 7 colors with redbackground from end to start",
                                  144:"Flows in olivary circularly, 7 colors with greenbackground from end to start",
                                  145:"Flows in olivary circularly, 7 colors with bluebackground from end to start",
                                  146:"Flows in olivary circularly, 7 colors with yellowbackground from end to start",
                                  147:"Flows in olivary circularly, 7 colors with purplebackground from end to start",
                                  148:"Flows in olivary circularly, 7 colors with cyanbackground from end to start",
                                  149:"Flows in olivary circularly, 7 colors with whitebackground from end to start",
                                  150:"Flows in olivary circularly, 7 colors with blackbackground from start to end and return back",
                                  151:"Flows in olivary circularly, 7 colors with redbackground from start to end and return back",
                                  152:"Flows in olivary circularly, 7 colors with greenbackground from start to end and return back",
                                  153:"Flows in olivary circularly, 7 colors with bluebackground from start to end and return back",
                                  154:"Flows in olivary circularly, 7 colors with yellowbackground from start to end and return back",
                                  155:"Flows in olivary circularly, 7 colors with purplebackground from start to end and return back",
                                  156:"Flows in olivary circularly, 7 colors with cyanbackground from start to end and return back",
                                  157:"Flows in olivary circularly, 7 colors with whitebackground from start to end and return back",
                                  158:"7 colors run circularly, each color in every 1 pointwith black background from start to end",
                                  159:"7 colors run circularly, each color in every 1 pointwith red background from start to end",
                                  160:"7 colors run circularly, each color in every 1 pointWith green background from start to end",
                                  161:"7 colors run circularly, each color in every 1 pointwith blue background from start to end",
                                  162:"7 colors run circularly, each color in every 1 pointwith yellow background from start to end",
                                  163:"7 colors run circularly, each color in every 1 pointwith purple background from start to end",
                                  164:"7 colors run circularly, each color in every 1 pointwith cyan background from start to end",
                                  165:"7 colors run circularly, each color in every 1 pointwith white background from start to end",
                                  166:"7 colors run circularly, each color in every 1 pointwith black background from end to start",
                                  167:"7 colors run circularly, each color in every 1 pointwith red background from end to start",
                                  168:"7 colors run circularly, each color in every 1 pointwith green background from end to start",
                                  169:"7 colors run circularly, each color in every 1 pointwith blue background from end to start",
                                  170:"7 colors run circularly, each color in every 1 pointwith yellow background from end to start",
                                  171:"7 colors run circularly, each color in every 1 pointwith purple background from end to start",
                                  172:"7 colors run circularly, each color in every 1 pointwith cyan background from end to start",
                                  173:"7 colors run circularly, each color in every 1 pointwith white background from end to start",
                                  174:"7 colors run circularly, each color in every 1 pointwith black background from start to end and return",
                                  175:"7 colors run circularly, each color in every 1 pointwith red background from start to end and return back",
                                  176:"7 colors run circularly, each color in every 1 pointwith green background from start to end and return",
                                  177:"7 colors run circularly, each color in every 1 pointwith blue background from start to end and return back",
                                  178:"7 colors run circularly, each color in every 1 pointWith yellow background from start to end and return",
                                  179:"7 colors run circularly, each color in every 1 pointwith purple background from start to end and return",
                                  180:"7 colors run circularly, each color in every 1 pointwith cyan background from start to end and return back",
                                  181:"7 colors run circularly, each color in every 1 pointwith white background from start to end and return",
                                  182:"7 colors run circularly, each color in multi pointswith red background from start to end",
                                  183:"7 colors run circularly, each color in multi pointswith green background from start to end",
                                  184:"7 colors run circularly, each color in multi pointsWith blue background from start to end",
                                  185:"7 colors run circularly, each color in multi pointsWith yellow background from start to end",
                                  186:"7 colors run circularly, each color in multi pointswith purple background from start to end",
                                  187:"7 colors run circularly, each color in multi pointswith cyan background from start to end",
                                  188:"7 colors run circularly, each color in multi pointswith white background from start to end",
                                  189:"7 colors run circularly, each color in multi pointswith red background from end to start",
                                  190:"7 colors run circularly, each color in multi pointswith green background from end to start",
                                  191:"7 colors run circularly, each color in multi pointswith blue background from end to start",
                                  192:"7 colors run circularly, each color in multi pointswith yellow background from end to start",
                                  193:"7 colors run circularly, each color in multi pointswith purple background from end to start",
                                  194:"7 colors run circularly, each color in multi pointsWith cyan background from end to start",
                                  195:"7 colors run circularly, each color in multi pointsWith white background from end to start",
                                  196:"7 colors run circularly, each color in multi pointswith red background from start to end and return back",
                                  197:"7 colors run circularly, each color in multi pointswith green background from start to end and return",
                                  198:"7 colors run circularly, each color in multi pointswith blue background from start to end and return back",
                                  199:"7 colors run circularly, each color in multi pointswith yellow background from start to end and return",
                                  200:"7 colors run circularly, each color in multi pointswith purple background from start to end and return",
                                  201:"7 colors run circularly, each color in multi pointswith cyan background from start to end and return back",
                                  202:"7 colors run circularly, each color in multi pointswith white background from start to end and return",
                                  203:"Fading out run circularly, 7 colors each in redfading from start to end",
                                  204:"Fading out run circularly, 7 colors each in greenfading from start to ena",
                                  205:"Fading out run circularly, 7 colors each in bluefading from start to end",
                                  206:"Fading out run circularly, 7 colors each in yellowfading from start to end",
                                  207:"Fading out run circularly, 7 colors each in purplefading from start to end",
                                  208:"Fading out run circularly, 7 colors each in cyanfading from start to end",
                                  209:"Fading out run circularly, 7 colors each in whitefading from start to end",
                                  210:"Fading out run circularly, 7 colors each in redfading from end to start",
                                  211:"Fading out run circularly, 7 colors each in greenfading from end to start",
                                  212:"Fading out run circularly, 7 colors each in bluefading from end to start",
                                  213:"Fading out run circularly, 7 colors each in yellowfading from end to start",
                                  214:"Fading out run circularly, 7 colors each in purplefading from end to start",
                                  215:"Fading out run circularly, 7 colors each in cyanfading from end to start",
                                  216:"Fading out run circularly, 7 colors each in whitefading from end to start",
                                  217:"Fading out run circularly, 7 colors each in redfading from start to end and return back",
                                  218:"Fading out run circularly, 7 colors each in greenfading from start to end and return back",
                                  219:"Fading out run circularly, 7 colors each in bluefading from start to end and return back",
                                  220:"Fading out run circularly, 7 colors each in yellowfading from start to end and return back",
                                  221:"Fading out run circularly, 7 colors each in purplefading from start to end and return back",
                                  222:"Fading out run circularly, 7 colors each in cyanfading from start to end and return back",
                                  223:"Fading out run circularly, 7 colors each in whitefading from start to end and return back",
                                  224:"7 colors each in red run circularly, multi pointsfrom start to end",
                                  225:"7 colors each in green run circularly, multi pointsfrom start to end",
                                  226:"7 colors each in blue run circularly, multi pointsfrom start to end",
                                  227:"7 colors each in yellow run circularly, multi pointsfrom start to end",
                                  228:"7 colors each in purple run circularly, multi pointsfrom start to end",
                                  229:"7 colors each in cyan run circularly, multi pointsfrom start to end",
                                  230:"7 colors each in white run circularly, multi pointsfrom start to end",
                                  231:"7 colors each in red run circularly, multi points fromend to start",
                                  232:"7 colors each in green run circularly, multi pointsfrom end to start",
                                  233:"7 colors each in blue run circularly, multi pointsfrom end to start",
                                  234:"7 colors each in yellow run circularly, multi pointsfrom end to start",
                                  235:"7 colors each in purple run circularly, multi pointsfrom end to start",
                                  236:"7 colors each in cyan run circularly, multi pointsfrom end to start",
                                  237:"7 colors each in white run circularly, multi pointsfrom end to start",
                                  238:"7 colors each in red run circularly, multi pointsfrom start to end and return back",
                                  239:"7 colors each in green run circularly, multi pointsfrom start to end and return back",
                                  240:"and return back7 colors each in blue runcircularly, multi points from start to end",
                                  241:"7 colors each in yellow run circularly, multi pointsfrom start to end and return back",
                                  242:"7 colors each in purple run circularly, multi pointsfrom start to end and return back",
                                  243:"7 colors each in cyan run circularly, multi pointsfrom start to end and return back",
                                  244:"7 colors each in white run circularly, multi pointsfrom start to end and return back",
                                  245:"Flows gradually and circularly, 6 colors with redbackground from start to end",
                                  246:"Flows gradually and circularly, 6 colors with greenbackground from start to end",
                                  247:"Flows gradually and circularly, 6 colors with bluebackground from start to end",
                                  248:"Flows gradually and circularly, 6 colors with yellowbackground from start to end",
                                  249:"Flows gradually and circularly, 6 colors with purplebackground from start to end",
                                  250:"Flows gradually and circularly, 6 colors with cyanbackground from start to end",
                                  251:"Flows gradually and circularly, 6 colors with whitebackground from start to end",
                                  252:"Flows gradually and circularly, 6 colors with redbackground from end to start",
                                  253:"Flows gradually and circularly, 6 colors with greenbackground from end to start",
                                  254:"Flows gradually and circularly, 6 colors with bluebackground from end to start",
                                  255:"Flows gradually and circularly, 6 colors with yellowbackground from end to start",
                                  256:"Flows gradually and circularly, 6 colors with purplebackground from end to start",
                                  257:"Flows gradually and circularly, 6 colors with cyanbackground from end to start",
                                  258:"Flows gradually and circularly, 6 colors with whitebackground from end to start",
                                  259:"Flows gradually and circularly, 6 colors with redbackground from start to end and return back",
                                  260:"Flows gradually and circularly, 6 colors with greenbackground from start to end and return back",
                                  261:"Flows gradually and circularly, 6 colors with bluebackground from start to end and return back",
                                  262:"Flows gradually and circularly, 6 colors with yellowbackground from start to end and return back",
                                  263:"Flows gradually and circularly, 6 colors with purplebackground from start to end and return back",
                                  264:"Flows gradually and circularly, 6 colors with cyanbackground from start to end and return back",
                                  265:"Flows gradually and circularly, 6 colors with whitebackground from start to end and return back",
                                  266:"7 colors run with black background from startto end",
                                  267:"7 colors run with red background from start to end",
                                  268:"7 colors run with green background from startto end",
                                  269:"7 colors run with blue background from startto end",
                                  270:"7 colors run with yellow background from startto end",
                                  271:"7 colors run with purple background from startto end",
                                  272:"7 colors run with cyan background from startto end",
                                  273:"7 colors run with white background from startto end",
                                  274:"7 colors run with black background from endto start",
                                  275:"7 colors run with red background from end to start",
                                  276:"7 colors run with green background from endto start",
                                  277:"7 colors run with blue background from endto start",
                                  278:"7 colors run with yellow background from endto start",
                                  279:"7 colors run with purple background from endto start",
                                  280:"7 colors run with cyan background from endto start",
                                  281:"7 colors run with white background from endto start",
                                  282:"7 colors run with black background from start toend and return back",
                                  283:"7 colors run with red background from start to endand return back",
                                  284:"7 colors run with green background from start toend and return back",
                                  285:"7 colors run with blue background from start toend and return back",
                                  286:"7 colors run with yellow background from start toend and return back",
                                  287:"7 colors run with purple background from start toend and return back",
                                  288:"7 colors run with cyan background from start toend and return back",
                                  289:"7 colors run with white background from start toend and return back",
                                  290:"7 colors run gradually + 7 colors run in olivary",
                                  291:"7 colors run gradually + 7 colors change quickly",
                                  292:"7 colors run gradually + 7 colors flash",
                                  293:"7 colors run in olivary + 7 colors change quickly",
                                  294:"7 colors run in olivary + 7 colors flash",
                                  295:"7 colors change quickly + 7 colors flash",
                                  296:"7 colors run gradually + 7 colors run in olivary + 7colors change quickly",
                                  297:"7 colors run gradually + 7 colors run in olivary + 7colors flash",
                                  298:"7 colors run gradually + 7 colors change quickly +7 colors flash",
                                  299:"7 colors run in olivary + 7 colors change quickly +7 colors flash",
                                  300:"7 colors run gradually + 7 colors run in olivary + 7 colors change quickly + 7 color flash"]



def on() {
    // Turn on the device

    logDebug "Sending on command" 
    byte[] data = [0x71, 0x23, 0x0F, 0xA3]
    sendCommand(data)
}

def off() {
    // Turn off the device

    logDebug "Sending off command" 
    byte[] data = [0x71, 0x24, 0x0F, 0xA4]
    sendCommand(data)
}

def setHue(hue){
    // Set the hue of a device ( 0 - 100 ) 

    logDebug "Sending hue change of ${hue}"

    settings.enableHueInDegrees ? (hue = clamp(hue, 0, 360)) : (hue = clamp(hue))
    setColor(hue: hue.toInteger())
}

def setSaturation(saturation){
    // Set the saturation of a device (0-100)

    logDebug "Saturation set to ${saturation}"
    setColor(saturation: clamp(saturation.toInteger()))
}

def setLevel(level, duration=0) {
    // Set the brightness of a device (0-100)

    logDebug "Level set to ${level}"

    device.currentValue("colorMode") == "RGB" ? setColor(level: clamp(level.toInteger())) : setColorTemperature(device.currentValue('colorTemperature'), clamp(level.toInteger()))
}

def setColor( parameters ){
    // Set RGB/W/W from HSL

    // initialize our own parameters in case something didn't get passed through
    def setParameters = [:]
    
    
    if(parameters.containsKey('setFromCT')){
        setColorMode("CT")   
    } else {
        setColorMode("RGB")
    }

    try{
        setParameters['hue'] = parameters.hue
        setParameters['hue'] = enableHueInDegrees ? setParameters['hue'] / 3.6 : setParameters['hue']
    } catch(noHue){
        setParameters['hue'] = device.currentValue('hue')
    }

    try{
        setParameters['saturation'] = parameters.saturation
    } catch(noSaturation){
        setParameters['saturation'] = device.currentValue('saturation')
    }

    try{
        setParameters['level'] = parameters.level
    } catch(noLevel){
        setParameters['level'] = device.currentValue('level')
    }  

    def rgbColors = ColorUtils.hsvToRGB([setParameters.hue.toFloat(), setParameters.saturation.toFloat(), setParameters.level.toFloat()])

    byte[] data = appendChecksum([0x31, rgbColors[0].toInteger(), rgbColors[1].toInteger(), rgbColors[2].toInteger(), 0x00, 0x00, 0x0F])

    sendCommand(data)
    powerOnWithChanges()
}

def setColorTemperature( setTemp = device.currentValue("colorTemperature"), level = device.currentValue("level"), duration = 0 ){
    // Adjust the color temperature of a device 

    sendEvent(name: "colorTemperature", value: colorTemperature)
    sendEvent(name: "level", value: level)
    setColorMode("CT")

    def temp = colorTemperature / 100;
    def red
    def green
    def blue

    if( temp <= 66 ){ 
        red = 255 
        green = temp
        green = 99.4708025861 * Math.log(green) - 161.1195681661

        if( temp <= 19){
            blue = 0
        } else {
            blue = temp-10
            blue = 138.5177312231 * Math.log(blue) - 305.0447927307
        }
    } else {
        red = temp - 60
        red = 329.698727446 * Math.pow(red, -0.1332047592)

        green = temp - 60
        green = 288.1221695283 * Math.pow(green, -0.0755148492 )

        blue = 255
    }

    def hsvColors = ColorUtils.rgbToHSV([red/2.55, green/2.55, blue/2.55])
    setColor([hue: hsvColors[0], saturation: hsvColors[1], level: level.toInteger(), setFromCT: true])
}

def setEffect(BigDecimal effectNumber){

    def effect = effectNumber.toInteger()
    setEffect(effect, 85)
}

def setEffect(String effect){

    logDebug "Setting effect by name ${effect}"
    try{
        def id = lightEffects.find{ it.value == effect }
        if (id) setEffect(id.key)
    } catch(effectNameError){
        logDescriptionText "Could not find effect with the name ${effect}"
    }
}

def setEffectSpeed(int speed){

    speed = clamp(speed, 1, 100)
    setEffect(null, speed)
}

def setEffect(int effectNumber = 1, int speed = 85){

    effectNumber = clamp(effectNumber, 1, 300)

    logDebug "Setting effect by number ${effectNumber}"
    
    def selectedEffect = lightEffects[effectNumber]
    logDescriptionText "Setting effect ${selectedEffect}"

    sendEvent(name:"effectName", value:selectedEffect)
    sendEvent(name: "effect", value: effectNumber)        

    setColorMode("FX")

    effectNumber += 99 // 0 - 299 are the effect values
    speed = clamp(speed, 1, 100)
    sendEvent( name: "effectSpeed", value: speed )

    effectNumber = effectNumber.toInteger()

    def byte[] setPresetByte = [(effectNumber & 0xFF), ((effectNumber >> 8) & 0xFF)]
    byte[] data = appendChecksum(  [ 0x61, setPresetByte[1], setPresetByte[0], speed, 0x0F ] )
    sendCommand( data ) 

    powerOnWithChanges()

}

def setNextEffect(){

    def currentEffect = device.currentValue('effect').toInteger()

    currentEffect++

    currentEffect > lightEffects.length ? (currentEffect = 1) : null
    
    setEffect(currentEffect, device.currentValue("effectSpeed").toInteger())
}

def setPreviousEffect(){

    def currentEffect = device.currentValue('effect').toInteger()

    currentEffect--

    currentEffect < 1 ? (currentEffect = lightEffects.length) : null

    setEffect(currentEffect, device.currentValue("effectSpeed").toInteger())
}


def void setColorMode(value) {
    // Set colormode of device, then fire setColor, setColorTemperature, or setEffect

    if(value == "RGB"){
        sendEvent(name: "colorMode", value: "RGB")
    } 
    else if(value == "CT" || value == "CCT"){
        sendEvent(name: "colorMode", value: "CT")
    }
    else if(value == "FX" || value == "EFFECTS"){
        sendEvent(name: "colorMode", value: "EFFECTS")
    }
}



def customEffectAdd(effectName, function){
    
    state.customEffects == null ? (state.customEffects = [:]) : null
    
    if(function.length() != 582){
        log.warn "It looks like your function wasn't copied in properly. Either there are extra characters in it, or it's been truncated prematurely. It's ${function.length()}/582 characters."
    } else {
        if(!state.customEffects.containsKey("${name}")){
            state.customEffects += ["${effectName}" : "${function}"]
            logDescriptionText "Stored effect: ${effectName} as effect number ${state.customEffects.size()}"  
        } else {

            log.warn "An effect with the name ${effectName} already exists. Remove this effect, or change your effect's name"
        }
    }
}

def customEffectRemove(String name){
    
    
    logDescriptionText "Removing ${name}"
    def stateArray = state.customEffects
    
    
    try {
        stateArray.remove(name)
        state.customEffects = stateArray
    } catch( removeError ){
        logDescriptionText "The effect ${name} cannot be removed because: ${removeError}"   
    }
    
    logDebug "Effects Array is now ${state.customEffects}"
}

def customEffectRemove( int effectNumber ){
    
    def effectName = state.customEffects[effectNumber + 1].key
    customEffectRemove(effectName)
    
}

def customEffectRun( int effectNumber ){
    
    def effectName = state.customEffects[effectNumber + 1].key

    customEffectRun( effectName )
    
}

def customEffectRun( String effectName ){

    try{
        def compiledArray = HexUtils.hexStringToByteArray(state.customEffects."${effectName}".value.toString())  

        byte[] data = appendChecksum(  [ *compiledArray ] )
        sendEvent(name: "effectName", value: "Custom") 
        sendEvent(name: "effect", value: 0)
        sendCommand( data ) 
        
    } catch(matchError){
        logDescriptionText "Could not find effect with name ${effectName}"
    }

}

// ------------------- Helper Functions ------------------------- //

def powerOnWithChanges( ){
    // If the device is off and light settings change, turn it on (if user settings apply)

    settings.enablePreStaging ? null : ( device.currentValue("switch") != "on" ? on() : null )
}

def hslToCT(){
    // Need to add
}

def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound}
        if(value > upperBound){ value = upperBound}
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound}
        if(value > lowerBound ){ value = lowerBound}
    }

    return value
}

def calculateChecksum( data ){
    // Totals an array of bytes

    int sum = 0;
    for(int d : data)
    sum += d;
    return sum & 255
}

def appendChecksum( data ){
    // Adds a checksum to an array

    data += calculateChecksum(data)
    return data 
}

def parse( response ) {
    // Parse data received back from this device

    state.noResponse = 0    

    def responseArray = HexUtils.hexStringToIntArray(response)  
    
    
    switch(responseArray.length) {
        
        
        case 4:
            logDebug( "Received power-status packet of ${response}" )
    
            if( responseArray[2] == 35 ){
                sendEvent(name: "switch", value: "on")
            }
            else{
                sendEvent(name: "switch", value: "off")
            }
        break

        case 14:
            logDebug( "Received general-status packet of ${response}" )
        
            
            byte effectPart1 = (byte) responseArray[3]
            byte effectPart2 = (byte) responseArray[4]
            int effectNumber = ((effectPart1 & 0xFF) << 8) | (effectPart2 & 0xFF)


            // 00 01 02 03 04 05 06 07 08 09 10 11 12 13
            // P1 DT PW FX FX SP RR GG BB ?? ?? ?? ?? CS
            //          |- 00 61 - Color
        
            // After Set Color
            // 81 A1 23 00 61 4D FF 00 00 03 03 00 0F 07
            
            // After Set Effect #1
            // 81 A1 23 64 00 55 FF 00 00 03 03 00 0F 12
        
            // After Set Effect #2
            // 81 A1 23 65 00 55 FF 00 00 03 03 00 0F 13
            
            // after effect 200
            // 81 A1 23 01 2B 55 FF 00 FF 03 03 00 0F D9
        
            // After set effect 300
            // 81 A1 23 01 8F 55 8D 00 00 03 03 00 0F CC
        
            // 81 A1 23 65 00 55 FF 00 00 03 03 00 0F 13
        
            // 81 A1 23 00 65 55 FF EE EE 03 03 00 0F EF
            // After set custom fn
            // 81 A1 23 64 00 55 FF 00 00 03 03 00 0F 12
        
            // from app
            // 81 A1 23 00 60 55 09 00 05 03 03 00 0F 1D
        
            if( responseArray[2] == 35 ){
                sendEvent(name: "switch", value: "on")
            }
            else{
                sendEvent(name: "switch", value: "off")
            }
            
            hsvMap = ColorUtils.rgbToHSV([responseArray[ 6 ], responseArray[ 7 ], responseArray[ 8 ]])

            // Always updated
        
            sendEvent(name: "level", value: hsvMap[2])
            sendEvent(name: "saturation", value: hsvMap[1])
            sendEvent(name: "hue", value: hsvMap[0])
            sendEvent(name: "effectSpeed", value: responseArray[5])
                      
            if( effectNumber == 97 ){
                // color
                setColorMode("RGB")
                
                // add color temperature reverse
                
            } else if(effectNumber == 96){
                // custom effect
                setColorMode("FX")
                sendEvent(name: "effectName", value: "Custom") 
                sendEvent(name: "effect", value: 0)

                
            } else {
                // preset effect 
                def presetEffect = effectNumber - 99
                sendEvent(name: "effect", value: presetEffect)
                sendEvent(name: "effectName", value: lightEffects[presetEffect])
                setColorMode("FX")
            }

                // parse different effects

        break

        case null:
            logDebug "Null response received from device"
        break

        default:
            logDebug "Received a response with an unexpected length of ${responseArray.length} containing ${response}"
        break
    }
}

private logDebug( debugText ){
    // If debugging is enabled in settings, pass text to the logs

    if( settings.logDebug ) { 
        log.debug "MagicHome (${settings.deviceIP}): ${debugText}"
    }
}

private logDescriptionText( descriptionText ){
    if( settings.logDescriptionText ) { 
        log.info "MagicHome (${settings.deviceIP}): ${descriptionText}"
    }
}

def sendCommand( data ) {
    // Sends commands to the device

    String stringBytes = HexUtils.byteArrayToHexString(data)
    logDebug "${data} was converted. Transmitting: ${stringBytes}"
    interfaces.rawSocket.sendMessage(stringBytes)
}

def refresh( ) {

    logDebug "Number of failed responses: ${state.noResponse}"
    state.noResponse++
        state.noResponse >= settings.reconnectPings ? ( initialize() ) : null // if a device hasn't responded after N attempts, reconnect
    byte[] data =  [0x81, 0x8A, 0x8B, 0x96 ]
    sendCommand(data)
}

def socketStatus( status ) { 
    logDescriptionText "A connection issue occurred."
    logDebug "socketStatus: ${status}"
    logDebug "Attempting to reconnect after ${clamp(settings.reconnectPings, 0, 10)-state.noResponse} more failed attempt(s)."
}

def poll() {
    refresh()
}

def updated(){
    initialize()
}

def connectDevice( data ){

    if(data.firstRun){
        logDebug "Stopping refresh loop. Starting connectDevice loop"
        unschedule() // remove the refresh loop
        schedule("0/${clamp(settings.refreshTime, 1, 59)} * * * * ? *", connectDevice, [data: [firstRun: false]])
    }

    interfaces.rawSocket.close()

    pauseExecution(1000)

    def tryWasGood = false
    try {
        logDebug "Opening Socket Connection."
        interfaces.rawSocket.connect(settings.deviceIP, settings.devicePort.toInteger(), byteInterface: true)
        pauseExecution(1000)
        logDescriptionText "Connection successfully established"
        tryWasGood = true

    } catch(e) {
        logDebug("Error attempting to establish socket connection to device.")
        logDebug("Next initialization attempt in ${settings.refreshTime} seconds.")
        settings.turnOffWhenDisconnected ? sendEvent(name: "switch", value: "off")  : null
        tryWasGood = false
    }

    if(tryWasGood){
        unschedule()
        logDebug "Stopping connectDevice loop. Starting refresh loop"
        schedule("0/${clamp(settings.refreshTime, 1, 59)} * * * * ? *", refresh)
        state.noResponse = 0
    }

}

def initialize() {
    // Establish a connection to the device
    device.currentValue("hue") == null ? sendEvent(name: "hue", value: 0) : null
    device.currentValue("saturation") == null ? sendEvent(name: "saturation", value: 100) : null
    device.currentValue("level") == null ? sendEvent(name: "level", value: 100) : null
    device.currentValue("switch") == null ? sendEvent(name: "switch", value: "off") : null
    device.currentValue("colorMode") == null ? setColorMode("RGB") : null
    device.currentValue("effect") == null ? sendEvent(name: "effect", value: 1) : null
    device.currentValue("effectName") == null ? sendEvent(name: "effectName", value: "Unset...") : null

    state.customEffects == null ? (state.customEffects = [:]) : null
    
    def lightEffectsJSON = new groovy.json.JsonBuilder( lightEffects )
    sendEvent( name:"lightEffects", value: lightEffectsJSON )

    logDebug "Initializing device."
    connectDevice([firstRun: true])
}

def installed(){
    state.noResponse = 0
}
