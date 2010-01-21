/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/jsp/CmsJspTagProperty.java,v $
 * Date   : $Date: 2010/01/21 08:56:59 $
 * Version: $Revision: 1.9 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.jsp;

import org.opencms.file.CmsObject;
import org.opencms.file.CmsProperty;
import org.opencms.flex.CmsFlexController;
import org.opencms.i18n.CmsEncoder;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.util.CmsStringUtil;
import org.opencms.xml.containerpage.CmsContainerElementBean;
import org.opencms.xml.content.CmsXmlContentProperty;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;

/**
 * Provides access to the properties of a resource in the OpenCms VFS .<p>
 * 
 * Of particular importance is the setting of the <code>file</code> attribute,
 * which can take the following values.<p>
 * 
 * This attribute allows you to specify where to search for the property.<BR>            
 * The following values are supported: 
 * </P>
 * <DL>
 *   <DT><b>uri</b> (default)</DT>
 *   <DD>  Look up  the property on the file with the 
 *   uri requested by the user.</DD>
 *   <DT><b>search.uri</b> or <b>search</b></DT>
 *   <DD>Look up the property by also checking all parent folders for the property, 
 *   starting with the file with uri requested by the user and 
 *   going "upward" if the property was not found there.</DD>
 *   <DT><b>element.uri</b></DT>
 *   <DD>Look up the property on the currently 
 *   processed sub - element. This is useful in templates or other pages that 
 *   consist of many elements.</DD>
 *   <DT><b>search.element.uri</b></DT>
 *   <DD>Look up the property by also checking all parent folders for the 
 *   property, starting with the file with the currently processed sub - 
 *   element and going "upward" if the property was not found there.</DD>
 *   <DT>sitemap</DT> 
 *   <DD>reads from the current sitemap entry</DD> 
 *   <DT>search.sitemap</DT> 
 *   <DD>Look up the property by also checking all parent sitemap entries 
 *   for the property, starting with the current sitemap entry and 
 *   going "upward" if the property was not found there.</DD>
 *   <DT>container</DT> 
 *   <DD>reads from the current container element</DD> 
 *   <DT><B>{some-file-uri}</B></DT>
 *   <DD>Look up the property on that exact file 
 *   uri in the OpenCms VFS,<EM> fallback if no other valid option is 
 *   selected for the file attribute.</EM></DD>
 * </DL>
 *   
 * <P>There are also some deprecated options for the "file" value that are 
 * still supported but should not longer be used:</P>
 * <DL>
 *   <DT>parent</DT>
 *   <DD>same as <STRONG>uri</STRONG></DD> 
 *   <DT>search-parent</DT> 
 *   <DD>same as <STRONG>search.uri</STRONG></DD> 
 *   <DT>this</DT>
 *   <DD>same as <STRONG>element.uri</STRONG></DD> 
 *   <DT>search-this</DT> 
 *   <DD>same as <STRONG>search.element.uri</STRONG></DD>
 * </DL>
 *
 * @author Alexander Kandzior 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.9 $ 
 * 
 * @since 6.0.0 
 */
public class CmsJspTagProperty extends TagSupport {

    /** Constants for <code>file</code> attribute interpretation. */
    private enum FileUse {

        /** Use container element. */
        CONTAINER("container"),
        /** Use element uri. */
        ELEMENT_URI("element.uri"),
        /** Use parent (same as {@link #URI}). */
        PARENT("parent"),
        /** Use search (same as {@link #SEARCH_URI}). */
        SEARCH("search"),
        /** Use search element uri. */
        SEARCH_ELEMENT_URI("search.element.uri"),
        /** Use search parent (same as {@link #SEARCH_URI}). */
        SEARCH_PARENT("search-parent"),
        /** Use search sitemap entries. */
        SEARCH_SITEMAP("search.sitemap"),
        /** Use seach this (same as {@link #SEARCH_ELEMENT_URI}). */
        SEARCH_THIS("search-this"),
        /** Use search uri. */
        SEARCH_URI("search.uri"),
        /** Use sitemap entries. */
        SITEMAP("sitemap"),
        /** Use this (same as {@link #ELEMENT_URI}). */
        THIS("this"),
        /** Use uri. */
        URI("uri");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private FileUse(String name) {

            m_name = name;
        }

        /**
         * Parses a string into an enumeration element.<p>
         * 
         * @param name the name of the element
         * 
         * @return the element with the given name or <code>null</code> if not found
         */
        public static FileUse parse(String name) {

            for (FileUse fileUse : FileUse.values()) {
                if (fileUse.getName().equals(name)) {
                    return fileUse;
                }
            }
            return null;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsJspTagProperty.class);

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = -4040833541258687977L;

    /** The default value. */
    private String m_defaultValue;

    /** Indicates if HTML should be escaped. */
    private boolean m_escapeHtml;

    /** The file to read the property from. */
    private String m_propertyFile;

    /** The name of the property to read. */
    private String m_propertyName;

