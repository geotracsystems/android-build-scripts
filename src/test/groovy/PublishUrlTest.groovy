import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll
import groovy.json.StringEscapeUtils

class PublishUrlTest extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    final String lib_common = StringEscapeUtils.escapeJava(System.getProperty("lib_common.gradle"))
    final String getPublishUrlTask = "getPublishUrl"
    File buildFile
    File serverPropertiesFile

    final String snapshotRepositoryUrl = "http://packages.geotracinternational.com/nexus/repository/maven-snapshots"
    final String releaseRepositoryUrl = "http://packages.geotracinternational.com/nexus/repository/maven-releases"

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        serverPropertiesFile = testProjectDir.newFile('version.properties')

        createBuildFile()
    }

    def createBuildFile() {
        //Build a basic Gradle file for testing
        //Overwrite if this has been called already
        buildFile.newWriter().withWriter { w ->
            w << """
            apply from: "${lib_common}"

            task ${getPublishUrlTask}() {
                println getPublishUrl()
            }
        """
        }
    }

    def setBranch(String branch) {
        serverPropertiesFile << """
            git.branch=${branch}
        """
    }

    def getPublishUrlRunner() {
        //Use the -q flag so only our own result is returned, not the other Gradle outputs
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments(getPublishUrlTask, '-q')
    }

    def getPublishUrl() {
        getPublishUrlRunner().build()
    }

    def getPublishUrlExpectingFailure() {
        getPublishUrlRunner().buildAndFail()
    }

    @Unroll
    def "Publish URL is snapshot repository for HEAD branch on build server"() {
        given:
        setBranch("HEAD")

        when:
        def result = getPublishUrl()

        then:
        result.output.trim() == snapshotRepositoryUrl
    }

    @Unroll
    def "Publish URL is snapshot repository for pre-release branch on build server: #branch"() {
        given:
        setBranch(branch)

        when:
        def result = getPublishUrl()

        then:
        result.output.trim() == snapshotRepositoryUrl

        where:
        branch << ['release/1.0', 'hotfix/2.0.3', 'MOCOM-4343-another', 'something-random', 'develop', 'feature/GAT-123-arbitrary', 'feature/no-key-for-some-reason']
    }

    def "Publish URL is release repository for master on build server"() {
        given:
        setBranch("master")

        when:
        def result = getPublishUrl()

        then:
        result.output.trim() == releaseRepositoryUrl
    }
}
