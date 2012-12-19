/*
 * Copyright 2012 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import groovy.mock.interceptor.MockFor
import org.apache.tools.ant.BuildException

/**
 * Unit tests for the CodeNarc script
 * 
 * @author Chris Mair
 */
class CodenarcScriptTests extends AbstractTestCase {

    private static final TASKDEF = [name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask']
    private static final RULESET_FILES = 'rulesets/basic.xml,rulesets/exceptions.xml,rulesets/imports.xml,rulesets/grails.xml,rulesets/unused.xml'
    private static final MAX = Integer.MAX_VALUE
    private static final REPORT_FILE = 'target/CodeNarcReport.html'
    private static final FILESET_DIR = '.'

    private static final SRC_GROOVY = 'src/groovy/**/*.groovy'
    private static final CONTROLLERS = 'grails-app/controllers/**/*.groovy'
    private static final DOMAIN = 'grails-app/domain/**/*.groovy'
    private static final SERVICES = 'grails-app/services/**/*.groovy'
    private static final TAGLIB = 'grails-app/taglib/**/*.groovy'
    private static final UTILS = 'grails-app/utils/**/*.groovy'
    private static final TEST_UNIT = 'test/unit/**/*.groovy'
    private static final TEST_INTEGRATION = 'test/integration/**/*.groovy'
    private static final VIEWS = 'grails-app/views/**/*.gsp'
    private static final DEFAULT_INCLUDES = [SRC_GROOVY, CONTROLLERS, DOMAIN, SERVICES, TAGLIB, UTILS, TEST_UNIT, TEST_INTEGRATION]

    private static final HTML = 'html'
    private static final PROPERTIES_FILE_PROP = "codenarc.properties.file"
    private static final BUILD_EXCEPTION = new BuildException('Test BuildException')

    private codeNarc
    private codeNarcAntTask
    private ant
    private binding
    private systemExitStatus

    private expectedRuleSetFiles
    private expectedMaxPriority1Violations
    private expectedMaxPriority2Violations
    private expectedMaxPriority3Violations
    private expectedReports
    private expectedFilesetIncludes
    private expectCallToConfigSlurper = true

    //-------------------------------------------------------------------------
    // Tests
    //-------------------------------------------------------------------------

    void testRun_Defaults() {
        testRun([:])
    }

    void testRun_CustomCodeNarcPropertiesFile() {
        assert !System.getProperty(PROPERTIES_FILE_PROP)
        def codeNarcConfig = [propertiesFile:'dir/xxx.properties']
        testRun(codeNarcConfig)
        assert System.getProperty(PROPERTIES_FILE_PROP) == 'file:dir/xxx.properties'
        System.setProperty(PROPERTIES_FILE_PROP, '')
    }

    void testRun_CodeNarcPropertiesClosure() {
        final PROPS_FILE_NAME = 'target/CodeNarcTemp.properties'
        assert !System.getProperty(PROPERTIES_FILE_PROP)
        final PROPERTIES_CLOSURE = {
            GrailsPublicControllerMethod.enabled = false
            EmptyIfStatement.priority = 1
        }
        final PROPERTIES = [
            'GrailsPublicControllerMethod.enabled':'false',
            'EmptyIfStatement.priority':'1'
        ] as Properties
        new File('target').mkdir()
        testRun([properties:PROPERTIES_CLOSURE])

        assert System.getProperty(PROPERTIES_FILE_PROP) == 'file:' + PROPS_FILE_NAME

        def propertiesFile = new File(PROPS_FILE_NAME)
        propertiesFile.withInputStream { inputStream ->
            def properties = new Properties()
            properties.load(inputStream)
            println "properties=$properties"
            assert properties == PROPERTIES
        }
    }

    void testRun_CodeNarcPropertiesNotAClosure_ThrowsException() {
        shouldFailWithMessageContaining('properties') { testRun(['properties':123]) }
    }

    void testRun_NoBuildConfigGroovy() {
        binding.configClassname = 'NoSuchClass'
        expectCallToConfigSlurper = false
        testRun([:])
    }

    void testRun_NoConfigGroovy() {
        binding.oldConfigClassName = 'NoSuchClass'
        expectCallToConfigSlurper = false
        testRun([:])
    }

    void testRun_ConfigGroovy_ContainsNoCodeNarcConfig() {
        testRun([:], [:])
    }

    void testRun_ConfigGroovy_ContainsCodeNarcConfig() {
        def sysErrOutput = captureSystemErr {
            testRun([:], [reportName:'MyCodeNarcReport'])
        }
        println "System.err=$sysErrOutput"
        assert sysErrOutput.contains('Config.groovy')
    }

    void testRun_SpecifyRuleSetFilesAsACollection() {
        def codeNarcConfig = [ruleSetFiles:['FFF', 'rulesets/basic.xml', 'http://myrules.com']]
        expectedRuleSetFiles = 'FFF,rulesets/basic.xml,http://myrules.com'
        testRun(codeNarcConfig)
    }

    void testRun_OverrideDefaults() {
        def codeNarcConfig = [
                reportName:'RRR', reportType:'xml', reportTitle:'TTT', ruleSetFiles:'FFF',
                maxPriority1Violations:11, maxPriority2Violations:12, maxPriority3Violations:13]

        expectedReports = [ [type:'xml', outputFile:'RRR', title:'TTT'] ]
        expectedMaxPriority1Violations = 11
        expectedMaxPriority2Violations = 12
        expectedMaxPriority3Violations = 13
        expectedRuleSetFiles = 'FFF'
        testRun(codeNarcConfig)
    }

    void testRun_TurnOffSomeDefaultIncludes() {
        expectedFilesetIncludes = [SERVICES, TAGLIB, UTILS, TEST_INTEGRATION]
        def codeNarcConfig = [processDomain:false, processSrcGroovy:false, processControllers:false, processTestUnit:false]
        testRun(codeNarcConfig)
    }

    void testRun_TurnOffOtherDefaultIncludes() {
        expectedFilesetIncludes = [SRC_GROOVY, CONTROLLERS, DOMAIN, TEST_UNIT]
        def codeNarcConfig = [processServices:false, processTaglib:false, processUtils:false, processTestIntegration:false]
        testRun(codeNarcConfig)
    }

    void testRun_ExplicitlyIncludeDefaultIncludes() {
        def codeNarcConfig = [
                processDomain:true, processSrcGroovy:true, processControllers:true, processServices:true,
                processTaglib:true, processUtils:true, processTestUnit:true, processTestIntegration:true]
        testRun(codeNarcConfig)
    }

    void testRun_IncludeViews() {
        expectedFilesetIncludes = DEFAULT_INCLUDES + [VIEWS]
        def codeNarcConfig = [processViews:true]
        testRun(codeNarcConfig)
    }

    void testRun_ExtraIncludeDirs() {
        final EXTRA = ['abc', 'def/ghi']
        expectedFilesetIncludes = [SRC_GROOVY, CONTROLLERS, DOMAIN, TEST_UNIT, 'abc/**/*.groovy', 'def/ghi/**/*.groovy']
        def codeNarcConfig = [processServices:false, processTaglib:false, processUtils:false, processTestIntegration:false, extraIncludeDirs:EXTRA]
        testRun(codeNarcConfig)
    }

    // Reports

    void testRun_ReportsClosure_OneReport() {
        final REPORTS = {
            MyXmlReport('xml') {
                outputFile = 'RRR'
                title = 'TTT'
            }
        }
        expectedReports = [ [type:'xml', outputFile:'RRR', title:'TTT'] ]
        testRun([reports:REPORTS])
    }

// CodeNarc jar is not part of classpath at test time
//    void testRun_ReportsClosure_ReportWriterClass() {
//        final REPORTS = {
//            MyXmlReport(XmlReportWriter) {
//                outputFile = 'RRR'
//                title = 'TTT'
//            }
//        }
//        expectedReports = [ [type:'org.codenarc.report.XmlReportWriter', outputFile:'RRR', title:'TTT'] ]
//        testRun([reports:REPORTS])
//    }

    void testRun_ReportsClosure_TwoReports() {
        final REPORTS = {
            MyConsoleReport('console') {
                title = 'CCC'
            }
            MyOtherReport('html') {
                title = 'TTT'
                writeToStandardOut = true
            }
        }
        expectedReports = [ [type:'console', title:'CCC'], [type:'html', title:'TTT', writeToStandardOut:true] ]
        testRun([reports:REPORTS])
    }

    void testRun_ReportsClosure_OverridesReportProperties() {
        final REPORTS = {
            MyXmlReport('xml') {
                outputFile = 'RRR'
                title = 'TTT'
            }
        }
        expectedReports = [ [type:'xml', outputFile:'RRR', title:'TTT'] ]
        testRun([reports:REPORTS, reportName:'XXX', reportType:'text', reportTitle:'Ignore'])
    }

    void testRun_ReportsClosure_DoesNotDefineReportType_ThrowsException() {
        final REPORTS = {
            MyReport {
                outputFile = 'RRR'
                title = 'TTT'
            }
        }
        shouldFailWithMessageContaining('type') { testRun([reports:REPORTS]) }
    }

    void testRun_ReportsNotAClosure_ThrowsException() {
        final REPORTS = [type:'xml']
        shouldFailWithMessageContaining('reports') { testRun([reports:REPORTS]) }
    }

    void testRun_ReportNotAClosure_ThrowsException() {
        final REPORTS = {
            MyReport('xml', 123)
        }
        shouldFailWithMessageContaining('MyReport') { testRun([reports:REPORTS]) }
    }

    // Throws BuildException

    void testRun_ThrowsBuildException_SystemExitOnBuildException_False() {
        def codeNarcConfig = [systemExitOnBuildException:false]
        shouldFail(BuildException) {
            testRunThrowsException(codeNarcConfig, BUILD_EXCEPTION)
        }
    }

    void testRun_ThrowsBuildException_SystemExitOnBuildException_True() {
        def codeNarcConfig = [systemExitOnBuildException:true]
        testRunThrowsException(codeNarcConfig, BUILD_EXCEPTION)
        assert systemExitStatus == 1
    }

    void testRun_ThrowsBuildException_SystemExitOnBuildException_NotSet_DefaultsToTrue() {
        def codeNarcConfig = [:]
        testRunThrowsException(codeNarcConfig, BUILD_EXCEPTION)
        assert systemExitStatus == 1
    }

    //-------------------------------------------------------------------------
    // Setup and Helper Methods
    //-------------------------------------------------------------------------

    /**
     * Perform a test of the Codenarc.groovy script. Populate the test-specific application configuration
     * (simulating "Config.groovy") with the specified config Map. Execute the script, mocking out the
     * Ant/Gant infrastructure and script dependencies. Compare the parameters passed to the CodeNarc
     * ant task with the "expected" variables set up for this test.
     *
     * @param codeNarcConfig - the Map of configuration elements made available through the
     *      (simulated) "Config.groovy", under the "codenarc" key.
     */
    private void testRun(Map codeNarcConfig, Map oldCodeNarcConfig=[:]) {
        ant.demand.taskdef { args -> assert args == TASKDEF }

        ant.demand.codenarc { props, closure ->
            println "codenarc ant task properties=$props"
            assert props.ruleSetFiles == expectedRuleSetFiles
			assert props.maxPriority1Violations == expectedMaxPriority1Violations
			assert props.maxPriority2Violations == expectedMaxPriority2Violations
			assert props.maxPriority3Violations == expectedMaxPriority3Violations

            expectedReports.each { expectedReport ->
                def otherProperties = new HashMap(expectedReport)
                otherProperties.remove('type')

                // Build internal delegate for the report closure; handle the calls to option()
                def options = [:]
                def optionDelegate = new Expando([option:{ map ->
                    println "call to option() with $map"
                    options << [(map.name):(map.value)]}])

                codeNarcAntTask.demand.report { reportProps, reportClosure ->
                    assert reportProps == [type:expectedReport.type]
                    reportClosure.delegate = optionDelegate
                    reportClosure.resolveStrategy = Closure.DELEGATE_FIRST
                    reportClosure()
                    assert options == otherProperties
                }
            }
            codeNarcAntTask.demand.fileset { args ->
                println "fileset properties=$args"
                assert args == [dir:FILESET_DIR, includes:expectedFilesetIncludes.join(',')]
            }

            def codeNarcAntTaskProxy = codeNarcAntTask.proxyInstance()
            closure.delegate = codeNarcAntTaskProxy
            closure()

            codeNarcAntTask.verify(codeNarcAntTaskProxy)
        }

        runCodeNarc(codeNarcConfig, oldCodeNarcConfig)
    }

    private void testRunThrowsException(Map codeNarcConfig, exception) {
        ant.demand.taskdef { args -> assert args == TASKDEF }

        ant.demand.codenarc { props, closure ->
            println "codenarc ant task properties=$props"
            throw exception
        }

        runCodeNarc(codeNarcConfig)
    }

    private void runCodeNarc(Map codeNarcConfig, Map oldCodeNarcConfig=[:]) {
        def config = [codenarc:codeNarcConfig]
        def oldConfig = [codenarc:oldCodeNarcConfig]
        def antProxy = ant.proxyInstance()
        binding.setVariable("ant", antProxy)

        if (expectCallToConfigSlurper) {
            def configObject = new ConfigObject()
            configObject.putAll(config)

            def oldConfigObject = new ConfigObject()
            oldConfigObject.putAll(oldConfig)

          // Stub out the call to parse the actual BuildConfig.groovy
          def configObjects = [configObject, oldConfigObject]
          binding.configParser = { className -> configObjects.pop()  }

          codeNarc.run()
        }
        else {
            codeNarc.run()
        }

        ant.verify(antProxy)
    }


    void setUp() {
        super.setUp()

        ant = new MockFor(AntBuilder)
        codeNarcAntTask = new MockFor(Object)

        // Stub out GANT infrastructure and script dependencies
        binding = new Binding();
        binding.setVariable("includeTargets", [])
        binding.setVariable("grailsScript", { })
        binding.setVariable("target", { args, closure -> closure() })
        binding.setVariable("compile", null)
        binding.setVariable("depends", { target -> })
        binding.setVariable("classesDirPath", '.')

        codeNarc = loadScriptClass()

        System.metaClass.static.exit = { int exitCode -> println "Calling System.exit($exitCode)"; systemExitStatus = exitCode }

        // Initialize expected values for all configurable values
        expectedRuleSetFiles = RULESET_FILES
        expectedMaxPriority1Violations = MAX
        expectedMaxPriority2Violations = MAX
        expectedMaxPriority3Violations = MAX
        expectedFilesetIncludes = DEFAULT_INCLUDES
        expectedReports = [ [type:HTML, outputFile:REPORT_FILE, title:''] ]
    }

    private loadScriptClass() {
        final SCRIPT = "scripts/Codenarc.groovy"
        def scriptText = new File(SCRIPT).text
        GroovyClassLoader gcl = new GroovyClassLoader()
        def scriptClass = gcl.parseClass(scriptText)
        return scriptClass.newInstance([binding] as Object[])
    }

}