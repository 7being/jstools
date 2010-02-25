package net.ileto.jstools.mojo

import java.io.File;
import org.codehaus.gmaven.mojo.GroovyMojo

import junit.textui.TestRunner
import junit.framework.TestResult

import net.jsunit.StandaloneTest

import org.apache.commons.io.IOUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.project.MavenProject

import net.ileto.jstools.utility.FileUtility
import net.ileto.jstools.jsunit.template.TestSuiteTemplate
import net.ileto.jstools.jsunit.template.TestPageTemplate

/**
 * Runs tests using jsunit.
 * 
 * @phase test
 * @goal jsunit
 */
class JsUnitMojo extends GroovyMojo {
	
	/**
	 * @readonly
	 * @required
	 * @parameter expression="${project}"
	 */
	private MavenProject project
	
	/**
	 * The working directory for unit tests.
	 * 
	 * @parameter expression="${project.build.directory}/jsunit-tests"
	 */
	private File workDirectory
	
	/**
	 * logsDirectory is the directory into which the JsUnit Server will
	 * write its XML logs.  If not specified, it defaults to 
	 * ${project.build.directory}/jsunit-reports
	 * 
	 * @parameter expression="${project.build.directory}/jsunit-reports"
	 */
	private File logsDirectory
	
	/**
	 * The javascript source directory.
	 * 
	 * @parameter expression="${basedir}/src/main/js"
	 */
	private File sourceDirectory
	
	/**
	 * The javascript test source directory
	 * 
	 * @parameter expression="${basedir}/src/test/js"
	 */
	private File testSourceDirectory
	
	/**
	 * The path to the jsunit distribution relative to workDirectory.
	 * 
	 * @readonly
	 * @parameter default-value="jsunit"
	 */
	private String jsUnitPath 
	
	/**
	 * The file name of the generated test page.
	 * 
	 * @readonly
	 * @parameter default-value="TestSuite.html"
	 */
	private String testSuiteFileName = 'TestSuite.html'

	/**
	 * Specify this parameter to run individual tests by file name.
	 * Each pattern you specify here will be used to create an include pattern
	 * formatted like **\/${test}.html, so you can just type -Djsunit.test=FooTest,BarTest
	 * and run myapp/FooTest.html and myapp/BarTest.html
	 * 
	 * @parameter expression="${jsunit.test}"
	 */
	private String test
	
	/**
	 * List of patterns (separated by commas) used to specify the tests that 
	 * should be included in testing.  When not specified and when the 
	 * test parameter is not specified, the default includes will be 
	 * **\/Test*.html, **\/*Test.html, **\/*TestCase.html
	 * 
	 * @parameter 
	 */
	private String includes = '**/Test*.html,**/*Test.html,**/*TestCase.html'
	
	/**
	 * List of patterns (separated by commas) used to specify the tests that
	 * should be excluded form testing.  
	 * 
	 * @parameter
	 */
	private String excludes = ''
	
	/**
	 * port is the port on which the JsUnit server will run.  
	 * 
	 * @parameter default-value="8090"
	 */
	private String port = "8090"
	
	/**
	 * browserFileNames is a comma-separated list of paths to the executables
	 * of the browsers on which you want to run your JsUnit test suite.
	 * For example, if you want to run the tests on Internet Explorer and 
	 * Firefox, its value might be 
	 * c:\program files\internet explorer\iexplore.exe,c:\program files\Mozilla Firefox\firefox.exe
	 * 
	 * @parameter
	 */
	private String browsers = ''
	
	/**
	 * autoRun is used to inform JsUnit whether to immediately kick off a run.
	 * For example, entering the URL "c:\jsunit\testRunner.html?testpage=c:\myTests\aTest.html&autoRun=true"
	 * will launch JsUnit and populate the Test Page box with c:\myTests\aTest.html and
	 * immediately kick off the tests.  Of course, if you pass auotRun=true, you 
	 * need to also pass in a testPage.  Defaults to true.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean autoRun = true
	
	/**
	 * closeBrowserAfterTestRuns determines whether to attempt to close browsers
	 * after test runs.  The default is true.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean closeBrowsers = true
	
	/**
	 * showTestFrame is used to tell JsUnit whether to make this Test Frame 
	 * visible (Internet Explorer only).  You can pass in "true" in which 
	 * case JsUnit will show the test frame at its default height.  You
	 * can pass in n to tell JsUnit to show the testFrame at n pixels.  
	 * Default is "true".
	 * 
	 * NOTE: Opera needs this value to be true in order to run?
	 * 
	 * @parameter default-value="true"
	 */
	private String showTestFrame = "true"
	
