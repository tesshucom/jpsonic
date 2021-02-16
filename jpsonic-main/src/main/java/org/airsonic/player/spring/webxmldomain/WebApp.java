/*
 * This file is part of Jpsonic.
 *
 * Jpsonic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jpsonic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * (C) 2009 Sindre Mehus
 * (C) 2016 Airsonic Authors
 * (C) 2018 tesshucom
 */

package org.airsonic.player.spring.webxmldomain;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "web-app")
@XmlAccessorType(XmlAccessType.FIELD)
public class WebApp {

    @XmlElement(name = "servlet")
    private List<ServletDef> servletDefs;

    @XmlElement(name = "servlet-mapping")
    private List<ServletMappingDef> servletMappingDefs;

    public List<ServletDef> getServletDefs() {
        return servletDefs;
    }

    public void setServletDefs(List<ServletDef> servletDefs) {
        this.servletDefs = servletDefs;
    }

    public List<ServletMappingDef> getServletMappingDefs() {
        return servletMappingDefs;
    }

    public void setServletMappingDefs(List<ServletMappingDef> servletMappingDefs) {
        this.servletMappingDefs = servletMappingDefs;
    }
}
