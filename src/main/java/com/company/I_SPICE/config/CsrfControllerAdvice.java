package com.company.I_SPICE.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CsrfControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(CsrfControllerAdvice.class);

    @ModelAttribute("_csrf")
    public CsrfToken csrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute("_csrf");

        if (csrfToken == null) {
            // Try other possible attribute names
            csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

            if (csrfToken == null) {
                // Try attribute names used by Spring Security
                csrfToken = (CsrfToken) request.getAttribute(
                        "org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository.CSRF_TOKEN");
                csrfToken = (CsrfToken) request.getAttribute("csrfToken");

                if (csrfToken == null) {
                    logger.warn("CSRF token not found in request attributes. Available attributes: {}",
                            request.getAttributeNames());
                } else {
                    logger.info("Found CSRF token via alternative attribute name");
                }
            } else {
                logger.info("Found CSRF token via class name");
            }
        } else {
            logger.info("Found CSRF token via '_csrf' attribute");
        }

        if (csrfToken != null) {
            csrfToken.getToken(); // Force the token to be realized (Spring Security 6 deferred token fix)
        }

        return csrfToken;
    }
}