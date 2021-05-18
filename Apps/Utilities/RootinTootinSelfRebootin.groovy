/**
 *   Rootin' Tootin' Self-Rebootin' (0.97)
 *
 *   Author:
 *       Adam Kempenich
 *
 *   Documentation: https://community.hubitat.com/t/release-rootin-tootin-self-rebootin-hub-0-9/27863
 *
 *  Changelog:
 *    0.97 (Mar 22, 2020) - More @codahq changes
 *        - Fixed CRON another way
 *        - Added a way to reset the boot loop counter
 *        - Totally unnecessary styling changes (credit Adam for the cool artwork)
 *    0.96 (Mar 17, 2020) - Credit to @Codahq for the big changes here!
 *        - Fixed an issue with cookie being expired
 *        - Changed an issue with CRON that may have had an issue
 *        - Allowed multiple maintenance windows
 *        - Fixed the hardcoded IP
 *    0.95 (Feb 11 2020) - Thanks to @CodaHQ for the updates! 
 *        - Added ignore restart during maintenance window 
 *        - Consolidated the options to the very top of file
 *        - Added more targeted logging with configuration options
 *    0.94 (Jan 03 2020)
 *        - Updated timecheck
 *        - Added variablity for initialization delay
 *        - Added cookie/login support
 *        - Boot loop limiter resets automatically after a successful boot
 *    0.93 (Dec08 2019)
 *        - Fixed the initialization routine
 *    0.92 (Dec 03 2019)
 *        - Fixed boot loop limiter
 *        - Added startup delay
 *    0.91 (Nov 25 2019)
 *        - Added a boot loop limiter. Chuck Schwer's beautiful idea.
 *    0.90 (Nov 25 2019)
 *        - Initial Release
 */

//TODO: comment these better so people who don't read code can understand what to change
//OPTIONS
@Field String hubIP = "X.X.X.X"
@Field def maintenanceWindow = [2, 3] //mine always starts at 2:00 am and goes until 3:15 so this means 2 am and 3 am
@Field boolean hubRequiresPassword = true //change to false if authentication is turned off
@Field String username = "YOUR_USERNAME"
@Field String password = "YOUR_PASSWORD"
//END OPTIONS

/*****  DO NOT MODIFY BELOW THIS LINE UNLESS YOU KNOW WHAT YOU ARE DOING *****/
import groovy.transform.Field
import hubitat.device.HubAction

definition(
  name: "Rootin' Tootin' Self-Rebootin'",
  namespace: "adamkempenich",
  author: "Adam Kempenich",
  importURL: "https://raw.githubusercontent.com/adamkempenich/hubitat/master/Apps/Utilities/RootinTootinSelfRebootin.groovy",
  description: "Reboots your Hub if the slowdown is detected or the db is unreadable.",
  category: "Wild West",
  iconUrl: "",
  iconX2Url: ""
)

