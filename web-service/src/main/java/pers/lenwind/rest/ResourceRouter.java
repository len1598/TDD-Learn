package pers.lenwind.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;

interface ResourceRouter {
    ServerResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);
}
