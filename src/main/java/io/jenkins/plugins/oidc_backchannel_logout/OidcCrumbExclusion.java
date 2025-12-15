package io.jenkins.plugins.oidc_backchannel_logout;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Extension
public class OidcCrumbExclusion extends CrumbExclusion {
    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/oidc/backchannel-logout")) {
            chain.doFilter(req, resp);
            return true;
        }
        return false; 
    }
}