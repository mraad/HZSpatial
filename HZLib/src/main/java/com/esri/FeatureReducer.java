package com.esri;

import com.hazelcast.mapreduce.Reducer;

/**
 */
public class FeatureReducer
        extends Reducer<Integer, Integer, Integer>
{
    private transient int sum = 0;

    @Override
    public void reduce(final Integer value)
    {
        sum += value;
    }

    @Override
    public Integer finalizeReduce()
    {
        return sum;
    }
}
