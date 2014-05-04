package com.esri;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import com.hazelcast.mapreduce.Context;
import com.hazelcast.mapreduce.LifecycleMapper;

import java.util.Collection;

/**
 */
public class FeatureMapper
        implements LifecycleMapper<Integer, SpatialPoint, Integer, Integer>, HazelcastInstanceAware
{
    private transient InsidePolygon m_insidePolygon;
    private transient IMap<Integer, SpatialPolygon> m_map;
    private final String m_mapName;

    public FeatureMapper(final String mapName)
    {
        m_mapName = mapName;
    }

    @Override
    public void map(
            final Integer key,
            final SpatialPoint value,
            final Context<Integer, Integer> collector)
    {
        final double x = value.point.getX();
        final double y = value.point.getY();

        m_insidePolygon.setXY(x, y);

        final Collection<SpatialPolygon> collection = m_map.values(m_insidePolygon);
        if (!collection.isEmpty())
        {
            collector.emit(collection.iterator().next().id, 1);
        }
    }

    @Override
    public void setHazelcastInstance(final HazelcastInstance hazelcastInstance)
    {
        m_map = hazelcastInstance.getMap(m_mapName);
    }

    @Override
    public void initialize(final Context<Integer, Integer> context)
    {
        m_insidePolygon = new InsidePolygon();
    }

    @Override
    public void finalized(final Context<Integer, Integer> context)
    {
    }
}
