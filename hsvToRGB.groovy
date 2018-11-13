def hsvToRGB(float conversionHue = 0, float conversionSaturation = 100, float conversionValue = 100, resolution = "low"){
    // Accepts conversionHue (0-100 or 0-360), conversionSaturation (0-100), and converstionValue (0-100), resolution ("low", "high")
    // If resolution is low, conversionHue accepts 0-100. If resolution is high, conversionHue accepts 0-360
    // Returns RGB map ([ red: 0-255, green: 0-255, blue: 0-255 ])
    
    // Check HSV limits
    resolution == "low" ? ( hueMax = 100 ) : ( hueMax = 360 ) 
    conversionHue > hueMax ? ( conversionHue = 1 ) : ( conversionHue < 0 ? ( conversionHue = 0 ) : ( conversionHue /= hueMax ) )
    conversionSaturation > 100 ? ( conversionSaturation = 1 ) : ( conversionSaturation < 0 ? ( conversionSaturation = 0 ) : ( conversionSaturation /= 100 ) )
    conversionValue > 100 ? ( conversionValue = 1 ) : ( conversionValue < 0 ? ( conversionValue = 0 ) : ( conversionValue /= 100 ) )
        
    int h = conversionHue * 6
    float f = ( conversionHue * 6 - h ) * 255
    float p = ( conversionValue * (1 - conversionSaturation) ) * 255
    float q = ( conversionValue * (1 - f * conversionSaturation) ) * 255
    float t = ( conversionValue * (1 - (1 - f) * conversionSaturation) ) * 255    
    conversionValue *= 255
          
    if      (h==0) { rgbMap = [red: conversionValue, green: t, blue: p] }
    else if (h==1) { rgbMap = [red: q, green: conversionValue, blue: p] }
    else if (h==2) { rgbMap = [red: p, green: conversionValue, blue: t] }
    else if (h==3) { rgbMap = [red: p, green: q, blue: conversionValue] }
    else if (h==4) { rgbMap = [red: t, green: p, blue: conversionValue] }
    else if (h==5) { rgbMap = [red: conversionValue, green: p,blue: q]  }
    else           { rgbMap = [red: 0, green: 0, blue: 0] }

    return rgbMap
}
