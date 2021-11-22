package com.zextras.team.core.api;

import java.io.IOException;
import javax.annotation.Generated;
import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public class ApiOriginFilter implements javax.servlet.Filter {
public void doFilter(ServletRequest request, ServletResponse response,
FilterChain chain) throws IOException, ServletException {
HttpServletResponse res = (HttpServletResponse) response;
res.addHeader("Access-Control-Allow-Origin", "*");
res.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
res.addHeader("Access-Control-Allow-Headers", "Content-Type");
chain.doFilter(request, response);
}

public void destroy() {}

public void init(FilterConfig filterConfig) throws ServletException {}
}