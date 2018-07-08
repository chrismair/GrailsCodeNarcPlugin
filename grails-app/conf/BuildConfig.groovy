grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"


grails.project.dependency.resolver = "maven"

grails.project.dependency.resolution = {
	inherits "global"
	log "warn" 
	repositories {
		grailsCentral()
		mavenCentral()
		mavenRepo "http://repo.grails.org/grails/plugins"
	}

	dependencies {
		compile "org.codenarc:CodeNarc:1.2", {
			excludes "groovy", "groovy-xml", "groovy-ant", "ant", "junit"
		}
	}

	plugins {
        build ':release:3.0.1', ':rest-client-builder:1.0.3', {
            export = false
        }
	}
}