preferences {
  page(name: "mainPage", nextPage: null, uninstall: true, install: true) {
    section('<div align="center" onclick="doReset();">'
      + '<img alt="Wot in tarnation!" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAgUAAABmCAMAAABsvRLiAAADAFBMVEUAAQPb4O8LCAf8/fvZo3/broPv36v27c7Xon7x6chETF6Ql6nu2aTjw4/g1cDq0JvitYjWoXzr4cTn04Z7Z1/6rHVyVEHWnnbUm3To2JzIlW7Fj2z+x0f+yEj/yUr/xEP7ykz/y0vhvkP/w0H+xkXnwkfcuz/evEH2yUz9ykvlwUX/qDH5ykz+vT7jv0T+v0D/qzP/zE3+wkHtxUrhu0H+sjf0dRvxx0vYtjv/tTjcuD7/yEr1yUz5pzH5qTP/vjnrxEn/lCl4UAD7mSr/nSz+uTv/0FPow0j/wT36rzb6szn9jyfuw0f5oi70eh34mSr6kyi5mjHjymz/rjX/sDbauDj7hiP8iyX/zlD/uz3rwUf6yEnscxf3eR/6rDT6sTf/pC/yxUj/pjB/VQD5hjjOrjLtdhnVtDj0x0n4hyLmvkXxyEz8nCz+tzr0y035pTD+yUn/oC63li/4niz3x0r7lSmjdRLLrC/4iiN7UgDJqi35linuyEvxtTrtexpnSBthQRjRsDTrtDjGqCnzyEz/0k/9ny10TgDjvUL9oi5QMhP6gyLjsTTvxkv8yUn/miv7uj1KLhHTsjdcPBb/lyr2hCH6tztWNxT5jyb2gSD4fB/5fyHBnje9njL7vkD3tTr+gB/4jSTJpjr1fh/brCy9mjberzBEKRDNqDzov0X/oy+cdilsTBzZs0HUrz88JA7/oEP9wkSScCj7z0+ccBHFojlzUR3EpTP/1FqtiDLxcRj/zEX/4F3et0TjukX/yECBXSHSu1vDpCP4kif5jyMyHw3Prjp5Vx/3zU7/2lTvgByjgDL1vD2JZiSLURh/Thn/00j9tTklFgv3w0XOdyHjiSb/iCLltzvXqSereyjwlSrThSeWWRusbSHwiCL/uzi0jjOVZyJtQhVkPgKIXQSkYx6+eiXsbROSZwp6RRbuoDDxvUHXmDDllSzgwFy/jS/MkS/30V/trDe5Zhz+ojXdpDeuiB39lD5+TQnqyFvyz1L/yln/vlH/sEq8mUzNsEvu0DVZAAAAHHRSTlM7O0lIwMDAgcCBOzu/wIG/wsB/+Vj9arq506enGc5hFAAAIABJREFUeNrMmj9o41gex2dZmGFg2INMta1ANqhQY8MJV8KNmmsScLOtMYYEUriIkPGROCkmBDdxijRxIxwnIbYHjB2fYQOZKeQihCs902U5Fy6WDHuF2Wt27v1+74+eZDmJ5yY3+9W/p6en9+f3+/j3JCXPvkctLb1cTD8wBTKXMGuJ6YewXn6hXjyxvlnD31wvl6j7nyEDP668W1CDa6rPcuYfkDP4g4uWGAxY2cHnd1+ku5Un1N3K3b1aedLWv7le//iSU/CXFW/n/WIqjJ3s79nfnezEz9uZZh2ybPiajCddmnDwyqKtkDp3inauXM1njMxTKJ/J1+sf7lH9Qz1PCs273zDykqr5Kojs6RmVQTpvPFH//2el7l6/ohQsrRSJtRdTYZylGk+LPO/9hJw73Y0ITScOKTohzSzaEoXASKViX1+pFJJQnwsCuVJtZBrEWKH2UyCDS1AgiSMghLekYrDG/jxKZeKvXyAFr3MLQ7DznlAwzo4RA5v5a4pUbEQLi74vLspAESkwnoYCxKDRaMzBoA4U1PMNIwwBASAmEQBO96qehxsTZnEcsGAMllTqz4QAKqO8BgpevgNjE+3A2ifbDj8pip2fZupmuSbTog05NoSC7BwINiAYZAu2aEk0sxOsN9w0UEDMCMaLo9ghqIcyY1FlKAZGA4IB1Qdpo/tyvZGHWIAG43fFGAR57n5QLufZOdf2bJsdOQ8ekGAgCoQeTkFEx2ORo4hULDZv7IFKYo+qc+XF98++e/U55IXHyJ4KCsjP/71tF3N0kpjMoaALFEzthdthE4JkuK8nEQ3Q5R+CHLAUCRUEgkxMGJvGAkSAud92bbvfL/b7fRd2KLvv9osk3xbBgc8OqQAF/wcZDxnh7vl3QAGxtU02sLmNySKeY6LIzmmOUG45K2ky3SnnNjDpzJsSMFDkoDJRaTFYp+03L7pg5zgFT2KhJFKQb9Tnq5Gp4IzgQwCBgCKQc12XuJvopkO9X+vPyuUsUBLorPAVOq/ouvJgIf34xtDvx+A/QMHzzyFf5IKJXISn7NzORKKAvCpMp+x50Rl3ox8MSDDoloP1RlYdyM5xCui4YcUdPdXMEpepUZPAVSVOF3quKHSL01Rc3M4wqDToo8EcCBr9+ohgkOQdIKGAxAHiU9sVfq71hwe1kCA6sBF4Xpk+HoD3FUWPx3yqFf+IncZV5Cmiu6zr/Cqcam1PmxmQKMdq0Hr7t6owW8B6vDKMBc9b0FNJ9kwiQuWpHAyyjn90HJgVurj4K1DgTMq5xyjQLkBgoNGUsDT33JfrWZamLKhkMjlKZRADBKHBNrFvNH7Zb+ZGqVE8qVCUAALPO2UM1PrU5dcHH2vLsmqdG1+dmw6sXAODBxclYlSPldY72Bs8OGJdH3bUe0uwWBCm4FGuKu+MnWxITraLOKSz7DOBjAF5PHTGuYVVphREWssanBwKbW41r1uWvpgl6ZzQwBeFBtthAvawVn492D8ZjCAWKEm8gcwHnucCA+h/5vTCVqdWkFW72PP7drgvn+zd5OlLTzy+EAQ6rkJq5+TkQnlwwNb5ULu/EKOAmBqVE1uOn3tlhKI8K2MjO0PBeKNLECCJtMO/F210/ZcEZ2yz2nMeVEnCZBVbCdQPTXpwoNlVOiEg1jruyEHHozXYjJsWleYWbrYOB5qm+8WgFBZV2A1woGlWz+4uBIMKTgoMgF/P6z4RZP/LyWZxlCJBY3cXKCChoIoQcAQKy+j0rZsgBQWIB2J2uBiyR4QcfW3w2vS9R2AgesQTbACKlKVduxorA1KPDvcHKjuXK9AVPn5IlQabuixF9y2Euzil4DLKyb6qZIlQfiYYOPDFqDtOO5gmIBASkIFudzLGuWJcrLIqoYJybTot5sv3Nw4Q+BQEZQ62YsQuuGqqWWp19joUg9CQgzv5OmJQyTQ4B43Kp3/WG5Iq9U8fRpVRcpTcVSgFbe/U7fMoQBkgFDTDFMg81JoXlAZ8eHTdU49zwDDQHyNreKTK5+qpq2oP3mXeNvV7S+0yCsrVL5FBPxjLS5eGfuQAQEgzTiCFFPThFZs2l6mR6cMZF/IPNJOHD4dxNBTxtRAkE4QCVcowzdu9a1MqoOsixU+CIjZADCq+1z8NfSQwg1ytpEYQC3SgwGh7GAkAAcnTm53lwlwRCujDAn1sZCC02y3kQFCgzRVesoY9yx8RZMhjlZO6lFcaXPk2kk0IaTRqJAX5/Fx/BE89+OsAeJussGXTk40uDQdpJ8tIQAAQAVh3DHavYRTGkO9McqHmQo3keSjg46cLGyBQgKc0Ry2dH7QsXkSXrKJz0+jSDtHYTY5GGeLoBl22P13AEZJ4DhxUMqMRQIAUtIACgKAguZ08F1w/ggIGwpHgoNWiGDAIdL/ruui+zp1qXfSswIh8b4o79dBgda103jGlejTZXvScUVDNf5EyNXQkZwA8ToIB5YA5nwKQFSz06Td3I+VNx2l6WyFzfysGpWA34pcCFFiaKuWYrYNBieRAHt94gh5ELtPqKmBQERhUbi/8NO5gG41GQAGZRFMt77R/VKOTwXKBr9ebhXkULDMK8FGBc3AU4kAOBGpgAKqf3eyZWngArLA0OFUNXLdubkuBEWMZ3zKaRim4ww+cxNy40qNBk+JCPpyBn8Gm2TTnAJXOIgbAwYSA8CZNRAAgieyYBI702IYv6UbensK0QTWNiaoDq8iECSGp6Di+oAgFcSuQY6nDqxKMUaU7vqmalCmLYLC7O6IYoNe3pxcVCgDfAIIKxIJVbVWPX1IKOAYIAfHs1VVtmXqccoGXlnkhKRYIEHqUgzZw4GPAesU7rkkdtnRCgTorTRTX1Nkhmu1hKuou6X6k4NUdMXWLGJ1scAyrNTcnZUzFL55h4FAMgITuZMI+JI0n5IVhAr/97pRoY5J1/Egxjc3Unw+0lILHgt0ICKIoKN1slix1AZ2RYEAgYBhUKtuDi8qsIBasEunKZat96h7xN0T2814eNAfBzwWCAdzRWCBK13g86HEOcMrTosYojU1p9hLqgrLMzqD0QBkWC4xIpXx/t+YyMflXCIP0eINzgCjwI1KQpuHBny1IeOjHRGtQeSvURAs/sLBQQN4IxUZ2a4QC+qaosvdFdb1zaHGBDUpra2ulhLjuX7HYNVOlGAgKPl5EQjBaXT3TOAXk8fAo4PXzThiC4G+fvCOEs2r4cMCnhctLgsEqpQC7tk66RgfG+27Gm72S1P/AmFSWnSit0RtZttk6100rXFq2A6Pg1V0m9UUyUurPv12FMXiTnXQjhBTMKj15REM4Iaiz4wAKEsEsQoEpjFBaV9tHR8e9eGLNDN1qltY1uOYmE389gymBcrCNFGxT3/+bccExODMTmh5vtdq9W/jPqWUJhAIJDrDQlafuoaDWX4ZKrgs975SGg8s4HyTrthtPrPvdNq1EvOmWTCHLNKWrawncm5e942NXXy+JCwQM6wGxWPBlDBjWbz//YxYD5000B04UBelsTXkEBEkyJ8NPJKT1wZaSCGaVrpprzEaJ9db5xcHJ27f7m8NbYiBTur+01u40D96+fbvXHB7/9DeAYBsZ2P77x+F2WAjB2dlP7rW1quvuzebhycnJ4WYH5oXaMVlrZIUFdOxvsOKFI/heEAgEtzebB6SS/a1mp3cKHGA4IBhYibVQt/9Lqdm7to5lAbybZv6LB34ZwuBGBkFIIVI8b+FmukWdmWLBAhUuBgIO0ho/HMwTrFGhRtnCIFZCsgt5pBeDbVRFCo6rKUI6L07xwAvD22LhpXl7zj1Xtpx4ZjJHlnS/7HvvOT+de65tEqGlngQDtbUngsDnerwYtoCEVar14Y1BOFSlV4ucU8D+/QBHhV3fsn/E4A+okMcqfqM/y7zlf5kpAwMg06m/7p6f7UkXosFP+yQ85mvBvpz/dFThHZ7Q5eTtzvz4owssBwBBuXEAAkZBax+ChrbgCmgJi57u27P5fLRKLM0t6qUl+T3Nj+bz+c0kseLR5v6+jRwE/V6vh2dRrIeLD1B979VXQRn0rQd+88pp+lo/btpo5isbXzaXj7Yd2VshLiKiwOHH1VXYs1J/1WxO/NQyJy5FB7PZvHwsSTjsiA87cFVmaHVxaGS9lM/eKGuOqo5SLcT5DkJLj5TXY5BTUKngT2Z0VlgCClhh5Xkxk6OTyhNjgGGwWq/PX3CA2wKICgEGDBK7Bxk46/78tzesryPe3RH9dEe98zz7vmbrCgRJQGHpDCgwoEjgpyRkTt9VsAUYemkuqpkKa7+iZqPUtEGfvKHSSMyJkCmsTh3EerS5Rz/Q/tX3H1Ldfyj8SPXg9x/u2gDBxqs3Y6kUBqsjFhpEkd+L0c5o7AjkI14G7BxgBvMfiQigwCZH4Vz9ANgkvaSJGw2EZpX2fBYeMAxKxweGDY5gCKuHb4aLokwCXRHYpIxyYI/twC/RG7NhYgH1TCUSv3CPkl9JXRJTFVHwpfIn5aRSzRlgGBydvXAHRMJODkPw+Pfj1/QHmzSvhBAIXCT2EoCCcmtbCKLO9QT9pCAYqm+6WYspCSsU3xqovJWhLLVRZkgEk9CRl6a7Qcffbt/d/eUhvduTtsUoaHtIQTlczqszToG7APtFzPZgeJSbgctkgC8siHh9EEfoFxxyHUvLj2xwCphx7GiJH0MYzKuhOciM7Xxa+bCVbPz+jTZ6Py7Ira+r9FAojcCO4kGmkF0FtRqkQitXU66yvQR/dlA/FB1+qVxW8ItUVDemLlnmkgumWAZavaGSS3IE74gBlMl6fcDQ3cL1QG338eMx+2zeb6VCqeIQKpfYZ+PYK0KQC1Kg7LJKNov1IdladSx7p02oUxK9xNuqE8vNtjqR5LoXxGjnNgsM/JgSH3j+A1IAK4K3qTeDZVgSGjPA4AaN7MYaWh/tjj8SjcCtb19wQhFDAhumA75MgNkXvTBiPBAIdpRaTSDFdUfD8qrnqIWZwrC1fNjKUBsoQu71cPqhrtIUlUYcpm5hvuATt9D/kdQ+ky+4/FNSGd5OiQG0/5NRKkmfp62fDj7w3d9kgEFQLb+mQ9inoysoDNwwChQYIPB8q2PBNi2uCkXSlpSCKrwI6kyfQAk0Veamn9cxLch111rd8UjwAinYCw2ZL2jjiuD0Q8molhpzwAA4cEcTazXC3wNQhhDgMZkNc4EyFGgRpy7zCgwELYAdAGJAcmU39WRg43cHo5G+ZIEtDQ4Gq87MSbajAKxutLixDaIAc0op0IsQGIoUhNs5Gi8tbxQqpM/kC9gXIq+Vy+MpR+DzbBUmcaBpQZyGq9UjcdDN44I81+V5nmBFKJ9cqfi55d/p89iTZYlZfystOIECIfeRZXcR/ysdZiKrVQcW+HzWKL+O/UBQMJE1rZm6+yCgYPNLkm5eUPCB33qMgs39pu6gp5EYBmBqMPygv5gx48+4vMFj/mZ+lBcgEtAwTsl7IAkraxE5NrM/5yBamk4EGLijlRVJHk4VRt1iIx+HgaFgoqUOdaCgtRs5UJCxGthaBouxsVe31LLd3Nm11WoVdLe7C7kvOKx7z3tZ1LiEkACXgulskeq6rgVBHAeBpmtxkp6f4cJAcUC3uxcYnD27d7s/DwVvvy/vN3tlUQGHQBTFfKrjVV8n0TTL7GuOATPHWnG8jBWFtRMN3lx1exFTTJYmmWIYeTliUP+19x/uDO6e+YILTkF7s/mlqc068OzIpQZyMIOn3gzLaGrYbJTLDOQSHCilY8jOUVh9mqBbYMvDzVJnISOBYDNnMOlBoAAYjJJgVm6A2zP4VGG0NGxIEQW8Bk+kgCbRKQWTLH8HqWahS4pxQHbay1XDVoRvv/nivV4aAkLw7no6sUywfxpzgQRkkwfkoGhzOvL0Th6bDen1vZbAFZBqxKKMVz36K1fSj31nmGUqr+gYmv9e3JeOYi3GeJfNyZiKmErEeq22ue+vChRc7K0IO1/Q1OYqLkaSLHsNZuIgrBKnclWuVuGUqjLbwFfxGx25REjPq2lSYd4D15EgveFLQ86BHZkJlgwG+rIxIwq2wxZp2KKIFGR70wcKFDYPFSnIp8RVozdO9xXAP7NwpWQ9p0CGwwOvi+c2jTlmA55gGfQEQMFUMgMwvKabJjyHum7qOkAQ61+ng3+v10jC/hbhmZw/DA1UpVf45ILJsahEI5FpMDJ5SRgzPOLsxMt4oiu370HGk3h2CxqhSkVU55qDG0HWjO6ikiUhNsmGsIQqJCLW1Ou1zSbwn1NwUfQFF+QL9PkpahMDNBpYGhpgf1mSpTwGMfZWYmwHDYw08ea0PAxdzR/h0kAYsBfEh7jntAeOPsHdIlAg0tDhVHHYmOADz2tEhVOAe8pqQGTTpMALjh2todL88okq+aRFrhk6FePzN98SBSQlVHyJ0nnZnngSeYJ310+BGeh6HDpfjwD62ddVGAAP/QjixZbrnxEJB2W9/vHhxjCou9+TUuEqSzXmCEgHdBORAgJeHYdagx4GNrdsBNtEnslFycJE7cCbbHOocpLoWQAINsny7qKNB1GAR/viv3BDCv4Btw1R8J3IVyQWqhtpqArFeEXJXzu/y3YlaSLI4BOQhMicDF2KEYgDOKKlhjAMINqE3aInw0eIOfEw7NMO5hgFW1NzCliNeCoHuX/jmIydgi9Qdjc+bSVngHwBowD9LdN1rYYXboUa6p7feFkZPQHK9XQYB+HgabqTJyfRQogb32Fm3gx/XFuIwg4GSENR91PzRBRrrEP62Nq2NxmLpW23MhtUDYpRiIJOUd5PdE9lqUxMUiHbVowH1jDrPJOxH5+ews0x58U68gXL5O4CDU8UXDAO6AIU/JOlOAUdpZOTYHTSMNupWsE6/oJT2bliCEVEvo54kdWcUYgQbR0ChIc/IAWLngMUyOAKlOfDhjnOgIK9CYX6+JSmjxTsT9bRvRcaOCwip6C2Fbl2WCSqpeWAkTC9BgSuWfKvjIvb6e0Th4TtIa9bo6v/PXbXO4GIsDl6upXFP+zugFBM0HlGgel9z2ct68tsO+1xdIiCRXz6HbxptU9BvV6Hx5xRgEIUEAhEA1Kw9QXf75QHo1FTf/wKPf+fM/PpTVtbAvgXuB8hbR3dLLDkqjqrLh7b0i78AVyz8g5xperqiWUQXnA26CUCWcKbKJEiIUAGqtoKAgmQd8QirFg8dUfXqdTbStnQzZs5f4xNkpa8MTbnj31mzszPcwz+K5iHWbaSmOZJp9YqsmeEBAdXvtNKUxBfmwUKMhlJQfAUCrT9KCjco+CX0t1MclsZRbkdSbaMEAvMEMfF6QCmezmYFjffEY7JcVd/upzoFmMgmxSk4FqUg4G9oBlergYd+KGY3ZHABwqyVYq5QLZVs4UCUnATMgog8HVvWC6XkxxwCgCDA0YBvw5NqWY1oCCbNKoqNzxk5alZoIDlbgChOnBbJiwNKQ7wpyJfETgFrxNjBv48w+bFKEhNHylgwihITdbAXJDdS6qcgu/gZLgjwNEWFtDluqWzgoVFKLNIHCchyAW5PYSjwCQa8cyRmxyz8U4s1MME9Z5wvTzgogyKT3jdOuF5IHuPAk2UMxRSrcQg6DuXwc5kM4EfXkN/0HH6ib5qobACCuZftrmgnBKg4CVCwFcELRHwLKMgs4ejkYIsTyCZqb2umt0kB0hBhT0drt31dFo0TxIUgNnLa6Yj2KEggxRw7ZpZ2aGA5YL9KOC54I/v4GWLuVrsTHYK8BA1EsEPRmkCRsGjAESRLIiFAzs2phXL65SqHdUnKQMKzDWZhDAKZOWajt0O5eWgaK9p6lToD4Z+AHdVMHXO6La5hLlgVba9Og96fZeCH5wCmQtSowZIQVo+ZB4QOg8DCCVkomrG7C0CWBkEBx32RgLfM+DrhjNnMZt1daAgVqNxs1EZUpAaFSlAyQZ6ZWwkuzAXmNeZ/UTkAusxec2jxES/Ht1LAwE+J0KQR7xN7AyUSfT2cuwvw/k8XPrjq7fRJF4vRh90a4eDhB7rIS4sq1AoVLM7gQUKzJgC8Jfvzqgo9pZ0Z66aXhtjm5Z1vG1fqVRarW5Xd+1v5w9TcAEUvEIIbm6PgAIlNSRQcE/NYxTIjiAcBlV4NEpx4C47QEFnYIfdoqknIACzG2v6GAUOlSdVxmlDKFCg7QdBSVIALsY9LVbiCN+QCgKxsWCzgjHZnHneVRQZSQZAolZoO67D/9nDQtg6nkQiHYz0+9p+K9UCc42WEKQg2FaNwrxnGbxnUbECLSW5M7dPWZ8/1BTRiBisVqs/x85/ErngIkHBDc8FN2VBQWpMpEDbkYx2XzgFTF8Gnk6L8NxmSQ5gWRiMa+MOvpWa+vbUNC2EIGU2qxGggCZHRwqEHUhBSiPmgkDbS/IJCn4n+mZkxBkgkIXJtFdznNr8LQtw3JyLFg38c5nLcDjsVWzX9o7xLCNnjDb/BwWFUon5JvMgBRlsp2ZlGbAGo1+7ojIo7AqFwj3IYkgvG4Oty5CC85teyCC4kBQkNngueIc03NwcfLNnJBXlYO7T3bhfc2MyOxSQ+Jqiizd31uIc4AvKfmjjy8fBYHrWaGV1KwEBmD0XUzSAAkPbkwLMBcpjcc+kipICcDCX+4W44WRkoASwGcbPgBeiTcXBOLtDyAaB7AcMornDXjMhA7brMiIqNXsasUGMkVV6ijBrMHnv3mJAAUnd733I9jh5xQgrmbTLztpn3FGKNpwb8rI8UnA0bnytMwguym/iXCAo+MEouLmQFCREeSgXaI/kAiWOq+/ohgYPJJBfTbM4mxZbbQ//Xe73p8V5RdcKpZTZAmegwOnQhykgD+YCsl8uUMTT4er3gbjd5AwJQA6/c7hPPJff7rXxBOvBTx7lXDRuOLAK4HumYevMCyuuU+kNG95E9F8/DQMu2uMUKGyDlqvGmGFA+zWfbjFQaNdeykDQTmNBhYvy+dXq4M798vICpXxx8d6rlJtlUcHvH43P7+Dr5ub8PgXGnEMXNzyyAQVGfJ4xc5bEwCxUYhzo/UoPX0fjSymrU/OJkt+aXbSXRIkpuEqGGnES2smDuYA8bUUo5dHJ+XwediywMhfWDh+LpQJBArvn4RO9dUTatysbmugADFrL9WZsY9f87SQ67viwJLQXkoKcoCDPVcsKN0OozCcJ4Ibk0xBLCiQDmgJNtQ66RqFwgymU+V4hlE57PZ1ur2svDIrezedPnz+7s4flOrvvIe7vPzd+1LHQrDfZF1Dw6uKRXEDSFOwGP7UibKtA4bKEprGZKf2hMzDZu6muqWv/bnt5MA19rxLaB7Mly6To8lArBp/Wryh4Qi5QRS6QEc+nJFktfcgRDK8iAg2i/CTRpQsUVHBz+5EkgJ8RRZOOi109t7KJaDSZeUP/g0BFGf21o2wviV0N4VZiCqAoPugP6rtTynzUqtlnlgGqqdZdNIYzQ2GC86bjRq+DfZSs/uu3QxZ3Lm++fvz8DuL/vvntK2KAFLxM5gIWYGYC5gKfioCzuCjMMK6GnyWw47lANCj0ynVaOsH3YKXiuFEZFMxu99a81XWrdLgGs28PKZo2Q7MJzJsPSno9jSqKQftr1iIpUAQFfHJcC88FvJfbxH0nXKdJa+CQT1PwCymyBV0xxFHBA+SCtcshAAouI4LthPdBmV7DMwN7NIDlghDAAuJBjADPgLVjtUfU75PCPcxFBQpck/CiPCiGOre7GPBDOg0/OqHnLfxh2/VKlF/EziJ0MPxoLz3P83tt53P9ZTOmoHkxbI+b795/Dd1/OAVtRkGzDhQ4MyJ1s7HI0KeqUPyAqFIjHYZExTo7KCqdLdtu6C08f9ho+F0VnxT1Wwt+razypM/N9pjZq0M+cRyItj6GJo3MsevxUX2X8vGJbo9ZMTaGthyT7JiiCjepoir7Rn8gBSPIiylfn+ZP2SfPD1AvQSoghgg/pwEagIKKEPcqYmdsQYkgFQAhsNu9gGInxRMIu5ZEllQpVJ1K1aJ4KrZE26lyymeKM2HOXPdgqupWsIOaPb/EfE7JdBHWGg3Xb+mUqPwq9lFVqvYX80aj4fjfyn9uMwEmg7te23Udd3j3hlHxw/78Hr4gFxy14M7kerg3VbJcGNwgNR5eVWM9iuyjS5+oSUMpmY2Zact1kaj4Y1UIFP91cMdMc798Kz87QjDQSTB1QhYN+EXm2GvCRqNez+DjE32+psImrpKeDU0i3CQNjquKpIXVTyUFpywU8TFVBBPU2w0sVYZCFAwmWINFCHV0xgMNH3cQEdYGR4LHw8m4xhiA3ZlBZyw4DOSE1X1VSfVSt2wWXUlXqocwmvD61v8q6D7kJcyrXIw4BsJTrA/OfP7sxat68yKFQfOfL+GXr803Ijk0WXe9fv7p4PnfpzKwTED/4bZBUZI8CpMUcR5JwqpsTTMO+eMYh+ATfD59Ojo6enH04sWLo6Pz808oOHkchs4WS79VAqRxXDF9JTZkO0fep6S8paZramy0oOAA/fsMNvUZaOPuZtU4GoGBExEIEJb5oR5NZS6w7WMqYsy64BjB7weZKCQiCr/yf8SdwWsbSRaH78Mu8w90q7dRHUYMjSKxPniFtCftWIKc7IO1XmxyWXLRqmHn6lgEBIMwQUIg3SQYCBqwZ01nmDChYe2LMXLsHTs6BA+7STSHPdgONgGdZg77Xr2q6m6rLTmKw75uVVdXl/pV1fv6V6VOSBY5Bbbwwx3p0lVMJfIyfmLq+qNAT8xYiJlm4MQMrRSLwU01x2m3a80gBcCBNWdlg0VNpMDR9EexqcwM8R7TYzb2TVtynD4PNkDQ9lkNNjS4yDTecXOxAhNrIuS25g1cXmNSCzQ+1mg2DbtOZzJ9dO9PMfEYy2eZ5xd7hxTpzepGj0o5JzzX26iueRTElBTEErzG4pKGtwYUbO7CFi2QXrGUOOCXH1EJjp5tez2w+T6d6QiB065xKcjn8x2+c+vQSScvi3FZABQwPczb+7WAmk+d1MAdDAOZAAAgAElEQVTgwUcI+m1JQA1aBGSiHSB8XA+YpvMvRm7s7catSgkKmD7etPuVWMKk6P3OU/VYDMSgwcMMUhCjUn6ANNZ7IykoN/5c4ZpFBHASYpX7TkqfxuzbMegVExA0KdorecXAiDU5BYzH4sO8yuCL+JMEOCr+AoCO2HAnDpim3Vrfr7QJKfhkIgV2fzHBhTUhthimCRBb+K3YqLZa9fK3Paxgigqwm0hIWUjBTo9/X9wBtAouV+4zeyoKHt0WBEgBh4AQWMlfax1JwYcFwo8Akwi0Han+zRopAES/0+RKBBsuS2qkQx+LAu2H33zio0BD3ESWTmjXzypikiEAxM4PvX8+2dvb+KLnm4XEtGX2dlAMymvl+te9yIgOJb7rkztNfHTfUXkOO+VNT8GeSsls0Kg8JSqNhaBNEOTzgoIVX54fRR4pCAZi1Kv0nLriVOfx17SrEiAVwJMAjL+YiJQeIQc1QpB7D9w7JRzaOpWKg69xKdGalK4GK3UdBdpY08++o8hFYhEzlsIDT7Ekgq8BehUVZaiAl01chH/R4BjUD3uJ0dkoUjnTpjL9FkzTaDogCCDSYyyHU4XUgmncq5YzL/6OWv/55gDfVBSUJsng7XR+pH2CAiMwzEykMAsyOutX5KImZpsxk0fZpIWOHQGhj9BFSqkCnJi9b9fgZ2/9SaVi82u2Vw0KkAJGbjzPgTyjFshWiKbxp0rolacfvqx+ZaxGSzEgAgIccR5rvq+IVJzlOpTpiDjQ83htsJUnzR972ZUQARBrwI4v+l5jYMvxVuRJDfAHpKaFqLZ3DLRAC46DFjY0vIhJCthYc5YS7zPPKM3Se397/Pz5P3qVsPksleiz6Uz7YOMhwUBICHIQbwx5jtIVdaA4rOS9MLy3J+/5HwVABb+jlIdvfBdtQPd58RNF+xh2Mwq0s/uJKZdDMFn0enbookZf/NL5v1HgCAiaggGKeC5HebIVOsVrHRkF9p7xH1GAtqcAebkO8ZpA7oR3mVnJ4ZxQq01HgR7Rb0bBp5O0oP+lOeXqlFQynBDz0ZQUfBgH+F6OB4WWBCsy6NcbSoGkYIJrpoKPTq6Lf9On//LZn9iCmzVgBALtTJuEgUFa8EvSwBeWjH/wzaVBqbT+UuQjLEoiS226PeObwcSJKFItoJepXlEIFLprwqbfiCC4g2a021bNYyA/Ozs7My4IUpENWqqMv7sBj78h5ObqFOCb+wOBX54AInyrRgsTz71mu9rE9tgXT89sFl5Nlhg/fIpagBSMs4P75kegwFw6M6Yxes/u64zr9l8Oh8OX/SP3RhA4w4dxIQQ4wjMzp+fn56c8Fsu5d5R62ePXr/84NxfddsHsiQTw5iWTyeHDaMgSQP4YyQUJWKb0nfIpN1mCICoxUAFeX9e1Sd3Vv9m9mDQqxi+SAoc2xyAhMzBjUIkDFET8sqL7UpXjy1ZN952NilMwjSwdeA4dQ6VKSkWRYfiuqRMSB+zn0fBwbevFixfdtY2XPj24qhmGVOnoQXcQ5SsCGPvl3Ozbndbui93WxcJymOVOBtKenAwfuuDBYPJPe0invBYjAfF4u51cbQyingAce4t/GXe0jPBAaSYnPGZytOVUE2BHCqx4UsgRmvu4u/XGlt1UEmlQ22SqsbVD1ytn/mqkrtBkqQVjDSiww2cU2/6QGRooSE5lNOz06OntJ/Xq4OLl+vrlYb27wbSxMmJglA7qgzkICw3zwiV8/fzt+cn5fCgEC+Xdal1atb72pu3KmcrhwkJgYrPiaCgAltUpD+IQ/+NjNQP4w+8zCLdKgxD4SjgF+WyznfRE23H3dnc3HSb7dV2X9Seb+vgRMZJCC+JJ3JL8E0LB6r2waDM7YZoVc+qlmr20Gh0f7fhIRmIgGID59+Bp91kbnlDd1d2Dje7eASMVc0LnEgyV1akPLMFAZv5t9enpzMLCwvw8jDZsYDyhDFBS3nldsOJRA2ae1fNnT7da37t8zB2l/jL+liVngMJqeVDwhV9FH++9rG4OiTrJyEvLyrmvJmJQGJxHk0I7cXFzsbX1TFNNuRpcae5Ji8MaUk+OkaTAio+1dl8XL27oVRK9y9HN9Kuf/u5U6I2OrECJet8kv0UvhNQSH3edHSTj0xliwcefJffqQ5fRmcHcYX0vygXGkbwEsvBVq1YACrIYFhzh+R+rp/OZ6w0pmD1uZi2Q46ihuWxY3roADCSe3Cw0NQGAABRz5UFxxRd+Fe7pDAQCKFi7NOJeXwy2vm4Y3rkv5/geGPdNA2s5yUBNJ+k4qm48QMG1LHT6YcvvSuYv+/sP9vOVaX/usRr3GeLW8h+ssEtiONyLrUvXU5SoO3zDrlUWHq9as5irD4oiKvNv6/9ZyEymoFazyGfUjR9uDZnXHA+A42M1/89kyoMZEf9bMRCDmezaZTTuU8Uo245Onj3dk6csOl5vLUFBwRpn8WbfDomi+dODV/uv9h80E7RKc90jMBd+vhg3osDt16xpTZAQNRo7LOoXCYPxcCeFZPDQqSzG6zh/nKkPZnFo0+n0wn93395JBywjN9yJgiKszBSLrNPYM+KiHTUfAN7sv5xLlwccrjT/qDvLlAplklGJLBDeKU8Y5PLHjcuoNaqKsrPy4VBqKZp7OGCBSqP6KynIFoCDAiQFzkOh4GGB2WbfvZ6C/X1mGwbE/7Ov8B/z+uozRCH0Z/2VNbt7UKDbWwWx07n0Lq74WlKQDUIOcECMYf2S3XAWwa9mm8f4oNYHMyIwdwat0uc8V/I4KHmnnIJisVazPAy2T7rDOYh/FieAIgLgzf/i4V8+bQzm02OsdMVfKbwSr1fiNOZmcmEUTJ47Ny/YpIHxKBhn2YMQCoyjNFIAYnC3Yhz9YWNns8X/0eDNneff3+MgTMDAXC1MaxKM5EX1XZQmFSERPBennNj4OX9wEQII1Wl9sJAugQEF//q5RNmSSinLc/AsLjR2itmsp1qwUjzvPpvj878AwBd+CRdQcEfcS0RS3lKdeW58eX+h16o0UnD8rgUUUOcsS3ZSdpvysoCeETwY52urUVlieVfFkVuRU/DbX6Gf46yw6oasuN3aPhqoQfLo960t/B8DG41Wq1rvdhs73xwdsQlvf9yH2WmtADu2f3ujWovfbA4BluG5XcnBQg0omBfD/PmPP5eusVOOAVKAHmsIX428d7ob8EtzZiYs/hS3z08b/+YUlO7iflfkAund0jjzX+WalJsFCpLvP3fuPdueNDJZ0oJfZ4vKssVgPgtJ9l0sJIqM7dOU8Ndc72KrUW4gAy38i9IN+IW9+fjoaCwEzO2MOOTe/K4DF321kASw6Em1M56CeHR7ezs6BwwABDMrELF0+rQ7uHOXDCiAIAXtlA5QjBigFgiPiNH/OLea17a1LN71QJl/YKAwzk4TIgJeaBFnEQuUQvADe2MIrkaL8hY2xpiuMnW8MliLcYVDtZsyA4OK+qFdYPDCgWDCZBorycQMli13bIhL/Wxi3GTXB3POvVfyR9L0vflZH1f36+jq/O65PyktQjBOUlMCrIWiAJ5n05i0VIAFHOmE4wFrrD/oOT3w+sYSTknfaXq2jNAApEbqRh2vxL+j4KQAjnaaId10X9w3M4ACL8TUVxoLhDOG1JmfSpGNpjeX7prYlU6OBINc5PQ/ukG+qpAPLCrGBM3Yf3cfDVYrPz0RfKPMUspL0HTKz2T3M5MgjpGuVFiiqZ5hP6ZwKKTq85urq6vhRFoWIXqD39BXUxakFWCBQt3i+37qDfBAKFo+8J8OBACY/8GEdSIQ/xPlEN0a9QEjhZt6tEhZUOSzLSxzOGWBaWl+QEpGWb44ZxGg8A4t2+LTlAVplAUisEBa0En+ChlYwUcQeImjHU+qEs3F8OU9FHb2VlRvrUduC18f/ubBg4dfg8hwtjO+z6aExx8q9KvVzB8cVh9VHMICiAi/O32/v//m76/fv3v/t/+e5DUD/++Eob0+vdUKL1YpC55vTo0QKzOX03tYzDwL+jSFeDAxmoE4XSFgSHGyWMRZpBCXV24OdOP8/NzSD4ZxYRMDAT5Xh7EgC0+83ssWvcefJXvWP6ML1qJlG2J/EODpv+jIcmM09MOsHXTLWu38vKb1+kWFdUJYoEBE6JYNLFP3W0rR65+Qr1HXSYlWvhwoU8uwpZWWTW4aytwsYSiGgkRQHCIL5vUaGbcoLk+6k+W49LKZJ7eiHtxIcZEUE0ezusTnce/pxFkS5hOw4MGDB7/9Ggt+B88rd3xSZLHgOHe8hO8I+JZI3hYr7/61izww8n+45xNx5VXw/4bg8UA6USfSN8TD8stuTe+OX716NXyjW29iUcqBLLCgzWXhpNUQFjnWzvtylmHLT+CkLdsx/HsOC/+AaN9qcYw4ct/QbHfkOCO3Z711vC6KDrBA6at6+yLiOBd1rdbOFrPT/ruWWu87jtNwbUPry75BaJmt16CV4wwa7r6ltjiiToLd2l3YXWYjnWhXkjQu6yc42vGBZYylX6SvaHQNEhY8/BqNxWL4GRUecIwkyQmPNCe6+efb/nwEuoCxYHVpNRAIkB0O1dPTH/9RNnRgQfWbJKj+OIl5Rug56B99u0F6J9MS7+aQCkiE+ETNT6Q4HdKCgny8bzRFWC9QSQabxkkUQzZxkdFW8In3XdfN6y7FZWPqpilAOZRtPuTJPzL9lZ6eJR1lC+alZY84uVgoFGW5VS6PzCkLOFd1I4oJRabitGv1QoH1WcjaVttRTGiVkZXGZ8s1fXuy0zMus1BWhFZcq2f1eXxFiA2bzWbX6DbnUNY9X0/UcbWpdh/j6hiXpJtdayj9UgrAc2Us4IEGUeYNPxGN4kYT6X/f9mflBSXBcS63srTwPap6+scTTX99CnriGyxY+kvI652co9QgvQHvFGU7rUXhFccID+JDVR2LkniHgjwyhiT/TICBpsaay9E5l/RYUJQVrt6TFUWRZUUu+K6IzNJAKducr/swknBuzWVz3nRrl2bGq2o6+c9J5mhgQT/fMr0+M+blue/rTN26mFozM91a3/SMFmytpRR828W61YquEVUQl27UYVWaIlA90CU61vhEv+rnh1KcjX39ib77DWE9C8aBWNRnAdW5uEW9NDl5l69uLwmV7DaEgVwOY8EtXwcqp7//0z+ryy+WKnd/uDyd8KR738I0SVPkDhb3KN4irUdDl/BxcFR7eyXE46l5TRFvWmMphWoDakE8j7naCGd7JIIskCMUGTsfSUYi2cgc/EukStmWifs9p/VrdjJJahQaRttMTpsVG0Y/Q1JJp5zvNcxpUTLZ0xxaFTpwZ1slM5+NRpiaM10giLcY4StMuqcOePzLZjCYGmrjOOokARWUIAjigb5Ol0ZxoO/vDtd9CSWs9Gtj8ew25kSWQBZXIAHPWEDeZxAJ/naKYHAqzbtRqq4CAXBNyOU6i1+qH1UrlWogGOnknh4/uZMGgWqaX0SC/xVZCeQC8kBMDY809WAYWhcF+ngQqYR28FFgA91MrK0pW+VL6qINYEGGuSAMLNiI3INsQbf9eZsxM436ec8Je41VL8ky2qw3YIHRyswVtSyXZGxE1F5yzmR4pNYzhHsFx7BNnwL4dsCPjMsgCNIgZYFIxhekQxSP1DgdcGqgqiB/yQXKZyE1UQ/WseYZq4tFOGegLX1CVF/FyGzjKAt+VjgAz4XIjqc1nqMIsS39oUpoIK1K9FitdLaJLsgdb0cqrAR+gUcgEVdfbnWOn25vP83ltp8uV7HQK2fJynN4Kab9MwshatAzzIV4mgUZfMgvCdGrEOFtIorxQEjFR823htodi6Lg6cePTetGZCOlrwayW6Yu+lUsiIR12zQzBOEL17Zq7QjzfLJhEF7tTOs2tFaYskC3M/P9lNTeBloKX1gX4fmiTJvFCfOL1fJZQD8aBu1yKEaGSVgwq5BTR+pHOl5xoHbXhdkycb8sCt+R2LjMAgV4TvmZskDmOIVDKuB+jZvChZQQJjmaez2pMBcSl65Uq53tHMP2GTJEklYDEAOqy4ls53ibMICWblWo56dhBLZKSOaUNWItpCjEIMfBdZociWW8I6iihDCxRmpfcxyrCtkcDQgoEVIfgwN4TcpfeSQPim93BaZsCQlgIre0RmkHgCwI71CU7PzOxs59KJVrmoHQVKNmWXar5NUPu7XGQtuN8mWYmtB9E17ttkpqh239cKGVFyeyZq8HGnKGAyE+BrFdiG3iGIEFqbkXpSOVsUIYqK445+H1EzUk3MsAwoEEmV8yYwEoXIJrRb6WUS4pMv5k1E2Qg6nIT7PSpBrofPJI8MmpIAOAAIEXiQiGAJ8fhAWHsw299h+yCvZOTSnXxNw1MUVsUuvX8IMNEjLTcAotoq0wPhCRgGOCCDk6sfYHAlWOA/XNepDwAPiuyJlwONxRvxAWPJtnweGzqRNLYQ+lvZ1DxgK1jmjrNfuy3wiX/Np7dhmq7e3RnRxLtr1HTej10oKrv9RakPXsULfB+CHrfWcHQslG2CBLQsHR6BfHIvlqzCVCwPJQzRWoCAIWCLEZCLaainmjdVOxubI36iAYuwcYBjC4ogOKjAUZs3iNG+xFk6Vx8w/mdSFUmXGidDyNBJ0qoCI9yf6VMOBpbh6ftpAFK9L0AKfKVobZgYNn0zRnbRPL5Ibofsem4LrhC9qoIAzL6kggyaHRHFCMEA1AS3VLh4BnHWDBIcEPe8iCQ4a9lu7/2zLjYo/VAK8RdHq9UvjZD15laFb+3FjE5x6pgSzYO5zDXuscu9z7H6dm85rIlgXw1du//6GXEigG8qAX6VDowjdFk4IK1iY0jgtBcAiFCEJAXBVoLYxYITYNzUgWQm2qV2/TCxdpN6HRTlpr4UcaBBXDa3iMDbN6MOece299mMTHzLGse+qe+1V1fnXvPSajxgJHITg4Igrmc9hwFpxJZxlj0wBOiLToaVqlzwIkuKXJSy0kQAG/3n9ozCIm7eWs9nCs7RBo+lXsIJPGZ25wChLGU2Kyk8m00GQAEKyCd/384/mvsA88WT0CACS7ukm+2NuaCfa6n/PRjkzj/5Af1WqGbxE02jFqLw+azQPUj//o3T2WmS0oqAvfAAW+Y5VRfyBkPeFwyJdrmRUd1dbFYuBWZVx7oou1ShTcP6JAHt1tkIKhuyHL2fURx+AobvQvj2ALmsGfo05PM6c0zb2ijbCmDebkM2l/0pns8xsl8veBAnZ9DBTs+9tnzEIKqBoLqkS1sA7TQJpteEyi4Oc/AXUzKsG1wdU6hAnkzr0X3RAEGCg8NQUAACcARvk62YVae5FP8uND4pneInkJ+gT2xFZJw6hizB/zQxxp/3d39hLTpbv8/Y8Jk6GQe7UFogIFdouJvK601JYQFeZ0LrbcumYlLtcKLzvsTOVQ4ftOf/hIRrwLoKAVEZ+CHp9lxHIQj+eNae3IMV/Hlu4ohgQc8H0x+WzQZG47BgqOpcCx0vGgxl3vU6C94VHUPlKwFVVpDCvKwKWgWq3i+2cmEn/+TBTUE3V+CMnTNWbkEzw7/rl7yKaC9irq7Wz2MQGr1cnN9cEt7CCSycPtXcHfi7T21nmXiXwC+8LD75YPJp/wVbIGo8QLuImqwzYInATtt8oBcA4UPLzUxJ4AnGrLIEq71Wq3W0gBaK02UtDEPFRRwNgGEZ5rCwoo55O96c1kKoxFlHt3UZe3RSG7AhTIVKot2lGAAhlRYhSoKlEACICkp7VC+nVVIgpoMWDBEIKg9ZtSlALpFR3AR+2Y3zT+31wknA4oCGLtVyLB5k+dKgFAj5NTQO9AnJyPSb7OrzHLZjmQd/0FHXrYPVih17PlLHqfAcA0zKIpYJW9GZ/e4n4hiVXeY7X3h3BQ0v3bETRLzeeDzuy8nRedxv3RhLAM5ebrfJwwNRhV3CyK+FKbuQ9wm9rEnUgw5VXNfPxMvSbfCmndu1OZ6wpS0N4hLQUo+MT0T8rUHcpBM42p8kwtoiCaJcO+ACiAFWFoq8RAHAUhMNP9WgFeTaRAyvCbyfDQWRpUWPCsAQUaD+W4paZxDSjQcAphQRXkHM8a8BjoIhOuwwjDtQAgEC+9zSj4INtx2OLaRTsQX4/7WeqbL4ewHiSzJwgAMpAVUg6l5ZujX8/pr/rJPSh/uEcM4ClJHMB6INvRnsIdPx4BU4v1Jww4j+Av9T4I2vLuOzxJbdRZSmEISoHoSAFTkYKSHrKVIiXxUqmtdZGnKIPORKF8aKbUHCAFT1XUgQLFNzAwhkSBPOlApFJkDOSJAbOaGVRMiH6kiTskCjLkNz4rzNeMAgkowJlceDagIAMULLQYr4ErSkabNcYSy4AvC7R5oxnOAL5J7CHKHzgFIEXZhq//CS54AnNq6/MXeJMLK+7vsp8EGpxuPyIBh1z4NMABgM/ex38r0KbtN4892UU58rExn/fLi8jBgPwUgIAZAZ4jckBhp8QoiD3UFlKVQQAzcsSzQEFfFvo2BY9EBwqCi1zl8kYJ6irPVbqv9ZUITG150RnBNlOGGSQRMGCar5GC5joNu/XY985Mgrs4pWD4B31imUafkUEUUD4cp5gxaGicGaCAmXh0nZGAgpjfSGCoQk3YD5gmrZI2X8Y4BegWWZGLCsMBE4Wvc1yDkyqr91+7hy9uTrK75KT87QV6H6eAvRADdML14GuLNch7YJ2yjlShbg1AlcWYVBwmt1Lpoh3PJzDEwJgn7UgzdxwDJmLNeQx4jxdVYiAXEv3G7StMLXnrZk7P7ZAUUOD51Ut6uTFP8QreonfxTF39ptYXJkZBy54327KqFu1KJcEZ4HvcqlNwFxJExRmnNpAwdoPg5wfFzU41Nu7NJFJjk84wRuF0lWzp2KBBpREfoAADbrJijrRojDOsBZbHwvBqGjQDNwS4UyqKvYyggERVhHCtJTRV5JlfurcnuymA2JDPBHv8G5EX3bGyJWqkCzVQw2NRuUXdypWLxTg90CqJNGgW0pDGFo3vDs0E7SgD4NiAghxQcJHaRUEOKAgX8IZu30sJdfMMBSmgwMsFGMBUABNQvagWz4xpZ2TwpYBBUI1hhEiDZ/uDsADVI7JViYKQxckMGvw6AxTEotWQguqTAjvqPLwdgID/iDkFunC6f95WuaQevrxZ7YYAdoffkIL3/Mu2hfw4THbHetBy61EXLUX3e945Et8A71eccYALQ6Ezpbt3xu7MYRCAIy7CghToXCcKLnZIzkMKwteb3sLLMXU+Tz1dmShg5S8AA5gK5Cn9dHgWT4zwT4oEAS5lGO+m55emQ96c9JZRbzpGc+5wVwMFEcemfQocTsFr3wYvwTj9CACwwwIEDAAEwWum6ETBTx90va23dB2+eNZJC1Kut/GUe7hdnfwVBbj681f/fWQiOE/+48hjLW515Oe0wp22Qv23twpSAhj4HCAIsVlvmKFXzOl3RnWCQLjGsrgjyy53Ebhr3Xyb20XBRaq2tiIZ3rS3YdUBiI23VZp3dVObe6IaTAYwFXTm9fgZ/Tiw7oxpMiiYZgFHGlvezTL8949KreCEfxGB+1kym5lGCsI2oECYgIKIycwABc7WDzAFwA77FQzovnz4iShIiWumwIaJnyCjxHJYqusX97/8xb5gdd89fFq+dbsFS/ebChI6UrzLkhhJiY2n5I+2JArykvfNKd6M4nOQHrlrx2Tz7bgxV2WC4C2JVx4yxUIKmPoWKLji9mfEAgqiGdbaHXq8ttC46e3whndx2Zl6WI9hoChrd1RnoaExcucmWxNoPgAXzvNwjcOuTnoDx2AGCiEn7sAg5wEiE3eZZsXIahqDRpos0GRjkcaXgCrhCSngfeT9bPZgiIGWHhKLUfAfK5VL+QecSEuV8CilhKVEqX6Fb/vzkl2tbnFBOKePSEk/T349slJMqMkS64tLybeIQYgCOT4CvyArk/Kmd/0LZLoFGBTjCWdUE4tuPp8Y9uYjRU+Rky3P29Sm3J0hCiyg4O3/RgG4uNkoe8LZG89jdugiO6+UeZFaf963GAc5Xbmf320YBIjBsjcfp40EDtJw0pMa7mC4OLO7QT5tmMzmDDuXvjONSW+RIX8a3L5u8HrGqLPwmyjQ71AhCsIC/Z+pEDXBe5TyPRFQkEpZ/NGm/MSiA74Wc4JF5/IvOyk4Kf+z++3wm//6h6aCZHL8jjVJjYrWmY95avlGfmWhiSi1WDExDkg8b3F3OfRoPlDtRH3pdiaGuNl4fdhxF+UbD+Qqu6m4/at3VyjvkIIrJtb68urt1S4BCt5tlbBOGs0ri7Ry5W49LP8LurDKw75bORFd1BbZZnNDlovyotPb2MXimY+B685G8XQ6bfy3XWtnbRyLwkFkwM1CXCQTZn7FNGoG1gSLwc1qVGUbC4YhCALTuBAYxAbXUeE1WeNYgl0FFYZp7NqNC6+aFDYY20VGSUBgjBfSjIu0e8596GHZ2exOmB3Y/fS4j3PvOUf3fLqS7Hs18b0GEhdnsjxspWuvtbz78O7de5ANR5WrEvYgE5102jCgZak0nmHLDxKygHxtIAtKeaIEdzg+AgskWsgnzsAB5TyiAAm2eU9ZYJvw0ITDJkmdlFiGn03W5lB8eC4oFt/2MPh845nbXO+3ss0V1rnWZBJZjyfctURLmpkuW6NgBkQ4OBjPKqOzSYndcXi5B5Oh5zWGg8HgrO0FS8eiQWxq3sBhAbWDhvwgCVy7FtirdQ5MMzLR5rh+e3S6ABNBy2vMXKbX1mr+9GTotYikPaqMD8jvhdS1ammyGHkVEA0bo/bgKvSZyD4Fo3YAssXpqO2/LREOEJRmo2BS+jgZeD4pS4sW6ykRFsRRmrUmUj4FQ1foR5N5GI4jDC1jgWXbJt1pYvMyzbIMSUz55GESzOc/FNgsEO6EBH9chkqYviYzWoez2YyMUEPUrhlrHrpicgfN6Ylf89oEXmOp8ys38BfD8o/Toh+02u3aYlm0HFemsLSG79Csaw0gnPIDcH0xmvcAAANtSURBVK1goLorlaqzbM1oN8vRlosaWA98mBO4KlcLluDi2D8DScMf16fli/NzRec8kKqTGTrWGi6vpJVoSfmJH0C3GsqqcYLkfc9rt9q16yqprg4qEmdB5TqpRbqu3FSTBMBlDzo8DIADh2ZiaC3CggyygMG142iuqbQ08S9ocFOAqEcghd6vd6plPw7NVLaZ9M+NVTlTZzzDZSDLMf6QZBhk0YiOPxuX603TobAsNQqiFZXUhGAd1rcAFWHW4Vg1odpcAp8LQIN+H3ig5+FFFt5h+GoWXLoUW+eCRImWuuSZBGMIDxTpkw/XaUi/6HRNDPTVUaZXq1V4LyIXztfLVPN8RZPOE4UMyvGxaSbH27rfBha86MCluqoLrtuqbbkW5lZgQ51LWlhF+REswLufPwtubwu97y/VtFI05KrUtAuaqWlqXaUOWVbCGxvLdrw/1Dn49CUfk+dKXyHoExLUm/wuFuWnhZjOi6K4sZmLL4nAA3RNB+BtiYm+DkZYbcSaYMao0lVQsa6GHpWMtMJIoCj0/5RjeFu2ExGw1M6LrS1hb9dW04AoqOtAWDAvbjzwiRBOASST6/10pakW9lX5KZYSWwA5aYZsyZqUh1yCl0PHGXjQVxjp665LgoPgaRxQp4kPQ/4beVle251QwWXukf+UvwjGP+wH9wWOCXy1QOCTg2nv7glbgvC8I4osCjLf43GRE0ESyWSwkQfzy0KOcQBPuULv5ztNTIeZmZFVNWlspZpWrBTXFFy838rIAxxqQgKcCTDOZGebjHtUI5IiE2BKE5lIRFlLnKgC1pHUiWuqmXJZi1vB6YA8FdA7FhXI9DFV+uFBpzKWVyhlaLtIoChsdRTry5WxGq6XVIdtGQdwTNyVOIhi57mALBB2Omo3yeKu2N14g8xP5g/AyMXfCgq5zzfFrvhYPL5l+r4jI40/1F38Tmc+4ED324DWFZl34NzT4eIxDeDAESmT28JNTVhdtbMjUBYI+7udoydC581rhlev3rz+fH/0tXB/dE/QIcDc0TeFyLd/BWRsjlJj0tndFzgLhL2X2e0QGdhipUxcQqTPMtmN2N/5LgQ2W6cpkzyFmjO8RSZpNoXMpvKzENwkRZYnuPESK2Z5i2wo+GJQVTF1See+Prb5iCSHLftyT4hYIGz9j/8maPj/BFJ8bIAnUEcuAAAAAElFTkSuQmCC"/>'
      + '<h1>It is yeehaw time my dudes</h1><style>h1{font-family:Comic Sans MS, Comic Sans, Cursive}</style>'
      + '<script>function doReset() { debugger; window.location.href = "./reset"; }</script>'
      + '</div>') {
    }
    section("Options:") {
      input "startupDelayTime", "enum", title: "Delay this app upon boot for this many minutes:", options: [1, 2, 3, 4, 5, 7, 10], required: true
      input "bootLoopLimiter", "enum", title: "Number of times a hub can unsuccessfully reboot in a row before this app stops rebooting:", options: [1, 2, 3, 4, 5, 7, 10], required: true
      input "checkEveryNSeconds", "enum", title: "Run a test every N seconds:", options: [20, 30, 60], required: true
      input "rebootOnSevereLoad", "bool", title: "Reboot if hub 'severeLoad' event occurs?", default: false
      input "rebootDuringMaintenance", "bool", title: "Allow Reboot During Maintenance?", Description: "Allow RTSR to reboot the hub during the nightly maintenance window?", default: false
      input "logLevel", "enum", title: "Logging Level", options: [1: "Error", 2: "Warn", 3: "Info", 4: "Debug", 5: "Trace"], required: false
    }
  }
  page(name: "reset")
}

