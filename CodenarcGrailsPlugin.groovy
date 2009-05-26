class CodenarcGrailsPlugin {

	String version = '0.1'
	Map dependsOn = [:]

	String author = 'Burt Beckwith'
	String authorEmail = 'burt@burtbeckwith.com'
	String title = 'CodeNarc plugin'
	String description = 'Runs CodeNarc static analysis rules for Groovy source.'
	String documentation = 'http://grails.org/plugin/codenarc'

	def doWithSpring = {}

	def doWithApplicationContext = { applicationContext -> }

	def doWithWebDescriptor = { xml -> }

	def doWithDynamicMethods = { ctx -> }

	def onChange = { event -> }

	def onConfigChange = { event -> }
}
