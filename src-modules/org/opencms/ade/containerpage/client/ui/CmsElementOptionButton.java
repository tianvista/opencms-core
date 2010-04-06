/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/org/opencms/ade/containerpage/client/ui/Attic/CmsElementOptionButton.java,v $
 * Date   : $Date: 2010/04/06 09:49:44 $
 * Version: $Revision: 1.1 $
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

package org.opencms.ade.containerpage.client.ui;

import org.opencms.ade.containerpage.client.draganddrop.CmsDragContainerElement;
import org.opencms.gwt.client.ui.CmsImageButton;

/**
 * An optional container element button.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.1 $
 * 
 * @since 8.0.0
 */
public class CmsElementOptionButton extends CmsImageButton {

    /** The associated container element. */
    private CmsDragContainerElement m_dragElement;

    /** The associated tool-bar button. */
    private I_CmsContainerpageToolbarButton m_toolbarButton;

    /**
     * Constructor.<p>
     * 
     * @param toolbarButton the tool-bar button associated with this button, providing all necessary information
     * @param element the element to create this button for
     */
    public CmsElementOptionButton(I_CmsContainerpageToolbarButton toolbarButton, CmsDragContainerElement element) {

        super(toolbarButton.getIconClass(), false);
        this.setTitle(toolbarButton.getTitle());
        this.addStyleName(toolbarButton.getIconClass());
        m_toolbarButton = toolbarButton;
        m_dragElement = element;
    }

    /**
     * Returns the dragElement.<p>
     *
     * @return the dragElement
     */
    public CmsDragContainerElement getDragElement() {

        return m_dragElement;
    }

    /**
     * Returns the associated tool-bar button.<p>
     * 
     * @return the associated tool-bar button
     */
    public I_CmsContainerpageToolbarButton getToolbarButton() {

        return m_toolbarButton;
    }

}
