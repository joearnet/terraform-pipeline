import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.instanceOf
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ConditionalApplyPluginTest {
    @AfterEach
    public void reset() {
        Jenkinsfile.instance = null
        ConditionalApplyPlugin.reset()
    }

    private configureJenkins(Map config = [:]) {
        Jenkinsfile.instance = mock(Jenkinsfile.class)
        when(Jenkinsfile.instance.getStandardizedRepoSlug()).thenReturn(config.repoSlug)
        when(Jenkinsfile.instance.getEnv()).thenReturn(config.env ?: [:])
    }

    @Test
    void modifiesTerraformEnvironmentStageByDefault() {
        Collection actualPlugins = TerraformEnvironmentStage.getPlugins()

        assertThat(actualPlugins, hasItem(instanceOf(ConditionalApplyPlugin.class)))
    }

    @Nested
    class ShouldApply {
        @Test
        void returnsTrueForMasterByDefault() {
            configureJenkins(env: [ BRANCH_NAME: 'master' ])
            def plugin = new ConditionalApplyPlugin()

            assertTrue(plugin.shouldApply())
        }

        @Test
        void returnsFalseForNonMasterByDefault() {
            configureJenkins(env: [ BRANCH_NAME: 'notMaster' ])
            def plugin = new ConditionalApplyPlugin()

            assertFalse(plugin.shouldApply())
        }

        @Test
        void returnsTrueForFirstConfiguredBranch() {
            configureJenkins(env: [ BRANCH_NAME: 'qa' ])
            ConditionalApplyPlugin.withApplyOnBranch('qa', 'someOtherBranch')
            def plugin = new ConditionalApplyPlugin()

            assertTrue(plugin.shouldApply())
        }

        @Test
        void returnsTrueForOtherConfiguredBranches() {
            configureJenkins(env: [ BRANCH_NAME: 'someOtherBranch' ])
            ConditionalApplyPlugin.withApplyOnBranch('qa', 'someOtherBranch')
            def plugin = new ConditionalApplyPlugin()

            assertTrue(plugin.shouldApply())
        }

        @Test
        void returnsFalseForNonMatchingBranch() {
            configureJenkins(env: [ BRANCH_NAME: 'notQa' ])
            ConditionalApplyPlugin.withApplyOnBranch('qa', 'someOtherBranch')
            def plugin = new ConditionalApplyPlugin()

            assertFalse(plugin.shouldApply())
        }

        @Test
        void returnsTrueWhenBranchIsUnknown() {
            configureJenkins(env: [ : ])
            def plugin = new ConditionalApplyPlugin()

            assertTrue(plugin.shouldApply())
        }
    }
}