def reset() {

  if (counterReset) {
    app.updateSetting("counterReset", false)
    logWarn "Boot loop counter reset!"
    state.loopCounter = 0
  }

  dynamicPage(name: "reset", nextPage: "mainPage") {
    section('Reset the counter?!') {
      preferences {
        input name: "counterReset", type: "bool", title: "Toggle this to reset the boot loop counter.", defaultValue: false, submitOnChange: true
      }
    }
  }
}

def installed() {
  unsubscribe()
  unschedule()
  initialize()
}

def updated() {
  log.warn "Logging level is ${logLevel}"
  getCookie()
  initialize()
}

def initialize() {
  unsubscribe()
  unschedule()
  // Have to do it this way or the app hangs on initialization for N minutes
  runIn((60 * settings.startupDelayTime.toInteger()), startApp)
  logInfo "Scheduled a Rootin' Tootin' Startup in ${60 * settings.startupDelayTime.toInteger()} seconds"
}

def getCookie(doReboot = false) {

  def body = "username=${username}&password=${password}"

  def params = [
    uri: "http://${hubIP}:8080",
    path: '/login',
    requestContentType: 'application/x-www-form-urlencoded',
    contentType: 'application/x-www-form-urlencoded',
    body: "username=${username}&password=${password}"
  ]

  logDebug "Post params: $params"

  asynchttpPost("loginResponse", params, [reboot: doReboot])
}

