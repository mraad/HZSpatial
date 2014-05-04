package com.esri;

import com.hazelcast.mapreduce.Reducer;
import com.hazelcast.mapreduce.ReducerFactory;

/**
 */
public class FeatureReducerFactory
        implements ReducerFactory<Integer, Integer, Integer>
{
    public FeatureReducerFactory()
    {
    }

    @Override
    public Reducer<Integer, Integer, Integer> newReducer(Integer key)
    {
        return new FeatureReducer();
    }
}
