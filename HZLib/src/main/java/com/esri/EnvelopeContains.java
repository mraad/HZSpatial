package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.query.Predicates;
import com.hazelcast.query.impl.QueryContext;
import com.hazelcast.query.impl.QueryableEntry;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 */
public class EnvelopeContains extends Predicates.AbstractPredicate implements SpatialPredicate
{
    public transient final Envelope2D envelope = new Envelope2D();

    public EnvelopeContains()
    {
        super("point");
    }

    @Override
    public boolean apply(final Map.Entry entry)
    {
        final Object value = entry.getValue();
        if (value instanceof SpatialShape)
        {
            final SpatialPoint spatialPoint = (SpatialPoint) value;
            return envelope.contains(spatialPoint.point);
        }
        return false;
    }

    @Override
    public boolean isIndexed(final QueryContext queryContext)
    {
        return queryContext.getIndex(this.attribute) != null;
    }

    @Override
    public Set<QueryableEntry> filter(final QueryContext queryContext)
    {
        return queryContext.getIndex(this.attribute).getRecords(this);
    }

    @Override
    public void writeData(final ObjectDataOutput out) throws IOException
    {
        super.writeData(out);
        out.writeDouble(envelope.xmin);
        out.writeDouble(envelope.ymin);
        out.writeDouble(envelope.xmax);
        out.writeDouble(envelope.ymax);
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException
    {
        super.readData(in);
        final double xmin = in.readDouble();
        final double ymin = in.readDouble();
        final double xmax = in.readDouble();
        final double ymax = in.readDouble();
        envelope.setCoords(xmin, ymin, xmax, ymax);
    }

    @Override
    public int compareTo(final Geometry geometry)
    {
        return 0;
    }

    @Override
    public Envelope2D getEnvelope2D()
    {
        return envelope;
    }

    @Override
    public boolean predicates(final Geometry geometry)
    {
        final Point point = (Point) geometry;
        return envelope.contains(point.getX(), point.getY());
    }

}
