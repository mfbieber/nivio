
package de.bonndan.nivio.input;

import de.bonndan.nivio.ProcessingException;
import de.bonndan.nivio.input.dto.Environment;
import de.bonndan.nivio.input.dto.ServiceDescription;
import de.bonndan.nivio.landscape.*;
import de.bonndan.nivio.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@Component
public class Indexer {

    private static final Logger _logger = LoggerFactory.getLogger(Indexer.class);

    private final LandscapeRepository landscapeRepo;
    private final ServiceRepository serviceRepo;
    private final NotificationService notificationService;

    private final SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver();

    @Autowired
    public Indexer(LandscapeRepository environmentRepo,
                   ServiceRepository serviceRepo,
                   NotificationService notificationService
    ) {
        this.landscapeRepo = environmentRepo;
        this.serviceRepo = serviceRepo;
        this.notificationService = notificationService;
    }

    public ProcessLog reIndex(final Environment input) {

        ProcessLog logger = new ProcessLog(_logger);

        Landscape landscape = landscapeRepo.findDistinctByIdentifier(input.getIdentifier());
        if (landscape == null) {
            logger.info("Creating new landscape " + input.getIdentifier());
            landscape = input.toLandscape();
            landscapeRepo.save(landscape);
        } else {
            landscape.setName(input.getName());
            landscape.setContact(input.getContact());
        }
        logger.setLandscape(landscape);

        try {
            sourceReferencesResolver.resolve(input, logger);

            diff(input, landscape, logger);
            linkDataflow(input, landscape, logger);
            landscapeRepo.save(landscape);
        } catch (ProcessingException e) {
            final String msg = "Error while reindexing landscape " + input.getIdentifier();
            logger.warn(msg, e);
            notificationService.sendError(e, msg);
        }

        logger.info("Reindexed landscape " + input.getIdentifier());
        return logger;
    }

    private void diff(final Environment environment, final Landscape landscape, ProcessLog logger) {

        List<Service> existingServices = serviceRepo.findAllByLandscape(landscape);

        //insert new ones
        List<ServiceItem> newItems = ServiceItems.added(environment.getServiceDescriptions(), existingServices);
        List<Service> inLandscape = new ArrayList<>();
        logger.info("Adding " + newItems.size() + " items in env " + landscape.getIdentifier());
        newItems.forEach(
                serviceDescription -> {
                    logger.info("Creating new service " + serviceDescription.getIdentifier() + " in env " + environment.getIdentifier());
                    Service created = ServiceFactory.fromDescription(serviceDescription, landscape);
                    serviceRepo.save(created);

                    landscape.addService(created);
                    inLandscape.add(created);
                }
        );

        //update existing
        List<ServiceItem> kept = new ArrayList<>();
        if (environment.isPartial()) {
            kept.addAll(existingServices); //we want to keep all, increment does not contain all services
        } else {
            kept = ServiceItems.kept(environment.getServiceDescriptions(), existingServices);
        }
        logger.info("Updating " + kept.size() + " services in landscape " + landscape.getIdentifier());
        kept.forEach(
                service -> {

                    ServiceDescription description = (ServiceDescription) ServiceItems.find(service.getFullyQualifiedIdentifier(), environment.getServiceDescriptions());
                    if (description == null) {
                        if (environment.isPartial()) {
                            inLandscape.add((Service) service);
                            return;
                        } else {
                            throw new ProcessingException(environment, "Service not found " + service.getIdentifier());
                        }
                    }

                    logger.info("Updating service " + service.getIdentifier() + " in landscape " + environment.getIdentifier());

                    ServiceFactory.assignAll((Service) service, description);
                    inLandscape.add((Service) service);
                }
        );

        landscape.setServices(inLandscape);
        linkAllProviders(inLandscape, environment, logger);
        deleteUnreferenced(environment, inLandscape, existingServices, logger);
    }

    private void deleteUnreferenced(final Environment environment, List<Service> kept, List<Service> all, ProcessLog logger) {
        if (environment.isPartial()) {
            logger.info("Incremental change, will not remove any unreferenced services.");
            return;
        }

        List<ServiceItem> removed = ServiceItems.removed(kept, all);
        logger.info("Removing " + removed.size() + " sources in env " + environment.getIdentifier());
        removed.forEach(
                service -> {
                    logger.info("Service " + service.getIdentifier() + " not contained anymore in env " + environment.getIdentifier() + ", deleting it.");
                    serviceRepo.delete(((Service) service).getId());
                }
        );
    }

    /**
     * Links all providers to a service
     */
    private void linkAllProviders(List<Service> services, Environment environment, ProcessLog logger) {

        boolean isPartial = environment.isPartial();
        services.forEach(
                service -> {
                    ServiceDescription description = (ServiceDescription) ServiceItems.find(service.getFullyQualifiedIdentifier(), environment.getServiceDescriptions());
                    if (description == null) {
                        if (isPartial)
                            return;
                        else
                            throw new ProcessingException(environment, "Service not found " + service.getIdentifier());
                    }

                    if (!isPartial) {
                        service.getProvidedBy().clear();
                    }

                    description.getProvided_by().forEach(providerName -> {
                        Service provider;
                        try {
                            var fqi = FullyQualifiedIdentifier.from(providerName);
                            provider = (Service) ServiceItems.find(fqi, services);
                            if (provider == null) {
                                logger.warn("Could not find service " + fqi + " in landscape " + environment + " while linking providers for service " + description.getFullyQualifiedIdentifier());
                                return;
                            }
                        } catch (IllegalArgumentException ex) {
                            logger.warn("Misconfigured provider in service " + description.getFullyQualifiedIdentifier());
                            return;
                        }

                        if (!ServiceItems.contains(provider, service.getProvidedBy())) {
                            service.getProvidedBy().add(provider);
                            provider.getProvides().add(service); //deprecated
                            logger.info("Adding provider " + provider + " to service " + service);
                        }
                    });
                }
        );
    }

    private void linkDataflow(final Environment input, final Landscape landscape, ProcessLog logger) {
        input.getServiceDescriptions().forEach(serviceDescription -> {
            Service origin = (Service) ServiceItems.pick(serviceDescription, landscape.getServices());
            if (!input.isPartial() && origin.getDataFlow().size() > 0) {
                logger.info("Clearing dataflow of " + origin);
                origin.getDataFlow().clear(); //delete all dataflow on full update
            }

            serviceDescription.getDataFlow().forEach(description -> {

                var fqi = FullyQualifiedIdentifier.from(description.getTarget());
                Service target = (Service) ServiceItems.find(fqi, landscape.getServices());
                if (target == null) {
                    logger.warn("Dataflow target service " + description.getTarget() + " not found");
                    return;
                }
                Iterator<DataFlowItem> iterator = origin.getDataFlow().iterator();
                DataFlow existing = null;
                DataFlow dataFlow = new DataFlow(origin, target.getFullyQualifiedIdentifier());
                while (iterator.hasNext()) {
                    existing = (DataFlow) iterator.next();
                    if (existing.equals(dataFlow)) {
                        logger.info(String.format("Updating dataflow between %s and %s", existing.getSource(), existing.getTarget()));
                        existing.setDescription(description.getDescription());
                        existing.setFormat(description.getFormat());
                        break;
                    }
                    existing = null;
                }

                if (existing == null) {
                    dataFlow.setDescription(description.getDescription());
                    dataFlow.setFormat(description.getFormat());

                    origin.getDataFlow().add(dataFlow);
                    logger.info(String.format("Adding dataflow between %s and %s", dataFlow.getSource(), dataFlow.getTarget()));
                }
            });
        });
    }
}