package de.bonndan.nivio.input.dto;


import de.bonndan.nivio.input.FileFetcher;
import de.bonndan.nivio.input.http.HttpService;
import de.bonndan.nivio.input.nivio.ServiceDescriptionFactoryNivio;
import de.bonndan.nivio.landscape.Lifecycle;
import de.bonndan.nivio.landscape.ServiceItem;
import de.bonndan.nivio.landscape.Status;
import de.bonndan.nivio.landscape.StatusItem;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ServiceDescriptionFactoryNivioTest {

    private FileFetcher fileFetcher;

    private ServiceDescriptionFactoryNivio descriptionFactory;

    @BeforeEach
    public void setup() {
        fileFetcher = new FileFetcher(new HttpService());
        descriptionFactory = new ServiceDescriptionFactoryNivio();
    }

    @Test
    public void readServiceAndInfra() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/wordpress.yml");
        String yml = fileFetcher.get(file);
        List<ServiceDescription> services = descriptionFactory.fromString(yml);
        ServiceDescription service = services.get(0);
        assertEquals(ServiceItem.LAYER_APPLICATION, service.getLayer());
        assertEquals("Demo Blog", service.getName());
        assertEquals("to be replaced", service.getNote());
        assertEquals("blog-server", service.getIdentifier());
        assertEquals("blog", service.getShort_name());
        assertEquals("1.0", service.getVersion());
        assertEquals("public", service.getVisibility());
        assertEquals("Wordpress", service.getSoftware());
        assertEquals("5", service.getScale());
        assertEquals("https://acme.io", service.getHomepage());
        assertEquals("https://git.acme.io/blog-server", service.getRepository());
        assertEquals("s", service.getMachine());
        assertNotNull(service.getNetworks());
        assertEquals("content", service.getNetworks().toArray()[0]);
        assertEquals("alphateam", service.getTeam());
        assertEquals("alphateam@acme.io", service.getContact());
        assertEquals("content", service.getGroup());
        assertEquals("docker", service.getHost_type());
        assertEquals(1, service.getTags().length);
        assertTrue(Arrays.asList(service.getTags()).contains("CMS"));
        assertEquals(Lifecycle.END_OF_LIFE, service.getLifecycle());

        assertNotNull(service.getStatuses());
        assertEquals(3, service.getStatuses().size());
        service.getStatuses().forEach(statusItem -> {
            Assert.assertNotNull(statusItem);
            Assert.assertNotNull(statusItem.getLabel());
            if (statusItem.getLabel().equals(StatusItem.SECURITY)) {
                Assert.assertEquals(Status.RED, statusItem.getStatus());
            }
            if (statusItem.getLabel().equals(StatusItem.CAPABILITY)) {
                Assert.assertEquals(Status.YELLOW, statusItem.getStatus());
            }
        });

        assertNotNull(service.getInterfaces());
        assertEquals(3, service.getInterfaces().size());
        service.getInterfaces().forEach(dataFlow -> {
            if (dataFlow.getDescription().equals("posts")) {
                Assert.assertEquals("form", dataFlow.getFormat());
            }
        });

        assertNotNull(service.getDataFlow());
        assertEquals(3, service.getDataFlow().size());
        service.getDataFlow().forEach(dataFlow -> {
            if (dataFlow.getDescription().equals("kpis")) {
                Assert.assertEquals("content-kpi-dashboard", dataFlow.getTarget());
            }
        });

        ServiceDescription web = services.get(2);
        assertEquals(ServiceItem.LAYER_INGRESS, web.getLayer());
        assertEquals("wordpress-web", web.getIdentifier());
        assertEquals("Webserver", web.getDescription());
        assertEquals("Apache", web.getSoftware());
        assertEquals("2.4", web.getVersion());
        assertEquals("Pentium 1 512MB RAM", web.getMachine());
        assertEquals("ops guys", web.getTeam());
        assertEquals("content", web.getNetworks().toArray()[0]);
        assertEquals("docker", web.getHost_type());
    }

    @Test
    public void readIngress() {

        SourceReference file = new SourceReference(getRootPath() + "/src/test/resources/example/services/dashboard.yml");
        String yml = fileFetcher.get(file);


        List<ServiceDescription> services = descriptionFactory.fromString(yml);
        ServiceDescription service = services.get(0);
        assertEquals(ServiceItem.LAYER_INGRESS, service.getLayer());
        assertEquals("Keycloak SSO", service.getName());
        assertEquals("keycloak", service.getIdentifier());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}