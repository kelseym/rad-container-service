package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.command.entity.CommandWrapperInputType;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xft.XFTItem;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.AssessedURII;
import org.nrg.xnat.helpers.uri.archive.ExperimentURII;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@JsonInclude(Include.NON_NULL)
public class Session extends XnatModelObject {
    @JsonIgnore private XnatImagesessiondataI xnatImagesessiondataI;
    private List<Scan> scans;
    private List<Assessor> assessors;
    private List<Resource> resources;
    @JsonProperty("project-id") private String projectId;
    @JsonProperty("subject-id") private String subjectId;
    private String directory;

    public Session() {}

    public Session(final String sessionId, final UserI userI, final boolean loadFiles,
                   @Nonnull final Set<String> loadTypes) {
        this.id = sessionId;
        loadXnatImagesessiondata(userI);
        this.uri = UriParserUtils.getArchiveUri(xnatImagesessiondataI);
        populateProperties(null, loadFiles, loadTypes);
    }

    public Session(final AssessedURII assessedURII, final boolean loadFiles, @Nonnull final Set<String> loadTypes) {
        final XnatImagesessiondata imagesessiondata = assessedURII.getSession();
        if (imagesessiondata != null) {
            this.xnatImagesessiondataI = imagesessiondata;
            this.uri = ((URIManager.DataURIA) assessedURII).getUri();
            populateProperties(null, loadFiles, loadTypes);
        }
    }

    public Session(final XnatImagesessiondataI xnatImagesessiondataI, final boolean loadFiles,
                   @Nonnull final Set<String> loadTypes) {
        this(xnatImagesessiondataI, loadFiles, loadTypes, null, null);
    }

