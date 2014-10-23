<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
	<%@ page isELIgnored="false" %>
	
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>
  <head>
    <meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="viewport" content="user-scalable=no, width=device-width,  initial-scale=0.5, minimum-scale=0.5,  maximum-scale=2"/>
<style>
* {
	-webkit-user-drag: none;
	-webkit-touch-callout: none;
	/* prevent callout to copy image, etc when tap to hold */
	-webkit-text-size-adjust: none;
	/* prevent webkit from resizing text to fit */
	-webkit-tap-highlight-color: rgba(0, 0, 0, 0);
	/* make transparent link [ remove highlighting ] selection, adjust last value opacity 0 to 1.0 */
}
</style>
<style>
canvas {
	border: 0px solid black;
}
</style>
    <meta charset="utf-8">
    <title>iiCaptain Player Distribution</title>
    <link href="../../map.css" rel="stylesheet">
    <script src="https://maps.googleapis.com/maps/api/js?key=AIzaSyD1HarMVY68xeAypKfrIH2Lka7YdJglZm4&v=3.exp&sensor=false"></script>
    <script>

      // Create an object containing LatLng, radius.
      var alllocs= '<c:out value="${locations}"/>';
      var autorefresh= <c:out value="${refresh}"/>;
      var forSolr= <c:out value="${forSolr}"/>;
      var solrQuery= '<c:out value="${solrQuery}"/>';
      var circles= new Array();
      var map;
      var mapoptions;
      
      alllocs=alllocs.replace(/;/g, "'");
      console.log("1: "+alllocs);
      alllocs= eval('(' + alllocs + ')');
      
      var iimap = new Array(alllocs.locations.length);
      
      for(var l= 0; l< alllocs.locations.length; l++) {
      	iimap[l] = {
        	center: new google.maps.LatLng(alllocs.locations[l].latitude, alllocs.locations[l].longitude),
        	n: Math.min(32, alllocs.locations[l].n)*1000
      	};
      }
      var circle;

      function updateLocations() {
    	  if (req.readyState==4 && req.status==200)
    	    {
    		  alllocs= req.responseText;
    	      alllocs=alllocs.replace(/;/g, "'");
    	      console.log("2: "+alllocs);
    	      alllocs= eval('(' + alllocs + ')');
    
    	      iimap = new Array(alllocs.locations.length);
    	      for(var l= 0; l< alllocs.locations.length; l++) {
    	        	iimap[l] = {
    	          	center: new google.maps.LatLng(alllocs.locations[l].latitude, alllocs.locations[l].longitude),
    	          	n: Math.min(32, alllocs.locations[l].n)*1000
    	        	};
    	      }
    	      
    	      for (var i= 0; i< circles.length; i++) {
 				circles[i].setMap(null);
    	      }
    	      
    	      for (var i= 0; i< iimap.length; i++) {
    	          var options = {
    	            strokeColor: '#FF0000',
    	            strokeOpacity: 0.8,
    	            strokeWeight: 2,
    	            fillColor: '#FF0000',
    	            fillOpacity: 0.35,
    	            map: map,
    	            center: iimap[i].center,
    	            radius: iimap[i].n*20
    	          };
    	          circle = new google.maps.Circle(options);
    	          circles[i]= circle;
    	        }
    	      
    	      setTimeout(hitDataUrl, 2000);
    	    }
      }
      
      function hitDataUrl() {
    		if (window.XMLHttpRequest) {
    			try {
    				req = new XMLHttpRequest();
    			} catch (e) {
    				req = false;
    			}
    			// branch for IE/Windows ActiveX version
    		} else {
    			if (window.ActiveXObject) {
    				try {
    					req = new ActiveXObject("Msxml2.XMLHTTP");
    				} catch (e) {
    					try {
    						req = new ActiveXObject("Microsoft.XMLHTTP");
    					} catch (e) {
    						req = false;
    					}
    				}
    			}
    		}
    		if (req) {
    			req.onreadystatechange = updateLocations;
    			if(forSolr==true) {
    				req.open("GET","solrData?"+solrQuery, true);
    			}
    			else {
    				req.open("GET","data", true);			
    			}
    			req.send("");
    		} else {
    			alert("req== false");
    		}
    	}

      
      
      function initialize() {
    	  
		var id= document.getElementById("iiCaptain");
    		w = window.innerWidth;
    		h = window.innerHeight;
    		// 920, 576
    		var perfcor = 0;
    		scale = Math.min((w - perfcor * 64) / 960, (h - perfcor * 64) / 640);
    		sX = (w - perfcor * 64) / 960;
    		sY = (h - perfcor * 64) / 640;
    		
    		var bgimg = new Image();
    		bgimg.src = "../../openres/start.png";
    		bgimg.setAttribute("id", "start");
    		id.appendChild(bgimg);

    		bgimg.setAttribute("style", "position:absolute; top:0px; left:0px; width: "
    				+ window.innerWidth + "px; height: " + window.innerHeight + "px;");
	
    		id= document.getElementById("map_canvas");
    		id.setAttribute("style", "position:absolute; top:" + (96 * sY)
    				+ "px; left:" + ((960 - 480 * 1.5) / 2) * sX + "px; width: "
    				+ (480 * sX * 1.5) + "px; height: " + (320 * sY * 1.5) + "px;");
    		var z= Math.floor(1.5*sX);
    	//	alert("zoom: "+z);
        mapOptions = {
          zoom: 2,
          center: new google.maps.LatLng(37.09024, -0.712891),
          mapTypeId: google.maps.MapTypeId.TERRAIN
        };

        	map = new google.maps.Map(document.getElementById('map_canvas'),
            mapOptions);

        
        for (var i= 0; i< iimap.length; i++) {
          var options = {
            strokeColor: '#FF0000',
            strokeOpacity: 0.8,
            strokeWeight: 2,
            fillColor: '#FF0000',
            fillOpacity: 0.35,
            map: map,
            center: iimap[i].center,
            radius: iimap[i].n*20
          };
          circle = new google.maps.Circle(options);
          circles[i]= circle;
        }
      }
      if(autorefresh)
     	 setTimeout(hitDataUrl, 2000);
    </script>
  </head>
  <body onload="initialize()">
  <div id="iiCaptain" style="position:absolute; top:0px; left: 0px;">
	</div>
    <div id="map_canvas"></div>
  </body>
</html>

