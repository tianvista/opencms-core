/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/containerpage/CmsContainerElementBean.java,v $
 * Date   : $Date: 2010/01/26 14:48:26 $
 * Version: $Revision: 1.5 $
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

package org.opencms.xml.containerpage;

import org.opencms.util.CmsUUID;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * One element of a container in a container page.<p>
 * 
 * @author Michael Moossen
 * 
 * @version $Revision: 1.5 $ 
 * 
 * @since 7.6 
 */
public class CmsContainerElementBean {

    /** The client id including properties-hash. */
    private final transient String m_clientId;

    /** The element's structure id. */
    private final CmsUUID m_elementId;

    /** The formatter's structure id. */
    private final CmsUUID m_formatterId;

    /** The configured properties. */
    private final Map<String, String> m_properties;

    /** The element site path, only set while rendering. */
    private String m_sitePath;

    /**
     * Creates a new container page element bean.<p> 
     *  
     * @param elementId the element's structure id
     * @param formatterId the formatter's structure id, could be <code>null</code>
     * @param properties the properties as a map of name/value pairs
     **/
    public CmsContainerElementBean(CmsUUID elementId, CmsUUID formatterId, Map<String, String> properties) {

        m_elementId = elementId;
        m_formatterId = formatterId;
        Map<String, String> props = (properties == null ? new HashMap<String, String>() : properties);
        m_properties = Collections.unmodifiableMap(props);
        String clientId = m_elementId.toString();
        if (!m_properties.isEmpty()) {
            int hash = m_properties.toString().hashCode();
            clientId += "#" + hash;
        }
        m_clientId = clientId;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof CmsContainerElementBean)) {
            return false;
        }
        return getClientId().equals(((CmsContainerElementBean)obj).getClientId());
    }

    /**
     * Returns the client side id including the property-hash.<p>
     * 
     * @return the id
     */
    public String getClientId() {

        return m_clientId;
    }

    /**
     * Returns the element's structure id.<p>
     *
     * @return the element's structure id
     */
    public CmsUUID getElementId() {

        return m_elementId;
    }

    /**
     * Returns the formatter's structure id.<p>
     *
     * @return the formatter's structure id
     */
    public CmsUUID getFormatterId() {

        return m_formatterId;
    }

    /**
     * Returns the configured properties.<p>
     * 
     * @return the configured properties
     */
    public Map<String, String> getProperties() {

        return m_properties;
    }

    /**
     * Returns the element site path, only set while rendering.<p>
     * 
     * @return the element site path
     */
    public String getSitePath() {

        return m_sitePath;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        return m_clientId.hashCode();
    }

    /**
     * Sets the element's site path.<p>
     * 
     * @param sitePath the element's site path to set
     */
    public void setSitePath(String sitePath) {

        m_sitePath = sitePath;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return getClientId();
    }
}
