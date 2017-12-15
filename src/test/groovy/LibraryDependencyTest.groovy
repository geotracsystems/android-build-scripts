import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildSuccess
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class LibraryDependencyTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final String lib_common = System.getProperty("lib_common.gradle")
    final String getDependencyVersionTask = "getDependencyVersion"
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

            task ${getDependencyVersionTask}() {
                println getDependencyVersion("${baseVersion}")
            }
        """
        }
    }

    def setBranch(String branch) {
        serverPropertiesFile << """
            git.branch=${branch}
        """
    }

    def getDependencyVersionRunner() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(getDependencyVersionTask, '-q')
    }

    def getDependencyVersion() {
        getDependencyVersionRunner().build()
    }

    def getDependencyVersionExpectingFailure() {
        getDependencyVersionRunner().buildAndFail()
    }

    @Unroll
    def "Dependency version is the library's develop snapshots while on develop branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("develop")

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == majorVersion + ".x-develop-SNAPSHOT"

        where:
        baseVersion << ['1.2.3', '1.1', '4.2', '3.2.1']
        majorVersion << ['1', '1', '4', '3']
    }

    @Unroll
    def "Dependency version is the library's develop snapshots while on other development branch: #branch with version #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch(branch)

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == majorVersion + ".x-develop-SNAPSHOT"

        where:
        branch << ['feature/GAT-123-text-that-should-be-cut-off', 'feature/arbitrary-branch', 'MOCOM-423-amazing-feature', 'quick-fix']
        baseVersion << ['3.4', '9.4.1', '0.5', '6.2.3']
        majorVersion << ['3', '9', '0', '6']
    }

    @Unroll
    def "Dependency version is a full release for release branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("release/" + baseVersion)

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == baseVersion

        where:
        baseVersion << ['1.2.0', '3.1.0', '5.5.0']
    }

    @Unroll
    def "Dependency version correctly adds a zero for release branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("release/" + baseVersion)

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim().contains(baseVersion + ".0")

        where:
        baseVersion << ['6.2', '0.1', '7.9']
    }

    @Unroll
    def "Dependency version is a full release for hotfix branch: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("hotfix/" + baseVersion)

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == baseVersion

        where:
        baseVersion << ['2.1.3', '1.1.2', '4.0.1']
    }

    @Unroll
    def "Dependency version uses exact version when all three elements of a semantic version are provided on master: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == baseVersion

        where:
        baseVersion << ['3.1.8', '1.0.1', '2.5.7']
    }

    @Unroll
    def "Dependency version adds a zero when only two elements of a semantic version are provided on master: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        def result = getDependencyVersion()

        then:
        result.output.trim() == baseVersion + ".0"

        where:
        baseVersion << ['3.1', '1.0', '2.5']
    }

    @Unroll
    def "Fail the build if a dependency version is requested for a non-semantic base version: #baseVersion"() {
        given:
        setVersion(baseVersion)
        setBranch("master")

        when:
        getDependencyVersionExpectingFailure()

        then:
        notThrown UnexpectedBuildSuccess

        where:
        baseVersion << ['1', 'MOCOM-353', 'Test.Version', '1.Something.0', '1.2.3.4']
    }
}
