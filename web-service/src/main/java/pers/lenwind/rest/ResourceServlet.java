package pers.lenwind.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {
    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        OutboundResponse response = router.dispatch(req, runtime.createResourceContext(req, resp));
        resp.setStatus(response.getStatus());
        response.getHeaders().forEach((key, objects) ->
            objects.forEach(obj ->
                resp.addHeader(key, RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj))));
    }
}
