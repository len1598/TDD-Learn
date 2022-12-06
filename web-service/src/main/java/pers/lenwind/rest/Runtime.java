package pers.lenwind.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;
import pers.lenwind.container.Context;

public interface Runtime {
    Context getApplicationContext();

    Providers getProviders();

    ResourceContext createResourceContext(HttpServletRequest request, HttpServletResponse httpServletResponse);

    ResourceRouter getResourceRouter();
}
