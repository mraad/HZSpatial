package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.hazelcast.nio.serialization.DataSerializable;

/**
 */
public abstract class SpatialShape implements DataSerializable, SpatialValue
{
    public transient final Envelope2D envelope2D = new Envelope2D();
    public transient int id;

    @Override
    public Envelope2D getEnvelope2D()
    {
        return envelope2D;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof SpatialPolygon))
        {
            return false;
        }

        final SpatialPolygon that = (SpatialPolygon) o;

        if (id != that.id)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return id;
    }
}
