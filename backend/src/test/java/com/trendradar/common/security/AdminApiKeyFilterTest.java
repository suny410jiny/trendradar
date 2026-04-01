package com.trendradar.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AdminApiKeyFilterTest {
    private final AdminApiKeyFilter filter = new AdminApiKeyFilter("test-admin-key");

    @Test
    void adminApi_withValidKey_passes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        request.addHeader("X-Admin-Key", "test-admin-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void adminApi_withInvalidKey_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        request.addHeader("X-Admin-Key", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void adminApi_withoutKey_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/collect");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void nonAdminApi_withoutKey_passes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/trending");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
