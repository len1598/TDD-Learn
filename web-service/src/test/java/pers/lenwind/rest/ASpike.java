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
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.*;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

        assertEquals("test", response.body());
    }

    static class ResourceServlet extends HttpServlet {
        private Application application;

        private Providers providers;

        public ResourceServlet(Application application, Providers providers) {
            this.application = application;
            this.providers = providers;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Stream<Class<?>> rootResources = application.getClasses().stream().filter(c -> c.isAnnotationPresent(Path.class));

            Object result = dispatch(req, rootResources);

            MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(result.getClass(), null, null, null);

            writer.writeTo(result, null, null, null, null, null, resp.getOutputStream());
        }

        // request scope
        Object dispatch(HttpServletRequest req, Stream<Class<?>> rootResources) {
            Class<?> rootClass = rootResources.findFirst().get();
            try {
                Object root = rootClass.getConstructor().newInstance();
                Method method = Arrays.stream(rootClass.getMethods()).filter(m -> m.isAnnotationPresent(GET.class)).findFirst().get();
                return method.invoke(root);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    static class TestApplication extends Application {
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
    }

    // application scope
    @NoArgsConstructor
    static class TestProvider implements Providers {
        private List<MessageBodyWriter> writers;
        private Application application;

        public TestProvider(TestApplication application) {
            this.application = application;
            ContextConfiguration configuration = new ContextConfiguration();
            configuration.from(application.getConfig());

            List<Class<?>> writerClasses = this.application.getClasses().stream().filter(MessageBodyWriter.class::isAssignableFrom).toList();
            for (Class writerClass : writerClasses) {
                configuration.bind(writerClass, writerClass);
            }

            Context context = configuration.toContext();


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
            writer.write(s);
            writer.flush();
        }
    }

    @Path("/test")
    @NoArgsConstructor
    static class TestResource {

        @GET
        public String get() {
            return "test";
        }
    }
}
