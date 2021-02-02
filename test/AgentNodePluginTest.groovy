import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.MatcherAssert.assertThat
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AgentNodePluginTest {
    private createJenkinsfileSpy() {
        def dummyJenkinsfile = spy(new DummyJenkinsfile())
        dummyJenkinsfile.docker = dummyJenkinsfile

        return dummyJenkinsfile
    }

    @Nested
    public class Init {
        @AfterEach
        void resetPlugins() {
            TerraformValidateStage.resetPlugins()
            TerraformEnvironmentStage.reset()
        }

        @Test
        void modifiesTerraformEnvironmentStage() {
            AgentNodePlugin.init()

            Collection actualPlugins = TerraformEnvironmentStage.getPlugins()
            assertThat(actualPlugins, hasItem(instanceOf(AgentNodePlugin.class)))
        }

        @Test
        void modifiesTerraformValidateStage() {
            AgentNodePlugin.init()

            Collection actualPlugins = TerraformValidateStage.getPlugins()
            assertThat(actualPlugins, hasItem(instanceOf(AgentNodePlugin.class)))
        }
    }

    @Nested
    class Apply {
        @Test
        void addsAgentClosureToTerraformEnvironmentStage() {
            def expectedClosure = { -> }
            def plugin = spy(new AgentNodePlugin())
            doReturn(expectedClosure).when(plugin).addAgent()
            def stage = mock(TerraformEnvironmentStage.class)

            plugin.apply(stage)

            verify(stage).decorate(anyString(), eq(expectedClosure))
        }

        @Test
        void addsAgentClosureToTerraformValidateStage() {
            def expectedClosure = { -> }
            def plugin = spy(new AgentNodePlugin())
            doReturn(expectedClosure).when(plugin).addAgent()
            def stage = mock(TerraformValidateStage.class)

            plugin.apply(stage)

            verify(stage).decorate(anyString(), eq(expectedClosure))
        }
    }

    @Nested
    class AddAgent {
        @Nested
        class WithNoDockerEnabled {
            @Test
            void callsTheInnerclosure() {
                def plugin = new AgentNodePlugin()
                def innerClosure = spy { -> }

                def agentClosure = plugin.addAgent()

                agentClosure(innerClosure)

                verify(innerClosure).call()
            }
        }

        @Nested
        class WithDockerImageNoDockerfile {
            private String expectedImage = 'someImage'
            @BeforeEach
            void useDockerImage() {
                AgentNodePlugin.withAgentDockerImage(expectedImage)
            }

            @AfterEach
            void reset() {
                AgentNodePlugin.reset()
            }

            @Test
            void callsTheInnerClosure() {
                def plugin = new AgentNodePlugin()
                def innerClosure = spy { -> }

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = new DummyJenkinsfile()
                agentClosure(innerClosure)

                verify(innerClosure).call()
            }

            @Test
            void usesTheGivenDockerImage() {
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).image(expectedImage)
            }

            @Test
            void usesTheGivenDockerOptions() {
                def expectedOptions = 'someOptions'
                AgentNodePlugin.withAgentDockerImageOptions(expectedOptions)
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()
                jenkinsfile.docker = jenkinsfile

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).inside(eq(expectedOptions), anyObject())
            }
        }

        @Nested
        class WithDockerImageAndDockerfile {
            private String expectedImage = 'someImage'
            @BeforeEach
            void useDockerImage() {
                AgentNodePlugin.withAgentDockerImage(expectedImage)
            }

            @AfterEach
            void reset() {
                AgentNodePlugin.reset()
            }

            @Test
            void callsTheInnerClosure() {
                AgentNodePlugin.withAgentDockerfile()
                def plugin = new AgentNodePlugin()
                def innerClosure = spy { -> }

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = new DummyJenkinsfile()
                agentClosure(innerClosure)

                verify(innerClosure).call()
            }

            @Test
            void usesTheGivenDockerImage() {
                AgentNodePlugin.withAgentDockerfile()
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).build(eq(expectedImage), anyString())
            }

            @Test
            void worksWithoutBuildOptions() {
                def expectedBuildCommand = '-f Dockerfile .'
                AgentNodePlugin.withAgentDockerfile('Dockerfile')
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).build(anyString(), eq(expectedBuildCommand))
            }

            @Test
            void usesTheGivenDockerBuildOptions() {
                def expectedOptions = 'expectedOptions'
                AgentNodePlugin.withAgentDockerfile()
                               .withAgentDockerBuildOptions(expectedOptions)
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).build(anyString(), contains(expectedOptions))
            }

            @Test
            void usesFileNamedDockerfileByDefault() {
                AgentNodePlugin.withAgentDockerfile()
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).build(anyString(), contains('-f Dockerfile'))
            }

            @Test
            void usesGivenDockerfile() {
                def expectedDockerfile = 'someDockerfile'
                AgentNodePlugin.withAgentDockerfile(expectedDockerfile)
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).build(anyString(), contains("-f ${expectedDockerfile}"))
            }

            @Test
            void usesGivenDockerOptions() {
                def expectedOptions = 'someOptions'
                AgentNodePlugin.withAgentDockerfile()
                               .withAgentDockerImageOptions(expectedOptions)
                def plugin = new AgentNodePlugin()
                def jenkinsfile = createJenkinsfileSpy()

                def agentClosure = plugin.addAgent()
                agentClosure.delegate = jenkinsfile
                agentClosure { -> }

                verify(jenkinsfile).inside(eq(expectedOptions), anyObject())
            }
        }
    }
}

