package com.esri;

import com.esri.shp.ShpReader;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import com.hazelcast.mapreduce.Job;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.KeyValueSource;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 */
public class Main
{

    public static final String GEOMAP = "geomap";
    public static final String HEXMAP = "hexmap";

    public static void main(final String[] args) throws ExecutionException, InterruptedException, IOException
    {
        final HazelcastInstance instance = createInstanceClient();
        try
        {
            if (args.length == 2 && "points".equalsIgnoreCase(args[0]))
            {
                doPoints(instance, args[1]);
            }
            if (args.length == 2 && "polygons".equalsIgnoreCase(args[0]))
            {
                doPolygons(instance, args[1]);
            }
            else if (args.length == 4)
            {
                doSearch(instance, args[0], args[1], args[2], args[3]);
            }
            else if (args.length == 2 && "mr".equalsIgnoreCase(args[0]))
            {
                doMapReduce(instance, args[1]);
            }
        }
        finally
        {
            instance.shutdown();
        }
    }

    private static void doSearch(
            final HazelcastInstance instance,
            final String xminText,
            final String yminText,
            final String xmaxText,
            final String ymaxText)
    {
        final double xmin = Double.parseDouble(xminText);
        final double ymin = Double.parseDouble(yminText);
        final double xmax = Double.parseDouble(xmaxText);
        final double ymax = Double.parseDouble(ymaxText);

        final IMap<Integer, SpatialPoint> map = instance.getMap(GEOMAP);
        final EnvelopeContains envelopeContains = new EnvelopeContains();
        envelopeContains.envelope.setCoords(xmin, ymin, xmax, ymax);
        final Date date = new Date();
        final Collection<SpatialPoint> collection = map.values(envelopeContains);
        for (final SpatialPoint spatialPoint : collection)
        {
            final Long eventDate = (Long) spatialPoint.attributes.get("EVENT_DATE");
            date.setTime(eventDate);
            System.out.format("%s %s %s%n",
                    date.toString(),
                    spatialPoint.attributes.get("LOCATION").toString(),
                    spatialPoint.attributes.get("NOTES").toString()
            );
        }
    }

    private static HazelcastInstance createInstanceClient()
    {
        final ClientNetworkConfig networkConfig = new ClientNetworkConfig();
        networkConfig.addAddress("192.168.172.235");

        final ClientConfig clientConfig = new ClientConfig();
        clientConfig.setNetworkConfig(networkConfig);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }

    private static HazelcastInstance createInstance()
    {
        final Config config = new ClasspathXmlConfig("hazelcast.xml");
        return Hazelcast.newHazelcastInstance(config);
    }

    private static void doMapReduce(
            final HazelcastInstance instance,
            final String filename) throws ExecutionException, InterruptedException, IOException
    {
        final IMap<Integer, SpatialPoint> geomap = instance.getMap(GEOMAP);
        final JobTracker tracker = instance.getJobTracker("default");
        final Job<Integer, SpatialPoint> job = tracker.newJob(KeyValueSource.fromMap(geomap));
        final ICompletableFuture<Map<Integer, Integer>> future = job.
                mapper(new FeatureMapper(HEXMAP)).
                reducer(new FeatureReducerFactory()).
                submit();

        final FileOutputStream fileOutputStream = new FileOutputStream(filename);
        try
        {
            final PrintStream printStream = new PrintStream(fileOutputStream);
            printStream.format("ID,POPULATION%n");
            final Map<Integer, Integer> result = future.get();
            for (final Map.Entry<Integer, Integer> entry : result.entrySet())
            {
                printStream.format("%d,%d%n", entry.getKey(), entry.getValue());
            }
            printStream.flush();
        }
        finally
        {
            fileOutputStream.close();
        }
    }

    private static void doPolygons(
            final HazelcastInstance instance,
            final String filename
    ) throws IOException
    {
        final IMap<Integer, SpatialPolygon> map = instance.getMap(HEXMAP);
        final FileInputStream fileInputStream = new FileInputStream(filename);
        try
        {
            int id = 0;
            final ShpReader shpReader = new ShpReader(new DataInputStream(fileInputStream));
            while (shpReader.hasMore())
            {
                final SpatialPolygon spatialPolygon = new SpatialPolygon();
                shpReader.queryPolygon(spatialPolygon.polygon);
                spatialPolygon.queryEnvelope2D();
                spatialPolygon.id = id;
                map.put(id++, spatialPolygon);
            }
        }
        finally
        {
            fileInputStream.close();
        }
    }

    private static void doPoints(
            final HazelcastInstance instance,
            final String filename) throws IOException
    {
        final IMap<Integer, SpatialPoint> map = instance.getMap(GEOMAP);
        final Shapefile shapefile = new Shapefile(filename);
        try
        {
            int id = 0;
            while (shapefile.hasNext())
            {
                final SpatialPoint spatialPoint = new SpatialPoint();
                shapefile.readNext(spatialPoint.point, spatialPoint.attributes);
                spatialPoint.queryEnvelope2D();
                spatialPoint.id = id;
                map.put(id++, spatialPoint);
            }
        }
        finally
        {
            shapefile.close();
        }
    }
}