    public Session(final XnatImagesessiondataI xnatImagesessiondataI, final boolean loadFiles,
                   @Nonnull final Set<String> loadTypes, final String parentUri, final String rootArchivePath) {
        this.xnatImagesessiondataI = xnatImagesessiondataI;
        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatImagesessiondataI);
        } else {
            this.uri = parentUri + "/experiments/" + xnatImagesessiondataI.getId();
        }
        populateProperties(rootArchivePath, loadFiles, loadTypes);
    }

    private void populateProperties(final String rootArchivePath, final boolean loadFiles,
                                    @Nonnull final Set<String> loadTypes) {
        this.id = xnatImagesessiondataI.getId();
        this.label = xnatImagesessiondataI.getLabel();
        this.xsiType = xnatImagesessiondataI.getXSIType();
        this.projectId = xnatImagesessiondataI.getProject();
        this.subjectId = xnatImagesessiondataI.getSubjectId();

        try {
            this.directory = ((XnatExperimentdata) xnatImagesessiondataI).getCurrentSessionFolder(true);
        } catch (UnknownPrimaryProjectException | InvalidArchiveStructure e) {
            // ignored, I guess?
        }

        this.scans = Lists.newArrayList();
        if (loadFiles || loadTypes.contains(CommandWrapperInputType.SCAN.getName())) {
            for (final XnatImagescandataI xnatImagescandataI : xnatImagesessiondataI.getScans_scan()) {
                this.scans.add(new Scan(xnatImagescandataI, loadFiles, loadTypes, this.uri, rootArchivePath));
            }
        }

        this.resources = Lists.newArrayList();
        if (loadFiles || loadTypes.contains(CommandWrapperInputType.RESOURCE.getName())) {
            for (final XnatAbstractresourceI xnatAbstractresourceI : xnatImagesessiondataI.getResources_resource()) {
                if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
                    resources.add(new Resource((XnatResourcecatalog) xnatAbstractresourceI, loadFiles,
                            loadTypes, this.uri, rootArchivePath));
                }
            }
        }

        this.assessors = Lists.newArrayList();
        if (loadFiles || loadTypes.contains(CommandWrapperInputType.ASSESSOR.getName())) {
            for (final XnatImageassessordataI xnatImageassessordataI : xnatImagesessiondataI.getAssessors_assessor()) {
                assessors.add(new Assessor(xnatImageassessordataI, loadFiles, loadTypes, this.uri, rootArchivePath));
            }
        }
    }

    public static Function<URIManager.ArchiveItemURI, Session> uriToModelObject(final boolean loadFiles,
                                                                                @Nonnull final Set<String> loadTypes) {
        return new Function<URIManager.ArchiveItemURI, Session>() {
            @Nullable
            @Override
            public Session apply(@Nullable URIManager.ArchiveItemURI uri) {
                XnatImagesessiondata imageSession;
                if (uri != null &&
                        AssessedURII.class.isAssignableFrom(uri.getClass())) {
                    imageSession = ((AssessedURII) uri).getSession();

                    if (imageSession != null) {
                        return new Session((AssessedURII) uri, loadFiles, loadTypes);
                    }
                } else if (uri != null &&
                        ExperimentURII.class.isAssignableFrom(uri.getClass())) {
                    final XnatExperimentdata experimentdata = ((ExperimentURII) uri).getExperiment();
                    if (experimentdata != null &&
                            XnatImagesessiondataI.class.isAssignableFrom(experimentdata.getClass())) {
                        return new Session((XnatImagesessiondataI) experimentdata, loadFiles, loadTypes);
                    }
                }

                return null;
            }
        };
    }

    public static Function<String, Session> idToModelObject(final UserI userI, final boolean loadFiles,
                                                            @Nonnull final Set<String> loadTypes) {
        return new Function<String, Session>() {
            @Nullable
            @Override
            public Session apply(@Nullable String s) {
                if (StringUtils.isBlank(s)) {
                    return null;
                }
                final XnatImagesessiondata imagesessiondata = XnatImagesessiondata.getXnatImagesessiondatasById(s,
                        userI, true);
                if (imagesessiondata != null) {
                    return new Session(imagesessiondata, loadFiles, loadTypes);
                }
                return null;
            }
        };
    }

    public Project getProject(final UserI userI, final boolean loadFiles, @Nonnull final Set<String> loadTypes) {
        loadXnatImagesessiondata(userI);
        return new Project(xnatImagesessiondataI.getProject(), userI, loadFiles, loadTypes);
    }

    public Subject getSubject(final UserI userI, final boolean loadFiles, @Nonnull final Set<String> loadTypes) {
        loadXnatImagesessiondata(userI);
        return new Subject(xnatImagesessiondataI.getSubjectId(), userI, loadFiles, loadTypes);
    }

    public void loadXnatImagesessiondata(final UserI userI) {
        if (xnatImagesessiondataI == null) {
            xnatImagesessiondataI = XnatImagesessiondata.getXnatImagesessiondatasById(id, userI, false);
        }
    }

    public XnatImagesessiondataI getXnatImagesessiondataI() {
        return xnatImagesessiondataI;
    }

    public void setXnatImagesessiondataI(final XnatImagesessiondataI xnatImagesessiondataI) {
        this.xnatImagesessiondataI = xnatImagesessiondataI;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(final List<Resource> resources) {
        this.resources = resources;
    }

    public List<Assessor> getAssessors() {
        return assessors;
    }

    public void setAssessors(final List<Assessor> assessors) {
        this.assessors = assessors;
    }

    public List<Scan> getScans() {
        return scans;
    }

    public void setScans(final List<Scan> scans) {
        this.scans = scans;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(final String projectId) {
        this.projectId = projectId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(final String subjectId) {
        this.subjectId = subjectId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    @Override
    public XFTItem getXftItem(final UserI userI) {
        loadXnatImagesessiondata(userI);
        return xnatImagesessiondataI == null ? null : ((XnatImagesessiondata)xnatImagesessiondataI).getItem();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Session that = (Session) o;
        return Objects.equals(this.xnatImagesessiondataI, that.xnatImagesessiondataI) &&
                Objects.equals(this.scans, that.scans) &&
                Objects.equals(this.assessors, that.assessors) &&
                Objects.equals(this.resources, that.resources) &&
                Objects.equals(this.projectId, that.projectId) &&
                Objects.equals(this.subjectId, that.subjectId) &&
                Objects.equals(this.directory, that.directory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), xnatImagesessiondataI, scans, assessors, resources, projectId, subjectId, directory);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("scans", scans)
                .add("assessors", assessors)
                .add("resources", resources)
                .add("projectId", projectId)
                .add("subjectId", subjectId)
                .add("directory", directory)
                .toString();
    }
}
