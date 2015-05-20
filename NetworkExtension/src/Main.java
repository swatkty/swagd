import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.rgi.common.BoundingBox;
import com.rgi.common.Pair;
import com.rgi.common.coordinate.Coordinate;
import com.rgi.geopackage.GeoPackage;
import com.rgi.geopackage.GeoPackage.OpenMode;
import com.rgi.geopackage.extensions.implementation.BadImplementationException;
import com.rgi.geopackage.extensions.network.AttributeDescription;
import com.rgi.geopackage.extensions.network.AttributedType;
import com.rgi.geopackage.extensions.network.DataType;
import com.rgi.geopackage.extensions.network.Edge;
import com.rgi.geopackage.extensions.network.GeoPackageNetworkExtension;
import com.rgi.geopackage.extensions.network.Network;
import com.rgi.geopackage.verification.ConformanceException;
import com.rgi.geopackage.verification.VerificationLevel;


public class Main
{
    private static final File file = new File("test.gpkg");

    public static void main(final String[] args)
    {
        //createGpkg();
        runRoute();
    }

    private static void runRoute()
    {
        try(final GeoPackage gpkg = new GeoPackage(file, OpenMode.Open))
        {
            final GeoPackageNetworkExtension networkExtension = gpkg.extensions().getExtensionImplementation(GeoPackageNetworkExtension.class);

            final Network network = networkExtension.getNetwork("mynetwork");

            final AttributeDescription distanceAttribute = networkExtension.getAttributeDescription(network,
                                                                                                    "distance",
                                                                                                    AttributedType.Edge);

            final AttributeDescription nodeLatitudeAttibute = networkExtension.getAttributeDescription(network,
                                                                                                       "latitude",
                                                                                                       AttributedType.Node);

            final AttributeDescription nodeLongitudeAttibute = networkExtension.getAttributeDescription(network,
                                                                                                        "longitude",
                                                                                                        AttributedType.Node);

            final int startNode = 9036;
            final int endNode   = 37236;

            if(networkExtension.getEntries(network, endNode).size() > 0)
            {
                final long startTime = System.nanoTime();

                List<Integer> path = dijkstra(networkExtension,
                         network,
                         startNode,
                         endNode,
                         (edge) -> { try
                                     {
                                         return networkExtension.getAttribute(edge.getIdentifier(), distanceAttribute);
                                     }
                                     catch(final Exception ex)
                                     {
                                         throw new RuntimeException(ex);
                                     }
                                   });
                path.forEach(node -> System.out.print(node + ", "));

                int firstNode = path.get(0);
                int secondNode = path.get(1);
                double totalWeight = 0.0;
                System.out.printf("\n(%d) -", firstNode);
                for(int i = 2; i < path.size(); i++)
                {
                    Edge edge1 = networkExtension.getEdge(network, firstNode, secondNode);
                    double cost = networkExtension.getAttribute(edge1.getIdentifier(), distanceAttribute);
                    System.out.printf("%f->(%d)-", cost, secondNode);
                    totalWeight = totalWeight + (double)networkExtension.getAttribute(edge1.getIdentifier(), distanceAttribute);
                    if(i < path.size())
                    {
                        firstNode = secondNode;
                        secondNode = path.get(i);
                    }
                }
                System.out.println(String.format("\nDijkstra total distance = %f", totalWeight));
                System.out.println(String.format("\nDijkstra took %.2f seconds to calculate.", (System.nanoTime() - startTime)/1.0e9));
            }

            if(networkExtension.getEntries(network, endNode).size() > 0)
            {
                final long startTime = System.nanoTime();

                List<Integer> path = astar(networkExtension,
                      network,
                      startNode,
                      endNode,
                      (startIdentifier, endIdentifier) -> { try
                                                            {
                                                                Coordinate<Double> startCoordinate = new Coordinate<>(networkExtension.getAttribute(startIdentifier, nodeLongitudeAttibute),
                                                                                                                      networkExtension.getAttribute(startIdentifier, nodeLatitudeAttibute));

                                                                Coordinate<Double> endCoordinate   = new Coordinate<>(networkExtension.getAttribute(endIdentifier,   nodeLongitudeAttibute),
                                                                                                                      networkExtension.getAttribute(endIdentifier,   nodeLatitudeAttibute));
                                                                return getDistance(startCoordinate, endCoordinate);
                                                            }
                                                            catch(final Exception ex)
                                                            {
                                                                throw new RuntimeException(ex);
                                                            }
                                                          });

                path.forEach(node -> System.out.print(node + ", "));

                int firstNode = path.get(0);
                int secondNode = path.get(1);
                double totalWeight = 0.0;
                System.out.printf("\n(%d) -", firstNode);
                for(int i = 2; i < path.size(); i++)
                {
                    Edge edge1 = networkExtension.getEdge(network, firstNode, secondNode);
                    double cost = networkExtension.getAttribute(edge1.getIdentifier(), distanceAttribute);
                    System.out.printf("%f->(%d)-", cost, secondNode);
                    totalWeight = totalWeight + (double)networkExtension.getAttribute(edge1.getIdentifier(), distanceAttribute);
                    if(i < path.size())
                    {
                        firstNode = secondNode;
                        secondNode = path.get(i);
                    }
                }
                System.out.println(String.format("\nAstar total distance = %f", totalWeight));

                System.out.println(String.format("\nAstar took %.2f seconds to calculate.", (System.nanoTime() - startTime)/1.0e9));
            }

            final int a = 2;
        }
        catch(final ClassNotFoundException | SQLException | ConformanceException | IOException | BadImplementationException ex)
        {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    private static Double getDistance(final Coordinate<Double>  startCoordinate,
                                      final Coordinate<Double>  endCoordinate)
    {
        double latitude  = startCoordinate.getY() - endCoordinate.getY();
        double longitude = startCoordinate.getX() - endCoordinate.getX();

        return Math.sqrt(latitude*latitude + longitude*longitude);
    }

    private static void createGpkg()
    {
        if(file.exists())
        {
            file.delete();
        }

        try(final GeoPackage gpkg = new GeoPackage(file, VerificationLevel.None, OpenMode.Create))
        {

            final GeoPackageNetworkExtension networkExtension = gpkg.extensions().getExtensionImplementation(GeoPackageNetworkExtension.class);

            final Network myNetwork = networkExtension.addNetwork("mynetwork",
                                                                  "Super Important Routing Stuff",
                                                                  "routing stuff. super important",
                                                                  new BoundingBox(0, 0, 0, 0),
                                                                  gpkg.core().getSpatialReferenceSystem(-1));

            final AttributeDescription longitudeAttribute = networkExtension.addAttributeDescription(myNetwork,
                                                                                                     "longitude",
                                                                                                     "degrees",
                                                                                                     DataType.Real,
                                                                                                     "longitude",
                                                                                                     AttributedType.Node);

            final AttributeDescription latitudeAttribute = networkExtension.addAttributeDescription(myNetwork,
                                                                                                    "latitude",
                                                                                                    "degrees",
                                                                                                    DataType.Real,
                                                                                                    "latitude",
                                                                                                    AttributedType.Node);

            final AttributeDescription distanceAttribute = networkExtension.addAttributeDescription(myNetwork,
                                                                                                    "distance",
                                                                                                    "degrees",
                                                                                                    DataType.Real,
                                                                                                    "distance",
                                                                                                    AttributedType.Edge);

            loadNodeAttributes(networkExtension,
                               new File("D:/contour.1/contour.1.node"),
                               myNetwork,
                               Arrays.asList(longitudeAttribute, latitudeAttribute));

            loadEdges(networkExtension,
                      new File("D:/contour.1/contour.1.edge"),
                      myNetwork);

            calculateDistanceCost(networkExtension,
                                  myNetwork,
                                  distanceAttribute,
                                  longitudeAttribute,
                                  latitudeAttribute);
        }
        catch(final ClassNotFoundException | SQLException | ConformanceException | IOException | BadImplementationException ex)
        {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
    }

    private static class VertexAstar
    {
        public VertexAstar previous;     // Parent node

        public final int nodeIdentifier;

        public double distanceFromStart = Double.MAX_VALUE;
        public double distanceFromEnd   = Double.MAX_VALUE;

        public VertexAstar(final int nodeIdentifier)
        {
            this.nodeIdentifier    = nodeIdentifier;
            this.previous          = null;
            this.distanceFromStart = 0.0;
            this.distanceFromEnd   = 0.0;
        }

        public VertexAstar(final int nodeIdentifier, final VertexAstar previous)
        {
            this.nodeIdentifier = nodeIdentifier;
            this.previous       = previous;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if(obj == this)
            {
                return true;
            }

            if(obj == null || this.getClass() != obj.getClass())
            {
                return false;
            }

            return this.nodeIdentifier == ((VertexAstar)obj).nodeIdentifier; // Is this enough? Node identifiers should be unique, right?
        }

        @Override
        public String toString()
        {
            return String.format("%d (%f, %f, %d)",
                                 this.nodeIdentifier,
                                 this.distanceFromStart,
                                 this.distanceFromEnd,
                                 this.previous.nodeIdentifier);
        }

        @Override
        public int hashCode()
        {

            return this.nodeIdentifier;
        }
    }

    private static class Vertex
    {
        public Vertex previous;     // Parent node

        public final int nodeIdentifier;

        public double minimumCost = Double.MAX_VALUE;

        public Vertex(final int nodeIdentifier)
        {
            this.nodeIdentifier = nodeIdentifier;
            this.previous       = null;
            this.minimumCost    = 0.0;
        }

        public Vertex(final int nodeIdentifier, final Vertex previous)
        {
            this.nodeIdentifier = nodeIdentifier;
            this.previous       = previous;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if(obj == this)
            {
                return true;
            }

            if(obj == null || this.getClass() != obj.getClass())
            {
                return false;
            }

            return this.nodeIdentifier == ((Vertex)obj).nodeIdentifier; // Is this enough? Node identifiers should be unique, right?
        }

        @Override
        public String toString()
        {
            return String.format("%d (%f, %d)",
                                 this.nodeIdentifier,
                                 this.minimumCost,
                                 this.previous.nodeIdentifier);
        }
    }

    /**
     * @param networkExtension e
     * @param network e
     * @param start e
     * @param end e
     * @param heuristics e
     * @return e
     * @throws SQLException e
     */
    public static List<Integer> astar(final GeoPackageNetworkExtension           networkExtension,
                                      final Network                              network,
                                      final Integer                              start,
                                      final Integer                              end,
                                      final BiFunction<Integer, Integer, Double> heuristics) throws SQLException
    {
        //changed comparator -> change back to distance from end
        PriorityQueue<VertexAstar> openList   = new PriorityQueue<>((o1, o2) ->{
                                                                                    return Double.compare((o1.distanceFromEnd + o1.distanceFromStart), (o2.distanceFromEnd + o2.distanceFromStart));
                                                                               });
        HashSet<VertexAstar>       closedList = new HashSet<>();

        final Map<Integer, VertexAstar> nodeMap = new HashMap<>();

        //initialize starting Vertex
        VertexAstar startVertex = new VertexAstar(start);
        startVertex.distanceFromEnd = heuristics.apply(start, end);
        openList.add(startVertex);
        nodeMap.put(start, startVertex);

        while(!openList.isEmpty())
        {
            VertexAstar currentVertex = openList.poll();//get the Vertex closest to the end

            //if current vertex is the target then we are done
            if(currentVertex.nodeIdentifier == end)
            {
                return getAstarPath( end, nodeMap);
            }

            closedList.add(currentVertex); //put it in "done" pile

            //for each reachable Vertex
            for(Edge edge: networkExtension.getExits(network, currentVertex.nodeIdentifier))
            {
                final int adjacentNode = edge.getTo();

                VertexAstar reachableVertex = nodeMap.get(adjacentNode);

                if(reachableVertex == null)
                {
                    reachableVertex = new VertexAstar(adjacentNode);
                    reachableVertex.distanceFromEnd = Double.MAX_VALUE;
                    reachableVertex.distanceFromStart = Double.MAX_VALUE;
                    nodeMap.put(adjacentNode, reachableVertex);
                }

                //calculate a tentative distance (see if this is better than what you already may have)
                double distanceFromStart = currentVertex.distanceFromStart + heuristics.apply(currentVertex.nodeIdentifier, reachableVertex.nodeIdentifier);
                double distanceFromEnd   = heuristics.apply(reachableVertex.nodeIdentifier, end);
                //if the closed list already searched this vertex, skip it
                if(closedList.contains(reachableVertex))
                {
                    continue;
                }
                if(!openList.contains(reachableVertex))//if we dont have it, add it
                {
                    reachableVertex.distanceFromStart = distanceFromStart;
                    reachableVertex.distanceFromEnd   = distanceFromEnd;
                    reachableVertex.previous = currentVertex;
                    openList.add(reachableVertex);

                }
                //if this is better then update the values and parent Vertex (previous)
                else if(distanceFromStart < reachableVertex.distanceFromStart)
                {
                    reachableVertex.distanceFromStart = distanceFromStart;
                    reachableVertex.distanceFromEnd   = distanceFromEnd;
                    reachableVertex.previous          = currentVertex;
                    openList.remove(reachableVertex);
                    openList.add(reachableVertex);
                }
            }
        }
        throw new RuntimeException("Didnt find correct path");
       //return getAstarPath(end, nodeMap);//it shouldnt reach here...throw exception?
    }

    private static List<Integer> getAstarPath(final Integer end,
                                              final  Map<Integer, VertexAstar> nodeMap)
    {
        final LinkedList<Integer> path = new LinkedList<>();

        for(VertexAstar backTrackVertex = nodeMap.get(end); backTrackVertex != null; backTrackVertex = backTrackVertex.previous)
        {
            path.addLast(backTrackVertex.nodeIdentifier);
        }

        return path;
    }

    /**
     * Based on http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm
     * This algorithm will find the shortest path from the starting node to the ending node.
     *
     * @param start starting vertex
     * @param end   ending vertex
     * @param edgeCostEvaluator
     * @throws SQLException
     */
    public static List<Integer> dijkstra(final GeoPackageNetworkExtension networkExtension,
                                         final Network                    network,
                                         final Integer                    start,
                                         final Integer                    end,
                                         final Function<Edge, Double>     edgeEvaluator) throws SQLException
    {
        final Map<Integer, Vertex> nodeMap = new HashMap<>();

        final PriorityQueue<Vertex> vertexQueue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.minimumCost, o2.minimumCost));

        final Vertex startVertex = new Vertex(start);

        vertexQueue.add(startVertex);

        while(!vertexQueue.isEmpty())
        {
            final Vertex currentVertex = vertexQueue.poll(); // Gets the vertex with min cost/distance

            networkExtension.getExits(network, currentVertex.nodeIdentifier)
                            .forEach(edge -> { final int adjacentNode = edge.getTo();

                                               Vertex reachableVertex = nodeMap.get(adjacentNode);

                                               if(reachableVertex == null)
                                               {
                                                   reachableVertex = new Vertex(adjacentNode, currentVertex);
                                                   nodeMap.put(adjacentNode, reachableVertex);
                                               }

                                               final double cost = currentVertex.minimumCost + edgeEvaluator.apply(edge); // Get the distance between nodes

                                               if(cost < reachableVertex.minimumCost) // If the cost is unset or less, change the cost
                                               {
                                                   vertexQueue.remove(reachableVertex);         // Remove it from the queue if it is there
                                                   reachableVertex.minimumCost = cost;          // Adjust to new values
                                                   reachableVertex.previous    = currentVertex; // Adjust the path
                                                   vertexQueue.add(reachableVertex);            // Add it to the queue in the correct order
                                               }
                                             });
        }

        final LinkedList<Integer> path = new LinkedList<>();

        for(Vertex backTrackVertex = nodeMap.get(end); backTrackVertex != null; backTrackVertex = backTrackVertex.previous)
        {
            path.addLast(backTrackVertex.nodeIdentifier);
        }

        return path;
    }

    private static void calculateDistanceCost(final GeoPackageNetworkExtension networkExtension,
                                              final Network                    myNetwork,
                                              final AttributeDescription       distanceDescription,
                                              final AttributeDescription       longitudeDescription,
                                              final AttributeDescription       latitudeDescription) throws SQLException
    {
        networkExtension.visitEdges(myNetwork,
                                    edge -> { try
                                              {
                                                  final double fromLongitude = networkExtension.getAttribute(edge.getFrom(), longitudeDescription);
                                                  final double fromLatitude  = networkExtension.getAttribute(edge.getFrom(), latitudeDescription);

                                                  final double toLongitude   = networkExtension.getAttribute(edge.getTo(), longitudeDescription);
                                                  final double toLatitude    = networkExtension.getAttribute(edge.getTo(), latitudeDescription);

                                                  final double longitude = toLongitude - fromLongitude;
                                                  final double latitude  = toLatitude  - fromLatitude;

                                                  final double distance = Math.sqrt((longitude*longitude) + (latitude*latitude));

                                                  networkExtension.addAttributes(edge.getIdentifier(),
                                                                                 Arrays.asList(distanceDescription),
                                                                                 Arrays.asList(distance));
                                              }
                                              catch(final Exception ex)
                                              {
                                                  // TODO Auto-generated catch block
                                                  ex.printStackTrace();
                                              }
                                            });
    }


    /**
     * Puts a file in the Triangle utility node format
     * (https://www.cs.cmu.edu/~quake/triangle.node.html) into a network
     */
    private static void loadNodeAttributes(final GeoPackageNetworkExtension networkExtension,
                                           final File triangleFormatNodes,
                                           final Network network,
                                           final List<AttributeDescription> attributeDescriptions) throws SQLException, IOException
    {
        final Function<String, Pair<Integer, List<Object>>> lineToPair = line -> { final String[] pieces = line.trim().split("\\s+");

                                                                                   return new Pair<>(Integer.valueOf(pieces[0]),                        // vertex # (node id)
                                                                                                     Arrays.asList((Object)Double.valueOf(pieces[1]),   // x (longitude)
                                                                                                                   (Object)Double.valueOf(pieces[2]))); // y (latitude)
                                                                                 };

        try(Stream<Pair<Integer, List<Object>>> pairs = Files.lines(triangleFormatNodes.toPath())
                                                                                       .skip(1) // the first line is a header
                                                                                       .filter(line -> !line.startsWith("#"))
                                                                                       .map(lineToPair))
        {
            networkExtension.addAttributes(attributeDescriptions, pairs::iterator);
        }
    }

    private static void loadEdges(final GeoPackageNetworkExtension networkExtension, final File triangleFormatEdges, final Network network) throws SQLException, IOException
    {
        final Function<String, Pair<Integer, Integer>> lineToPair = line -> { final String[] pieces = line.trim().split("\\s+");

                                                                              // Integer.valueOf(pieces[0]),                 // edge # (edge id), unused, we use our own id, but it should be the same in most cases

                                                                              return new Pair<>(Integer.valueOf(pieces[1]),  // from node
                                                                                                Integer.valueOf(pieces[2])); // to node
                                                                            };

        try(final Stream<Pair<Integer, Integer>> pairs = Files.lines(triangleFormatEdges.toPath())
                                                                                        .skip(1) // the first line is a header
                                                                                        .filter(line -> !line.startsWith("#"))
                                                                                        .map(lineToPair))
        {
            networkExtension.addEdges(network, pairs::iterator);
        }

        // Now add the links in reverse (i.e., we've added one direction, A->B, now add B->A, since the original data had no directionality
        final Function<String, Pair<Integer, Integer>> lineToPair2 = line -> { final String[] pieces = line.trim().split("\\s+");

                                                                               return new Pair<>(Integer.valueOf(pieces[2]),  // from node
                                                                                                 Integer.valueOf(pieces[1])); // to node
                                                                            };

        try(final Stream<Pair<Integer, Integer>> pairs = Files.lines(triangleFormatEdges.toPath())
                                                                                        .skip(1) // the first line is a header
                                                                                        .filter(line -> !line.startsWith("#"))
                                                                                        .map(lineToPair2))
        {
            networkExtension.addEdges(network, pairs::iterator);
        }
    }

//    private String firstLine(final File file) throws IOException
//    {
//        try(Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.US_ASCII))
//        {
//            return lines.findFirst().orElseThrow(() -> new IOException("Empty file"));
//        }
//    }
}
