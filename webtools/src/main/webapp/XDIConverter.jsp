<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="xdi2.core.properties.XDI2Properties" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>XDI Converter</title>
<link rel="stylesheet" target="_blank" href="style.css" TYPE="text/css" MEDIA="screen">
</head>
<body>
	<div id="imgtop"><img id="imgtopleft" src="images/xdi2-topleft.png"><a href="http://projectdanube.org/"><img id="imgtopright" src="images/xdi2-topright.png"></a></div>
	<div id="main">
	<div class="header">
	<span id="appname"><img src="images/app20b.png"> XDI Converter</span>
	&nbsp;&nbsp;&nbsp;&nbsp;Examples: 
	<% for (int i=0; i<((Integer) request.getAttribute("sampleInputs")).intValue(); i++) { %>
		<a href="XDIConverter?sample=<%= i+1 %>"><%= i+1 %></a>&nbsp;&nbsp;
	<% } %>
	<a href="index.jsp">&gt;&gt;&gt; Other Apps...</a><br>
	This is version <%= XDI2Properties.properties.getProperty("project.version") %> <%= XDI2Properties.properties.getProperty("project.build.timestamp") %>, Git commit <%= XDI2Properties.properties.getProperty("git.commit.id").substring(0,6) %> <%= XDI2Properties.properties.getProperty("git.commit.time") %>.
	</div>

	<% if (request.getAttribute("error") != null) { %>
			
		<p style="font-family: monospace; white-space: pre; color: red;"><%= request.getAttribute("error") != null ? request.getAttribute("error") : "" %></p>

	<% } %>

	<form action="XDIConverter" method="post" id="form" accept-charset="UTF-8">

		<textarea class="input" name="input" style="width: 100%" rows="12"><%= request.getAttribute("input") != null ? request.getAttribute("input") : "" %></textarea><br>

		<% String resultFormat = (String) request.getAttribute("resultFormat"); if (resultFormat == null) resultFormat = ""; %>
		<% String writeImplied = (String) request.getAttribute("writeImplied"); if (writeImplied == null) writeImplied = ""; %>
		<% String writeOrdered = (String) request.getAttribute("writeOrdered"); if (writeOrdered == null) writeOrdered = ""; %>
		<% String writePretty = (String) request.getAttribute("writePretty"); if (writePretty == null) writePretty = ""; %>
		<% String from = (String) request.getAttribute("from"); if (from == null) from = ""; %>

		<p>
		Convert from:
		<select name="from">
		<option value="AUTO" <%= from.equals("AUTO") ? "selected" : "" %>>auto-detect</option>
		<option value="JXD" <%= from.equals("JXD") ? "selected" : "" %>>JXD</option>
		<option value="XDI DISPLAY" <%= from.equals("XDI DISPLAY") ? "selected" : "" %>>XDI DISPLAY</option>
		<option value="XDI/JSON" <%= from.equals("XDI/JSON") ? "selected" : "" %>>XDI/JSON</option>
		<option value="XDI/JSON/QUAD" <%= from.equals("XDI/JSON/QUAD") ? "selected" : "" %>>XDI/JSON/QUAD</option>
		<option value="RAW JSON" <%= from.equals("RAW JSON") ? "selected" : "" %>>RAW JSON</option>
		</select>
		to:
		<select name="resultFormat">
		<option value="XDI DISPLAY" <%= resultFormat.equals("XDI DISPLAY") ? "selected" : "" %>>XDI DISPLAY</option>
		<option value="JXD" <%= resultFormat.equals("JXD") ? "selected" : "" %>>JXD</option>
		<option value="XDI/JSON" <%= resultFormat.equals("XDI/JSON") ? "selected" : "" %>>XDI/JSON</option>
		<option value="XDI/JSON/TREE" <%= resultFormat.equals("XDI/JSON/TREE") ? "selected" : "" %>>XDI/JSON/TREE</option>
		<option value="XDI/JSON/PARSE" <%= resultFormat.equals("XDI/JSON/PARSE") ? "selected" : "" %>>XDI/JSON/PARSE</option>
		<option value="XDI/JSON/TRIPLE" <%= resultFormat.equals("XDI/JSON/TRIPLE") ? "selected" : "" %>>XDI/JSON/TRIPLE</option>
		<option value="XDI/JSON/QUAD" <%= resultFormat.equals("XDI/JSON/QUAD") ? "selected" : "" %>>XDI/JSON/QUAD</option>
		<option value="KEYVALUE" <%= resultFormat.equals("KEYVALUE") ? "selected" : "" %>>KEYVALUE</option>
		<option value="XDI/RDF/TriG" <%= resultFormat.equals("XDI/RDF/TriG") ? "selected" : "" %>>XDI/RDF/TriG</option>
		<option value="XDI/RDF/JSON-LD" <%= resultFormat.equals("XDI/RDF/JSON-LD") ? "selected" : "" %>>XDI/RDF/JSON-LD</option>
		</select>

		<input name="writeImplied" type="checkbox" <%= writeImplied.equals("on") ? "checked" : "" %>>implied=1

		<input name="writeOrdered" type="checkbox" <%= writeOrdered.equals("on") ? "checked" : "" %>>ordered=1

		<input name="writePretty" type="checkbox" <%= writePretty.equals("on") ? "checked" : "" %>>pretty=1

		<input type="submit" name="submit" value="Go!" onclick="document.getElementById('form').removeAttribute('target')">
		<input type="submit" name="submit" value="Html!" onclick="document.getElementById('form').setAttribute('target','_blank')">
		&nbsp;&nbsp;&nbsp;&nbsp;<a href="XDIConverterHelp.jsp">What can I do here?</a>
		</p>

	</form>

	<div class="line"></div>

	<% if (request.getAttribute("stats") != null) { %>
		<p>
		<%= request.getAttribute("stats") %>

		<% if (request.getAttribute("output") != null) { %>
			Copy&amp;Paste: <textarea style="width: 100px; height: 1.2em; overflow: hidden"><%= request.getAttribute("output") %></textarea>
		<% } %>
		</p>
	<% } %>

	<% if (request.getAttribute("output") != null) { %>
		<% if (request.getAttribute("outputId") != null && ! "".equals(request.getAttribute("output")) && ! "".equals(request.getAttribute("outputId"))) { %>
			<a class="graphit" target="_blank" href="http://neustar.github.io/xdi-grapheditor/xdi-grapheditor/public_html/index.html?input=<%= request.getRequestURL().toString().replaceFirst("/[^/]+$", "/XDIOutput?outputId=" + request.getAttribute("outputId")) %>">Graph It!</a>
		<% } %>
		<div class="result"><pre><%= request.getAttribute("output") %></pre></div><br>
	<% } %>

	</div>	
</body>
</html>
