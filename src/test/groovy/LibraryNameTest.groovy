import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class LibraryNameTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final String lib_common = System.getProperty("lib_common.gradle")
    File buildFile
    File serverPropertiesFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        serverPropertiesFile = testProjectDir.newFile('version.properties')

        //Use an arbitrary starting version for tests that need to specify a version
        setVersion("0.3.1")
    }

    def setVersion(String baseVersion) {
        //Build a basic Gradle file for testing
        //Overwrite if this has been called already
        buildFile.newWriter().withWriter { w ->
            w << """
            apply from: "${lib_common}"

            task getLibraryVersion() {
                println getLibraryVersion("${baseVersion}")
            }
        """
        }
    }

    def setBranch(String branch) {
        serverPropertiesFile << """
            git.branch=${branch}
        """
    }

    def getLibraryVersion() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('getLibraryVersion', '-q')
                .build()
    }

    def "getLibraryVersionGetsCorrectVersionForDevelop"() {
        given:
        setVersion("1.2.3")
        setBranch("develop")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "1.x-develop-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForFeatureWithKey"() {
        given:
        setBranch("feature/GAT-123-text-that-should-be-cut-off")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "GAT-123-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForNoPrefixWithKey"() {
        given:
        setBranch("GGG-453-get-rid-of-this")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "GGG-453-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForFeatureWithoutKey"() {
        given:
        setBranch("feature/some-arbitrary-name")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "some-arbitrary-name-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForNoPrefixWithoutKey"() {
        given:
        setBranch("another-arbitrary-name")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "another-arbitrary-name-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForReleaseBranch"() {
        given:
        setVersion("1.2")
        setBranch("release/1.2")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "1.2.0-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForHotfixBranch"() {
        given:
        setVersion("2.1.3")
        setBranch("hotfix/2.1.3")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "2.1.3-SNAPSHOT"
    }

    def "getLibraryVersionGetsCorrectVersionForMaster"() {
        given:
        setVersion("3.1.8")
        setBranch("master")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "3.1.8"
    }
}
