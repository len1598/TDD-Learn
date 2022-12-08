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
import java.util.Locale;

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
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

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
        responseBuilder.status(Response.Status.NOT_MODIFIED).build();

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), get("/test").statusCode());
    }

    @Test
    void should_use_headers_from_response() throws Exception {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        NewCookie sessionId = new NewCookie.Builder("SessionId").value("session").build();
        NewCookie userId = new NewCookie.Builder("UserId").value("user").build();
        headers.addAll("Set-Cookie", sessionId, userId);

        responseBuilder.headers(headers).build();

        assertArrayEquals(new String[]{"SessionId=session", "UserId=user"}, get("/test").headers().allValues("Set-Cookie").toArray(String[]::new));
    }

    @Test
    void should_use_generic_entity_from_response() throws Exception {
        GenericEntity<String> entity = new GenericEntity<>("txt", String.class);

        responseBuilder.body(entity, new Annotation[0], MediaType.TEXT_PLAIN_TYPE).build();

        assertEquals("txt", get("/test").body());
    }

    // TODO 500 if message writer missing
    // TODO exist exception to client
    //      - WebApplicationException with response, use response
    //      - WebApplicationException with null response, use ExceptionMapper
    //      - other Exception, use ExceptionMapper

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

        OutboundResponseBuilder body(GenericEntity entity, Annotation[] annotations, MediaType mediaType) {
            this.entity = entity;
            this.annotations = annotations;
            this.mediaType = mediaType;
            return this;
        }

        void build() {
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
        }
    }
}
