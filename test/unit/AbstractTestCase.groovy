/*
 * Copyright 2011 the original author or authors.
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
 
/**
 * Abstract superclass for test classes.
 *
 * @author Chris Mair
 * @version $Revision: $ - $Date:  $
 */
abstract class AbstractTestCase extends GroovyTestCase {

    /**
     * Assert that the specified closure should throw an exception whose message contains text
     * @param text - the text expected within the message; may be a single String or a List of Strings
     * @param closure - the Closure to execute
     */
    protected void shouldFailWithMessageContaining(text, Closure closure) {
        def message = shouldFail(closure)
        log("exception message=[$message]")
        def strings = text instanceof List ? text : [text]
        strings.each { string ->
            assert message.contains(string), "[$message] does not contain [$string]"
        }
    }

    /**
     * Capture and return the output written to System.err while executing the specified Closure
     * @param closure - the Closure to execute
     * @return the System.err contents as a String
     */
    protected String captureSystemErr(Closure closure) {
        def originalSystemErr = System.err
        def outputStream = new ByteArrayOutputStream()
        try {
            System.err = new PrintStream(outputStream)
            closure()
        }
        finally {
            System.err = originalSystemErr
        }
        outputStream.toString()
    }

    protected void log(message) {
        println "${getName()}: $message"
    }

    private String classNameNoPackage() {
        def className = getClass().name
        def index = className.lastIndexOf('.')
        (index > -1) ? className.substring(index+1) : className
    }

    //------------------------------------------------------------------------------------
    // Test Setup and Tear Down
    //------------------------------------------------------------------------------------

//    void setUp() {
//        log "----------[ ${classNameNoPackage()}.${getName()} ]----------"
//        super.setUp()
//    }


}