	/**
	 * Set this to 'true' to bypass unit tests entirely.  
	 * 
	 * @parameter expression="${jsunit.skip}" default-value="false"
	 */
	private boolean skip = false
	
	/**
	 * Set this to 'true' to bypass running the unit tests, while still
	 * creating the jsunit work directory.
	 * 
	 * @parameter expression="${jsunit.skipTests}" default-value="false"
	 */
	private boolean skipTests = false
	
	void execute() {
		if(skip) {
			return
		}
		
		makeWorkDirectory()
		copyJsUnitResources()
		
		copySources()
		copyTestSources()
		
		configureServer(generateTestSuite())
		
		if(!skipTests) {
			TestRunner.run(StandaloneTest)
		}
	}
	
	void makeWorkDirectory() {
		ant.mkdir(dir:workDirectory)
	}
	
	void copyJsUnitResources() {
		File temp = File.createTempFile('E3658158', '')
		IOUtils.copy(getClass().getResourceAsStream('/jsunit.zip'), new FileOutputStream(temp))
		ant.unzip(src:temp, dest:workDirectory)
		temp.delete()
	}
	
	void copySources() {
		if(!sourceDirectory.exists()) {
			return
		}
		ant.copy(todir:workDirectory) { fileset(dir:sourceDirectory) }
	}
	
	void copyTestSources() {
		if(!testSourceDirectory.exists()) {
			return 
		}
		ant.copy(todir:workDirectory) { fileset(dir:testSourceDirectory) }
		
		generateTestPagesForJS()
	}
	
	void configureServer(File testPage) {
		System.setProperty("url", "${testRunnerUrl}?testPage=${testPage}&autoRun=${autoRun}&submitResults=localhost:${port}/jsunit/acceptor&showTestFrame=${showTestFrame}")
		System.setProperty("port", port)
		System.setProperty("browserFileNames", getBrowserFileNames())
		System.setProperty("logsDirectory", logsDirectory.getAbsolutePath())
		System.setProperty("closeBrowsersAfterTestRuns", closeBrowsers.toString())
	}
	
	File generateTestSuite() {
		String html = new TestSuiteTemplate().process([
			files: getTests(),
			jsUnitCore: "$jsUnitPath/app/jsUnitCore.js"
		])
		File result = new File(workDirectory, testSuiteFileName)
		result.write(html)
		return result
	}
	
	File generateTestPagesForJS() {
		if(!testSourceDirectory.exists()) {
			return
		}
		def	scanner = ant.fileScanner {
			fileset(dir: testSourceDirectory) {
				include(name:'**/*.js')
			}
		}
		
		File jsUnitCore = new File(workDirectory, "$jsUnitPath/app/jsUnitCore.js")
		for (f in scanner) {
			File copyed = FileUtility.replaceStart(f, testSourceDirectory, workDirectory)
			File parsed = FileUtility.replaceExtension(copyed, 'html')
			
			String html = new TestPageTemplate().process([
				file: copyed,
				jsUnitCore: FileUtility.relativePathTo(jsUnitCore, copyed).replace(File.separator, '/')
			])      
			
			parsed.write(html)
		}
	}
	
	List<File> getTests() {
		if(!testSourceDirectory.exists()) {
			return []
		}
		def scanner = ant.fileScanner {
			fileset(dir: testSourceDirectory) {
				if(!test) {
					includes.split(',').each { include(name:it) }
					excludes.split(',').each { exclude(name:it) }
				} else {
					test.split(',').each {
						include(name:"**/${it}.html")
					}
				}
			}
		}
		
		List<File> result = []
		for(f in scanner) {
			result << FileUtility.replaceStart(f, testSourceDirectory, workDirectory)
		}
		return result
	}
	
	private URL getTestRunnerUrl() {
		return new File(workDirectory, "${jsUnitPath}/testRunner.html").toURI().toURL()
	}
	
	String getBrowserFileNames() {
		return trimTokens(browsers, ',')
	}
	
	private String trimTokens(String s, String delim) {
		List<String> result = []
		s.tokenize(delim).each {
			it = it.trim()
			if(it) {
				result << it
			}
		}
		return result.join(delim)
	}
}
