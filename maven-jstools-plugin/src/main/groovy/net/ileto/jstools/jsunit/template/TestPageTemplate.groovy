package net.ileto.jstools.jsunit.template

import java.util.Map;
import groovy.text.SimpleTemplateEngine

class TestPageTemplate {
	private static String html = 
'''<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
    <title>${file.name}</title>
    <script src="$jsUnitCore"></script>
	<% for (script in scripts) { %>
    <script src="$script"></script>
	<% } %>
    <script>
      $srcbody
    </script>
  </head>
  <body>
  </body>
</html>
'''
	
	def process(Map<String,Object> model) {
		model['scripts'] = []
		model['srcbody'] = new StringBuffer()
		
		
		model['file'].eachLine {
			def m = ( ~/^import\s(.+\.js)$/ ).matcher(it)
			if (m.matches()) {
				model['scripts'] << m.group(1)
			} else {
				model['srcbody'] << it << "\n"
			}
		}
		
		def engine = new SimpleTemplateEngine()
		def template = engine.createTemplate(html)
		template.make(model).toString()
	}
}
