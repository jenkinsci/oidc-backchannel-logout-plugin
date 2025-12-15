package io.jenkins.plugins.oidc_backchannel_logout;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import jenkins.model.Jenkins;
import hudson.security.SecurityRealm;
import org.jenkinsci.plugins.oic.OicSecurityRealm;
import org.jenkinsci.plugins.oic.OicServerConfiguration;
import org.jenkinsci.plugins.oic.OicServerManualConfiguration;
import org.jenkinsci.plugins.oic.OicServerWellKnownConfiguration;

import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.accmod.restrictions.suppressions.SuppressRestrictedWarnings;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.JOSEObjectType;

import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Extension
public class OidcRootAction implements UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(OidcRootAction.class.getName());

    @Override
    public String getIconFileName() { return null; }

    @Override
    public String getDisplayName() { return null; }

    @Override
    public String getUrlName() { return "oidc"; }

    @RequirePOST
    @SuppressFBWarnings(value = "LSC_PERMISSION_CHECK", justification = "Public endpoint for OIDC Backchannel Logout. Authenticity is validated via JWT signature verification.")
    // lgtm[jenkins/no-permission-check]
    public void doDynamic(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        String path = req.getRestOfPath();
        if ("/backchannel-logout".equals(path)) {
            doBackchannelLogout(req, rsp);
        } else {
            rsp.sendError(404);
        }
    }

    @RequirePOST
    private void doBackchannelLogout(StaplerRequest2 req, StaplerResponse2 rsp) throws IOException, ServletException {
        LOGGER.info("Received Backchannel Logout Request");
        String logoutToken = req.getParameter("logout_token");

        if (logoutToken == null) {
            rsp.setStatus(400);
            return;
        }

        try {
            SecurityRealm realm = Jenkins.get().getSecurityRealm();
            if (!(realm instanceof OicSecurityRealm)) {
                LOGGER.warning("OIDC Security Realm is not configured.");
                rsp.setStatus(500);
                return;
            }
            OicSecurityRealm oicRealm = (OicSecurityRealm) realm;
            String clientId = oicRealm.getClientId();

            String jwksUri = resolveJwksUri(oicRealm);

            if (jwksUri == null || clientId == null) {
                LOGGER.warning("Could not resolve JWKS URI or Client ID.");
                rsp.setStatus(500);
                return;
            }

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("logout+jwt")));

            JWKSource<SecurityContext> keySource = new RemoteJWKSet<>(new URL(jwksUri));
            JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);

            JWTClaimsSet claims = jwtProcessor.process(logoutToken, null);

            if (!claims.getAudience().contains(clientId)) {
                 LOGGER.warning("Token verification failed: Audience mismatch.");
                 rsp.setStatus(400);
                 return;
            }
            
            String sid = (String) claims.getClaim("sid");
            if (sid != null) {
                SessionTracker.invalidate(sid);
                rsp.setStatus(200);
            } else {
                LOGGER.warning("Token verification passed, but SID claim is missing.");
                rsp.setStatus(400);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Backchannel verification failed: {0}", e.getMessage());
            rsp.setStatus(400);
        }
    }

    @SuppressRestrictedWarnings({OicSecurityRealm.class})
    private String resolveJwksUri(OicSecurityRealm oicRealm) {
        try {
            OicServerConfiguration serverConfig = oicRealm.getServerConfiguration();
            
            if (serverConfig == null) {
                return null;
            }

            if (serverConfig instanceof OicServerManualConfiguration) {
                return ((OicServerManualConfiguration) serverConfig).getJwksServerUrl();
            }

            if (serverConfig instanceof OicServerWellKnownConfiguration) {
                String wellKnownUrl = ((OicServerWellKnownConfiguration) serverConfig).getWellKnownOpenIDConfigurationUrl();
                return fetchJwksFromWellKnown(wellKnownUrl);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error resolving JWKS URI: {0}", e.getMessage());
        }
        return null;
    }

    private String fetchJwksFromWellKnown(String wellKnownUrl) {
        if (wellKnownUrl != null && !wellKnownUrl.isEmpty()) {
            try (InputStream is = new URL(wellKnownUrl).openStream()) {
                String jsonMetadata = IOUtils.toString(is, StandardCharsets.UTF_8);
                JSONObject json = JSONObject.fromObject(jsonMetadata);
                if (json.containsKey("jwks_uri")) {
                    return json.getString("jwks_uri");
                }
            } catch (Exception e) {
                 LOGGER.log(Level.WARNING, "Failed to fetch/parse Well-Known configuration: {0}", e.getMessage());
            }
        }
        return null;
    }
}