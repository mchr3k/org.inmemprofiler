<html>
    <head>
        <title>InMemProfiler Console</title>
    </head>
    <body>
        <script>
          function inmem_start()
          {
        	  testFrame(addCB("/inmem_start.jsp"));
          }
          
          function inmem_pause()
          {
        	  testFrame(addCB("/inmem_pause.jsp"));
          }
          
          function inmem_reset()
          {
        	  testFrame(addCB("/inmem_reset.jsp"));
          }
          
          function inmem_output()
          {
        	  testFrame(addCB("/inmem_output.jsp"));
          }
          
          function doGo()
          {
        	  var url = document.getElementById("url");
            testFrame(addCB(url.value), true);
          }           

          function addCB(url)
          {
        	  var nowdate = new Date();
        	  var nowtime = nowdate.getTime();

        	  return addUrlParam(url, "cb=" + nowtime);
          }
          
          function addUrlParam(url, param)
          {
            var querystrIndex = url.indexOf("?");
            if (querystrIndex == -1)
            {
            	url += "?" + param;
            }
            else
            {
            	url += "&" + param;
            }
            return url;
          }
          
          function testFrame(url)
          {
        	  var testFrame = document.getElementById("testframe");
            testFrame.src = url;
            var testFrameUrl = document.getElementById("url");
            testFrameUrl.value = url;
          }          
        </script>
        
        <fieldset id="actions">
        <legend>InMemProfiler Actions:</legend>        
        
        <input type="button" value="Start" onclick="inmem_start();">
        <input type="button" value="Pause" onclick="inmem_pause();">
        <input type="button" value="Reset" onclick="inmem_reset();">
        <input type="button" value="Output" onclick="inmem_output();">
        
        </fieldset>             
        
        <br>
        
        <fieldset id="content" style="padding: 0px; height: 60%;">
        <iframe name="testframe"
                id="testframe" 
                style="border: 0px; width: 100%; height: 100%;">
        </iframe>
        </fieldset>
    </body>
</html>