/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/galleries/Attic/CmsGallerySearchServer.java,v $
 * Date   : $Date: 2010/01/26 14:48:26 $
 * Version: $Revision: 1.54 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.galleries;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsResourceFilter;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.json.JSONArray;
import org.opencms.json.JSONException;
import org.opencms.json.JSONObject;
import org.opencms.loader.CmsLoaderException;
import org.opencms.loader.CmsResourceManager;
import org.opencms.lock.CmsLock;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsCategory;
import org.opencms.relations.CmsCategoryService;
import org.opencms.search.galleries.CmsGallerySearch;
import org.opencms.search.galleries.CmsGallerySearchParameters;
import org.opencms.search.galleries.CmsGallerySearchResult;
import org.opencms.search.galleries.CmsGallerySearchResultList;
import org.opencms.util.CmsStringUtil;
import org.opencms.workplace.A_CmsAjaxServer;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.CmsWorkplaceMessages;
import org.opencms.workplace.editors.ade.CmsFormatterInfoBean;
import org.opencms.workplace.explorer.CmsExplorerTypeSettings;
import org.opencms.xml.containerpage.CmsADEManager;
import org.opencms.xml.containerpage.CmsContainerElementBean;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;

/**
 * Gallery search server used for client/server communication.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.54 $
 * 
 * @since 7.6
 */
public class CmsGallerySearchServer extends A_CmsAjaxServer {

    /** Gallery mode constants. */
    public enum GalleryMode {

        /** The advanced direct edit mode. */
        ade,

        /** The FCKEditor mode. */
        editor,

        /** The sitemap editor mode. */
        sitemap,

        /** The explorer mode. */
        view,

        /** The widget mode. */
        widget;
    }

    /** Request parameter name constants. */
    public enum ReqParam {

        /** The action of execute. */
        action,

        /** The current element. */
        currentelement,

        /** Generic data parameter. */
        data,

        /** The dialog mode. */
        dialogmode,

        /** The current gallery item parameter. */
        galleryitem,

        /** Specific image data parameter. */
        imagedata,

        /** The path to the editor plugin script. */
        integrator,

        /** The current locale. */
        locale;

    }

    /** Request parameter action value constants. */
    protected enum Action {

        /** To get all available galleries and categories. */
        ALL,

        /** To get all available categories. */
        CATEGORIES,

        /** To get all available container types. */
        CONTAINERS,

        /** To get all available galleries. */
        GALLERIES,

        /** To get the preview html of a resource. */
        PREVIEW,

        /** To retrieve search results. */
        SEARCH,

        /** To set the resource properties. */
        SETPROPERTIES,

        /** To retrieve the path to the given resource. */
        VFSPATH;
    }

    /**
     * Gallery info object.<p>
     */
    protected class CmsGalleryTypeInfo {

        /** The content types using this gallery. */
        private List<I_CmsResourceType> m_contentTypes;

        /** The gallery folder resources. */
        private List<CmsResource> m_galleries;

        /** The resource type of this gallery. */
        private I_CmsResourceType m_resourceType;

        /**
         * Constructor.<p>
         * 
         * @param resourceType the resource type of the gallery
         * @param contentType the resource type of the gallery content
         * @param galleries the gallery resources
         */
        protected CmsGalleryTypeInfo(
            I_CmsResourceType resourceType,
            I_CmsResourceType contentType,
            List<CmsResource> galleries) {

            m_resourceType = resourceType;
            m_contentTypes = new ArrayList<I_CmsResourceType>();
            m_contentTypes.add(contentType);
            m_galleries = galleries;
        }

        /**
         * Adds a type to the list of content types.<p>
         * 
         * @param type the type to add
         */
        protected void addContentType(I_CmsResourceType type) {

            m_contentTypes.add(type);
        }

        /**
         * Returns the contentTypes.<p>
         *
         * @return the contentTypes
         */
        protected List<I_CmsResourceType> getContentTypes() {

            return m_contentTypes;
        }

        /**
         * Returns the gallery folder resources.<p>
         *
         * @return the resources
         */
        protected List<CmsResource> getGalleries() {

            return m_galleries;
        }

        /**
         * Returns the resourceType.<p>
         *
         * @return the resourceType
         */
        protected I_CmsResourceType getResourceType() {

            return m_resourceType;
        }

        /**
         * Sets the contentTypes.<p>
         *
         * @param contentTypes the contentTypes to set
         */
        protected void setContentTypes(List<I_CmsResourceType> contentTypes) {

            m_contentTypes = contentTypes;
        }

        /**
         * Sets the galleries.<p>
         *
         * @param galleries the gallery resource list to set
         */
        protected void setGalleries(List<CmsResource> galleries) {

            m_galleries = galleries;
        }

        /**
         * Sets the resourceType.<p>
         *
         * @param resourceType the resourceType to set
         */
        protected void setResourceType(I_CmsResourceType resourceType) {

            m_resourceType = resourceType;
        }
    }

    /** Json property name constants. */
    protected enum ItemKey {

        /** The client-side resource-id. */
        clientid,

        /** The content types. */
        contenttypes,

        /** The date modified. */
        datemodified,

        /** The gallery types. */
        gallerytypeid,

        /** The icon. */
        icon,

        /** The additional image query data */
        imagedata,

        /** The info. */
        info,

        /** The item html-content. */
        itemhtml,

        /** The level. */
        level,

        /** The linkpath of the resource. */
        linkpath,

        /** The locale. */
        locale,

        /** The name. */
        name,

        /** The path. */
        path,

        /** The properties. */
        properties,

        /** The resource path. */
        resourcepath,

        /** The root-path. */
        rootpath,

        /** The resource state. */
        state,

        /** The title. */
        title,

        /** The type. */
        type,

        /** The type id. */
        typeid,

        /** The value. */
        value;

    }

    /** JSON keys used in the query data. */
    protected enum QueryKey {

        /** The categories. */
        categories,

        /** The galleries. */
        galleries,

        /** The locale. */
        locale,

        /** The matches per page. */
        matchesperpage,

        /** The page. */
        page,

        /** The query-string. */
        query,

        /** The query data */
        querydata,

        /** The result count. */
        resultcount,

        /** The result list. */
        resultlist,

        /** The result page number. */
        resultpage,

        /** The sort-order. */
        sortorder,

        /** The tab-id. */
        tabid,

        /** The gallery-types. */
        types

    }

    /** JSON keys used in the main response data. */
    protected enum ResponseKey {

        /** The categories. */
        categories,

        /** The galleries. */
        galleries,

        /** The locale. */
        locale,

        /** The locales. */
        locales,