    /**
     * Internal action method.<p>
     * 
     * @param action the search action
     * @param req the current request
     * 
     * @return String the value of the property or <code>null</code> if not found (and no defaultValue provided)
     *      
     * @throws CmsException if something goes wrong
     */
    public static Map<String, String> propertiesTagAction(String action, ServletRequest req) throws CmsException {

        CmsFlexController controller = CmsFlexController.getController(req);

        FileUse useAction = FileUse.URI;
        if (action != null) {
            // if action is set overwrite default
            useAction = FileUse.parse(action);
        }

        String vfsUri = null;
        String sitemapUri = null;
        boolean search = false;
        if (useAction != null) {
            switch (useAction) {
                case URI:
                case PARENT:
                    // read properties of parent (i.e. top requested) file
                    vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    break;
                case SEARCH:
                case SEARCH_URI:
                case SEARCH_PARENT:
                    // try to find property on parent file and all parent folders
                    vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    search = true;
                    break;
                case ELEMENT_URI:
                case THIS:
                    // read properties of this file            
                    vfsUri = controller.getCurrentRequest().getElementUri();
                    break;
                case SEARCH_ELEMENT_URI:
                case SEARCH_THIS:
                    // try to find property on this file and all parent folders
                    vfsUri = controller.getCurrentRequest().getElementUri();
                    search = true;
                    break;
                case CONTAINER:
                    CmsObject cms = controller.getCmsObject();
                    // try to find property on the container element
                    try {
                        CmsContainerElementBean currentElement = OpenCms.getADEManager().getCurrentElement(req);
                        return CmsXmlContentProperty.mergeDefaults(
                            cms,
                            cms.readResource(currentElement.getElementId()),
                            currentElement.getProperties());
                    } catch (CmsException e) {
                        // most likely we are not in a container page
                        LOG.debug(e.getLocalizedMessage(), e);
                        return new HashMap<String, String>();
                    }
                case SITEMAP:
                    // try to find property on this sitemap entry
                    sitemapUri = OpenCms.getSitemapManager().getCurrentUri(req);
                    if (sitemapUri == null) {
                        // fall back
                        vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    }
                    break;
                case SEARCH_SITEMAP:
                    // try to find property on this sitemap entry all parent entries
                    sitemapUri = OpenCms.getSitemapManager().getCurrentUri(req);
                    if (sitemapUri == null) {
                        // fall back
                        vfsUri = controller.getCmsObject().getRequestContext().getUri();
                    }
                    search = true;
                    break;
            }
        } else {
            // read properties of the file named in the attribute  
            vfsUri = CmsLinkManager.getAbsoluteUri(action, controller.getCurrentRequest().getElementUri());
            search = false;
        }

        // now read the property from the VFS
        Map<String, String> value;
        if (vfsUri != null) {
            value = CmsProperty.toMap(controller.getCmsObject().readPropertyObjects(vfsUri, search));
        } else if (!search) {
            value = OpenCms.getSitemapManager().getCurrentEntry(req).getProperties();
        } else {
            value = OpenCms.getSitemapManager().getSearchProperties(controller.getCmsObject(), sitemapUri);
        }
        return value;
    }

    /**
     * Internal action method.<p>
     * 
     * @param property the property to look up
     * @param action the search action
     * @param defaultValue the default value
     * @param escape if the result html should be escaped or not
     * @param req the current request
     * 
     * @return String the value of the property or <code>null</code> if not found (and no
     *      defaultValue provided)
     *      
     * @throws CmsException if something goes wrong
     */
    public static String propertyTagAction(
        String property,
        String action,
        String defaultValue,
        boolean escape,
        ServletRequest req) throws CmsException {

        String value = propertiesTagAction(action, req).get(property);
        if (value == null) {
            value = defaultValue;
        }
        if (escape) {
            // HTML escape the value 
            value = CmsEncoder.escapeHtml(value);
        }
        return value;
    }

    /**
     * @return SKIP_BODY
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        ServletRequest req = pageContext.getRequest();

        // This will always be true if the page is called through OpenCms 
        if (CmsFlexController.isCmsRequest(req)) {

            try {
                String prop = propertyTagAction(getName(), getFile(), m_defaultValue, m_escapeHtml, req);
                // Make sure that no null String is returned
                if (prop == null) {
                    prop = "";
                }
                pageContext.getOut().print(prop);

            } catch (Exception ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(Messages.get().getBundle().key(Messages.ERR_PROCESS_TAG_1, "property"), ex);
                }
                throw new javax.servlet.jsp.JspException(ex);
            }
        }
        return SKIP_BODY;
    }

    /**
     * Returns the default value.<p>
     * 
     * @return the default value
     */
    public String getDefault() {

        return m_defaultValue != null ? m_defaultValue : "";
    }

    /**
     * The value of the escape html flag.<p>
     * 
     * @return the value of the escape html flag
     */
    public String getEscapeHtml() {

        return "" + m_escapeHtml;
    }

    /**
     * Returns the file name.<p>
     * 
     * @return the file name
     */
    public String getFile() {

        return m_propertyFile != null ? m_propertyFile : "parent";
    }

    /**
     * Returns the property name.<p>
     * 
     * @return String the property name
     */
    public String getName() {

        return m_propertyName != null ? m_propertyName : "";
    }

    /**
     * @see javax.servlet.jsp.tagext.Tag#release()
     */
    @Override
    public void release() {

        super.release();
        m_propertyFile = null;
        m_propertyName = null;
        m_defaultValue = null;
        m_escapeHtml = false;
    }

    /**
     * Sets the default value.<p>
     * 
     * This is used if a selected property is not found.<p>
     * 
     * @param def the default value
     */
    public void setDefault(String def) {

        if (def != null) {
            m_defaultValue = def;
        }
    }

    /**
     * Set the escape html flag.<p>
     * 
     * @param value should be <code>"true"</code> or <code>"false"</code> (all values other then <code>"true"</code> are
     * considered to be false)
     */
    public void setEscapeHtml(String value) {

        if (value != null) {
            m_escapeHtml = Boolean.valueOf(value.trim()).booleanValue();
        }
    }

    /**
     * Sets the file name.<p>
     * 
     * @param file the file name
     */
    public void setFile(String file) {

        if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(file)) {
            m_propertyFile = file;
        }
    }

    /**
     * Sets the property name.<p>
     * 
     * @param name the property name to set
     */
    public void setName(String name) {

        if (name != null) {
            m_propertyName = name;
        }
    }

}
