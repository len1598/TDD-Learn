package pers.lenwind.rest;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pers.lenwind.container.Config;
import pers.lenwind.container.Context;
import pers.lenwind.container.ContextConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASpike {
    Server server;

    @BeforeEach
    void setUp() throws Exception {
        server = new Server(8080);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        ServletContextHandler handler = new ServletContextHandler(server, "/");
        TestApplication application = new TestApplication();
        handler.addServlet(new ServletHolder(new ResourceServlet(application, new TestProvider(application))), "/");

        server.setHandler(handler);
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void name() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(new URI("http://localhost:8080/")).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals("testtesttest", response.body());
    }

    static class ResourceServlet extends HttpServlet {
        private final Context context;
        private TestApplication application;

        private Providers providers;

        public ResourceServlet(TestApplication application, Providers providers) {
            this.application = application;
            this.providers = providers;
            context = application.getContext();
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Stream<Class<?>> rootResources = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));

            ResourceContext rc = application.createResourceContext(req, resp);

            ServerResponse result = dispatch(req, rootResources, rc);
            GenericEntity entity = result.getGenericEntity();

            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType());

            writer.writeTo(result, entity.getRawType(), entity.getType(), result.getAnnotations(), result.getMediaType(), result.getHeaders(), resp.getOutputStream());
        }

        // request scope
        ServerResponse dispatch(HttpServletRequest req, Stream<Class<?>> rootResources, ResourceContext rc) {
            Class<?> rootClass = rootResources.findFirst().get();
            try {
                Object root = rc.initResource(context.getInstance(rootClass).get());
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                Object result = method.invoke(root);

                GenericEntity entity = new GenericEntity(result, method.getGenericReturnType());
                // code, header, media type, body
                // types: pojo, void, GenericEntity -> Response
                return new ServerResponse() {
                    @Override
                    GenericEntity getGenericEntity() {
                        return entity;
                    }

                    @Override
                    Annotation[] getAnnotations() {
                        return new Annotation[0];
                    }

                    @Override
                    public int getStatus() {
                        return 0;
                    }

                    @Override
                    public StatusType getStatusInfo() {
                        return null;
                    }

                    @Override
                    public Object getEntity() {
                        return entity;
                    }

                    @Override
                    public <T> T readEntity(Class<T> entityType) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> entityType) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
                        return null;
                    }

                    @Override
                    public boolean hasEntity() {
                        return false;
                    }

                    @Override
                    public boolean bufferEntity() {
                        return false;
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public MediaType getMediaType() {
                        return null;
                    }

                    @Override
                    public Locale getLanguage() {
                        return null;
                    }

                    @Override
                    public int getLength() {
                        return 0;
                    }

                    @Override
                    public Set<String> getAllowedMethods() {
                        return null;
                    }

                    @Override
                    public Map<String, NewCookie> getCookies() {
                        return null;
                    }

                    @Override
                    public EntityTag getEntityTag() {
                        return null;
                    }

                    @Override
                    public Date getDate() {
                        return null;
                    }

                    @Override
                    public Date getLastModified() {
                        return null;
                    }

                    @Override
                    public URI getLocation() {
                        return null;
                    }

                    @Override
                    public Set<Link> getLinks() {
                        return null;
                    }

                    @Override
                    public boolean hasLink(String relation) {
                        return false;
                    }

                    @Override
                    public Link getLink(String relation) {
                        return null;
                    }

                    @Override
                    public Link.Builder getLinkBuilder(String relation) {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, Object> getMetadata() {
                        return null;
                    }

                    @Override
                    public MultivaluedMap<String, String> getStringHeaders() {
                        return null;
                    }

                    @Override
                    public String getHeaderString(String name) {
                        return null;
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    interface ResourceRouter {
        ServerResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);
    }

    static abstract class ServerResponse extends Response {
        abstract GenericEntity getGenericEntity();

        abstract Annotation[] getAnnotations();
    }

    @Getter
    static class TestApplication extends Application {

        private final Context context;

        public TestApplication() {
            ContextConfiguration config = new ContextConfiguration();
            config.from(this.getConfig());

            this.getClasses().stream()
                .filter(c -> c.isAnnotationPresent(Path.class))
                .map(c -> (Class) c)
                .forEach(c -> config.bind(c, c));

            List<Class<?>> writerClasses = this.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                config.bind(writerClass, writerClass);
            }

            context = config.toContext();
        }

        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(TestResource.class, StringMessageBodyWriter.class);
        }

        public Config getConfig() {
            return new Config() {
                @Named("prefix")
                public String prefix = "test";
            };
        }

        public ResourceContext createResourceContext(HttpServletRequest req, HttpServletResponse resp) {
            return new ResourceContext() {
                @Override
                public <T> T getResource(Class<T> resourceClass) {
                    return null;
                }

                @Override
                public <T> T initResource(T resource) {
                    return resource;
                }
            };
        }
    }

    // application scope
    @NoArgsConstructor
    static class TestProvider implements Providers {
        private List<MessageBodyWriter> writers;
        private TestApplication application;

        public TestProvider(TestApplication application) {
            this.application = application;
            Context context = this.application.getContext();

            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();

            writers = writerClasses.stream().map(c -> (MessageBodyWriter) context.getInstance(c).get()).toList();
        }

        @Override
        public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return null;
        }

        @Override
        public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return writers.stream().filter(w -> w.isWriteable(type, genericType, annotations, mediaType)).findFirst().get();
        }

        @Override
        public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
            return null;
        }

        @Override
        public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
            return null;
        }
    }

    @NoArgsConstructor
    @Provider
    static class StringMessageBodyWriter implements MessageBodyWriter<String> {
        @Inject
        @Named("prefix")
        String prefix;

        @Override
        public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
            return type == String.class;
        }

        @Override
        public void writeTo(String s, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
            PrintWriter writer = new PrintWriter(entityStream);
            writer.write(prefix+s);
            writer.flush();
        }
    }

    @Path("/test")
    @NoArgsConstructor
    static class TestResource {
        @Inject
        @Named("prefix")
        private String prefix;

        @GET
        public String get() {
            return prefix + "test";
        }
    }
}
