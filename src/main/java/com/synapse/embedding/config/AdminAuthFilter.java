package com.synapse.embedding.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * 管理后台鉴权过滤器。
 * 仅拦截 /api/admin/** 路径（排除 /api/admin/login 和 /api/admin/check）。
 * 如果 admin.password 为空，则自动放行。
 */
public class AdminAuthFilter implements Filter {

    private final EmbeddingConfig config;

    public AdminAuthFilter(EmbeddingConfig config) {
        this.config = config;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        String path = httpReq.getRequestURI();

        // 放行: 登录和检查接口
        if (path.equals("/api/admin/login") || path.equals("/api/admin/check")) {
            chain.doFilter(request, response);
            return;
        }

        // 无密码模式: 放行
        if (!config.getAdmin().isAuthRequired()) {
            chain.doFilter(request, response);
            return;
        }

        // 检查 session
        HttpSession session = httpReq.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("admin_authenticated"))) {
            chain.doFilter(request, response);
            return;
        }

        // 未认证
        httpResp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        httpResp.setContentType("application/json");
        httpResp.getWriter().write("{\"error\":\"Unauthorized\"}");
    }
}
