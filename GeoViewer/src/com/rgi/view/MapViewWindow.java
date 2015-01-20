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

package com.rgi.view;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.TileStoreLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;

import com.rgi.common.tile.profile.SphericalMercatorTileProfile;
import com.rgi.common.tile.profile.TileProfile;
import com.rgi.common.tile.profile.TileProfileFactory;
import com.rgi.common.tile.store.TileStore;
import com.rgi.common.tile.store.TileStoreException;
import com.rgi.common.tile.store.TmsTileStore;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.GeoPackage.OpenMode;
import com.rgi.geopackage.GeoPackageTileStore;
import com.rgi.geopackage.tiles.TileSet;

public class MapViewWindow extends JFrame implements JMapViewerEventListener
{
    private static final long serialVersionUID = 1337L;
    private JMapViewerTree    treeMap;
    private TileLoader        loader;
    //private final File        location;

    public MapViewWindow(final File location)
    {
        this("Tile Viewer", location);
    }

    public MapViewWindow(final String title, final File location)
    {
        super(title);

        this.treeMap = new JMapViewerTree("Visualized tile set");

        this.map().addJMVListener(this);
        this.setLayout(new BorderLayout());
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        final JPanel panel = new JPanel();
        this.add(panel, BorderLayout.NORTH);

        TileStore tileStore = MapViewWindow.pickTileStore(location);

        this.loader = new TileStoreLoader(tileStore, this.map());

        this.map().setTileLoader(this.loader);


        TileProfile profile = TileProfileFactory.create(tileStore.getCoordinateReferenceSystem());

        try
        {
            final com.rgi.common.coordinates.Coordinate<Double> center = profile.toGlobalGeodetic(tileStore.calculateBounds().getCenter());

            this.map().setDisplayPosition(new Coordinate(center.getY(),
                                                         center.getX()),
                                          Collections.min(tileStore.getZoomLevels()));
        }
        catch(TileStoreException ex)
        {
            ex.printStackTrace();
        }

        this.add(this.treeMap, BorderLayout.CENTER);
    }

    @Override
    public void processCommand(final JMVCommandEvent command)
    {
        // TODO:
        // This fires whenever the map is moved or zoomed in. Use this to call
        // methods that update relevant zoom-dependent information like pixel
        // resolution or current zoom level.
    }

    private static TileStore pickTileStore(final File location)
    {
        if(location.isDirectory()) // TMS or WMTS based directory create a TMS tile store
        {
            return new TmsTileStore(new SphericalMercatorTileProfile(), location.toPath());
        }

        if(location.getName().toLowerCase().endsWith(".gpkg"))
        {
            try
            {
                final GeoPackage gpkg = new GeoPackage(location, OpenMode.Open);

                final Collection<TileSet> tileSets = gpkg.tiles().getTileSets();

                if(tileSets.size() > 0)
                {
                    final TileSet set = tileSets.iterator().next(); // TODO this just picks the first one
                    return new GeoPackageTileStore(gpkg, set);
                }
            }
            catch(final Exception e)
            {
                e.printStackTrace();
            }
        }

        throw new NullPointerException("Tile store unable to be generated.");
    }

    private JMapViewer map()
    {
        return this.treeMap.getViewer();
    }
}