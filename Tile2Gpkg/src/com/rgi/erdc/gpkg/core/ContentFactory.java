/*  Copyright (C) 2014 Reinventing Geospatial, Inc
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>,
 *  or write to the Free Software Foundation, Inc., 59 Temple Place -
 *  Suite 330, Boston, MA 02111-1307, USA.
 */

package com.rgi.erdc.gpkg.core;

import java.util.Date;

import com.rgi.erdc.BoundingBox;

/**
 * @author Luke Lambert
 *
 * @param <T> Extends {@link Content}
 */
public interface ContentFactory<T extends Content>
{
    public T create(final String                 tableName,
                    final String                 dataType,
                    final String                 identifier,
                    final String                 description,
                    final Date                   lastChange,
                    final BoundingBox            boundingBox,
                    final SpatialReferenceSystem spatialReferenceSystem);
}
