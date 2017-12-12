import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class LibraryNameTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final File lib_common = new File(System.getProperty("lib_common.gradle"))
    File buildFile
    File serverPropertiesFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        serverPropertiesFile = testProjectDir.newFile('version.properties')

        //Load the common script
        testProjectDir.newFile('lib_common.gradle') << lib_common.text

        //Use an arbitrary starting version for tests that need to specify a version
        makeGetLibraryVersionTask("0.3.1")
    }

    def makeGetLibraryVersionTask(String baseVersion) {
        //Build a basic Gradle file for testing
        //Overwrite if this has been called already
        buildFile.newWriter().withWriter { w ->
            w << """
            apply from: "lib_common.gradle"

            task getLibraryVersion() {
                println getLibraryVersion("${baseVersion}")
            }
        """
        }
    }

    def runGetLibraryVersionTask() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('getLibraryVersion', '-q')
                .build()
    }

    def setBranch(String branch) {
        serverPropertiesFile << """
            git.branch=${branch}
        """
    }

    def assertOutput(def result, String expected) {
        //Trim off the new line that Gradle will print
        result.output.trim() == expected
    }

    def "getLibraryVersionGetsCorrectVersionForDevelop"() {
        given:
        makeGetLibraryVersionTask("1.2.3")
        setBranch("develop")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "1.x-develop-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForFeatureWithKey"() {
        given:
        setBranch("feature/GAT-123-text-that-should-be-cut-off")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "GAT-123-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForNoPrefixWithKey"() {
        given:
        setBranch("GGG-453-get-rid-of-this")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "GGG-453-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForFeatureWithoutKey"() {
        given:
        setBranch("feature/some-arbitrary-name")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "some-arbitrary-name-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForNoPrefixWithoutKey"() {
        given:
        setBranch("another-arbitrary-name")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "another-arbitrary-name-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForReleaseBranch"() {
        given:
        makeGetLibraryVersionTask("1.2")
        setBranch("release/1.2")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "1.2.0-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForHotfixBranch"() {
        given:
        makeGetLibraryVersionTask("2.1.3")
        setBranch("hotfix/2.1.3")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "2.1.3-SNAPSHOT")
    }

    def "getLibraryVersionGetsCorrectVersionForMaster"() {
        given:
        makeGetLibraryVersionTask("3.1.8")
        setBranch("master")

        when:
        def result = runGetLibraryVersionTask()

        then:
        assertOutput(result, "3.1.8")
    }
}
