package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.Environment;
import de.bonndan.nivio.input.dto.ServiceDescription;
import de.bonndan.nivio.input.dto.SourceReference;
import de.bonndan.nivio.landscape.ServiceItem;
import de.bonndan.nivio.landscape.ServiceItems;
import de.bonndan.nivio.util.RootPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.util.StringUtils;

import java.io.File;

import static de.bonndan.nivio.landscape.ServiceItems.find;
import static de.bonndan.nivio.landscape.ServiceItems.pick;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.MockitoAnnotations.initMocks;

public class SourceReferencesResolverTest {

    @Mock
    ProcessLog log;

    @BeforeEach
    public void setup() {
        log = new ProcessLog(Mockito.mock(Logger.class));
    }

    @Test
    public void resolve() {

        File file = new File(RootPath.get() + "/src/test/resources/example/example_incremental_env.yml");
        Environment environment = EnvironmentFactory.fromYaml(file);
        assertFalse(environment.getSourceReferences().isEmpty());

        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();
        sourceReferencesResolver.resolve(environment, log);

        ServiceDescription mapped = (ServiceDescription) pick("blog-server", null, environment.getServiceDescriptions());
        assertNotNull(mapped);
        assertEquals("blog1", mapped.getShort_name());
        assertEquals("name2", mapped.getName());
    }

    @Test
    public void resolveOneReferenceIsNotAvailable() {

        //given
        File file = new File(RootPath.get() + "/src/test/resources/example/example_broken.yml");
        Environment environment = EnvironmentFactory.fromYaml(file);
        assertFalse(environment.getSourceReferences().isEmpty());
        assertFalse(environment.isPartial());

        //when
        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();
        sourceReferencesResolver.resolve(environment, log);

        //then
        assertFalse(StringUtils.isEmpty(log.getError()));
        assertTrue(environment.isPartial());
    }

    @Test
    public void assignTemplateToAll() {

        File file = new File(RootPath.get() + "/src/test/resources/example/example_templates.yml");
        Environment environment = EnvironmentFactory.fromYaml(file);
        assertFalse(environment.getSourceReferences().isEmpty());

        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();
        sourceReferencesResolver.resolve(environment, log);

        ServiceDescription redis = (ServiceDescription) pick("redis", null, environment.getServiceDescriptions());
        assertNotNull(redis);
        assertEquals("allinsamegroup", redis.getGroup());

        ServiceDescription datadog = (ServiceDescription) pick("datadog", null, environment.getServiceDescriptions());
        assertNotNull(datadog);
        assertEquals("allinsamegroup", datadog.getGroup());

        //web has previously been assigned to group "content" and will not be overwritten by further templates
        ServiceDescription web = (ServiceDescription) pick("web", null, environment.getServiceDescriptions());
        assertNotNull(web);
        assertEquals("content", web.getGroup());
    }

    @Test
    public void assignsAllValues() {

        File file = new File(RootPath.get() + "/src/test/resources/example/example_templates.yml");
        Environment environment = EnvironmentFactory.fromYaml(file);
        assertFalse(environment.getSourceReferences().isEmpty());

        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();
        sourceReferencesResolver.resolve(environment, log);


        //web has previously been assigned to group "content" and will not be overwritten by further templates
        ServiceDescription web = (ServiceDescription) pick("web", null, environment.getServiceDescriptions());
        assertNotNull(web);
        assertEquals("content", web.getGroup());

        //other values from template
        assertNull(web.getName());
        assertNull(web.getShort_name());
        assertEquals("Wordpress", web.getSoftware());
        assertEquals("alphateam", web.getTeam());
        assertEquals("alphateam@acme.io", web.getContact());
        assertEquals(1, web.getTags().length);
    }

    @Test
    public void assignsOnlyToReferences() {

        File file = new File(RootPath.get() + "/src/test/resources/example/example_templates.yml");
        Environment environment = EnvironmentFactory.fromYaml(file);
        assertFalse(environment.getSourceReferences().isEmpty());

        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();
        sourceReferencesResolver.resolve(environment, log);


        ServiceDescription redis = (ServiceDescription) pick("redis", null, environment.getServiceDescriptions());
        assertNotNull(redis);
        assertNull(redis.getSoftware());
    }
}
