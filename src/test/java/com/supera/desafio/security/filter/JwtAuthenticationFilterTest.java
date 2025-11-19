package com.supera.desafio.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.supera.desafio.core.domain.user.DepartmentType;
import com.supera.desafio.security.model.AuthenticatedUser;
import com.supera.desafio.security.service.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtTokenService jwtTokenService = org.mockito.Mockito.mock(JwtTokenService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenService);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldIgnoreWhenHeaderMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(jwtTokenService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAuthenticateWhenTokenValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        AuthenticatedUser principal = new AuthenticatedUser(1L, "user@corp.com", "User", DepartmentType.TI);
        when(jwtTokenService.parse("token123")).thenReturn(principal);

        filter.doFilterInternal(request, response, chain);

        assertEquals(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void shouldSwallowParsingErrors() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        doThrow(new RuntimeException("invalid")).when(jwtTokenService).parse("bad");

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldIgnoreHeadersWithoutBearer() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Token token123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void shouldNotOverrideExistingAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        AuthenticatedUser principal = new AuthenticatedUser(1L, "user@corp.com", "User", DepartmentType.TI);
        when(jwtTokenService.parse("token123")).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null));

        filter.doFilterInternal(request, response, chain);

        assertEquals(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
}
