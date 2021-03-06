/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rgi.geopackage.extensions.network;

import com.rgi.common.BoundingBox;
import com.rgi.common.Pair;
import com.rgi.common.util.jdbc.JdbcUtility;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.core.ContentFactory;
import com.rgi.geopackage.core.GeoPackageCore;
import com.rgi.geopackage.core.SpatialReferenceSystem;
import com.rgi.geopackage.extensions.Extension;
import com.rgi.geopackage.extensions.GeoPackageExtensions;
import com.rgi.geopackage.extensions.Scope;
import com.rgi.geopackage.extensions.implementation.ExtensionImplementation;
import com.rgi.geopackage.extensions.implementation.ImplementsExtension;
import com.rgi.geopackage.utility.DatabaseUtility;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Implementation of the SWAGD Network GeoPackage extension
 *
 * @author Luke Lambert
 *
 */
@ImplementsExtension(name = "SWAGD_network")
public class GeoPackageNetworkExtension extends ExtensionImplementation
{
    /**
     * Constructor
     *
     * @param databaseConnection
     *             The open connection to the database that contains a GeoPackage
     * @param geoPackageCore
     *             'Core' subsystem of the {@link GeoPackage} implementation
     * @param geoPackageExtensions
     *             'Extensions' subsystem of the {@link GeoPackage} implementation
     * @throws SQLException
     *             if getting the corresponding {@link Extension} from the
     *             {@link GeoPackage} fails
     */
    public GeoPackageNetworkExtension(final Connection           databaseConnection,
                                      final GeoPackageCore       geoPackageCore,
                                      final GeoPackageExtensions geoPackageExtensions) throws SQLException
    {
        super(databaseConnection, geoPackageCore, geoPackageExtensions);
    }

    @Override
    public String getTableName()
    {
        return null;
    }

    @Override
    public String getColumnName()
    {
        return null;
    }

    @Override
    public String getExtensionName()
    {
        return ExtensionName;
    }

    @Override
    public String getDefinition()
    {
        return "definition"; // TODO
    }

    @Override
    public Scope getScope()
    {
        return Scope.ReadWrite;
    }

    /**
     * Gets all entries in the GeoPackage's contents table with the "network"
     * data_type
     *
     * @return Returns a collection of {@link Network}s
     * @throws SQLException
     *             throws if the method
     *             {@link #getNetworks(SpatialReferenceSystem) getNetworks}
     *             throws
     */
    public Collection<Network> getNetworks() throws SQLException
    {
        return this.getNetworks(null);
    }

    /**
     * Gets all entries in the GeoPackage's contents table with the "network"
     * data_type that also match the supplied spatial reference system
     *
     * @param matchingSpatialReferenceSystem
     *            Spatial reference system that returned {@link Network}s refer
     *            to
     * @return Returns a collection of {@link Network}s
     * @throws SQLException
     *             Throws if there's an SQL error
     */
    public Collection<Network> getNetworks(final SpatialReferenceSystem matchingSpatialReferenceSystem) throws SQLException
    {
        return this.geoPackageCore.getContent(Network.NetworkContentType,
                                              (tableName,
                                               dataType,
                                               identifier,
                                               description,
                                               lastChange,
                                               minimumX,
                                               minimumY,
                                               maximumX,
                                               maximumY,
                                               spatialReferenceSystemIdentifier) -> new Network(tableName,
                                                                                                identifier,
                                                                                                description,
                                                                                                lastChange,
                                                                                                minimumX,
                                                                                                minimumY,
                                                                                                maximumX,
                                                                                                maximumY,
                                                                                                spatialReferenceSystemIdentifier),
                                              matchingSpatialReferenceSystem);
    }

