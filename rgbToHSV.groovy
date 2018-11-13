def rgbToHSV( r=255, g=255, b=255, resolution="low" ) {
	// Takes RGB (0-255) and returns HSV in 0-360, 0-100, 0-100
  // resolution ("low", "high") will return 0-100, or 0-360, respectively.
  
	r /= 255
	g /= 255
	b /= 255

	float h
	float s
	
	float max =   Math.max( Math.max( r, g ), b )
	float min = Math.min( Math.min( r, g ), b )
	float delta = ( max - min )
	float v = ( max * 100.0 )

	max != 0.0 ? ( s = delta / max * 100.0 ) : ( s = 0 )

	if (s == 0.0) {
		h = 0.0
	}
	else{
		if (r == max){
        	h = ((g - b) / delta)
		}
		else if(g == max) {
        	h = (2 + (b - r) / delta)
		}
		else if (b == max) {
        	h = (4 + (r - g) / delta)
		}
	}

	h *= 60.0
    h < 0 ? ( h += 360 ) : null
  
  resolution == "low" ? h /= 3.6 : null
  return [ hue: h, saturation: s, value: v ]
}
