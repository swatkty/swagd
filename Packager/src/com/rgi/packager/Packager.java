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

package com.rgi.packager;

import java.io.File;

import store.GeoPackageWriter;

import com.rgi.common.coordinate.referencesystem.profile.SphericalMercatorCrsProfile;
import com.rgi.common.task.AbstractTask;
import com.rgi.common.task.MonitorableTask;
import com.rgi.common.task.Settings;
import com.rgi.common.task.Settings.Setting;
import com.rgi.common.task.TaskFactory;
import com.rgi.common.task.TaskMonitor;
import com.rgi.common.tile.TileOrigin;
import com.rgi.common.tile.scheme.TileScheme;
import com.rgi.common.tile.store.tms.TmsReader;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.GeoPackage.OpenMode;

public class Packager extends AbstractTask implements MonitorableTask {
	public Packager(TaskFactory factory) {
		super(factory);
	}

	@Override
	public void execute(Settings opts) {
		File[] files = opts.getFiles(Setting.FileSelection);
		File gpkgFile = new File("foo.gpkg");
		if (files.length == 1) {
			try
			{
				// Get the stream of tile handles from the TmsReader
				SphericalMercatorCrsProfile smcp = new SphericalMercatorCrsProfile();
				TmsReader reader = new TmsReader(smcp, files[0].toPath());
				// Write them all to the geopackage
				Integer minZoom = reader.getZoomLevels().stream().min(Integer::compare).get();
				Integer maxZoom = reader.getZoomLevels().stream().max(Integer::compare).get();
				//TileScheme ts = new TileScheme(minZoom, maxZoom, tileMatrixHeight, tileMatrixWidth, TileOrigin.UpperLeft);
				//GeoPackageWriter gpkgWriter = new GeoPackageWriter(gpkgFile, "footiles", "1", "shitty tiles", reader.getBounds(), smcp, "mercator",	"2.0", "undefined", tileScheme, imageOutputFormat, imageWriteOptions)
				
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		} else {
			// i dunno
		}
	}

	@Override
	public void addMonitor(TaskMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void requestCancel() {
		// TODO Auto-generated method stub

	}
}
