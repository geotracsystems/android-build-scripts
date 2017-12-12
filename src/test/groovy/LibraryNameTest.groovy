import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildSuccess

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class LibraryNameTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final String lib_common = System.getProperty("lib_common.gradle")
    final String getLibraryVersionTask = "getLibraryVersion"
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

            task ${getLibraryVersionTask}() {
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

    def getLibraryVersionRunner() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(getLibraryVersionTask, '-q')
    }

    def getLibraryVersion() {
        getLibraryVersionRunner().build()
    }

    def getLibraryVersionExpectingFailure() {
        getLibraryVersionRunner().buildAndFail()
    }

    def "Library version is correct for develop branch"() {
        given:
        setVersion("1.2.3")
        setBranch("develop")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "1.x-develop-SNAPSHOT"
    }

    def "Library version is correct for feature branch with JIRA key (feature/<JIRA-Key>-<descriptor>)"() {
        given:
        setBranch("feature/GAT-123-text-that-should-be-cut-off")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "GAT-123-SNAPSHOT"
    }

    def "Library version is correct for arbitrary branch with JIRA key (<JIRA-Key>-<Descriptor>)"() {
        given:
        setBranch("GGG-453-get-rid-of-this")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "GGG-453-SNAPSHOT"
    }

    def "Library version is correct for release branch"() {
        given:
        setVersion("1.2")
        setBranch("release/1.2")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "1.2.0-SNAPSHOT"
    }

    def "Library version is correct for hotfix branch"() {
        given:
        setVersion("2.1.3")
        setBranch("hotfix/2.1.3")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "2.1.3-SNAPSHOT"
    }

    def "Library version is correct for master"() {
        given:
        setVersion("3.1.8")
        setBranch("master")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == "3.1.8"
    }

    def "Fail the build if a library version is requested for a feature branch without a JIRA key"() {
        given:
        setBranch("feature/some-arbitrary-name")

        when:
        getLibraryVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess
    }

    def "Fail the build if a library version is requested for a dev branch without a JIRA key"() {
        given:
        setBranch("another-arbitrary-name")

        when:
        getLibraryVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess
    }

    @Unroll
    def "Fail the build if a library version is requested for a non-semantic base version: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        getLibraryVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess

        where:
        baseVersion << ['1', 'MOCOM-353', 'Test.Version', '1.Something.0', '1.2.3.4']
    }
}
