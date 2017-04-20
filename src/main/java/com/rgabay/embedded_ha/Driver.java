package com.rgabay.embedded_ha;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;

import org.neo4j.cluster.ClusterSettings;
import org.neo4j.kernel.ha.HaSettings;


public class Driver
{
    private static final String DB_PATH = "target/neo4j-embedded-db";
    private static final int SERVER_ID_1_VALUE = 1;
    private static final int SERVER_ID_2_VALUE = 2;
    private static final int BASE_BOLT_PORT = 7687;
    private static final int BASE_CLUSTER_PORT = 5000;
    private static final String INITIAL_HOSTS = "localhost:5001, localhost:5002";

    private GraphDatabaseService graphDb;

    public static void main( final String[] args ) throws IOException
    {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable command1 = new InstanceLaunchCommand(SERVER_ID_1_VALUE);
        Runnable command2 = new InstanceLaunchCommand(SERVER_ID_2_VALUE);

        executor.execute(command1);
        executor.execute(command2);
    }

    void createDb(int instanceId) throws IOException
    {
        FileUtils.deleteRecursively( new File(DB_PATH + instanceId) );

        int boltPort = BASE_BOLT_PORT + instanceId;
        int clusterServer = BASE_CLUSTER_PORT + instanceId;

        System.out.printf("launching db instance, boltPort = %d and instance id = %d\n", boltPort, instanceId);
        //graph db with bolt enabled on port 7687 + instanceId
        synchronized (this) {
            GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector( "" + (instanceId - 1) );

            graphDb = new HighlyAvailableGraphDatabaseFactory()
                    .newEmbeddedDatabaseBuilder(new File(DB_PATH + instanceId))
                    .setConfig(ClusterSettings.server_id, "" + instanceId) // 1 or 2
                    .setConfig(ClusterSettings.cluster_server, "localhost:" + clusterServer)
                    .setConfig(ClusterSettings.cluster_server, "localhost:" + clusterServer)
                    .setConfig(ClusterSettings.initial_hosts, INITIAL_HOSTS) //localhost:5001 and 5002
                    .setConfig(HaSettings.pull_interval, "1") //localhost:5001 and 5002
                    .setConfig(bolt.type, "BOLT")
                    .setConfig(bolt.enabled, "true")
                    .setConfig(bolt.listen_address, "localhost:" + boltPort)
                    .newGraphDatabase();
        }

        registerShutdownHook( graphDb );
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
