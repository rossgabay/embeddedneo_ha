package com.rgabay.embedded_ha;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.beust.jcommander.JCommander;
import com.rgabay.embedded_ha.util.JCommanderSetup;
import org.neo4j.graphdb.*;

import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.jmx.JmxUtils;

import javax.management.ObjectName;

import org.neo4j.kernel.ha.HaSettings;

import org.neo4j.cluster.ClusterSettings;


public class Driver
{
    private static final String DB_PATH = "neo4j-embedded-db";
    private static final String CONFIG_FILE = "/Users/rossgabay/neo/ha_instance/neo4j1.conf";

    private GraphDatabaseService graphDb;
    private static String configFile;


    private enum RelTypes implements RelationshipType
    {
        HAS_FLIGHTS_TO
    }

    public static void main( final String[] args ) throws IOException
    {

        JCommanderSetup jcommanderSetup = new JCommanderSetup();
        new JCommander(jcommanderSetup, args);

        configFile = (jcommanderSetup.getConfigFile() == null) ?  CONFIG_FILE : jcommanderSetup.getConfigFile();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable command1 = new InstanceLaunchCommand();
        executor.execute(command1);
    }

    void createDb() throws IOException
    {
        FileUtils.deleteRecursively( new File(DB_PATH) );

        System.out.printf("launching db instance using config: %s \n", configFile);

        synchronized (this) {

            // NOTE: this example loads config props from the file.
            // To set configs directly on the graph database object use a setConfig() call on the builder with Settings from
            // GraphDatabaseSettings, ClusterSettings, HaSettings
            // For example: .setConfig(GraphDatabaseSettings.logs_directory, "/logs")

            graphDb = new HighlyAvailableGraphDatabaseFactory()
                    .newEmbeddedDatabaseBuilder(new File(DB_PATH))
                    .loadPropertiesFromFile(configFile)
                    .newGraphDatabase();


            ObjectName objectName = JmxUtils.getObjectName(graphDb, "High Availability");
            String role = JmxUtils.getAttribute( objectName, "Role" );
            System.out.printf("JMX says, my role is %s\n", role);
        }

        registerShutdownHook( graphDb );

    }

    void loadData(){

        Node firstNode;
        Node secondNode;
        Relationship relationship;
        Label label;

        IndexDefinition indexDefinition;

        try ( Transaction tx = graphDb.beginTx() )
        {
            Schema schema = graphDb.schema();
            indexDefinition = schema.indexFor( Label.label( "Airport" ) )
                    .on( "code" )
                    .create();
            tx.success();

        }

        try ( Transaction tx = graphDb.beginTx() )
        {

            label = Label.label( "Airport" );

            firstNode = graphDb.createNode(label);
            firstNode.setProperty( "code", "DTW" );
            secondNode = graphDb.createNode(label);
            secondNode.setProperty( "code", "SFO" );

            relationship = firstNode.createRelationshipTo( secondNode, RelTypes.HAS_FLIGHTS_TO);
            relationship.setProperty( "class", "business" );

            System.out.printf("%s %s %s %s\n",
                    firstNode.getProperty( "code" ),
                    relationship.getType(),
                    secondNode.getProperty( "code" ),
                    relationship.getProperty( "class" ));

            tx.success();
        }

        try ( Transaction tx = graphDb.beginTx() )
        {
            Schema schema = graphDb.schema();
            System.out.println( String.format( "index completion: %1.0f%%",
                    schema.getIndexPopulationProgress( indexDefinition ).getCompletedPercentage() ) );
            tx.success();
        }

        try ( Transaction tx = graphDb.beginTx() )
        {
            Node foundNode =    graphDb.findNode(label, "code", "DTW");
            System.out.printf("Sanity check, what's the code property value for DTW? -> %s\n ", foundNode.getProperty("code"));

            tx.success();
        }
    }

    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );

        graphDb.shutdown();

    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }

}
