package de.bonndan.nivio.landscape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static de.bonndan.nivio.util.SafeAssign.assignSafe;

public class ServiceFactory {

    private static final Logger logger = LoggerFactory.getLogger(ServiceFactory.class);

    public static Service fromDescription(ServiceItem item, Landscape landscape) {
        if (item == null) {
            throw new RuntimeException("landscape item is null");
        }

        Service service = new Service();
        service.setLandscape(landscape);
        service.setIdentifier(item.getIdentifier());
        assignAll(service, item);
        return service;
    }

    /**
     * Assigns all values from the description except data flow and provided_by/provides. Description values
     * overwrite all fields except the group
     */
    public static void assignAll(Service service, ServiceItem description) {
        if (description == null) {
            logger.warn("ServiceDescription for service " + service.getIdentifier() + " is null in assignAllValues");
            return;
        }
        service.setName(description.getName());
        service.setLayer(description.getLayer() != null ? description.getLayer() : ServiceItem.LAYER_APPLICATION);
        service.setType(description.getType() != null ? description.getType() : ServiceItem.TYPE_SERVICE);

        service.setNote(description.getNote());
        service.setShort_name(description.getShort_name());
        service.setIcon(description.getIcon());
        service.setDescription(description.getDescription());
        service.setTags(description.getTags());
        service.setOwner(description.getOwner());

        service.setSoftware(description.getSoftware());
        service.setVersion(description.getVersion());
        service.setInterfaces(description.getInterfaces().stream().map(ServiceInterface::new).collect(Collectors.toSet()));

        service.setHomepage(description.getHomepage());
        service.setRepository(description.getRepository());
        service.setContact(description.getContact());
        service.setTeam(description.getTeam());

        service.setVisibility(description.getVisibility());
        service.setLifecycle(description.getLifecycle());
        assignSafe(description.getGroup(), service::setGroup);

        service.setCosts(description.getCosts());
        service.setCapability(description.getCapability());

        if (description.getStatuses() != null)
            description.getStatuses().forEach(statusItem -> {
                try {
                    service.setStatus(statusItem);
                } catch (IllegalArgumentException ex) {
                    logger.warn("Failed to set status", ex);
                }
            });

        service.setHost_type(description.getHost_type());
        service.setNetworks(description.getNetworks());
        service.setMachine(description.getMachine());
        service.setScale(description.getScale());
    }
}
