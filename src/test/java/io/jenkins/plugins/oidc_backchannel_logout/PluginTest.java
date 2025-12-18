package io.jenkins.plugins.oidc_backchannel_logout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@WithJenkins
class PluginTest {

    private JenkinsRule j;

    @BeforeEach
    void beforeEach(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void testPluginLoads() {
        assertNotNull(j.jenkins.getPlugin("oidc-backchannel-logout"));
    }
}