def sendReboot() {
  logError "An error occured. Hub is unreachable or has taken too long to respond..."

  if (!rebootDuringMaintenance && isMaintenanceWindow()) {
    logWarn "Maintenance happening.  Skipping reboot!"
    return
  }

  if (!hubRequiresPassword) {
    // NO-COOKIE METHOD
    asynchttpPost('rebootResponse', [uri: "http://${hubIP}:8080/hub/reboot"])
  }
  else {
    // COOKIE METHOD
    asynchttpPost('rebootResponse', [uri: "http://${hubIP}:8080", path: '/hub/reboot', headers: ['Cookie': "${state.storedCookie}"]])
  }

  state.loopCounter++
}

def startApp() {
  // Check if the loop counter has been initialized yet
  state.loopCounter ?: 0
  logInfo "Loop count is ${state.loopCounter}..."
  state.checkInTime = now() // Reset the last check-in

  if (hubRequiresPassword) {
    getCookie()
  }

  logInfo "Initializing Rootin' Tootin' Self-Rebootin'"
  if (state.loopCounter >= settings.bootLoopLimiter.toInteger()) {
    logError "The hub has rebooted too many times in a row (${state.loopCounter} times). This app will now stop."
  }
  else {
    logInfo "Scheduling Rootin' Tootin' check-ins and readability tests"
    schedule("0/${settings.checkEveryNSeconds.toInteger() / 2} * * * * ?", checkIn)
    schedule("0${settings.checkEveryNSeconds == "60" ? "" : "/${settings.checkEveryNSeconds}"} * * * * ?", rebootTest)
    if (rebootOnSevereLoad) {
        subscribe(location, 'severeLoad', highCpuLoad)
    }
  }
}

