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

package utility;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import store.GeoPackageReader;

import com.rgi.common.coordinate.CoordinateReferenceSystem;
import com.rgi.common.tile.store.TileStoreReader;
import com.rgi.common.tile.store.tms.TmsReader;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.GeoPackage.OpenMode;
import com.rgi.geopackage.tiles.TileSet;

public class TileStoreUtility
{
    public static Collection<TileStoreReader> getStores(final File file, final CoordinateReferenceSystem inputCoordinateReferenceSystem)
    {
        if(file.isDirectory()) // TMS or WMTS based directory create a TMS tile store
        {
            return Arrays.asList(new TmsReader(inputCoordinateReferenceSystem, file.toPath()));
        }

        if(file.getName().toLowerCase().endsWith(".gpkg"))
        {
            try(final GeoPackage gpkg = new GeoPackage(file, OpenMode.Open))
            {
                final Collection<TileSet> tileSets = gpkg.tiles().getTileSets();

                if(tileSets.size() > 0)
                {
                    final String tableName = tileSets.iterator().next().getTableName(); // TODO this just picks the first one

                    final GeoPackageReader reader = new GeoPackageReader(file, tableName);

                    this.resource = reader;

                    return reader;
                }
            }
            catch(final Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
