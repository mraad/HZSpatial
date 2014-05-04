package com.esri;

import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ICompletableFuture;
import com.hazelcast.core.IMap;
import com.hazelcast.mapreduce.Job;
import com.hazelcast.mapreduce.JobTracker;
import com.hazelcast.mapreduce.KeyValueSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 */
public class MRTest
{
    @Test
    public void testMR() throws IOException, ExecutionException, InterruptedException
    {
        final Config config = new ClasspathXmlConfig("hazelcast.xml");
        final HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        try
        {
            final IMap<Integer, SpatialPolygon> hexmap = instance.getMap("hexmap");

            final SpatialPolygon spatialPolygon = new SpatialPolygon();
            spatialPolygon.id = 0;
            spatialPolygon.polygon.startPath(0, 0);
            spatialPolygon.polygon.lineTo(10, 0);
            spatialPolygon.polygon.lineTo(10, 10);
            spatialPolygon.polygon.lineTo(0, 10);
            spatialPolygon.polygon.lineTo(0, 0);
            spatialPolygon.polygon.closeAllPaths();
            spatialPolygon.polygon.queryEnvelope2D(spatialPolygon.envelope2D);
            hexmap.put(spatialPolygon.id, spatialPolygon);

            final IMap<Integer, SpatialPoint> geomap = instance.getMap("geomap");

            final SpatialPoint spatialPoint = new SpatialPoint();
            spatialPoint.id = 0;
            spatialPoint.point.setXY(5, 5);
            spatialPoint.point.queryEnvelope2D(spatialPoint.envelope2D);
            geomap.put(spatialPoint.id, spatialPoint);

            final JobTracker tracker = instance.getJobTracker("default");
            final Job<Integer, SpatialPoint> job = tracker.newJob(KeyValueSource.fromMap(geomap));
            final ICompletableFuture<Map<Integer, Integer>> future = job.
                    mapper(new FeatureMapper("hexmap")).
                    reducer(new FeatureReducerFactory()).
                    submit();

            final Map<Integer, Integer> result = future.get();
            Assert.assertEquals(1, result.size());
            final Map.Entry<Integer, Integer> next = result.entrySet().iterator().next();
            Assert.assertEquals(0, next.getKey().intValue());
            Assert.assertEquals(1, next.getValue().intValue());
        }
        finally
        {
            instance.shutdown();
        }
    }
}
