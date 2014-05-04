package com.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class SpatialPoint extends SpatialShape
{
    public transient final Point point = new Point();
    public transient final Map<String, Object> attributes = new HashMap<String, Object>();

    public SpatialPoint()
    {
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException
    {
        out.writeInt(id);
        out.writeDouble(point.getX());
        out.writeDouble(point.getY());
        out.writeInt(attributes.size());
        for (final Map.Entry<String, Object> e : attributes.entrySet())
        {
            out.writeUTF(e.getKey());
            out.writeObject(e.getValue());
        }
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException
    {
        id = in.readInt();
        final double x = in.readDouble();
        final double y = in.readDouble();
        point.setXY(x, y);
        envelope2D.setCoords(x, y, x, y);
        final int size = in.readInt();
        for (int s = 0; s < size; s++)
        {
            final String key = in.readUTF();
            final Object val = in.readObject();
            attributes.put(key, val);
        }
    }

    @Override
    public Geometry getGeometry()
    {
        return point;
    }

    public void queryEnvelope2D()
    {
        point.queryEnvelope2D(envelope2D);
    }
}
