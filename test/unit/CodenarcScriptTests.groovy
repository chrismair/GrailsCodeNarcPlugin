/*
 * Copyright 2009 the original author or authors.
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

/**
 * Unit tests for the CodeNarc script
 * 
 * @version $Revision: $ - $Date: $
 *
 * @author Chris Mair
 */
class CodenarcScriptTests extends GroovyTestCase {
    static final TASKDEF = [name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask']
    static final RULESET_FILES = 'rulesets/basic.xml,rulesets/exceptions.xml,rulesets/imports.xml,rulesets/grails.xml'
    static final MAX = Integer.MAX_VALUE
    static final REPORT_FILE = 'CodeNarcAntReport.html'
    static final FILESET_DIR = '.'
    static final SRC_GROOVY = 'src/groovy/**/*.groovy'
    static final CONTROLLERS = 'grails-app/controllers/**/*.groovy'
    static final DOMAIN = 'grails-app/domain/**/*.groovy'
    static final SERVICES = 'grails-app/services/**/*.groovy'
    static final TAGLIB = 'grails-app/taglib/**/*.groovy'
    static final UTILS = 'grails-app/utils/**/*.groovy'
    static final HTML = 'html'

    private codeNarc
    private codeNarcAntTask
    private ant
    private binding

    private expectedRuleSetFiles
    private expectedMaxPriority1Violations
    private expectedMaxPriority2Violations
    private expectedMaxPriority3Violations
    private expectedReportType
    private expectedReportToFile
    private expectedReportTitle
    private expectedFilesetIncludes

    //-------------------------------------------------------------------------
    // Tests
    //-------------------------------------------------------------------------

    void testRun_Defaults() {
        testRun([:])
    }

    void testRun_OverrideDefaults() {
        def codeNarcConfig = [
                reportName:'RRR', reportType:'xml', reportTitle:'TTT', ruleSetFiles:'FFF',
                maxPriority1Violations:11, maxPriority2Violations:12, maxPriority3Violations:13]
        expectedReportToFile = 'RRR'
        expectedReportType = 'xml'
        expectedReportTitle = 'TTT'
        expectedMaxPriority1Violations = 11
        expectedMaxPriority2Violations = 12
        expectedMaxPriority3Violations = 13
        expectedRuleSetFiles = 'FFF'
        testRun(codeNarcConfig)
    }

    void testRun_TurnOffSomeDefaultIncludes() {
        expectedFilesetIncludes = [SERVICES, TAGLIB, UTILS].join(',')
        def codeNarcConfig = [processDomain:false, processSrcGroovy:false, processControllers:false]
        testRun(codeNarcConfig)
    }

    void testRun_TurnOffOtherDefaultIncludes() {
        expectedFilesetIncludes = [SRC_GROOVY, CONTROLLERS, DOMAIN].join(',')
        def codeNarcConfig = [processServices:false, processTaglib:false, processUtils:false]
        testRun(codeNarcConfig)
    }

    void testRun_ExtraIncludeDirs() {
        final EXTRA = ['abc', 'def/ghi']
        expectedFilesetIncludes = [SRC_GROOVY, CONTROLLERS, DOMAIN, 'abc/**/*.groovy', 'def/ghi/**/*.groovy'].join(',')
        def codeNarcConfig = [processServices:false, processTaglib:false, processUtils:false, extraIncludeDirs:EXTRA]
        testRun(codeNarcConfig)
    }

    //-------------------------------------------------------------------------
    // Setup and Helper Methods
    //-------------------------------------------------------------------------

    /**
     * Perform a test of the Codenarc.groovy script. Populate the test-specific appliction configuration
     * (simulating "Config.groovy") with the specified config Map. Execute the script, mocking out the
     * Ant/Gant infrastructure and script dependencies. Compare the parameters passed to the CodeNarc
     * ant task with the "expected" variables set up for this test.
     *
     * @param codeNarcConfig - the Map of configuration elements made available through the
     *      (simulated) "Config.groovy", under the "codenarc" key.
     */
    private void testRun(Map codeNarcConfig) {
        def config = [codenarc:codeNarcConfig]
        ant.demand.taskdef { args -> assert args == TASKDEF }

        ant.demand.codenarc { props, closure ->
            println "codenarc ant task properties=$props"
            assert props.ruleSetFiles == expectedRuleSetFiles
			assert props.maxPriority1Violations == expectedMaxPriority1Violations
			assert props.maxPriority2Violations == expectedMaxPriority2Violations
			assert props.maxPriority3Violations == expectedMaxPriority3Violations

            codeNarcAntTask.demand.report { args ->
                println "report properties=$args"
                assert args == [type:expectedReportType, toFile:expectedReportToFile, title:expectedReportTitle]
            }
            codeNarcAntTask.demand.fileset { args ->
                println "fileset properties=$args"
                assert args == [dir:FILESET_DIR, includes:expectedFilesetIncludes]
            }

            def codeNarcAntTaskProxy = codeNarcAntTask.proxyInstance()
            closure.delegate = codeNarcAntTaskProxy
            closure()

            codeNarcAntTask.verify(codeNarcAntTaskProxy)
        }

        def antProxy = ant.proxyInstance()
        binding.setVariable("ant", antProxy)

        // Intercept and mock call to ConfigSlurper.parse() to provide test config data
        def mockConfigSlurper = new MockFor(ConfigSlurper)
        def configObject = new ConfigObject()
        configObject.putAll(config) 
        mockConfigSlurper.demand.parse { arg -> configObject }
        mockConfigSlurper.use {
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

        // Initialize expected values for all configurable values
        expectedRuleSetFiles = RULESET_FILES
        expectedMaxPriority1Violations = MAX
        expectedMaxPriority2Violations = MAX
        expectedMaxPriority3Violations = MAX
        expectedReportToFile = REPORT_FILE
        expectedReportType = HTML
        expectedReportTitle = ""
        expectedFilesetIncludes = [SRC_GROOVY, CONTROLLERS, DOMAIN, SERVICES, TAGLIB, UTILS].join(',')
    }

    private loadScriptClass() {
        final SCRIPT = "scripts/Codenarc.groovy"
        def scriptText = new File(SCRIPT).text
        GroovyClassLoader gcl = new GroovyClassLoader()
        def scriptClass = gcl.parseClass(scriptText)
        return scriptClass.newInstance([binding] as Object[])
    }

}
