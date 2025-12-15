package io.jenkins.plugins.oidc_backchannel_logout;

import hudson.Extension;
import hudson.model.User;
import jenkins.security.SecurityListener;
import org.jenkinsci.plugins.oic.OicCredentials;
import com.nimbusds.jwt.SignedJWT;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpSession;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

@Extension
public class OidcSessionListener extends SecurityListener {
    private static final Logger LOGGER = Logger.getLogger(OidcSessionListener.class.getName());

    @Override
    protected void loggedIn(String username) {
        try {
            User user = User.get(username, false, null);
            if (user == null) return;

            OicCredentials credentials = user.getProperty(OicCredentials.class);
            if (credentials != null) {
                String idToken = credentials.getIdToken();
                if (idToken != null) {
                    SignedJWT jwt = SignedJWT.parse(idToken);
                    String sid = (String) jwt.getJWTClaimsSet().getClaim("sid");
                    
                    StaplerRequest2 req = Stapler.getCurrentRequest2();
                    if (req != null) {
                        HttpSession session = req.getSession(false);
                        if (sid != null && session != null) {
                            SessionTracker.track(sid, session);
                            LOGGER.fine("Successfully mapped SID to Jenkins Session.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to track OIDC session: {0}", e.getMessage());
        }
    }
}