        /** The path. */
        path,

        /** The preview data. */
        previewdata,

        /** The search result. */
        searchresult,

        /** The type id's. */
        typeids,

        /** The gallery-types. */
        types
    }

    /** The advanced gallery index name. */
    public static final String ADVANCED_GALLERY_INDEX = "ADE Gallery Index";

    /** The advanced gallery path to the JSPs in the workplace. */
    public static final String ADVANCED_GALLERY_PATH = "/system/workplace/editors/ade/galleries.jsp";

    /** The excerpt field constant. */
    public static final String EXCERPT_FIELD_NAME = "excerpt";

    /** The result tab id. */
    public static final String RESULT_TAB_ID = "#tabs-result";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsGallerySearchServer.class);

    /** The default matchers per search result page. */
    private static final int MATCHES_PER_PAGE = 8;

    /** Html script tag end fragment. */
    private static final String SCRIPT_TAG_END = "\" ></script>\n";

    /** Html script tag start fragment. */
    private static final String SCRIPT_TAG_START = "<script type=\"text/javascript\" src=\"";

    /** The instance of the resource manager. */
    CmsResourceManager m_resourceManager;

    /** The users locale. */
    private Locale m_locale;

    /** The galleries mode name(widget,view, editor, ade, sitemap). */
    private String m_modeName;

    /** The JSON data request object. */
    private JSONObject m_reqDataObj;

    /** The additional JSON data request object for image gallery. */
    private JSONObject m_reqImageDataObj;

    /** The available resource type id's. */
    private JSONArray m_resourceTypeIds;

    /** The available resource types. */
    private List<I_CmsResourceType> m_resourceTypes;

    /**
     * Empty constructor, required for every JavaBean.<p>
     */
    public CmsGallerySearchServer() {

        super();
    }

    /**
     * Returns script tags for resource type specific handling of the gallery preview for the given types.<p>
     * 
     * @param types the resource types
     * 
     * @return the script tags
     */
    public static String getAdditionalJavascriptForTypes(List<I_CmsResourceType> types) {

        StringBuffer result = new StringBuffer();
        Iterator<I_CmsResourceType> resIt = types.iterator();
        while (resIt.hasNext()) {
            I_CmsResourceType type = resIt.next();
            String jsPath = type.getGalleryJavascriptPath();
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(jsPath)) {
                result.append(SCRIPT_TAG_START);
                result.append(CmsWorkplace.getResourceUri(jsPath));
                result.append(SCRIPT_TAG_END);
            }
        }
        return result.toString();
    }

    /**
     * Generates the java-script script-tags necessary for the given resource types.
     * Providing extra functionality within the galleries.<p>
     * 
     * @param cms the current instance of the cms object 
     * @param types the resource types
     * @return the script tags to include into the gallery page
     */
    public static String getJSIncludeForTypes(CmsObject cms, List<I_CmsResourceType> types) {

        StringBuffer result = new StringBuffer(32);
        Iterator<I_CmsResourceType> typeIt = types.iterator();
        while (typeIt.hasNext()) {
            I_CmsResourceType type = typeIt.next();
            String jsPath = type.getConfiguration().get("js.path");
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(jsPath) && cms.existsResource(jsPath)) {
                result.append(SCRIPT_TAG_START);
                result.append(CmsWorkplace.getResourceUri(jsPath));
                result.append(SCRIPT_TAG_END);
            }
        }
        return result.toString();
    }

    /**
     * Handles all publish related requests.<p>
     * 
     * @return the result
     * 
     * @throws Exception if there is a problem
     */
    @Override
    public JSONObject executeAction() throws Exception {

        JSONObject result = new JSONObject();
        HttpServletRequest request = getRequest();
        if (checkParameters(request, result, ReqParam.action.toString(), ReqParam.data.toString())) {
            String actionParam = request.getParameter(ReqParam.action.toString());
            String localeParam = request.getParameter(ReqParam.locale.toString());
            String dataParam = request.getParameter(ReqParam.data.toString());
            Action action = Action.valueOf(actionParam.toUpperCase());
            m_locale = CmsLocaleManager.getLocale(localeParam);
            getCmsObject().getRequestContext().setLocale(m_locale);
            JSONObject data = new JSONObject(dataParam);
            if (action.equals(Action.ALL)) {
                result.merge(getAllLists(null), true, true);

                // TODO: Add containers.
            } else if (action.equals(Action.CATEGORIES)) {
                result.put(ResponseKey.categories.toString(), readSystemCategories());
            } else if (action.equals(Action.CONTAINERS)) {
                // TODO:  Will be implemented later.
            } else if (action.equals(Action.GALLERIES)) {
                JSONArray resourceTypesParam = data.getJSONArray(ResponseKey.types.toString());
                List<I_CmsResourceType> resourceTypes = readContentTypes(resourceTypesParam);
                Map<String, CmsGalleryTypeInfo> galleryTypes = readGalleryTypes(resourceTypes);

                result.put(ResponseKey.galleries.toString(), buildJSONForGalleries(galleryTypes));
            } else if (action.equals(Action.SEARCH)) {
                JSONObject query = data.getJSONObject(QueryKey.querydata.toString());
                result.put(ResponseKey.searchresult.toString(), search(query));
            } else if (action.equals(Action.PREVIEW)) {
                String resourcePath = data.getString(ItemKey.path.toString());
                result.put(ResponseKey.previewdata.toString(), getPreviewData(resourcePath));
            } else if (action.equals(Action.SETPROPERTIES)) {
                String resourcePath = data.getString(ItemKey.path.toString());
                JSONArray properties = data.getJSONArray(ItemKey.properties.toString());
                result.put(ResponseKey.previewdata.toString(), setProperties(resourcePath, properties));
            } else if (action.equals(Action.VFSPATH)) {
                String path = data.getString(ItemKey.linkpath.toString());
                if (path.startsWith(OpenCms.getSystemInfo().getOpenCmsContext())) {
                    path = path.substring(OpenCms.getSystemInfo().getOpenCmsContext().length());
                }
                result.put(ResponseKey.path.toString(), path);
            }
        }
        return result;
    }

    /**
     * Returns the search result page for a given resource, or an empty JSON object if result page could not be found(This should never happen).<p>
     * 
     * @param resourceName the resource name
     * 
     * @return the search result page containing the given resource
     * 
     * @throws JSONException if something goes wrong 
     */
    public JSONObject findResourceInGallery(String resourceName) throws JSONException {

        CmsResource resource = null;
        try {
            resource = getCmsObject().readResource(resourceName);
        } catch (CmsException e) {
            LOG.warn(e.getLocalizedMessage(), e);
        }
        if (resource == null) {
            return new JSONObject();
        }
        String rootPath = resource.getRootPath();
        JSONObject queryData = new JSONObject();
        JSONArray types = new JSONArray();
        types.put(resource.getTypeId());
        queryData.put(QueryKey.types.toString(), types);
        JSONArray galleries = new JSONArray();
        galleries.put(CmsResource.getFolderPath(resourceName));
        queryData.put(QueryKey.galleries.toString(), galleries);
        queryData.put(QueryKey.categories.toString(), new JSONArray());
        queryData.put(QueryKey.matchesperpage.toString(), MATCHES_PER_PAGE);
        queryData.put(QueryKey.query.toString(), "");
        queryData.put(QueryKey.sortorder.toString(), CmsGallerySearchParameters.CmsGallerySortParam.DEFAULT.toString());
        int currentPage = 1;
        boolean found = false;
        queryData.put(QueryKey.page.toString(), currentPage);
        CmsGallerySearchParameters params = prepareSearchParams(queryData);
        CmsGallerySearch searchBean = new CmsGallerySearch();
        searchBean.init(getCmsObject());
        searchBean.setIndex(ADVANCED_GALLERY_INDEX);

        CmsGallerySearchResultList searchResults = null;
        while (!found) {
            params.setResultPage(currentPage);
            searchResults = searchBean.getResult(params);
            Iterator<CmsGallerySearchResult> resultsIt = searchResults.listIterator();
            while (resultsIt.hasNext()) {
                CmsGallerySearchResult searchResult = resultsIt.next();
                if (searchResult.getPath().equals(rootPath)) {
                    found = true;
                    break;
                }
            }
            if (!found && (searchResults.getHitCount() / (currentPage * params.getMatchesPerPage()) >= 1)) {
                currentPage++;
            } else {
                break;
            }
        }
        JSONObject result = new JSONObject();
        if (found && (searchResults != null)) {
            JSONObject sResult = new JSONObject();
            sResult.put(QueryKey.sortorder.toString(), params.getSortOrder());
            sResult.put(QueryKey.resultcount.toString(), searchResults.getHitCount());
            sResult.put(QueryKey.page.toString(), params.getResultPage());
            sResult.put(QueryKey.resultlist.toString(), buildJSONForSearchResult(searchResults));
            queryData.put(QueryKey.page.toString(), currentPage);
            queryData.put(QueryKey.tabid.toString(), RESULT_TAB_ID);
            result.put(QueryKey.querydata.toString(), queryData);
            result.put(ResponseKey.searchresult.toString(), sResult);
        }

        return result;
    }

    /**
     * Returns the JSON as string for the additional parameters of the resources of the type image.<p>
     * 
     * All parameters are taken from the request parameters.<p>
     * 
     * @return the search result data as JSON string
     */
    public String getAdditionalImageParams() {

        JSONObject result = new JSONObject();
        try {
            JSONObject imageDate = getReqImageDataObj();
            if (imageDate != null) {
                result = getReqImageDataObj();
            }
        } catch (JSONException e) {
            // TODO: improve error handling
            LOG.error(e.getLocalizedMessage(), e);
        }
        return result.toString();
    }

    /**
     * Returns specific script tags for the handling of the galleries and the gallery preview.<p>
     * 
     * @return the script tags
     */
    public String getAdditionalJavaScript() {

        StringBuffer result = new StringBuffer();
        result.append(getAdditionalJavascriptForTypes(getResourceTypes()));
        result.append("\n");
        result.append(getAdditionalJavascriptForEditor());
        return result.toString();
    }

    /**
     * Returns script tags for editor specific handling of the galleries preview.<p>
     * 
     * @return the script tags
     */
    public String getAdditionalJavascriptForEditor() {

        StringBuffer result = new StringBuffer();
        String modeName = getModeName();
        String integratorPath = link(CmsWorkplace.VFS_PATH_EDITORS + getIntegratorPath());
        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(modeName) && modeName.equals(GalleryMode.editor.toString())) {
            result.append(SCRIPT_TAG_START);
            result.append(integratorPath);
            result.append(SCRIPT_TAG_END);
        }
        return result.toString();
    }

    /**
     * Returns the JSON for type, galleries and categories tab. Uses the given types or all available resource types.<p>
     * 
     * @param modeName the dialog mode name
     * 
     * @return available types, galleries and categories as JSON 
     * 
     * @throws JSONException if something goes wrong generating the JSON
     */
    public JSONObject getAllLists(String modeName) throws JSONException {

        JSONObject result = new JSONObject();
        result.put(ResponseKey.types.toString(), buildJSONForTypes(getResourceTypes()));
        result.put(ResponseKey.typeids.toString(), getResourceTypeIds());
        Map<String, CmsGalleryTypeInfo> galleryTypes = readGalleryTypes(getResourceTypes());

        result.put(ResponseKey.galleries.toString(), buildJSONForGalleries(galleryTypes));
        List<CmsResource> galleryFolders = new ArrayList<CmsResource>();
        Iterator<Entry<String, CmsGalleryTypeInfo>> iGalleryTypes = galleryTypes.entrySet().iterator();
        while (iGalleryTypes.hasNext()) {
            galleryFolders.addAll(iGalleryTypes.next().getValue().getGalleries());
        }
        result.put(ResponseKey.categories.toString(), buildJSONForCategories(readCategories(galleryFolders)));
        result.put(ResponseKey.locale.toString(), getCmsObject().getRequestContext().getLocale());

        result.put(ResponseKey.locales.toString(), buildJSONForLocales());
        return result;
    }

    /**
     * Returns the URI of the gallery JSP.<p>
     * 
     * @return the URI string
     */
    public String getGalleryUri() {

        return link(ADVANCED_GALLERY_PATH);
    }

    /**
     * Returns the JSON as string for the initial search. All search parameters are taken from the request parameters.<p>
     * 
     * @return the search result data as JSON string
     */
    public String getInitialSearch() {

        JSONObject result = new JSONObject();

        try {
            JSONObject data = getReqDataObj();
            if ((data != null) && data.has(ItemKey.resourcepath.toString())) {
                String resourcePath = data.getString(ItemKey.resourcepath.toString());
                result = findResourceInGallery(resourcePath);
            } else if ((data != null) && data.has(QueryKey.querydata.toString())) {
                JSONObject queryData = data.getJSONObject(QueryKey.querydata.toString());
                result.put(QueryKey.querydata.toString(), queryData);
                result.put(ResponseKey.searchresult.toString(), search(queryData));
            }
        } catch (JSONException e) {
            // TODO: improve error handling
            LOG.error(e.getLocalizedMessage(), e);
        }
        return result.toString();
    }

    /**
     * Returns the JSON as string for type, galleries and categories tab. Uses the given types or all available resource types.<p>
     * 
     * @return the type, galleries and categories data as JSON string
     * 
     * @throws JSONException if something goes wrong
     */
    public String getListConfig() throws JSONException {

        JSONObject result = getAllLists(getModeName());
        return result.toString();
    }

    /**
     * Returns the search result.<p>
     * 
     * @param query the query parameters
     * @return the search result
     * @throws JSONException if something goes wrong
     */
    public JSONObject search(JSONObject query) throws JSONException {

        JSONObject result = new JSONObject();
        if (query == null) {
            return result;
        }

        // search
        CmsGallerySearchParameters params = prepareSearchParams(query);
        CmsGallerySearch searchBean = new CmsGallerySearch();
        searchBean.init(getCmsObject());
        searchBean.setIndex(ADVANCED_GALLERY_INDEX);
        CmsGallerySearchResultList searchResults = searchBean.getResult(params);
        result.put(QueryKey.sortorder.toString(), params.getSortOrder());
        result.put(QueryKey.resultcount.toString(), searchResults.getHitCount());
        result.put(QueryKey.resultpage.toString(), params.getResultPage());
        result.put(QueryKey.resultlist.toString(), buildJSONForSearchResult(searchResults));

        return result;
    }

    /**
     * Sets the content locale.<p>
     * 
     * @param localeName the name of the locale to set
     */
    public void setLocale(String localeName) {

        m_locale = CmsLocaleManager.getLocale(localeName);
        getCmsObject().getRequestContext().setLocale(m_locale);
    }

    /**
     * Returns the JSON-Object for the given list of categories.<p>
     * 
     * @param categories the categories
     * @return the JSON-Object
     */
    private JSONArray buildJSONForCategories(List<CmsCategory> categories) {

        JSONArray result = new JSONArray();
        if ((categories == null) || (categories.size() == 0)) {
            return result;
        }
        // the next lines sort the categories according to their path 
        Map<String, CmsCategory> sorted = new TreeMap<String, CmsCategory>();

        Iterator<CmsCategory> i = categories.iterator();
        while (i.hasNext()) {
            CmsCategory category = i.next();
            String categoryPath = category.getPath();
            if (sorted.get(categoryPath) != null) {
                continue;
            }
            sorted.put(categoryPath, category);
        }

        List<CmsCategory> sortedCategories = new ArrayList<CmsCategory>(sorted.values());

        i = sortedCategories.iterator();
        while (i.hasNext()) {
            CmsCategory cat = i.next();

            JSONObject jsonObj = new JSONObject();
            try {
                CmsFormatterInfoBean formatterInfo = new CmsFormatterInfoBean(
                    OpenCms.getResourceManager().getResourceType("folder"),
                    false);
                // 1: category title
                jsonObj.put(ItemKey.title.toString(), cat.getTitle());
                formatterInfo.setTitleInfo(ItemKey.title.toString(), ItemKey.title.toString(), cat.getTitle());

                // 2: category path
                jsonObj.put(ItemKey.path.toString(), cat.getPath());
                formatterInfo.setSubTitleInfo(ItemKey.path.toString(), ItemKey.path.toString(), cat.getPath());
                // 3: category root path
                jsonObj.put(ItemKey.rootpath.toString(), cat.getRootPath());
                // 4 category level
                jsonObj.put(ItemKey.level.toString(), CmsResource.getPathLevel(cat.getPath()));
                String iconPath = CmsWorkplace.getResourceUri(CmsWorkplace.RES_PATH_FILETYPES
                    + OpenCms.getWorkplaceManager().getExplorerTypeSetting("folder").getIcon());
                formatterInfo.setIcon(iconPath);
                jsonObj.put(ItemKey.itemhtml.toString(), getFormattedListContent(formatterInfo));
                result.put(jsonObj);
            } catch (Exception e) {
                // TODO: Improve error handling
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * Returns the JSON-Object for the given list of galleries.<p>
     * 
     * @param cms the cms-object
     * @param galleryTypes the galleries
     * @return the JSON-Object
     */
    private JSONArray buildJSONForGalleries(Map<String, CmsGalleryTypeInfo> galleryTypes) {

        JSONArray result = new JSONArray();
        if (galleryTypes == null) {
            return result;
        }
        Iterator<Entry<String, CmsGalleryTypeInfo>> iGalleryTypes = galleryTypes.entrySet().iterator();
        while (iGalleryTypes.hasNext()) {
            Entry<String, CmsGalleryTypeInfo> ent = iGalleryTypes.next();
            CmsGalleryTypeInfo tInfo = ent.getValue();
            String iconPath = CmsWorkplace.getResourceUri(CmsWorkplace.RES_PATH_FILETYPES
                + OpenCms.getWorkplaceManager().getExplorerTypeSetting(tInfo.getResourceType().getTypeName()).getIcon());
            JSONArray contentTypes = new JSONArray();
            Iterator<I_CmsResourceType> it = tInfo.getContentTypes().iterator();
            while (it.hasNext()) {
                contentTypes.put(it.next().getTypeId());
            }
            Iterator<CmsResource> ir = tInfo.getGalleries().iterator();
            while (ir.hasNext()) {
                CmsResource res = ir.next();
                JSONObject jsonObj = new JSONObject();
                String sitePath = getCmsObject().getSitePath(res);
                String title = "";
                try {
                    // read the gallery title
                    title = getCmsObject().readPropertyObject(sitePath, CmsPropertyDefinition.PROPERTY_TITLE, false).getValue(
                        "");
                } catch (CmsException e) {
                    // error reading title property
                    if (LOG.isErrorEnabled()) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }

                try {
                    CmsFormatterInfoBean formatterInfo = new CmsFormatterInfoBean(tInfo.getResourceType(), false);
                    formatterInfo.setResource(res);
                    jsonObj.put(ItemKey.contenttypes.toString(), contentTypes);
                    jsonObj.put(ItemKey.title.toString(), title);
                    formatterInfo.setTitleInfo(ItemKey.title.toString(), ItemKey.title.toString(), title);

                    // 2: gallery path
                    jsonObj.put(ItemKey.path.toString(), sitePath);
                    formatterInfo.setSubTitleInfo(ItemKey.path.toString(), ItemKey.path.toString(), sitePath);
                    formatterInfo.setIcon(iconPath);
                    // 3: active flag
                    jsonObj.put(ItemKey.gallerytypeid.toString(), tInfo.getResourceType().getTypeId());
                    jsonObj.put(ItemKey.icon.toString(), iconPath);
                    jsonObj.put(ItemKey.itemhtml.toString(), getFormattedListContent(formatterInfo));
                } catch (Exception e) {
                    // TODO: Improve error handling
                    if (LOG.isErrorEnabled()) {
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
                result.put(jsonObj);
            }
        }
        return result;
    }

    /**
     * Returns the JSON representation of all available locales.<p>
     * 
     * @param currentLocale the current locale
     * 
     * @return the JSON representation of all available locales
     * 
     * @throws JSONException if something goes wrong with the JSON manipulation
     */
    private JSONArray buildJSONForLocales() throws JSONException {

        JSONArray result = new JSONArray();
        Iterator<Locale> it = OpenCms.getLocaleManager().getAvailableLocales().iterator();
        while (it.hasNext()) {
            Locale locale = it.next();
            JSONObject jLocale = new JSONObject();
            jLocale.put(ItemKey.title.toString(), locale.getDisplayName(getCmsObject().getRequestContext().getLocale()));
            jLocale.put(ItemKey.value.toString(), locale.toString());
            result.put(jLocale);
        }
        return result;
    }

    /**
     * Returns the JSON-Object for the given list of search-results.<p>
     * 
     * @param searchResult the search-result-list
     * 
     * @return the JSON representation of the search-result
     */
    private JSONArray buildJSONForSearchResult(List<CmsGallerySearchResult> searchResult) {

        JSONArray result = new JSONArray();
        if ((searchResult == null) || (searchResult.size() == 0)) {
            return result;
        }
        Iterator<CmsGallerySearchResult> iSearchResult = searchResult.iterator();
        while (iSearchResult.hasNext()) {
            try {
                Locale wpLocale = getWorkplaceLocale();
                CmsGallerySearchResult sResult = iSearchResult.next();
                JSONObject resultEntry = new JSONObject();
                String path = sResult.getPath();
                path = getRequestContext().removeSiteRoot(path);
                String fileIcon = getFileIconName(path);
                String iconPath = CmsWorkplace.RES_PATH_FILETYPES;
                if (CmsStringUtil.isEmptyOrWhitespaceOnly(fileIcon)) {
                    iconPath += OpenCms.getWorkplaceManager().getExplorerTypeSetting(sResult.getResourceType()).getIcon();
                } else {
                    iconPath += "mimetype/" + fileIcon;
                }
                iconPath = CmsWorkplace.getResourceUri(iconPath);
                resultEntry.put(ItemKey.datemodified.toString(), sResult.getDateLastModified());
                resultEntry.put(ItemKey.title.toString(), sResult.getTitle());
                resultEntry.put(ItemKey.info.toString(), sResult.getDescription());
                resultEntry.put(ItemKey.type.toString(), sResult.getResourceType());
                resultEntry.put(ItemKey.path.toString(), path);
                resultEntry.put(ItemKey.icon.toString(), iconPath);
                resultEntry.put(ItemKey.clientid.toString(), sResult.getStructureId());

                I_CmsResourceType type = OpenCms.getResourceManager().getResourceType(sResult.getResourceType());
                CmsFormatterInfoBean formatterInfo = new CmsFormatterInfoBean(type, false);
                formatterInfo.setTitleInfo(ItemKey.title.toString(), ItemKey.title.toString(), sResult.getTitle());
                formatterInfo.setSubTitleInfo(
                    ItemKey.type.toString(),
                    ItemKey.type.toString(),
                    CmsWorkplaceMessages.getResourceTypeName(wpLocale, type.getTypeName()));
                formatterInfo.setIcon(iconPath);

                // only add excerpt if not empty
                if (!CmsStringUtil.isEmptyOrWhitespaceOnly(sResult.getExcerpt())) {
                    formatterInfo.addAdditionalInfo(EXCERPT_FIELD_NAME, OpenCms.getWorkplaceManager().getMessages(
                        wpLocale).key(Messages.GUI_LABEL_EXCERPT), sResult.getExcerpt());
                }
                resultEntry.put(ItemKey.itemhtml.toString(), getFormattedListContent(formatterInfo));
                result.put(resultEntry);
            } catch (Exception e) {
                // TODO: Improve error handling
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        }
        return result;
    }

    /**
     * Generates a JSON-Map for all available content types.<p>
     * 
     * @param types the gallery-types
     * 
     * @return the content-types
     * 
     * @throws CmsLoaderException if something goes wrong
     */
    private JSONArray buildJSONForTypes(List<I_CmsResourceType> types) {

        JSONArray result = new JSONArray();
        if (types == null) {
            return result;
        }
        Iterator<I_CmsResourceType> it = types.iterator();
        while (it.hasNext()) {
            I_CmsResourceType type = it.next();
            CmsFormatterInfoBean formatterInfo = new CmsFormatterInfoBean(type, true);
            try {
                Locale wpLocale = getWorkplaceLocale();
                JSONObject jType = new JSONObject();
                jType.put(ItemKey.title.toString(), CmsWorkplaceMessages.getResourceTypeName(
                    wpLocale,
                    type.getTypeName()));
                formatterInfo.setTitleInfo(
                    ItemKey.title.toString(),
                    ItemKey.title.toString(),
                    CmsWorkplaceMessages.getResourceTypeName(wpLocale, type.getTypeName()));
                jType.put(ItemKey.typeid.toString(), type.getTypeId());
                jType.put(ItemKey.type.toString(), type.getTypeName());
                jType.put(ItemKey.info.toString(), CmsWorkplaceMessages.getResourceTypeDescription(
                    wpLocale,
                    type.getTypeName()));
                formatterInfo.setSubTitleInfo(
                    ItemKey.info.toString(),
                    ItemKey.info.toString(),
                    CmsWorkplaceMessages.getResourceTypeDescription(wpLocale, type.getTypeName()));
                String iconPath = CmsWorkplace.getResourceUri(CmsWorkplace.RES_PATH_FILETYPES
                    + OpenCms.getWorkplaceManager().getExplorerTypeSetting(type.getTypeName()).getIcon());
                jType.put(ItemKey.icon.toString(), iconPath);
                formatterInfo.setIcon(iconPath);
                JSONArray galleryIds = new JSONArray();
                Iterator<I_CmsResourceType> galleryTypes = type.getGalleryTypes().iterator();
                while (galleryTypes.hasNext()) {
                    I_CmsResourceType galleryType = galleryTypes.next();
                    galleryIds.put(galleryType.getTypeId());

                }
                jType.put(ItemKey.gallerytypeid.toString(), galleryIds);
                jType.put(ItemKey.itemhtml.toString(), getFormattedListContent(formatterInfo));
                result.put(jType);
            } catch (Exception e) {
                // TODO: Improve error handling
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        }

        return result;
    }

    /**
     * Gets the file-icon-name.<p>
     * 
     * @param path the file path
     * @return the file-icon-name for this files mime-type, or an empty string if none available
     */
    private String getFileIconName(String path) {

        String mt = OpenCms.getResourceManager().getMimeType(path, null);
        if (mt.equals("application/msword")
            || mt.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            mt = "msword.png";
        } else if (mt.equals("application/pdf")) {
            mt = "pdf.png";
        } else if (mt.equals("application/vnd.ms-excel")
            || mt.equals("application/excel")
            || mt.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            mt = "excel.png";
        } else if (mt.equals("application/vnd.ms-powerpoint")
            || mt.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
            mt = "powerpoint.png";
        } else if (mt.equals("image/jpeg")
            || mt.equals("image/gif")
            || mt.equals("image/png")
            || mt.equals("image/tiff")) {
            mt = "image.png";
        } else if (mt.equals("text/plain")) {
            mt = "plain.png";
        } else if (mt.equals("application/zip") || mt.equals("application/x-gzip") || mt.equals("application/x-tar")) {
            mt = "archiv.png";
        } else {
            mt = "";
        }

        return mt;
    }

    /**
     * Returns the rendered item html.<p>
     * 
     * @param type the resource-type
     * @param title the title
     * @param subtitle the subtitle
     * @param iconPath the icon path
     * @param searchResult the search-result if applicable 
     * @return the html string
     * @throws UnsupportedEncodingException if something goes wrong
     * @throws ServletException if something goes wrong
     * @throws IOException if something goes wrong
     * @throws CmsException if something goes wrong
     */
    private String getFormattedListContent(CmsFormatterInfoBean formatterInfo)
    throws UnsupportedEncodingException, ServletException, IOException, CmsException {

        Map<String, Object> reqAttributes = new HashMap<String, Object>();
        reqAttributes.put(CmsADEManager.ATTR_FORMATTER_INFO, formatterInfo);
        return formatterInfo.getResourceType().getFormattedContent(
            getCmsObject(),
            getRequest(),
            getResponse(),
            I_CmsResourceType.Formatter.GALLERY_LIST,
            reqAttributes);
    }

    /**
     * Generates a list of available galleries for the given gallery-type.<p>
     * 
     * @param galleryTypeId the gallery-type
     * @return the galleries
     */
    private List<CmsResource> getGalleriesByType(int galleryTypeId) {

        List<CmsResource> galleries = new ArrayList<CmsResource>();
        try {
            // get the galleries of the current site
            galleries = getCmsObject().readResources(
                "/",
                CmsResourceFilter.ONLY_VISIBLE_NO_DELETED.addRequireType(galleryTypeId));
        } catch (CmsException e) {
            // error reading resources with filter
            LOG.error(e.getLocalizedMessage(), e);
        }

        // if the current site is NOT the root site - add all other galleries from the system path
        if (!getCmsObject().getRequestContext().getSiteRoot().equals("")) {
            List<CmsResource> systemGalleries = null;
            try {
                // get the galleries in the /system/ folder
                systemGalleries = getCmsObject().readResources(
                    CmsWorkplace.VFS_PATH_SYSTEM,
                    CmsResourceFilter.ONLY_VISIBLE_NO_DELETED.addRequireType(galleryTypeId));
            } catch (CmsException e) {
                // error reading resources with filter
                LOG.error(e.getLocalizedMessage(), e);
            }

            if ((systemGalleries != null) && (systemGalleries.size() > 0)) {
                // add the found system galleries to the result
                galleries.addAll(systemGalleries);
            }
        }

        return galleries;
    }

    /**
     * Returns the current dialog mode name.<p>
     * 
     * @return the mode name
     */
    private String getIntegratorPath() {

        String integratorPath = getRequest().getParameter(ReqParam.integrator.toString());
        return CmsStringUtil.isNotEmptyOrWhitespaceOnly(integratorPath) ? integratorPath : "";

    }

    /**
     * Returns the current dialog mode name.<p>
     * 
     * @return the mode name
     */
    private String getModeName() {

        if (m_modeName == null) {
            String dialogMode = getRequest().getParameter(ReqParam.dialogmode.toString());
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(dialogMode)) {
                m_modeName = dialogMode;
            }
        }
        return m_modeName;
    }

    /**
     * Returns the JSON-data for the preview of a resource.<p>
     * 
     * @param resourcePath the resource site-path
     * @return the JSON-data
     * @throws Exception if something goes wrong
     */
    private JSONObject getPreviewData(String resourcePath) throws Exception {

        JSONObject result = new JSONObject();
        CmsObject cms = getCmsObject();

        // getting formatted content
        Map<String, Object> reqAttributes = new HashMap<String, Object>();
        CmsResource resource = cms.readResource(resourcePath);
        I_CmsResourceType type = getResourceManager().getResourceType(resource.getTypeId());
        CmsGalleryItemBean reqItem = new CmsGalleryItemBean(resource);
        reqItem.setTypeId(resource.getTypeId());
        reqItem.setTypeName(type.getTypeName());
        reqAttributes.put(ReqParam.galleryitem.toString(), reqItem);
        CmsContainerElementBean cntElem = new CmsContainerElementBean(resource.getStructureId(), null, null);
        cntElem.setSitePath(cms.getSitePath(resource));
        reqAttributes.put(CmsADEManager.ATTR_CURRENT_ELEMENT, cntElem);
        result.put(ItemKey.itemhtml.toString(), type.getFormattedContent(
            cms,
            getRequest(),
            getResponse(),
            I_CmsResourceType.Formatter.GALLERY_PREVIEW,
            reqAttributes));
        result.put(ItemKey.state.toString(), resource.getState().getState());

        // reading default explorer-type properties
        JSONArray propertiesJSON = new JSONArray();
        CmsExplorerTypeSettings setting = OpenCms.getWorkplaceManager().getExplorerTypeSetting(type.getTypeName());
        List<String> properties = setting.getProperties();
        String reference = setting.getReference();

        // looking up properties from referenced explorer types if properties list is empty
        while ((properties.size() == 0) && !CmsStringUtil.isEmptyOrWhitespaceOnly(reference)) {
            setting = OpenCms.getWorkplaceManager().getExplorerTypeSetting(reference);
            properties = setting.getProperties();
            reference = setting.getReference();
        }

        Iterator<String> propIt = properties.iterator();
        while (propIt.hasNext()) {
            String propertyName = propIt.next();
            CmsProperty property = cms.readPropertyObject(resource, propertyName, false);
            JSONObject propertyJSON = new JSONObject();
            propertyJSON.put(ItemKey.name.toString(), propertyName);
            propertyJSON.put(ItemKey.value.toString(), property.getValue());
            propertiesJSON.put(propertyJSON);
        }
        result.put(ItemKey.properties.toString(), propertiesJSON);
        // link path for the resource
        String linkpath = link(resourcePath);
        result.put(ItemKey.linkpath.toString(), linkpath);
        return result;

    }

    /**
     * Returns the request data parameter as JSON.<p> 
     * 
     * @return the JSON object
     * @throws JSONException if something goes wrong parsing the parameter string
     */
    private JSONObject getReqDataObj() throws JSONException {

        if (m_reqDataObj == null) {
            String dataParam = getRequest().getParameter(ReqParam.data.toString());
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(dataParam)) {
                m_reqDataObj = new JSONObject(dataParam);
            }
        }
        return m_reqDataObj;
    }

    /**
     * Returns the additional request data parameter for image gallery as JSON.<p> 
     * 
     * @return the JSON object
     * @throws JSONException if something goes wrong parsing the parameter string
     */
    private JSONObject getReqImageDataObj() throws JSONException {

        if (m_reqImageDataObj == null) {
            String imageDataParam = getRequest().getParameter(ReqParam.imagedata.toString());
            if (!CmsStringUtil.isEmptyOrWhitespaceOnly(imageDataParam)) {
                m_reqImageDataObj = new JSONObject(imageDataParam);
            }
        }
        return m_reqImageDataObj;
    }

    /**
     * Returns the resourceManager.<p>
     *
     * @return the resourceManager
     */
    private CmsResourceManager getResourceManager() {

        if (m_resourceManager == null) {
            m_resourceManager = OpenCms.getResourceManager();
        }
        return m_resourceManager;
    }

    /**
     * Returns the available resource type id's.<p>
     * 
     * @return the resource type id's
     */
    private JSONArray getResourceTypeIds() {

        if (m_resourceTypeIds == null) {
            try {
                JSONObject data = getReqDataObj();
                if ((data != null) && data.has(ResponseKey.types.toString())) {
                    m_resourceTypeIds = data.getJSONArray(ResponseKey.types.toString());
                }
            } catch (JSONException e) {
                // TODO: improve error handling
                LOG.error(e.getLocalizedMessage(), e);
            }
            if ((m_resourceTypeIds == null) || (m_resourceTypeIds.length() == 0)) {
                // using all available types if typeIds is null or empty
                m_resourceTypes = getResourceManager().getResourceTypes();
                m_resourceTypeIds = new JSONArray();
                for (I_CmsResourceType type : m_resourceTypes) {
                    m_resourceTypeIds.put(type.getTypeId());
                }
            }

        }
        return m_resourceTypeIds;
    }

    /**
     * Returns the available resource types.<p>
     * 
     * @return the resource types
     */
    private List<I_CmsResourceType> getResourceTypes() {

        if (m_resourceTypes == null) {
            m_resourceTypes = readContentTypes(getResourceTypeIds());
        }
        return m_resourceTypes;
    }

    /**
     * Returns the type names for the given type-ids.<p>
     * 
     * @param types the types
     * @return the type names
     * @throws CmsLoaderException if something goes wrong
     * @throws JSONException if something goes wrong
     */
    private List<String> getTypeNames(JSONArray types) throws CmsLoaderException, JSONException {

        List<String> ret = new ArrayList<String>();
        if ((types == null) || (types.length() == 0)) {
            return ret;
        }
        CmsResourceManager rm = OpenCms.getResourceManager();
        for (int i = 0; i < types.length(); i++) {
            ret.add(rm.getResourceType(types.getInt(i)).getTypeName());
        }
        return ret;
    }

    /**
     * Returns the search parameters for the given query data.<p>
     * 
     * @param queryData the query data
     * 
     * @return the prepared search parameters
     * @throws JSONException if something goes wrong reading the JSON
     */
    private CmsGallerySearchParameters prepareSearchParams(JSONObject queryData) throws JSONException {

        JSONArray types = queryData.getJSONArray(QueryKey.types.toString());
        JSONArray galleries = queryData.getJSONArray(QueryKey.galleries.toString());
        List<String> galleriesList = transformToStringList(galleries);
        JSONArray categories = queryData.getJSONArray(QueryKey.categories.toString());
        List<String> categoriesList = transformToStringList(categories);
        String queryStr = queryData.getString(QueryKey.query.toString());
        int matches = queryData.getInt(QueryKey.matchesperpage.toString());
        CmsGallerySearchParameters.CmsGallerySortParam sortOrder;
        if (queryData.has(QueryKey.sortorder.toString())) {
            String temp = queryData.getString(QueryKey.sortorder.toString());
            try {
                sortOrder = CmsGallerySearchParameters.CmsGallerySortParam.valueOf(temp);
            } catch (Exception e) {
                sortOrder = CmsGallerySearchParameters.CmsGallerySortParam.DEFAULT;
            }
        } else {
            sortOrder = CmsGallerySearchParameters.CmsGallerySortParam.DEFAULT;
        }
        int page = queryData.getInt(QueryKey.page.toString());
        String locale;
        if (queryData.has(QueryKey.locale.toString())
            && !CmsStringUtil.isEmptyOrWhitespaceOnly(queryData.getString(QueryKey.locale.toString()))) {
            locale = queryData.getString(QueryKey.locale.toString());
        } else {
            locale = getRequest().getParameter(ReqParam.locale.toString());
        }
        List<String> typeNames = new ArrayList<String>();

        try {
            typeNames = getTypeNames(types);
        } catch (CmsLoaderException e) {
            // TODO: Improve error handling
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getLocalizedMessage(), e);
            }
        }

        CmsGallerySearchParameters params = new CmsGallerySearchParameters();
        if (!CmsStringUtil.isEmptyOrWhitespaceOnly(queryStr)) {
            params.setSearchWords(queryStr);
        }
        params.setSearchLocale(locale);
        params.setSortOrder(sortOrder);
        params.setMatchesPerPage(matches);
        params.setResultPage(page);
        if (typeNames != null) {
            params.setResourceTypes(typeNames);
        }
        if (categoriesList != null) {
            params.setCategories(categoriesList);
        }
        if (galleriesList != null) {
            params.setGalleries(galleriesList);
        }
        return params;
    }

    /**
     * Generates a JSON-Map of all available categories. The category-path is used as the key.<p>
     * 
     * @param galleries the galleries
     * @return a JSON-Map of categories
     */
    private List<CmsCategory> readCategories(List<CmsResource> galleries) {

        CmsCategoryService catService = CmsCategoryService.getInstance();
        Iterator<CmsResource> iGalleries = galleries.iterator();
        List<String> repositories = new ArrayList<String>();
        while (iGalleries.hasNext()) {
            CmsResource res = iGalleries.next();
            repositories.addAll(catService.getCategoryRepositories(getCmsObject(), getCmsObject().getSitePath(res)));
        }
        List<CmsCategory> categories = null;
        try {
            categories = catService.readCategoriesForRepositories(getCmsObject(), "", true, repositories);
        } catch (CmsException e) {
            // error reading categories
            LOG.error(e.getLocalizedMessage(), e);
        }
        return categories;
    }

    /**
     * Reads the resource-types for given resource-type-id's.<p>
     * 
     * @param types the type-id's
     * @return the list of resource-types
     */
    private List<I_CmsResourceType> readContentTypes(JSONArray types) {

        List<I_CmsResourceType> result = new ArrayList<I_CmsResourceType>();
        if ((types == null) || (types.length() == 0)) {
            return result;
        }
        for (int i = 0; i < types.length(); i++) {
            try {
                result.add(getResourceManager().getResourceType(types.getInt(i)));
            } catch (CmsLoaderException e) {
                // TODO: Improve error handling
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            } catch (JSONException e) {
                // TODO: Improve error handling
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }
        }
        return result;
    }

    /**
     * Returns the list of galleries for the given types.<p>
     * 
     * @param types the gallery-types
     * @return the galleries
     */
    private Map<String, CmsGalleryTypeInfo> readGalleryTypes(List<I_CmsResourceType> resourceTypes) {

        Map<String, CmsGalleryTypeInfo> galleryTypeInfos = new HashMap<String, CmsGalleryTypeInfo>();
        Iterator<I_CmsResourceType> iTypes = resourceTypes.iterator();
        while (iTypes.hasNext()) {
            I_CmsResourceType contentType = iTypes.next();
            Iterator<I_CmsResourceType> galleryTypes = contentType.getGalleryTypes().iterator();
            while (galleryTypes.hasNext()) {
                I_CmsResourceType galleryType = galleryTypes.next();
                if (galleryTypeInfos.containsKey(galleryType.getTypeName())) {
                    CmsGalleryTypeInfo typeInfo = galleryTypeInfos.get(galleryType.getTypeName());
                    typeInfo.addContentType(contentType);
                } else {
                    CmsGalleryTypeInfo typeInfo = new CmsGalleryTypeInfo(
                        galleryType,
                        contentType,
                        getGalleriesByType(galleryType.getTypeId()));
                    galleryTypeInfos.put(galleryType.getTypeName(), typeInfo);
                }
            }
        }
        return galleryTypeInfos;
    }

    /**
     * Generates a JSON-Map of all system categories. The category-path is used as the key.<p>
     * 
     * @return a JSON-Map of categories
     */
    private JSONArray readSystemCategories() {

        CmsCategoryService catService = CmsCategoryService.getInstance();
        List<CmsCategory> foundCategories = Collections.emptyList();
        // String editedResource = null;
        //        if (CmsStringUtil.isNotEmpty(getParamResource())) {
        //            editedResource = getParamResource();
        //        }
        try {
            foundCategories = catService.readCategories(getCmsObject(), "", true, null);
        } catch (CmsException e) {
            // error reading categories
        }
        return buildJSONForCategories(foundCategories);

    }

    /**
     * Sets the properties for a resource and returns the updated preview data.<p>
     * 
     * @param resourcePath the site-path of the resource
     * @param properties the properties to set as JSON-data
     * @return the preview data
     * @throws Exception if something goes wrong
     */
    private JSONObject setProperties(String resourcePath, JSONArray properties) throws Exception {

        CmsResource resource = getCmsObject().readResource(resourcePath);
        if (properties != null) {
            for (int i = 0; i < properties.length(); i++) {
                String propertyName = properties.getJSONObject(i).getString(ItemKey.name.toString());
                String propertyValue = properties.getJSONObject(i).getString(ItemKey.value.toString());
                if (CmsStringUtil.isEmptyOrWhitespaceOnly(propertyValue)) {
                    propertyValue = "";
                }
                try {
                    CmsProperty currentProperty = getCmsObject().readPropertyObject(resource, propertyName, false);
                    // detect if property is a null property or not
                    if (currentProperty.isNullProperty()) {
                        // create new property object and set key and value
                        currentProperty = new CmsProperty();
                        currentProperty.setName(propertyName);
                        if (OpenCms.getWorkplaceManager().isDefaultPropertiesOnStructure()) {
                            // set structure value
                            currentProperty.setStructureValue(propertyValue);
                            currentProperty.setResourceValue(null);
                        } else {
                            // set resource value
                            currentProperty.setStructureValue(null);
                            currentProperty.setResourceValue(propertyValue);
                        }
                    } else if (currentProperty.getStructureValue() != null) {
                        // structure value has to be updated
                        currentProperty.setStructureValue(propertyValue);
                        currentProperty.setResourceValue(null);
                    } else {
                        // resource value has to be updated
                        currentProperty.setStructureValue(null);
                        currentProperty.setResourceValue(propertyValue);
                    }
                    CmsLock lock = getCmsObject().getLock(resource);
                    if (lock.isUnlocked()) {
                        // lock resource before operation
                        getCmsObject().lockResource(resourcePath);
                    }
                    // write the property to the resource
                    getCmsObject().writePropertyObject(resourcePath, currentProperty);
                    // unlock the resource
                    getCmsObject().unlockResource(resourcePath);
                } catch (CmsException e) {
                    // writing the property failed, log error
                    LOG.error(e.getLocalizedMessage(), e);
                }
            }

        }
        return getPreviewData(resourcePath);
    }

    /**
     * Transforms an <code>JSONArray</code> to a <code>List&lt;String&gt;</code>.
     * Returns null if the JSON is null or empty.<p>
     * 
     * @param arr
     * @return the resulting list
     * @throws JSONException if something goes wrong
     */
    private List<String> transformToStringList(JSONArray arr) throws JSONException {

        if ((arr == null) || (arr.length() == 0)) {
            return null;
        }
        List<String> ret = new ArrayList<String>();
        for (int i = 0; i < arr.length(); i++) {
            ret.add(arr.getString(i));
        }
        return ret;
    }
}
