package pers.lenwind.rest;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;

public class ResourceServlet extends HttpServlet {
    private Runtime runtime;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Providers providers = runtime.getProviders();
        ResourceRouter router = runtime.getResourceRouter();

        OutboundResponse response;
        try {
            response = router.dispatch(req, runtime.createResourceContext(req, resp));
        } catch (WebApplicationException e) {
            response = (OutboundResponse) e.getResponse();
        } catch (Throwable throwable) {
            ExceptionMapper mapper = providers.getExceptionMapper(throwable.getClass());
            response = (OutboundResponse) mapper.toResponse(throwable);
        }

        resp.setStatus(response.getStatus());
        response.getHeaders().forEach((key, objects) ->
            objects.forEach(obj ->
                resp.addHeader(key, RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj))));

        GenericEntity entity = response.getGenericEntity();
        MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), response.getHeaders(), resp.getOutputStream());
    }
}
