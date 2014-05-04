package com.esri;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point2D;
import com.esri.core.geometry.Polygon;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import java.io.IOException;

/**
 */
public class SpatialPolygon extends SpatialShape
{
    public transient final Polygon polygon = new Polygon();

    public SpatialPolygon()
    {
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException
    {
        out.writeInt(id);
        final Point2D point2D = new Point2D();
        final int pointCount = polygon.getPointCount();
        out.writeInt(pointCount);
        for (int p = 0; p < pointCount; p++)
        {
            polygon.getXY(p, point2D);
            out.writeDouble(point2D.x);
            out.writeDouble(point2D.y);
        }
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException
    {
        id = in.readInt();
        final int count = in.readInt();
        for (int c = 0; c < count; c++)
        {
            final double x = in.readDouble();
            final double y = in.readDouble();
            if (c > 0)
            {
                polygon.lineTo(x, y);
            }
            else
            {
                polygon.startPath(x, y);
            }
        }
        polygon.closeAllPaths();
        polygon.queryEnvelope2D(envelope2D);
    }

    @Override
    public Geometry getGeometry()
    {
        return polygon;
    }

    public void queryEnvelope2D()
    {
        polygon.queryEnvelope2D(envelope2D);
    }
}
