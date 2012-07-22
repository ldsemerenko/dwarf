package org.skife.galaxy.dwarf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

public class LibraryBehaviorTest
{

    @Test
    public void testPathsResolveAsDesired() throws Exception
    {
        Path full = Paths.get("/waffles/pancakes").resolve(Paths.get("sausage/bacon")).normalize();
        Path desired = Paths.get("/waffles/pancakes/sausage/bacon");
        assertThat((Object)full).isEqualTo(desired);
    }

    @Test
    public void testUriBehavior2() throws Exception
    {
        URI uri = new File(".").toURI().resolve(URI.create("http://skife.org/"));
        assertThat(uri).isEqualTo(URI.create("http://skife.org/"));
    }


    @Test
    @Ignore("fails, jackson bug")
    public void testFoo() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        String yaml = mapper.writeValueAsString(asList(new Foooo(UUID.randomUUID()),
                                                       new Foooo(UUID.randomUUID()),
                                                       new Foooo(UUID.randomUUID())));

        mapper.readValue(yaml, new TypeReference<List<Foooo>>()
        {
        });
    }

    private static class Foooo
    {

        Foooo(@JsonProperty("id") UUID id)
        {
            this.id = id;
        }

        private final UUID id;

        public UUID getId()
        {
            return id;
        }
    }


    @Test
    public void testBaggag() throws Exception
    {
        System.out.println(UUID.randomUUID());
    }

    @Test
    public void testBar() throws Exception
    {
        DataFormatDetector det = new DataFormatDetector(new JsonFactory(),
                                                        new YAMLFactory());
        ObjectMapper mapper = new ObjectMapper(new JsonFactory());

        C c = mapper.readValue("{\"foo\":{\n\"type\":\"bar\",\"say\":\"hello\"}}", C.class);

        assertThat(c.foo).isInstanceOf(FooBar.class);
        assertThat(c.foo.yarp()).isEqualTo("hello");
    }


    @Test
    public void testYaml() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        C c = mapper.readValue("foo: { type: bar, say: hello }", C.class);

        assertThat(c.foo).isInstanceOf(FooBar.class);
        assertThat(c.foo.yarp()).isEqualTo("hello");
    }

    @Test
    public void testBaz() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        C c = mapper.readValue("{\"foo\":{\n\"type\":\"baz\",\"nice\":\"hello\", \"willy\":\"you\"}}", C.class);

        assertThat(c.foo).isInstanceOf(FooBaz.class);
        assertThat(c.foo.yarp()).isEqualTo("hello you");
    }

    public static class C
    {
        public Foo foo;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = FooBar.class, name = "bar"),
                   @JsonSubTypes.Type(value = FooBaz.class, name = "baz")})
    public static interface Foo
    {
        public String yarp();
    }

    public static class FooBar implements Foo
    {

        private final String say;

        @JsonCreator
        public FooBar(@JsonProperty("say") String say)
        {
            this.say = say;
        }

        @Override
        public String yarp()
        {
            return say;
        }
    }

    public static class FooBaz implements Foo
    {

        private final String nice;
        private final String willy;

        @JsonCreator
        public FooBaz(@JsonProperty("nice") String nice, @JsonProperty("willy") String willy)
        {
            this.nice = nice;
            this.willy = willy;
        }

        @Override
        public String yarp()
        {
            return String.format("%s %s", nice, willy);
        }
    }


    @Test
    public void testDeserializeDeployDescriptorWithJackson() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        DD dd = mapper.readValue(new File("src/test/resources/echo-descriptor.yml"), DD.class);

        assertThat(dd.bundle).isEqualTo(URI.create("src/test/resources/echo.tar.gz"));

        Map<String, URI> config = ImmutableMap.of("/etc/runtime.properties",
                                                  URI.create("src/test/resources/runtime.properties"));
        assertThat(dd.config).isEqualTo(config);
    }

    public static class DD
    {
        private final URI              bundle;
        private final Map<String, URI> config;

        public DD(@JsonProperty("bundle") URI bundle,
                  @JsonProperty("config") Map<String, URI> config)
        {
            this.bundle = bundle;
            this.config = config;
        }

        public URI getBundle()
        {
            return bundle;
        }

        public Map<String, URI> getConfig()
        {
            return config;
        }
    }
}
