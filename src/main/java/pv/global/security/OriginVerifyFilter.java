package pv.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OriginVerifyFilter extends OncePerRequestFilter {
    static final String HEADER = "X-Origin-Verify";
    private final byte[] secret;

    public OriginVerifyFilter(@Value("${origin.verify-secret:}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length == 0) {
            log.warn("origin.verify-secret is empty: origin header check is disabled");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secret.length == 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String value = request.getHeader(HEADER);
        if (value == null || !MessageDigest.isEqual(secret, value.getBytes(StandardCharsets.UTF_8))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
