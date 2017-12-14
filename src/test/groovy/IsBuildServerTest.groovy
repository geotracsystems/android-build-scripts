import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class IsBuildServerTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final String lib_common = System.getProperty("lib_common.gradle")
    final String isBuildServerTask = "isBuildServer"
    File buildFile
    File serverPropertiesFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        createBuildFile()
    }

    def createBuildFile() {
        //Build a basic Gradle file for testing
        //Overwrite if this has been called already
        buildFile.newWriter().withWriter { w ->
            w << """
            apply from: "${lib_common}"

            task ${isBuildServerTask}() {
                println isBuildServer()
            }
        """
        }
    }

    def isBuildServerRunner() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(isBuildServerTask, '-q')
    }

    def isBuildServer() {
        isBuildServerRunner().build()
    }

    def isBuildServerExpectingFailure() {
        isBuildServerRunner().buildAndFail()
    }

    def "Presence of server properties file means we are a build server"() {
        given:
        serverPropertiesFile = testProjectDir.newFile('version.properties')

        when:
        def result = isBuildServer()

        then:
        result.output.trim() == "true"
    }

    def "Absence of server properties file means we are not a build server"() {
        given: "No server properties file created"

        when:
        def result = isBuildServer()

        then:
        result.output.trim() == "false"
    }
}
