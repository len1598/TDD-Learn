package pers.lenwind.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.Test;

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

    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        response = mock(OutboundResponse.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

        return new ResourceServlet(runtime);
    }

    // TODO response code to client

    @Test
    void should_use_status_from_response() throws Exception {
        when(response.getStatus()).thenReturn(Response.Status.NOT_MODIFIED.getStatusCode());

        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), get("/test").statusCode());
    }

    // TODO response header to client

    @Test
    void should_use_headers_from_response() throws Exception {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        NewCookie sessionId = new NewCookie.Builder("SessionId").value("session").build();
        NewCookie userId = new NewCookie.Builder("UserId").value("user").build();
        headers.addAll("Set-Cookie", sessionId, userId);

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
        when(response.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(response.getHeaders()).thenReturn(headers);

        assertArrayEquals(new String[]{"SessionId=session", "UserId=user"}, get("/test").headers().allValues("Set-Cookie").toArray(String[]::new));

    }


    // TODO response body to client
    // TODO 500 if message writer missing
    // TODO exist exception to client
    //      - WebApplicationException with response, use response
    //      - WebApplicationException with null response, use ExceptionMapper
    //      - other Exception, use ExceptionMapper

}
