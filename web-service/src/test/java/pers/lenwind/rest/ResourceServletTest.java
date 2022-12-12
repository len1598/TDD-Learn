package pers.lenwind.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceServletTest extends ServletTest {


    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;
    private OutboundResponse response;
    private Providers providers;
    private OutboundResponseBuilder responseBuilder;

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        response = mock(OutboundResponse.class);
        providers = mock(Providers.class);
        responseBuilder = new OutboundResponseBuilder();

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    void setUp() throws Exception {
        super.setUp();
        when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE))).thenReturn(new MessageBodyWriter<>() {
            @Override
            public boolean isWriteable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
                return true;
            }

            @Override
            public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
                PrintWriter writer = new PrintWriter(entityStream);
                writer.write(s);
                writer.flush();
            }
        });

        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        when(delegate.createHeaderDelegate(NewCookie.class)).thenReturn(new RuntimeDelegate.HeaderDelegate<>() {
            @Override
            public NewCookie fromString(String value) {
                return null;
            }

            @Override
            public String toString(NewCookie value) {
                return String.format(Locale.ENGLISH, "%s=%s", value.getName(), value.getValue());
            }
        });
        RuntimeDelegate.setInstance(delegate);
    }

    @Test
    void should_use_status_from_response() throws Exception {
        responseBuilder.status(Response.Status.NOT_MODIFIED).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie", new NewCookie.Builder("SessionId").value("session").build(), new NewCookie.Builder("UserId").value("user").build());
        responseBuilder.headers(headers).returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertArrayEquals(new String[]{"SessionId=session", "UserId=user"}, httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    @Test
    void should_use_generic_entity_from_response() throws Exception {
        OutboundResponseBuilder outboundResponseBuilder = responseBuilder.body(new GenericEntity<>("txt", String.class), new Annotation[0], MediaType.TEXT_PLAIN_TYPE);
        outboundResponseBuilder.returnFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals("txt", httpResponse.body());
    }

    // TODO exist exception to client
    @Test
    void should_use_response_while_throw_web_application_exception_with_response() throws Exception {
        responseBuilder.body("error").status(Response.Status.FORBIDDEN).throwFrom(router);

        HttpResponse<String> httpResponse = get("/test");

        assertEquals("error", httpResponse.body());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }
    //      - WebApplicationException with null response, use ExceptionMapper

    @Test
    void should_use_exception_mapper_while_throw_other_exception() throws Exception {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(e -> responseBuilder.status(Response.Status.FORBIDDEN).build());

        HttpResponse<String> httpResponse = get("/test");

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    // TODO 500 if message writer missing
    // TODO entity is null, ignore messageBodyWriter
    class OutboundResponseBuilder {
        Response.Status status = Response.Status.OK;
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        GenericEntity entity = new GenericEntity("txt", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        OutboundResponseBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        OutboundResponseBuilder headers(MultivaluedHashMap<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        OutboundResponseBuilder body(String body) {
            this.entity = new GenericEntity(body, String.class);
            return this;
        }

        OutboundResponseBuilder body(GenericEntity entity, Annotation[] annotations, MediaType mediaType) {
            this.entity = entity;
            this.annotations = annotations;
            this.mediaType = mediaType;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            build(r -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(r));
        }

        void throwFrom(ResourceRouter router) {
            build(r -> {
                WebApplicationException exception = new WebApplicationException(r);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
            });
        }

        OutboundResponse build() {
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            return response;
        }

        private void build(Consumer<OutboundResponse> consumer) {
            consumer.accept(build());
        }
    }
}
