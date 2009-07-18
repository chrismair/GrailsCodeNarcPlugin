import grails.util.GrailsUtil

includeTargets << grailsScript('Compile')

target('default': 'Run CodeNarc') {
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
	return new ConfigSlurper(GrailsUtil.environment).parse(classLoader.loadClass('Config')).codenarc
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
