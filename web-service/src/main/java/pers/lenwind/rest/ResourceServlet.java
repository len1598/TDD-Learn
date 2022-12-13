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
import java.util.function.Supplier;

public class ResourceServlet extends HttpServlet {
    private Runtime runtime;
    private Providers providers;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;
        this.providers = runtime.getProviders();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();

        _respond(resp, () -> router.dispatch(req, runtime.createResourceContext(req, resp)));
    }

    private void _respond(HttpServletResponse resp, Supplier<OutboundResponse> supplier) {
        try {
            respond(resp, supplier.get());
        } catch (WebApplicationException exception) {
            _respond(resp, () -> (OutboundResponse) exception.getResponse());
        } catch (Throwable throwable) {
            _respond(resp, () -> from(throwable));
        }
    }

    private OutboundResponse from(Throwable throwable) {
        ExceptionMapper mapper = providers.getExceptionMapper(throwable.getClass());
        return (OutboundResponse) mapper.toResponse(throwable);
    }

    private void respond(HttpServletResponse resp, OutboundResponse response) throws IOException {
        resp.setStatus(response.getStatus());
        response.getHeaders().forEach((key, objects) ->
            objects.forEach(obj ->
                resp.addHeader(key, RuntimeDelegate.getInstance().createHeaderDelegate((Class<Object>) obj.getClass()).toString(obj))));

        GenericEntity entity = response.getGenericEntity();
        if (entity == null) {
            return;
        }
        MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(), entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(), response.getHeaders(), resp.getOutputStream());
    }
}
