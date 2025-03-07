package de.bonndan.nivio.landscape;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"landscape_identifier", "identifier", "group"}))
public class Service implements ServiceItem {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Pattern(regexp = ServiceItem.IDENTIFIER_VALIDATION)
    private String identifier;

    @NotNull
    @ManyToOne
    @JsonBackReference
    private Landscape landscape;

    private String layer = ServiceItem.LAYER_APPLICATION;

    private String type = ServiceItem.TYPE_SERVICE;

    private String name;

    private String short_name;

    private String icon;

    private String owner;

    private String team;

    private String contact;

    private String homepage;

    private String description;

    private String version;

    private String software;

    private String repository;

    @Column(name = "`group`")
    private String group;

    private String visibility;

    private String[] tags;

    private String[] networks;

    private String machine;

    private String scale;

    private String host_type;

    private String costs;

    private String capability;

    @JsonManagedReference
    @OneToMany(targetEntity = ServiceStatus.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "service", orphanRemoval = true)
    private Set<StatusItem> statuses = new HashSet<>();

    @JsonManagedReference
    @OneToMany(targetEntity = DataFlow.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "sourceEntity", orphanRemoval = true)
    private Set<DataFlowItem> dataFlow = new HashSet<>();

    @JsonBackReference
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "TYPE_INFRASTRUCTURE",
            joinColumns = {@JoinColumn(name = "service_id")},
            inverseJoinColumns = {@JoinColumn(name = "infrastructure_identifier")})
    private Set<Service> providedBy = new HashSet<>();

    @JsonBackReference
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "providedBy")
    private Set<Service> provides = new HashSet<>();

    private String note;

    @JsonManagedReference
    @OneToMany(targetEntity = ServiceInterface.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "service", orphanRemoval = true)
    private Set<InterfaceItem> interfaces = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Lifecycle lifecycle;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            throw new RuntimeException("Identifier must not be empty");
        }
        this.identifier = identifier.toLowerCase();
    }

    @Override
    @Transient
    public FullyQualifiedIdentifier getFullyQualifiedIdentifier() {
        return FullyQualifiedIdentifier.build(landscape == null ? "" : landscape.getIdentifier(), group, identifier);
    }

    public Landscape getLandscape() {
        return landscape;
    }

    public void setLandscape(Landscape landscape) {
        this.landscape = landscape;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShort_name() {
        return short_name;
    }

    public void setShort_name(String short_name) {
        this.short_name = short_name;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSoftware() {
        return software;
    }

    public void setSoftware(String software) {
        this.software = software;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getVisibility() {
        return visibility;
    }

    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    @Override
    public Set<StatusItem> getStatuses() {
        return statuses;
    }

    public void setStatus(StatusItem statusItem) {

        if (statusItem == null)
            throw new IllegalArgumentException("Status item is null");
        if (StringUtils.isEmpty(statusItem.getLabel()))
            throw new IllegalArgumentException("Status item has no label");

        Optional<StatusItem> existing = this.statuses.stream()
                .filter(serviceStatus -> statusItem.getLabel().equals(serviceStatus.getLabel()))
                .findFirst();

        existing.ifPresentOrElse(
                serviceStatus -> {
                    ((ServiceStatus) serviceStatus).setStatus(statusItem.getStatus());
                    ((ServiceStatus) serviceStatus).setMessage(statusItem.getMessage());
                },
                () -> {
                    var added = new ServiceStatus();
                    added.setService(this);
                    added.setLabel(statusItem.getLabel());
                    added.setStatus(statusItem.getStatus());
                    added.setMessage(statusItem.getMessage());
                    this.statuses.add(added);
                });
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public Set<String> getNetworks() {
        return networks == null? new HashSet<>() : Set.of(networks);
    }

    public void setNetworks(Set<String> networks) {
        this.networks = networks.stream().toArray(String[]::new);
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getScale() {
        return scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public String getHost_type() {
        return host_type;
    }

    public void setHost_type(String host_type) {
        this.host_type = host_type;
    }

    public Set<DataFlowItem> getDataFlow() {
        return dataFlow;
    }

    public void setDataFlow(Set<DataFlowItem> outgoing) {
        dataFlow.addAll(outgoing);
    }

    public Set<Service> getProvidedBy() {
        return providedBy;
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public Set<Service> getProvides() {
        return provides;
    }

    public void setProvides(Set<Service> provides) {
        this.provides = provides;
    }

    public void setInterfaces(Set<InterfaceItem> interfaces) {
        this.interfaces = interfaces;
    }

    public Set<InterfaceItem> getInterfaces() {
        return interfaces;
    }

    @Override
    public String getCosts() {
        return costs;
    }

    public void setCosts(String costs) {
        this.costs = costs;
    }

    @Override
    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        if (!(o instanceof ServiceItem))
            return false;

        ServiceItem landscapeItem = (ServiceItem) o;
        if (toString() == null)
            return false;

        return toString().equals(landscapeItem.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }

    /**
     * @return the fully qualified identifier for this service
     */
    @Override
    public String toString() {
        if (landscape == null)
            return identifier;

        return getFullyQualifiedIdentifier().toString();
    }

}
