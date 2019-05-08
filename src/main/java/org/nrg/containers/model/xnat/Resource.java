package org.nrg.containers.model.xnat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.containers.model.command.entity.CommandWrapperInputType;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.XFTItem;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.ResourceURII;
import org.nrg.xnat.utils.CatalogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@JsonInclude(Include.NON_NULL)
public class Resource extends XnatModelObject {

    @JsonIgnore private XnatResourcecatalog xnatResourcecatalog;
    @JsonProperty("integer-id") private Integer integerId;
    private String directory;
    private List<XnatFile> files;

    public Resource() {}

    public Resource(final ResourceURII resourceURII, final boolean loadFiles,
                    final Map<String, Boolean> loadTypesMap) {
        final XnatAbstractresourceI xnatAbstractresourceI = resourceURII.getXnatResource();
        if (xnatAbstractresourceI instanceof XnatResourcecatalog) {
            this.xnatResourcecatalog = (XnatResourcecatalog) xnatAbstractresourceI;
        }
        this.uri = resourceURII.getUri();
        populateProperties(null, loadFiles, loadTypesMap);
    }

    public Resource(final XnatResourcecatalog xnatResourcecatalog, final boolean loadFiles,
                    final Map<String, Boolean> loadTypesMap) {
        this(xnatResourcecatalog, loadFiles, loadTypesMap, null, null);
    }

    public Resource(final XnatResourcecatalog xnatResourcecatalog, final boolean loadFiles,
                    final Map<String, Boolean> loadTypesMap,
                    final String parentUri, final String rootArchivePath) {
        this.xnatResourcecatalog = xnatResourcecatalog;

        if (parentUri == null) {
            this.uri = UriParserUtils.getArchiveUri(xnatResourcecatalog); // <-- Does not actually work
            log.error("Cannot construct a resource URI. Parent URI is null.");
        } else {
            this.uri = parentUri + "/resources/" + xnatResourcecatalog.getLabel();
        }

        populateProperties(rootArchivePath, loadFiles, loadTypesMap);
    }

    private void populateProperties(final String rootArchivePath, final boolean loadFiles,
                                    final Map<String, Boolean> loadTypesMap) {
        this.integerId = xnatResourcecatalog.getXnatAbstractresourceId();
        this.id = xnatResourcecatalog.getLabel();
        this.label = xnatResourcecatalog.getLabel();
        this.xsiType = xnatResourcecatalog.getXSIType();
        this.directory = xnatResourcecatalog.getCatalogFile(rootArchivePath).getParent();
        this.files = Lists.newArrayList();

        // Only get catalog entry details if we need them
        if (loadFiles || (loadTypesMap != null && (loadTypesMap.get(CommandWrapperInputType.FILE.getName()) ||
                loadTypesMap.get(CommandWrapperInputType.FILES.getName())))) {
            final CatCatalogBean cat = xnatResourcecatalog.getCatalog(rootArchivePath);
            final List<Object[]> entryDetails = CatalogUtils.getEntryDetails(cat, this.directory, null,
                    xnatResourcecatalog, loadFiles, null, null, "absolutePath");
            for (final Object[] entry : entryDetails) {
                // Don't retrieve the actual file during preresolution
                File file = loadFiles ? (File) entry[8] : null;
                // See CatalogUtils.getEntryDetails to see where all these "entry" elements come from
                files.add(new XnatFile(this.uri, (String) entry[0], (String) entry[2], (String) entry[4],
                        (String) entry[5], (String) entry[6], file));
            }
        }
    }

    public static Function<URIManager.ArchiveItemURI, Resource> uriToModelObject(final boolean loadFiles,
                                                                                 final Map<String, Boolean> loadTypesMap) {
        return new Function<URIManager.ArchiveItemURI, Resource>() {
            @Nullable
            @Override
            public Resource apply(@Nullable URIManager.ArchiveItemURI uri) {
                XnatAbstractresourceI resource;
                if (uri != null &&
                        ResourceURII.class.isAssignableFrom(uri.getClass())) {
                    resource = ((ResourceURII) uri).getXnatResource();

                    if (resource != null &&
                            XnatAbstractresourceI.class.isAssignableFrom(resource.getClass())) {
                        return new Resource((ResourceURII) uri, loadFiles, loadTypesMap);
                    }
                }

                return null;
            }
        };
    }

    public static Function<String, Resource> idToModelObject(final UserI userI, final boolean loadFiles,
                                                             final Map<String, Boolean> loadTypesMap) {
        return new Function<String, Resource>() {
            @Nullable
            @Override
            public Resource apply(@Nullable String s) {
                if (StringUtils.isBlank(s)) {
                    return null;
                }
                final XnatAbstractresourceI xnatAbstractresourceI =
                        XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(s, userI, true);
                if (xnatAbstractresourceI != null && xnatAbstractresourceI instanceof XnatResourcecatalog) {
                    return new Resource((XnatResourcecatalog) xnatAbstractresourceI, loadFiles, loadTypesMap);
                }
                return null;
            }
        };
    }

    public Project getProject(final UserI userI) {
        loadXnatResourcecatalog(userI);
        // TODO This does not work. I wish it did.
        // return new Project(xnatResourcecatalog.getProject(), userI);
        return null;
    }

    public void loadXnatResourcecatalog(final UserI userI) {
        if (xnatResourcecatalog == null) {
            xnatResourcecatalog = XnatResourcecatalog.getXnatResourcecatalogsByXnatAbstractresourceId(integerId, userI, false);
        }
    }

    public XnatResourcecatalogI getXnatResourcecatalog() {
        return xnatResourcecatalog;
    }

    public void setXnatResourcecatalog(final XnatResourcecatalog xnatResourcecatalog) {
        this.xnatResourcecatalog = xnatResourcecatalog;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public List<XnatFile> getFiles() {
        return files;
    }

    public void setFiles(final List<XnatFile> files) {
        this.files = files;
    }

    @Override
    public XFTItem getXftItem(final UserI userI) {
        loadXnatResourcecatalog(userI);
        return xnatResourcecatalog == null ? null : xnatResourcecatalog.getItem();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final Resource that = (Resource) o;
        return Objects.equals(this.directory, that.directory) &&
                Objects.equals(this.files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), directory, files);
    }

    @Override
    public String toString() {
        return addParentPropertiesToString(MoreObjects.toStringHelper(this))
                .add("directory", directory)
                .add("files", files)
                .toString();
    }
}