def highCpuLoad(evt) {
    logInfo "Hub severeLoad event received."
    getCookie(true)
}

def parseResponse(response, data) {
  log.trace "What's this? //TODO: Make it so that errors are logged as errors but success is logged as info"
  log.trace "Received ${response}, ${data}"
}

def checkIn() {
  state.checkInTime = now()
}

def rebootTest() {

  logInfo "Checking hub for DB readability..."
  try {
    if ((now() - state.checkInTime) / 1000 > settings.checkEveryNSeconds.toInteger()) {
      // If the current time minus the last time checked in is more seconds than the last scheduled check in, reboot
      getCookie(true)
    }
    else {
      state.loopCounter = 0
      logInfo "DB read success. Boot loop counter has been restarted."
    }
  }
  catch (e) {
    getCookie(true)
  }
}


def loginResponse(response, data) {
  // Handle responses from sending username/password to the hub

  if (response.status == 200 || response.status == 302) {
    logInfo "Sucessfully logged in to hub."
    def cookie = response.headers["Set-Cookie"].split(";")
    state.storedCookie = cookie[0]
    logTrace "Stored cookie ${cookie[0]} from ${response.headers['Set-Cookie']}"
    if (data.reboot) {
      sendReboot()
    }
  }
  else {
    logError "Login failed with status ${response.status}.  Please check configuration!"
    response.properties.each { logDebug "resp prop: $it" }
  }
}

def rebootResponse(response, data) {
  // Handle resonses from a reboot with cookie

  logDebug "Reboot Response Status: ${response.status}"

  if (response.status == 200) {
    logWarn "Sucessfully sent reboot packet. Rebooting momentarily..."
  }
  else {
    logError "Something went wrong when trying to send a reboot packet."
  }
}

def isMaintenanceWindow() {
  Calendar cal = Calendar.getInstance();
  def hour = cal.get(Calendar.HOUR_OF_DAY)
  logTrace "hour: $hour"
  //TODO: Get the maintenance window dynamically for all regions
  return maintenanceWindow.contains(hour)
}

def logError(msg) {
  if (logLevel?.toInteger() >= 1) {
    log.error msg
  }
}

def logWarn(msg) {
  if (logLevel?.toInteger() >= 2) {
    log.warn msg
  }
}

def logInfo(msg) {
  if (logLevel?.toInteger() >= 3) {
    log.info msg
  }
}

def logDebug(msg) {
  if (logLevel?.toInteger() >= 4) {
    log.debug msg
  }
}

def logTrace(msg) {
  if (logLevel?.toInteger() >= 5) {
    log.trace msg
  }
}
