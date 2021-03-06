package com.rgi.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Random;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import com.rgi.common.BoundingBox;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.verification.ConformanceException;
import com.rgi.store.tiles.TileStoreReader;
import com.rgi.store.tiles.geopackage.GeoPackageReader;
import com.rgi.view.pane.BrowserPane;
import com.rgi.view.pane.TreePane;
import com.rgi.view.pane.ViewerMenuBar;

public class ViewerMainWindow extends Application
{

    private Scene scene;
    private final static BorderPane layout = new BorderPane();

    @Override
    public void start(final Stage primaryStage) throws Exception
    {
        primaryStage.setTitle("Map Viewer");
        this.scene = new Scene(layout, 750, 500, Color.web("#666970"));

        try(TileStoreReader baseReader = this.createTestTileStoreReader())
        {
            TreePane tree = new TreePane(Arrays.asList(baseReader));

            layout.setLeft(tree);
            layout.setCenter(new BrowserPane());
            layout.setTop(new ViewerMenuBar(primaryStage, tree));

            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(500);
            primaryStage.setScene(this.scene);
            primaryStage.show();;
        }
    }

    public static void main(final String[] args)
    {
       launch(args);
    }

    /*
     *  Delete these methods (testing purposes only)
     */
    @Deprecated
    private TileStoreReader createTestTileStoreReader()
    {
        String tileSetName = "tileSet";
        File testFile = getRandomFile(5);
        testFile.deleteOnExit();
        try(GeoPackage gpkg = createAGeoPackage(testFile, tileSetName))
        {
            gpkg.close();
            return new GeoPackageReader(testFile, tileSetName);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        throw new RuntimeException("cannot create test reader");

    }

    @Deprecated
    private static String getRanString(final int length)
    {
        Random randomGenerator = new Random();
        final String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final char[] text = new char[length];
        for (int i = 0; i < length; i++)
        {

         text[i] = characters.charAt(randomGenerator.nextInt(characters.length()));
        }
        return new String(text);
    }

    @Deprecated
    private static File getRandomFile(final int length)
    {
        File testFile;

        do
        {
            testFile = new File(String.format(FileSystems.getDefault().getPath(getRanString(length)).toString() + ".gpkg"));
        }
        while (testFile.exists());

        return testFile;
    }

    @Deprecated
    private static GeoPackage createAGeoPackage(final File testFile, final String tileSetName) throws ClassNotFoundException, SQLException, ConformanceException, IOException
    {
        try(GeoPackage gpkg = new GeoPackage(testFile))
        {
            gpkg.tiles().addTileSet(tileSetName, getRanString(6), getRanString(7), new BoundingBox(-180, -90, 180, 90), gpkg.core().getSpatialReferenceSystem("EPSG", 4326));
            return gpkg;
        }
    }
}
