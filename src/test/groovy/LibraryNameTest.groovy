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

    @Unroll
    def "Library version is correct for develop branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("develop")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == majorVersion + ".x-develop-SNAPSHOT"

        where:
        baseVersion << ['1.2.3', '1.1', '4.2', '3.2.1']
        majorVersion << ['1', '1', '4', '3']
    }

    @Unroll
    def "Library version is correct for feature branch with JIRA key: #branch"() {
        given:
        setBranch(branch)

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == jiraKey + "-SNAPSHOT"

        where:
        branch << ['feature/GAT-123-text-that-should-be-cut-off', 'feature/MOCOM-1212-arbitrary', 'feature/GIOCPIIA-1-description']
        jiraKey << ['GAT-123', 'MOCOM-1212', 'GIOCPIIA-1']
    }

    @Unroll
    def "Library version is correct for arbitrary branch with JIRA key: #branch"() {
        given:
        setBranch(branch)

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == jiraKey + "-SNAPSHOT"

        where:
        branch << ['GGG-453-get-rid-of-this', 'GIOCP-123123-something', 'GA-9-short']
        jiraKey << ['GGG-453', 'GIOCP-123123', 'GA-9']
    }

    @Unroll
    def "Library version correctly adds a zero for release branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("release/" + baseVersion)

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == baseVersion + ".0-SNAPSHOT"

        where:
        baseVersion << ['1.2', '3.1', '5.5']
    }

    @Unroll
    def "Library version is correct for hotfix branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("hotfix/" + baseVersion)

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == baseVersion + "-SNAPSHOT"

        where:
        baseVersion << ['2.1.3', '1.1.2', '4.0.1']
    }

    @Unroll
    def "Library version uses exact version when all three elements of a semantic version are provided on master: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == baseVersion

        where:
        baseVersion << ['3.1.8', '1.0.1', '2.5.7']
    }

    @Unroll
    def "Library version adds a zero when only two elements of a semantic version are provided on master: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        def result = getLibraryVersion()

        then:
        result.output.trim() == baseVersion + ".0"

        where:
        baseVersion << ['3.1', '1.0', '2.5']
    }

    @Unroll
    def "Fail the build if a library version is requested for a feature branch without a JIRA key: #branch"() {
        given:
        setBranch(branch)

        when:
        getLibraryVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess

        where:
        branch << ['feature/some-arbitrary-name', 'feature/GGA-12R-not-actually-a-key', 'feature/123-GAT-reversed', 'feature/GGA5-123-not-actually-a-key']
    }

    @Unroll
    def "Fail the build if a library version is requested for a dev branch without a JIRA key: #branch"() {
        given:
        setBranch(branch)

        when:
        getLibraryVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess

        where:
        branch << ['another-arbitrary-name', 'GPA-56H-not-quite', '555-HTY-reversed-again', 'YTT4-424-still-not-right']
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
