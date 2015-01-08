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

package com.rgi.erdc.gpkg;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.rgi.erdc.BoundingBox;
import com.rgi.erdc.CoordinateReferenceSystem;
import com.rgi.erdc.Dimension2D;
import com.rgi.erdc.coordinates.AbsoluteTileCoordinate;
import com.rgi.erdc.gpkg.tiles.TileMatrix;
import com.rgi.erdc.gpkg.tiles.TileSet;
import com.rgi.erdc.tile.Tile;
import com.rgi.erdc.tile.TileException;
import com.rgi.erdc.tile.profile.TileProfile;
import com.rgi.erdc.tile.profile.TileProfileFactory;
import com.rgi.erdc.tile.store.TileStore;
import com.rgi.erdc.tile.store.TileStoreException;

/**
 * @author Luke Lambert
 *
 */
public class GeoPackageTileStore implements TileStore
{
    public GeoPackageTileStore(final GeoPackage geoPackage,
                               final TileSet    tileSet) throws SQLException
    {
        if(geoPackage == null)
        {
            throw new IllegalArgumentException("GeoPackage may not be null");
        }

        if(tileSet == null)
        {
            throw new IllegalArgumentException("Tile set may not be null or empty");
        }

        this.geoPackage    = geoPackage;
        this.tileSet       = tileSet;
        this.tileProfile   = TileProfileFactory.create(tileSet.getSpatialReferenceSystem().getOrganization(),
                                                       tileSet.getSpatialReferenceSystem().getOrganizationSrsId());
        this.tileMatricies = geoPackage.tiles()
                                       .getTileMatrices(tileSet)
                                       .stream()
                                       .collect(Collectors.toMap(tileMatrix -> tileMatrix.getZoomLevel(),
                                                                 tileMatrix -> tileMatrix));
    }

    @Override
    public BoundingBox calculateBounds() throws TileStoreException
    {
        try
        {
            return this.geoPackage.tiles()
                                  .getTileMatrixSet(this.tileSet)
                                  .getBoundingBox();
        }
        catch(final Exception ex)
        {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public long countTiles() throws TileStoreException
    {
        try
        {
            return this.geoPackage.core().getRowCount(this.tileSet);
        }
        catch(final SQLException ex)
        {
            throw new TileStoreException(ex);
        }
    }

    @Override
    public long calculateSize() throws TileStoreException
    {
        return this.geoPackage.getFile().getTotalSpace();
    }

    @Override
    public Tile getTile(final AbsoluteTileCoordinate coordinate) throws TileException
    {
        if(coordinate == null)
        {
            throw new IllegalArgumentException("Coordinate may not be null");
        }

        try
        {
            final com.rgi.erdc.gpkg.tiles.Tile tile = this.geoPackage.tiles().getTile(this.tileSet,
                                                                                      this.tileProfile.absoluteToCrsCoordinate(coordinate));

            return tile != null ? new Tile(coordinate, ImageIO.read(new ByteArrayInputStream(tile.getImageData())))
                                : null;
        }
        catch(final SQLException | IOException ex)
        {
            throw new TileException(ex);
        }
    }

    @Override
    public Tile addTile(final AbsoluteTileCoordinate coordinate, final BufferedImage image) throws TileException
    {
        if(coordinate == null)
        {
            throw new IllegalArgumentException("Coordinate may not be null");
        }

        if(image == null)
        {
            throw new IllegalArgumentException("Image may not be null");
        }

        final String outputFormat = "PNG";  // TODO how do we want to pick this ?

        try(final ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            if(!ImageIO.write(image, outputFormat, outputStream))
            {
                throw new TileException(String.format("No appropriate image writer found for format '%s'", outputFormat));
            }

            TileMatrix tileMatrix = null;

            if(!this.tileMatricies.containsKey(coordinate.getZoomLevel()))
            {
                tileMatrix = this.addTileMatrix(coordinate.getZoomLevel(), image.getHeight(), image.getWidth());
                this.tileMatricies.put(coordinate.getZoomLevel(),
                                       tileMatrix);
            }
            else
            {
                tileMatrix = this.tileMatricies.get(coordinate.getZoomLevel());
            }

            this.geoPackage.tiles()
                           .addTile(this.tileSet,
                                    tileMatrix,
                                    this.tileProfile.absoluteToCrsCoordinate(coordinate),
                                    outputStream.toByteArray());
        }
        catch(final Exception ex)
        {
           throw new TileException(ex);
        }

        return new Tile(coordinate, image);
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem()
    {
        return new CoordinateReferenceSystem(this.tileSet.getSpatialReferenceSystem().getOrganization(),
                                             this.tileSet.getSpatialReferenceSystem().getOrganizationSrsId());
    }

    private TileMatrix addTileMatrix(final int zoomLevel, final int pixelHeight, final int pixelWidth) throws SQLException
    {
        final int tileDimension = (int)Math.pow(2.0, zoomLevel);    // Assumes zoom*2 convension, with 1 tile at zoom level 0

        final Dimension2D dimensions = this.tileProfile.getTileDimensions(zoomLevel);

        return this.geoPackage.tiles()
                              .addTileMatrix(this.tileSet,
                                             zoomLevel,
                                             tileDimension,
                                             tileDimension,
                                             pixelHeight,
                                             pixelWidth,
                                             dimensions.getHeight() / pixelHeight,
                                             dimensions.getWidth()  / pixelWidth);
    }

    @Override
    public Set<Integer> getZoomLevels() throws TileStoreException
    {
        try
        {
            return this.geoPackage.tiles()
                                  .getTileZoomLevels(this.tileSet);
        }
        catch(final SQLException ex)
        {
            throw new TileStoreException(ex);
        }
    }

    private final GeoPackage               geoPackage;
    private final TileSet                  tileSet;
    private final TileProfile              tileProfile;
    private final Map<Integer, TileMatrix> tileMatricies;
}
