package net.ileto.jstools.mojo

import org.codehaus.gmaven.mojo.GroovyMojo
import org.apache.maven.project.MavenProject
import java.io.File
import org.mozilla.javascript.tools.ToolErrorReporter
import com.yahoo.platform.yui.compressor.JavaScriptCompressor

import net.ileto.jstools.utility.FileUtility

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
	 * @parameter expression="${project.artifactId}"
	 */
	private String name;
	
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
	 * @parameter expression="${project.build.outputDirectory}"
	 */
	private File outputDirectory
	
	/**
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
	 * @parameter default-value="false"
	 */
	private boolean skip = false
	
	void execute() {
		if(skip) {
			return
		}
		
                beforeStart()
		generateMinFile(generateAllFile())
	}

        def beforeStart() {
            ant.mkdir(dir:outputDirectory)
            includes = FileUtility.trimTokens(includes, ',')
            excludes = FileUtility.trimTokens(excludes, ',')
        }

	def getJavascripts() {
		if(!sourceDirectory.exists()) {
			return []
		}
                
                def result = []

                for (inc in includes.split(',')) {
                    def scanner = ant.fileScanner {
                            fileset(dir: sourceDirectory) {
                                    include(name:inc)
                                    excludes.split(',').each { exclude(name:it) }
                            }
                    }
		
                    for(f in scanner) {
                            result << f
                    }
                }
		
		result.unique {a, b -> a.canonicalPath.compareTo(b.canonicalPath) }
	}

	
	def generateAllFile() {
		File output = getAllOutput();
		Writer writer = new OutputStreamWriter(output.newOutputStream(), 'UTF-8');
		for (file in getJavascripts()) {	
			writer.append(file.getText('UTF-8'));
			//output.append(file.newInputStream())
			writer.append(';')
		}
		writer.close();
		output
	}
	
	def generateMinFile(File input) {
		//JavaScriptCompressor compressor = new JavaScriptCompressor(input.newReader('UTF-8'), new ToolErrorReporter(true));
		JavaScriptCompressor compressor = new JavaScriptCompressor(new InputStreamReader(input.newInputStream(), 'UTF-8'), new ToolErrorReporter(true));
		
		Writer writer = new OutputStreamWriter(getMinOutput().newOutputStream(), 'UTF-8');
		compressor.compress(writer, Integer.MAX_VALUE, false, false, false, false);
		writer.close();
	}
	
	def getAllOutput() {
		File f = new File(outputDirectory, name + '-' + version + '.js')
		f.createNewFile()
		f
	}
	
	def getMinOutput() {
		File f = new File(outputDirectory, name + '-' + version + '.min.js')
		f.createNewFile()
		f
	}
}