    /**
     * Gets the number of edges in the given network
     *
     * @param network
     *             Network table reference
     * @return the number of edges in the network
     * @throws SQLException
     *             if there is a database error
     *
     */
    public int getEdgeCount(final Network network) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("The network may not be null");
        }

        final String query = String.format("SELECT COUNT(*) FROM %s;",
                                           network.getTableName());

        final Integer count = JdbcUtility.selectOne(this.databaseConnection,
                                                    query,
                                                    null,
                                                    results -> results.getInt(1));
        if(count == null)
        {
            throw new SQLException("Edge count query failed to return a result");
        }

        return count;
    }

    /**
     * Gets the number of nodes in the given Network
     *
     * @param network
     *             Network table reference
     * @return the number of nodes in the network
     * @throws SQLException
     *             if there is a database error
     */
    public int getNodeCount(final Network network) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("The network may not be null");
        }

        final String query = String.format("Select COUNT(*) from(select from_node from %s UNION select to_node from %s);",
                                           network.getTableName(),
                                           network.getTableName());

        final Integer count = JdbcUtility.selectOne(this.databaseConnection,
                                                    query,
                                                    null,
                                                    results -> results.getInt(1));

        if(count == null)
        {
            throw new SQLException("Node count query failed to return a result");
        }

        return count;
    }

    /**
     * @param network
     *             Network table reference
     * @return the name of the unique corresponding network attribute table
     */
    public static String getNodeAttributesTableName(final Network network)
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        return GeoPackageNetworkExtension.getNodeAttributesTableName(network.getTableName());
    }

    /**
     * @param networkTableName
     *             Network table name
     * @return the name of the unique corresponding network attribute table
     */
    public static String getNodeAttributesTableName(final String networkTableName)
    {
        if(networkTableName == null || networkTableName.isEmpty())
        {
            throw new IllegalArgumentException("Network may not be null or empty");
        }

        return networkTableName + NodeAttributesTableSuffix;
    }

    /**
     * Gets a network object based on its table name
     *
     * @param networkTableName
     *             Name of a network set table
     * @return Returns a {@link Network} or null if there isn't with the
     *             supplied table name
     * @throws SQLException
     *             throws if the method
     *             {@link GeoPackageCore#getContent(String, ContentFactory, SpatialReferenceSystem)}
     *             throws an SQLException
     */
    public Network getNetwork(final String networkTableName) throws SQLException
    {
        return this.geoPackageCore.getContent(networkTableName,
                                              (tableName,
                                               dataType,
                                               identifier,
                                               description,
                                               lastChange,
                                               minimumX,
                                               minimumY,
                                               maximumX,
                                               maximumY,
                                               spatialReferenceSystemIdentifier) -> new Network(tableName,
                                                                                                identifier,
                                                                                                description,
                                                                                                lastChange,
                                                                                                minimumX,
                                                                                                minimumY,
                                                                                                maximumX,
                                                                                                maximumY,
                                                                                                spatialReferenceSystemIdentifier));
    }

    /**
     * Creates a user defined network table, and adds a corresponding entry to the
     * content table
     *
     * @param tableName
     *            The name of the network table. The table name must begin with a
     *            letter (A..Z, a..z) or an underscore (_) and may only be
     *            followed by letters, underscores, or numbers, and may not
     *            begin with the prefix "gpkg_"
     * @param identifier
     *            A human-readable identifier (e.g. short name) for the
     *            tableName content
     * @param description
     *            A human-readable description for the tableName content
     * @param boundingBox
     *            Bounding box for all content in tableName
     * @param spatialReferenceSystem
     *            Spatial Reference System (SRS)
     * @return Returns a newly created user defined network table
     * @throws SQLException
     *             throws if the method {@link #getNetwork(String) getNetwork}
     *             or the method
     *             {@link DatabaseUtility#tableOrViewExists(Connection, String)
     *             tableOrViewExists} or if the database cannot roll back the
     *             changes after a different exception throws will throw an SQLException
     *
     */
    public Network addNetwork(final String                 tableName,
                              final String                 identifier,
                              final String                 description,
                              final BoundingBox            boundingBox,
                              final SpatialReferenceSystem spatialReferenceSystem) throws SQLException
    {
        DatabaseUtility.validateTableName(tableName);

        if(boundingBox == null)
        {
            throw new IllegalArgumentException("Bounding box cannot be mull.");
        }

        if(DatabaseUtility.tableOrViewExists(this.databaseConnection, tableName))
        {
            throw new IllegalArgumentException("A table already exists with this network's table name");
        }

        final String nodeAttributesTableName = getNodeAttributesTableName(tableName);

        if(DatabaseUtility.tableOrViewExists(this.databaseConnection, nodeAttributesTableName))
        {
            throw new IllegalArgumentException("A table already exists with this node attribute's table name");
        }

        try
        {
            if(!DatabaseUtility.tableOrViewExists(this.databaseConnection, AttributeDescriptionTableName))
            {
                JdbcUtility.update(this.databaseConnection, GeoPackageNetworkExtension.getAttributeDescriptionCreationSql());
            }

            // Create the network table
            JdbcUtility.update(this.databaseConnection, GeoPackageNetworkExtension.getNetworkCreationSql(tableName));

            // Create the network's attributes table
            JdbcUtility.update(this.databaseConnection, getNodeAttributeTableCreationSql(nodeAttributesTableName));

            // Add the network to the content table
            this.geoPackageCore.addContent(tableName,
                                           Network.NetworkContentType,
                                           identifier,
                                           description,
                                           boundingBox,
                                           spatialReferenceSystem);

            this.databaseConnection.commit();

            final Network network = this.getNetwork(tableName);

            this.addExtensionEntry();

            return network;
        }
        catch(final Throwable th)
        {
            this.databaseConnection.rollback();
            throw th;
        }
    }

    /**
     * Get an edge based on its 'from' and 'to' node identifiers
     *
     * @param network
     *             Network table reference
     * @param from
     *             The 'from' node of the edge
     * @param to
     *             The 'to' node of the edge
     * @return a unique id representing entry in the supplied {@link Network},
     *             or an IllegalArgumentException if no matching edge exists
     * @throws SQLException
     *             if there is a database error
     */
    public int getEdge(final Network network, final int from, final int to) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        final String edgeQuery = String.format("Select %s FROM %s WHERE %s = ? AND %s = ? LIMIT 1;",
                                               "id",
                                               network.getTableName(),
                                               "from_node",
                                               "to_node");

        final Integer edgeIdentifier =  JdbcUtility.selectOne(this.databaseConnection,
                                                              edgeQuery,
                                                              preparedStatement -> {
                                                                  preparedStatement.setInt(1, from);
                                                                  preparedStatement.setInt(2, to);
                                                              },
                                                              results -> results.getInt(1));

        if(edgeIdentifier == null)
        {
            throw new IllegalArgumentException("The network contains no edge with the specified start and end node identifiers");
        }

        return edgeIdentifier;
    }

    /**
     * Get an edge based on its identifier
     *
     * @param network
     *             Network table reference
     * @param edgeIdentifier
     *             Unique identifier for a network edge
     * @return an {@link Edge} entry in the supplied {@link Network}, or null
     *             if no matching edge exists
     * @throws SQLException
     *             if there is a database error
     */
    public Edge getEdge(final Network network, final int edgeIdentifier) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        final String edgeQuery = String.format("Select %s, %s FROM %s WHERE %s = ? LIMIT 1;",
                                               "from_node",
                                               "to_node",
                                               network.getTableName(),
                                               "id");

        return JdbcUtility.selectOne(this.databaseConnection,
                                     edgeQuery,
                                     preparedStatement -> preparedStatement.setInt(1, edgeIdentifier),
                                     results -> new Edge(edgeIdentifier,
                                                         results.getInt(1),
                                                         results.getInt(2)));
    }

    /**
     * Get the 'from' nodes that share an edge with a node
     *
     * @param network
     *             Network table reference
     * @param node
     *             'to' node identifier
     * @return a {@link List} of node identifiers
     * @throws SQLException
     *             if there is a database error
     */
    public List<Edge> getEntries(final Network network, final int node) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        final String edgeQuery = String.format("SELECT %s, %s FROM %s WHERE %s = ?;",
                                               "id",
                                               "from_node",
                                               network.getTableName(),
                                               "to_node");

        return JdbcUtility.select(this.databaseConnection,
                                  edgeQuery,
                                  preparedStatement -> preparedStatement.setInt(1, node),
                                  resultSet -> new Edge(resultSet.getInt(1),
                                                        resultSet.getInt(2),
                                                        node));
    }

    /**
     * Get the 'to' nodes that share an edge with a node
     *
     * @param network
     *             Network table reference
     * @param node
     *             'from' node identifier
     * @return a {@link List} of node identifiers
     * @throws SQLException
     *             if there is a database error
     */
    public List<Edge> getExits(final Network network, final int node) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        final String edgeQuery = String.format("SELECT %s, %s FROM %s WHERE %s = ?;",
                                               "id",
                                               "to_node",
                                               network.getTableName(),
                                               "from_node");

        return JdbcUtility.select(this.databaseConnection,
                                  edgeQuery,
                                  preparedStatement -> preparedStatement.setInt(1, node),
                                  resultSet -> new Edge(resultSet.getInt(1),
                                                        node,
                                                        resultSet.getInt(2)));
    }

    /**
     * Iterate through the edges of a {@link Network}, applying a supplied
     * operation
     *
     * @param network
     *             Network table reference
     * @param consumer
     *             Callback applied to each edge
     * @throws SQLException
     *             if there is a database error
     */
    public void visitEdges(final Network network, final Consumer<Edge> consumer) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(consumer == null)
        {
            throw new IllegalArgumentException("Consumer callback may not be null");
        }

        final String edgeQuery = String.format("SELECT %s, %s, %s FROM %s;",
                                               "id",
                                               "from_node",
                                               "to_node",
                                               network.getTableName());

        JdbcUtility.forEach(this.databaseConnection,
                            edgeQuery,
                            null,
                            resultSet -> consumer.accept(new Edge(resultSet.getInt(1),
                                                                  resultSet.getInt(2),
                                                                  resultSet.getInt(3))));
    }

    /**
     * Iterate through the nodes of a {@link Network}, applying a supplied
     * operation
     *
     * @param network
     *             Network table reference
     * @param consumer
     *             Callback applied to each node
     * @param attributeDescriptions
     *             Indicates which attributes to make available to the consumer
     * @throws SQLException
     *             if there is a database error
     */
    public void visitNodes(final Network                          network,
                           final Consumer<AttributedNode>         consumer,
                           final Collection<AttributeDescription> attributeDescriptions) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(consumer == null)
        {
            throw new IllegalArgumentException("Consumer callback may not be null");
        }

        final List<String> columnNames = getColumnNames(AttributedType.Node, attributeDescriptions);

        columnNames.add(0, "node_id");

        final String nodeQuery = String.format("SELECT %s FROM %s;",
                                               String.join(", ", columnNames),
                                               getNodeAttributesTableName(network));

        JdbcUtility.forEach(this.databaseConnection,
                            nodeQuery,
                            null,
                            resultSet -> consumer.accept(new AttributedNode(resultSet.getInt(1),
                                                                            JdbcUtility.getObjects(resultSet, 1, attributeDescriptions.size()))));
    }

    /**
     * Adds an edge to a {@link Network}
     *
     * @param network
     *             Network table reference
     * @param from
     *             'from' node
     * @param to
     *             'to' node
     * @return a unique edge identifier
     * @throws SQLException
     *             if there is a database error
     */
    public int addEdge(final Network network, final int from, final int to) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        final String insert = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                                            network.getTableName(),
                                            "from_node",
                                            "to_node");

        final int identifier = JdbcUtility.update(this.databaseConnection,
                                                  insert,
                                                  preparedStatement -> {
                                                      preparedStatement.setInt(1, from);
                                                      preparedStatement.setInt(2, to);
                                                  },
                                                  resultSet -> resultSet.getInt(1));

        this.databaseConnection.commit();

        return identifier;
    }

    /**
     * Adds edges to a {@link Network}
     *
     * @param network
     *             Network table reference
     * @param edges
     *             Collection of from/to node pairs
     * @throws SQLException
     *             if there is a database error
     */
    public void addEdges(final Network network, final Iterable<Pair<Integer, Integer>> edges) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(edges == null)
        {
            throw new IllegalArgumentException("Edge collection may not be null");
        }

        final String insert = String.format("INSERT INTO %s (%s, %s) VALUES (?, ?)",
                                            network.getTableName(),
                                            "from_node",
                                            "to_node");

        JdbcUtility.update(this.databaseConnection,
                           insert,
                           edges,
                           (preparedStatement, edge) -> { preparedStatement.setInt(1, edge.getLeft());
                                                          preparedStatement.setInt(2, edge.getRight());
                                                        });

        this.databaseConnection.commit();
    }

    /**
     * Adds edges to a {@link Network} along with each edge's attributes
     *
     * @param attributedEdges
     *             Collection of edge/attribute pairs, where the edges are each a pair of nodes
     * @param attributeDescriptions
     *             Collection of {@link AttributeDescription}s
     * @throws SQLException
     *             if there is a database error
     */
    public void addAttributedEdges(final Collection<Pair<Pair<Integer, Integer>, List<Object>>> attributedEdges,
                                   final List<AttributeDescription>                             attributeDescriptions) throws SQLException
    {
        if(attributedEdges == null || attributedEdges.isEmpty())
        {
            throw new IllegalArgumentException("Attributed edges collection may not be null or empty");
        }

        final Pair<String, List<String>> schema = getSchema(AttributedType.Edge, attributeDescriptions); // Checks attribute description collection for null/empty/all referencing the same network table, and attributed type

        final String       networkTableName = schema.getLeft();
        final List<String> columnNames      = schema.getRight();

        final String insert = String.format("INSERT INTO %s (%s, %s, %s) VALUES (?, ?, %s)",
                                            networkTableName,
                                            "from_node",
                                            "to_node",
                                            String.join(", ", columnNames),
                                            String.join(", ", Collections.nCopies(attributeDescriptions.size(), "?")));

        JdbcUtility.update(this.databaseConnection,
                           insert,
                           attributedEdges,
                           (preparedStatement, attributedEdge) -> { final Pair<Integer, Integer> edge   = attributedEdge.getLeft();
                                                                    final List<Object>           values = attributedEdge.getRight();

                                                                    if(values.size() != attributeDescriptions.size())
                                                                    {
                                                                        throw new IllegalArgumentException(String.format("Edge (%d -> %d) has %d values; expected %d",
                                                                                                                         edge.getLeft(),
                                                                                                                         edge.getRight(),
                                                                                                                         values.size(),
                                                                                                                         attributeDescriptions.size()));
                                                                    }

                                                                    int parameterIndex = 1;

                                                                    preparedStatement.setInt(parameterIndex++, edge.getLeft());
                                                                    preparedStatement.setInt(parameterIndex++, edge.getRight());

                                                                    for(final Object value : values)
                                                                    {
                                                                        preparedStatement.setObject(parameterIndex++, value);
                                                                    }
                                                                  });

        this.databaseConnection.commit();
    }

    /**
     * Get's a list of a {@link Network}'s {@link AttributeDescription}s for
     * either its nodes or edges
     *
     * @param network
     *             Network table reference
     * @param attributedType
     *             Indicates whether you want the {@link AttributeDescription}s for an node or a edge
     * @return a collection of {@link AttributeDescription}s
     * @throws SQLException
     *             if there is a database error
     */
    public List<AttributeDescription> getAttributeDescriptions(final Network        network,
                                                               final AttributedType attributedType) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(attributedType == null)
        {
            throw new IllegalArgumentException("Attributed type may not be null");
        }

        final String attributeDescriptionQuery = String.format("SELECT %s, %s, %s, %s, %s FROM %s WHERE %s = ? AND %s = ?;",
                                                               "id",
                                                               "name",
                                                               "units",
                                                               "data_type",
                                                               "description",
                                                               AttributeDescriptionTableName,
                                                               "table_name",
                                                               "attributed_type");

        return JdbcUtility.select(this.databaseConnection,
                                  attributeDescriptionQuery,
                                  preparedStatement -> {
                                      preparedStatement.setString(1, network.getTableName());
                                      preparedStatement.setString(2, attributedType.toString());
                                  },
                                  resultSet -> new AttributeDescription(resultSet.getInt(1),                      // attribute unique identifier
                                                                        network.getTableName(),                   // network table name
                                                                        resultSet.getString(2),                   // attribute name
                                                                        resultSet.getString(3),                   // attribute units
                                                                        DataType.valueOf(resultSet.getString(4)), // attribute data type
                                                                        resultSet.getString(5),                   // attribute description
                                                                        attributedType));                         // attributed type
    }

    /**
     * Get's a {@link Network}'s named {@link AttributeDescription}
     *
     * @param network
     *             Network table reference
     * @param name
     *             Name of the attribute
     * @param attributedType
     *             Indicates whether you want the {@link AttributeDescription}s for an node or a edge
     * @return an {@link AttributeDescription}, or null if none match the
     *             supplied criteria
     * @throws SQLException
     *             if there is a database error
     */
    public AttributeDescription getAttributeDescription(final Network        network,
                                                        final String         name,
                                                        final AttributedType attributedType) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(name == null)
        {
            throw new IllegalArgumentException("Attribute name may not be null");
        }

        if(attributedType == null)
        {
            throw new IllegalArgumentException("Attributed type may not be null");
        }

        final String attributeDescriptionQuery = String.format("SELECT %s, %s, %s, %s FROM %s WHERE %s = ? AND %s = ? AND %s = ? LIMIT 1;",
                                                               "id",
                                                               "units",
                                                               "data_type",
                                                               "description",
                                                               AttributeDescriptionTableName,
                                                               "table_name",
                                                               "attributed_type",
                                                               "name");

        return JdbcUtility.selectOne(this.databaseConnection,
                                     attributeDescriptionQuery,
                                     preparedStatement -> { preparedStatement.setString(1, network.getTableName());
                                                            preparedStatement.setString(2, attributedType.toString());
                                                            preparedStatement.setString(3, name);
                                                          },
                                     resultSet -> new AttributeDescription(resultSet.getInt(1),                      // attribute unique identifier
                                                                           network.getTableName(),                   // network table name
                                                                           name,                                     // attribute name
                                                                           resultSet.getString(2),                   // attribute units
                                                                           DataType.valueOf(resultSet.getString(3)), // attribute data type
                                                                           resultSet.getString(4),                   // attribute description
                                                                           attributedType));                         // attributed type
    }

    /**
     * Adds an attribute description to a {@link Network} for a node or edge
     *
     * @param network
     *             Network table reference
     * @param name
     *             Name of the attribute. The combination of network, name, and
     *             attributed type are unique in a {@link GeoPackage}.
     * @param units
     *             Description of the attribute's value unit
     * @param dataType
     *             Database storage type for the attribute's value
     * @param description
     *             Human readable description of the attribute
     * @param attributedType
     *             Indication of whether this is a description for nodes or
     *             edges. The combination of network, name, and attributed
     *             type are unique in a {@link GeoPackage}.
     * @return handle to the added {@link AttributeDescription}
     * @throws SQLException
     *             if there is a database error
     */
    public AttributeDescription addAttributeDescription(final Network        network,
                                                        final String         name,
                                                        final String         units,
                                                        final DataType       dataType,
                                                        final String         description,
                                                        final AttributedType attributedType) throws SQLException
    {
        if(network == null)
        {
            throw new IllegalArgumentException("Network may not be null");
        }

        if(name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("Name may not be null or empty");
        }

        if(units == null || units.isEmpty())
        {
            throw new IllegalArgumentException("Units may not be null or empty");
        }

        if(dataType == null)
        {
            throw new IllegalArgumentException("Data type may not be null");
        }

        if(description == null || description.isEmpty())
        {
            throw new IllegalArgumentException("Description may not be null or empty");
        }

        if(attributedType == null)
        {
            throw new IllegalArgumentException("Attributed type may not be null");
        }

        final String tableName = attributedType == AttributedType.Edge ? network.getTableName()
                                                                       : getNodeAttributesTableName(network);

        final String insert = String.format("INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)",
                                            AttributeDescriptionTableName,
                                            "table_name",
                                            "name",
                                            "units",
                                            "data_type",
                                            "description",
                                            "attributed_type");

        final int attributeDescriptionIdentifier = JdbcUtility.update(this.databaseConnection,
                                                                      insert,
                                                                      preparedStatement -> {
                                                                          preparedStatement.setString(1, network.getTableName());
                                                                          preparedStatement.setString(2, name);
                                                                          preparedStatement.setString(3, units);
                                                                          preparedStatement.setString(4, dataType.toString());
                                                                          preparedStatement.setString(5, description);
                                                                          preparedStatement.setString(6, attributedType.toString());
                                                                      },
                                                                      keySet -> keySet.getInt(1));

        final String alter = String.format("ALTER TABLE %s ADD COLUMN %s %s DEFAULT NULL;",
                                           tableName,
                                           name,
                                           dataType.toString().toUpperCase(Locale.getDefault()));

        JdbcUtility.update(this.databaseConnection, alter);

        this.databaseConnection.commit();

        return new AttributeDescription(attributeDescriptionIdentifier,
                                        network.getTableName(),
                                        name,
                                        units,
                                        dataType,
                                        description,
                                        attributedType);
    }

    /**
     * Get multiple attribute values from an edge
     *
     * @param edgeIdentifier
     *             Unique edge identifier
     * @param attributeDescriptions
     *             Collection of which attributes should be retrieved
     * @return the edge's attribute values in the same order as the specified attribute descriptions
     * @throws SQLException
     *             if there is a database error
     */
    public List<Object> getEdgeAttributes(final int                        edgeIdentifier,
                                          final List<AttributeDescription> attributeDescriptions) throws SQLException
    {
        final Pair<String, List<String>> schema = getSchema(AttributedType.Edge, attributeDescriptions); // Checks attribute description collection for null/empty/all referencing the same network table, and attributed type

        final String       networkTableName = schema.getLeft();
        final List<String> columnNames      = schema.getRight();

        final String attributeQuery = String.format("SELECT %s FROM %s WHERE %s = ? LIMIT 1",
                                                    String.join(", ", columnNames),
                                                    networkTableName,
                                                    "id");

        final List<Object> attributes =  JdbcUtility.selectOne(this.databaseConnection,
                                                               attributeQuery,
                                                               preparedStatement -> preparedStatement.setInt(1, edgeIdentifier),
                                                               resultSet -> JdbcUtility.getObjects(resultSet, 1, attributeDescriptions.size()));
        if(attributes == null)
        {
            throw new IllegalArgumentException("The given edge is not in the table");
        }

        return attributes;
    }

    /**
     * Gets an object that allows for getting many exit edges by reusing a
     * stored PreparedStatement
     *
     * @param network
     *             Network table reference
     * @param nodeAttributeDescriptions
     *         Collection of attributes that will be retrieved for the edge's
     *         two endpoints
     * @param edgeAttributeDescriptions
     *         Collection of attributes that will be retrieved for the edge
     * @return a {@link NodeExitGetter} that allows for getting many exit edges by
     *         reusing a stored PreparedStatement
     * @throws SQLException
     *         if there is a database error
     */
    public NodeExitGetter getNodeExitGetter(final Network                          network,
                                            final Collection<AttributeDescription> nodeAttributeDescriptions,
                                            final Collection<AttributeDescription> edgeAttributeDescriptions) throws SQLException
    {
        return new NodeExitGetter(this.databaseConnection,
                                  network,
                                  nodeAttributeDescriptions,
                                  edgeAttributeDescriptions);
    }

    /**
     * Get multiple attribute values from multiple nodes
     *
     * @param nodeIdentifier
     *             Unique node identifier
     * @param attributeDescriptions
     *             Collection of which attributes should be retrieved
     * @return The node's attribute values in the same order as the specified attribute descriptions
     * @throws SQLException
     *             if there is a database error
     */
    public List<Object> getNodeAttributes(final int                        nodeIdentifier,
                                          final List<AttributeDescription> attributeDescriptions) throws SQLException
    {
        final Pair<String, List<String>> schema = getSchema(AttributedType.Node, attributeDescriptions); // Checks attribute description collection for null/empty/all referencing the same network table, and attributed type

        final String       networkTableName = schema.getLeft();
        final List<String> columnNames      = schema.getRight();

        final String nodeAttributeQuery = String.format("SELECT %s FROM %s WHERE %s = ? LIMIT 1;",
                                                        String.join(", ", columnNames),
                                                        getNodeAttributesTableName(networkTableName),
                                                        "node_id");

        final List<Object> nodeAttributes = JdbcUtility.selectOne(this.databaseConnection,
                                                                  nodeAttributeQuery,
                                                                  preparedStatement -> preparedStatement.setInt(1, nodeIdentifier),
                                                                  resultSet -> JdbcUtility.getObjects(resultSet, 1, attributeDescriptions.size()));

        if(nodeAttributes == null)
        {
            throw new IllegalArgumentException("The given node is not in the table");
        }

        return nodeAttributes;
    }

    /**
     * Get multiple attribute values from multiple nodes
     *
     * @param nodeIdentifier
     *             Unique node identifier
     * @param attributeDescriptions
     *             Collection of which attributes should be retrieved
     * @return A {@link AttributedNode} object that contains node's attribute values in
     *             the same order as the specified attribute descriptions
     * @throws SQLException
     *             if there is a database error
     */
    public AttributedNode getAttributedNode(final int                        nodeIdentifier,
                                            final List<AttributeDescription> attributeDescriptions) throws SQLException
    {
        return new AttributedNode(nodeIdentifier, this.getNodeAttributes(nodeIdentifier, attributeDescriptions));
    }

    /**
     * Adds attributes to a node
     *
     * @param nodeIdentifier
     *             Unique node identifier
     * @param attributeDescriptions
     *             Collection of which attributes should be set
     * @param values
     *             Values of the attributes in corresponding order to the given
     *             attribute descriptions
     * @throws SQLException
     *             if there is a database error
     */
    public void addNodeAttributes(final int                        nodeIdentifier,
                                  final List<Object>               values,
                                  final List<AttributeDescription> attributeDescriptions) throws SQLException
    {
        this.addNodeAttributes(Arrays.asList(Pair.of(nodeIdentifier,
                                                     values)),
                               attributeDescriptions);
    }

    /**
     * Adds attributes to a node
     *
     * @param nodes
     *             Collection of identifier and attribute pairs
     * @throws SQLException
     *             if there is a database error
     */
    public void addNodeAttributes(final Iterable<Pair<Integer, List<Object>>> nodes,
                                  final List<AttributeDescription>            attributeDescriptions) throws SQLException
    {
        for(final Pair<Integer, List<Object>> nodePair : nodes)
        {
            final int nodeIdentifier = nodePair.getLeft();

            final List<Object> values = nodePair.getRight();

            if(values == null)
            {
                throw new IllegalArgumentException("Values list may not be null");
            }

            final Pair<String, List<String>> schema = getSchema(AttributedType.Node, attributeDescriptions); // Checks attribute description collection for null/empty/all referencing the same network table, and attributed type

            if(values.size() != attributeDescriptions.size())
            {
                throw new IllegalArgumentException("The size of the attribute description list must match the size of the values list");
            }

            final String       networkTableName = schema.getLeft();
            final List<String> columnNames      = schema.getRight();

            final String update = String.format("UPDATE %s SET %s WHERE %s = ?",
                                                getNodeAttributesTableName(networkTableName),
                                                String.join(", ", columnNames.stream().map(name -> name + " = ?").collect(Collectors.toList())),
                                                "node_id");

            final int size = values.size(); // Same as attributeDescriptions.size()

            JdbcUtility.update(this.databaseConnection,
                               update,
                               preparedStatement -> { int valueIndex = 0;

                                                      for(final AttributeDescription attributeDescription : attributeDescriptions)
                                                      {
                                                          final Object value = values.get(valueIndex);

                                                          if(!attributeDescription.dataTypeAgrees(value))
                                                          {
                                                              throw new IllegalArgumentException("Value does not match the data type specified by the attribute description");
                                                          }

                                                          preparedStatement.setObject(valueIndex+1, value);
                                                          ++valueIndex;
                                                      }

                                                      preparedStatement.setInt(size+1, nodeIdentifier);
                                                    });
        }

        this.databaseConnection.commit();
    }

    /**
     * Adds a collection of nodes with their attributes
     *
     * @param nodeAttributePairs
     *             Collection of node-attribute pairs
     * @param attributeDescriptions
     *             Collection of which attributes should be set
     * @throws SQLException
     *             if there is a database error
     */
    public void addNodes(final Iterable<Pair<Integer, List<Object>>> nodeAttributePairs,
                         final List<AttributeDescription>            attributeDescriptions) throws SQLException
    {
        if(attributeDescriptions == null)
        {
            throw new IllegalArgumentException("Attribute descriptions list may not be null");
        }

        if(nodeAttributePairs == null)
        {
            throw new IllegalArgumentException("Collection of node-attribute may not be null");
        }

        final Pair<String, List<String>> schema = getSchema(AttributedType.Node, attributeDescriptions); // Checks attribute description collection for null/empty/all referencing the same network table, and attributed type

        final String       networkTableName = schema.getLeft();
        final List<String> columnNames      = schema.getRight();

        columnNames.add(0, "node_id");

        final String insert = String.format("INSERT INTO %s (%s) VALUES (%s)",
                                            getNodeAttributesTableName(networkTableName),
                                            String.join(", ", columnNames),
                                            String.join(", ", Collections.nCopies(columnNames.size(), "?")));

        JdbcUtility.update(this.databaseConnection,
                           insert,
                           nodeAttributePairs,
                           (preparedStatement, nodeAttributePair) -> { final int          nodeIdentifier = nodeAttributePair.getLeft();
                                                                       final List<Object> values         = nodeAttributePair.getRight();

                                                                       if(values == null)
                                                                       {
                                                                           throw new IllegalArgumentException("Values list may not be null");
                                                                       }

                                                                       if(values.size() != columnNames.size()-1) // We subtract 1 because "node_id" was added above...
                                                                       {
                                                                           throw new IllegalArgumentException("The size of the column name list must match the size of the values list");
                                                                       }

                                                                       int argumentIndex = 1;

                                                                       preparedStatement.setInt(argumentIndex++, nodeIdentifier);

                                                                       for(final Object value : values)
                                                                       {
                                                                           preparedStatement.setObject(argumentIndex++, value);
                                                                       }
                                                                      });

        this.databaseConnection.commit();
    }

    private static Pair<String, List<String>> getSchema(final AttributedType             attributedType,
                                                        final List<AttributeDescription> attributeDescriptions)
    {
        if(attributeDescriptions == null || attributeDescriptions.isEmpty())
        {
            throw new IllegalArgumentException("Collection of attribute descriptions may not be null or empty");
        }

        if(attributedType == null)
        {
            throw new IllegalArgumentException("Attributed type may not be null");
        }

        final String firstNetworkTableName = attributeDescriptions.get(0).getNetworkTableName();

        return Pair.of(firstNetworkTableName,
                       attributeDescriptions.stream()
                               .map(description -> {
                                   if(!description.getNetworkTableName().equals(firstNetworkTableName))
                                   {
                                       throw new IllegalArgumentException("Attribute descriptions must all refer to the same network table");
                                   }

                                   if(description.getAttributedType() != attributedType)
                                   {
                                       throw new IllegalArgumentException("Attribute descriptions must all refer exclusively to nodes or exclusively to edges");
                                   }

                                   return description.getName();
                               })
                               .collect(Collectors.toList()));
    }

    private static List<String> getColumnNames(final AttributedType                   attributedType,
                                               final Collection<AttributeDescription> attributeDescriptions)
    {
        if(attributedType == null)
        {
            throw new IllegalArgumentException("Attributed type may not be null");
        }

        if(attributeDescriptions == null || attributeDescriptions.isEmpty())
        {
            return Collections.emptyList();
        }

        final String firstNetworkTableName = attributeDescriptions.iterator().next().getNetworkTableName();

        return attributeDescriptions.stream()
                                    .map(description -> {
                                        if(!description.getNetworkTableName().equals(firstNetworkTableName))
                                        {
                                            throw new IllegalArgumentException("Attribute descriptions must all refer to the same network table");
                                        }

                                        if(description.getAttributedType() != attributedType)
                                        {
                                            throw new IllegalArgumentException("Attribute descriptions must all refer exclusively to nodes or exclusively to edges");
                                        }

                                        return description.getName();
                                    })
                                    .collect(Collectors.toList());
    }

    private static String getNetworkCreationSql(final String networkTableName)
    {
        return "CREATE TABLE " + networkTableName + '\n' +
               "(id        INTEGER PRIMARY KEY AUTOINCREMENT, -- Autoincrement primary key\n" +
               " from_node INTEGER NOT NULL,                  -- Starting point of an edge\n" +
               " to_node   INTEGER NOT NULL,                  -- End of an edge\n"            +
               " UNIQUE (from_node, to_node));";
    }

    private static String getAttributeDescriptionCreationSql()
    {
        return "CREATE TABLE " + AttributeDescriptionTableName + '\n' +
               "(id              INTEGER PRIMARY KEY AUTOINCREMENT, -- Autoincrement primary key\n"            +
               " table_name      TEXT NOT NULL,                     -- Name of network table\n"                +
               " name            TEXT NOT NULL,                     -- Name of attribute\n"                    +
               " units           TEXT NOT NULL,                     -- Attribute value's units\n"              +
               " data_type       TEXT NOT NULL,                     -- Data type of attribute\n"               +
               " description     TEXT NOT NULL,                     -- Attribute description\n"                +
               " attributed_type TEXT NOT NULL,                     -- Target attribute type (edge or node)\n" +
               " UNIQUE (table_name, name, attributed_type),"                                                  +
               " CONSTRAINT fk_natd_table_name FOREIGN KEY (table_name) REFERENCES gpkg_contents(table_name));";
    }

    private static String getNodeAttributeTableCreationSql(final String nodeAttributeTableName)
    {
        return "CREATE TABLE " + nodeAttributeTableName + '\n'      +
               "(node_id INTEGER PRIMARY KEY, -- Node identifier\n" +
               " UNIQUE (node_id));";   // An index wasn't being automatically created for "node_id"
    }

    private static final String ExtensionName             = "SWAGD_network";
    private static final String NodeAttributesTableSuffix = "_node_attributes";

    /**
     * Name of the singular table describing attributes for network tables
     */
    public static final String AttributeDescriptionTableName = "network_attribute_description";
}
