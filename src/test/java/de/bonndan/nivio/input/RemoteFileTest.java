package de.bonndan.nivio.input;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.bonndan.nivio.input.dto.SourceReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class RemoteFileTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    void configureSystemUnderTest() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void stopWireMockServer() {
        wireMockServer.stop();
    }

    @Test
    void fetchesYaml() {

        String path = getRootPath() + "/src/test/resources/example/services/wordpress.yml";
        String yml = FileFetcher.readFile(new File(path));

        givenThat(
                get("/some/file.yml").willReturn(aResponse().withStatus(200).withBody(yml))
        );

        String serverUrl = buildApiUrl();
        SourceReference sourceReference = new SourceReference(serverUrl);
        FileFetcher fetcher = new FileFetcher(new HttpService());
        String s = fetcher.get(sourceReference);
        assertEquals(yml, s);
    }

    private String buildApiUrl() {
        return String.format("http://localhost:%d/some/file.yml", wireMockServer.port());
    }

    private String getRootPath() {
        Path currentRelativePath = Paths.get("");
        return currentRelativePath.toAbsolutePath().toString();
    }
}
