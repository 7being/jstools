package net.ileto.jstools.mojo

import org.codehaus.gmaven.mojo.GroovyMojo
import org.apache.maven.project.MavenProject
import java.io.File;
import org.mozilla.javascript.tools.ToolErrorReporter;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Compress javascripts using yuicompressor
 * 
 * @phase process-resources
 * @goal compress
 */
class YuiCompressorMojo extends GroovyMojo {
	
	/**
	 * @readonly
	 * @required
	 * @parameter expression="${project}"
	 */
	private MavenProject project
	
	/**
	 * @required
	 * @parameter
	 */
	private File name;
	
	/**
	 * @parameter default-value="${project.version}"
	 */
	private String version;
	
	/**
	 * The javascript source directory.
	 * 
	 * @parameter expression="${basedir}/src/main/js"
	 */
	private File sourceDirectory
	
	/**
	 * @parameter expression="${project.build.directory}"
	 */
	private File outputDirectory
	
	/**
	 * List of patterns (separated by commas) used to specify the tests that 
	 * should be included in testing.  When not specified and when the 
	 * test parameter is not specified, the default includes will be 
	 * **\/Test*.html, **\/*Test.html, **\/*TestCase.html
	 * 
	 * @parameter 
	 */
	private String includes = '**/*.js'
	
	/**
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be excluded form testing.  
	 * 
	 * @parameter
	 */
	private String excludes = ''
	
	/**
	 * Set this to 'true' to bypass unit tests entirely.  
	 * 
	 * @parameter expression="${jsunit.skip}" default-value="false"
	 */
	private boolean skip = false
	
	void execute() {
		if(skip) {
			return
		}
		
		generateMinFile(generateAllFile())
	}

	List<File> getJavascripts() {
		if(!sourceDirectory.exists()) {
			return []
		}
		def scanner = ant.fileScanner {
			fileset(dir: sourceDirectory) {
				includes.split(',').each { include(name:it) }
				excludes.split(',').each { exclude(name:it) }
			}
		}
		
		List<File> result = []
		for(f in scanner) {
			result << f
		}
		return result
	}

	
	File generateAllFile() {
		for (file in getJavascripts()) {
			output.append(new FileInputStream(file))
			output.write(';')
		}
		return output
	}
	
	def	genereateMinFile(File input) {
		JavaScriptCompressor compressor = new JavaScriptCompressor(new InputStreamReader(new FileInputStream(input)),
				new ToolErrorReporter(true));
		
		Writer writer = new OutputStreamWriter(new FileOutputStream(getMinOutput()), "UTF-8");
		compressor.compress(writer, Integer.MAX_VALUE, false, false, false, false);
		writer.close();
	}
	
	File getAllOutput() {
		return new File(outputDirectory, name + '-' + version + '.js')
	}
	
	File getMinOutput() {
		return new File(outputDirectory, name + '-' + version + '.min.js')
	}
}
