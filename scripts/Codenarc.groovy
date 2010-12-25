/*
 * Copyright 2010 the original author or authors.
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
import grails.util.GrailsUtil

includeTargets << grailsScript('Compile')

configClassname = 'Config'

target('codenarc': 'Run CodeNarc') {
	depends(compile)

	runCodenarc()
}

private void runCodenarc() {
	ant.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')

	def config = loadConfig()

	String reportName = config.reportName ?: 'CodeNarcReport.html'
	String reportType = config.reportType ?: 'html'
	String reportTitle = config.reportTitle ?: ''
	int maxPriority1Violations = getConfigInt(config, 'maxPriority1Violations', Integer.MAX_VALUE)
	int maxPriority2Violations = getConfigInt(config, 'maxPriority2Violations', Integer.MAX_VALUE)
	int maxPriority3Violations = getConfigInt(config, 'maxPriority3Violations', Integer.MAX_VALUE)
	String ruleSetFiles = config.ruleSetFiles ?:
		'rulesets/basic.xml,rulesets/exceptions.xml,rulesets/imports.xml,rulesets/grails.xml,rulesets/unused.xml'
	List includes = configureIncludes(config)

    configureCodeNarcPropertiesFile(config)

	println "Running CodeNarc ..."

	ant.codenarc(ruleSetFiles: ruleSetFiles,
			maxPriority1Violations: maxPriority1Violations,
			maxPriority2Violations: maxPriority2Violations,
			maxPriority3Violations: maxPriority3Violations) {

		report(type: reportType, toFile: reportName, title: reportTitle)
		fileset(dir: '.', includes: includes.join(','))
	}

	println "CodeNarc finished; report generated: $reportName"
}

private ConfigObject loadConfig() {
	def classLoader = Thread.currentThread().contextClassLoader
	classLoader.addURL(new File(classesDirPath).toURL())
    try {
        def className = getProperty('configClassname')
        return new ConfigSlurper(GrailsUtil.environment).parse(classLoader.loadClass(className)).codenarc
    }
    catch(ClassNotFoundException e) {
        return new ConfigObject()
    }
}

private void configureCodeNarcPropertiesFile(ConfigObject config) {
    final PROPERTIES_FILE_PROP = "codenarc.properties.file"
    if (config.propertiesFile) {
        def propValue = "file:" + config.propertiesFile
        System.setProperty(PROPERTIES_FILE_PROP, propValue)
    }
}

private int getConfigInt(config, String name, int defaultIfMissing) {
	def value = config[name]
	return value instanceof Integer ? value : defaultIfMissing
}

private boolean getConfigBoolean(config, String name) {
	def value = config[name]
	return value instanceof Boolean ? value : true
}

private List configureIncludes(config) {
	List includes = []

	if (getConfigBoolean(config, 'processSrcGroovy')) {
		includes << 'src/groovy/**/*.groovy'
	}

	if (getConfigBoolean(config, 'processControllers')) {
		includes << 'grails-app/controllers/**/*.groovy'
	}

	if (getConfigBoolean(config, 'processDomain')) {
		includes << 'grails-app/domain/**/*.groovy'
	}

	if (getConfigBoolean(config, 'processServices')) {
		includes << 'grails-app/services/**/*.groovy'
	}

	if (getConfigBoolean(config, 'processTaglib')) {
		includes << 'grails-app/taglib/**/*.groovy'
	}

	if (getConfigBoolean(config, 'processUtils')) {
		includes << 'grails-app/utils/**/*.groovy'
	}

    if (getConfigBoolean(config, 'processTestUnit')) {
        includes << 'test/unit/**/*.groovy'
    }

    if (getConfigBoolean(config, 'processTestIntegration')) {
        includes << 'test/integration/**/*.groovy'
    }

	for (includeDir in config.extraIncludeDirs) {
		includes << "$includeDir/**/*.groovy"
	}

	return includes
}

try {
    // Required for Grails 1.3 and later
	setDefaultTarget("codenarc")
}
catch(MissingMethodException e) {
	// Ignore. Older versions of Groovy/Grails do not implement this method
}

