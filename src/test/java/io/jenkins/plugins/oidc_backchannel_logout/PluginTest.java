package io.jenkins.plugins.oidc_backchannel_logout;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PluginTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testPluginLoads() {
        assert j.jenkins.getPlugin("oidc-backchannel-logout") != null;
    }
}