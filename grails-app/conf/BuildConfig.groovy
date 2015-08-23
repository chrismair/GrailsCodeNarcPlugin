grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir	= "target/test-reports"

grails.project.dependency.resolution = {
	inherits "global"
	log "warn" 
	repositories {
		grailsCentral()
		mavenCentral()
	}

	dependencies {
		compile "org.codenarc:CodeNarc:0.24.1", {
			excludes "log4j", "groovy", "groovy-xml", "groovy-ant", "ant", "junit"
		}
	}

	plugins {
        build ':release:2.2.0', ':rest-client-builder:1.0.3', {
            export = false
        }
	}
}
