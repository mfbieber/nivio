package de.bonndan.nivio.output.docs;

import de.bonndan.nivio.api.NotFoundException;
import de.bonndan.nivio.landscape.Landscape;
import de.bonndan.nivio.landscape.LandscapeRepository;
import de.bonndan.nivio.output.IconService;
import org.asciidoctor.Asciidoctor;

import static org.asciidoctor.Asciidoctor.Factory.create;

import org.asciidoctor.Attributes;
import org.asciidoctor.OptionsBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(path = "/docs")
public class DocsController {

    private final LandscapeRepository landscapeRepository;
    private final IconService iconService;

    @Autowired
    public DocsController(LandscapeRepository landscapeRepository, IconService iconService) {
        this.landscapeRepository = landscapeRepository;
        this.iconService = iconService;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/{landscape}")
    public ResponseEntity<String> docResource(@PathVariable(name = "landscape") final String landscapeIdentifier) {

        Landscape landscape = landscapeRepository.findDistinctByIdentifier(landscapeIdentifier);
        if (landscape == null)
            throw new NotFoundException("Landscape " + landscapeIdentifier + " not found");

        Map<String, Object> attributes = new HashMap<String, Object>();
        //attributes.put(Attributes.LINK_CSS, true);
        attributes.put(Attributes.COPY_CSS, true);
        attributes.put(Attributes.STYLESHEET_NAME, "http://themes.asciidoctor.org/stylesheets/github.css");
        Map<String, Object> options = OptionsBuilder.options()
                .headerFooter(true)
                .attributes(attributes)
                .asMap();

        AsciiDocGenerator generator = new AsciiDocGenerator();
        Asciidoctor asciidoctor = create();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/html");
        return new ResponseEntity<>(
                asciidoctor.convert(generator.toDocument(landscape), options),
                //generator.toDocument(landscape),
                headers,
                HttpStatus.OK
        );

    }

    @RequestMapping(method = RequestMethod.GET, path = "/{landscape}/report.html")
    public ResponseEntity<String> htmlResource(@PathVariable(name = "landscape") final String landscapeIdentifier) {

        Landscape landscape = landscapeRepository.findDistinctByIdentifier(landscapeIdentifier);
        if (landscape == null)
            throw new NotFoundException("Landscape " + landscapeIdentifier + " not found");

        ReportGenerator generator = new ReportGenerator(iconService);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/html");
        return new ResponseEntity<>(
                generator.toDocument(landscape),
                headers,
                HttpStatus.OK
        );

    }

    @RequestMapping(method = RequestMethod.GET, path = "/{landscape}/owners.html")
    public ResponseEntity<String> owners(@PathVariable(name = "landscape") final String landscapeIdentifier) {

        Landscape landscape = landscapeRepository.findDistinctByIdentifier(landscapeIdentifier);
        if (landscape == null)
            throw new NotFoundException("Landscape " + landscapeIdentifier + " not found");

        OwnersReportGenerator generator = new OwnersReportGenerator();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "text/html");
        return new ResponseEntity<>(
                generator.toDocument(landscape),
                headers,
                HttpStatus.OK
        );

    }
}
