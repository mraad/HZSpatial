package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorContains;
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
public class InsidePolygon extends Predicates.AbstractPredicate implements SpatialPredicate
{
    private transient final OperatorContains m_contains = OperatorContains.local();
    private transient final Point m_point = new Point();
    private transient final Envelope2D m_envelope = new Envelope2D();

    public InsidePolygon()
    {
        super("polygon");
    }

    public void setXY(
            final double x,
            final double y)
    {
        m_point.setXY(x, y);
        m_envelope.setCoords(x, y, x, y);
    }

    @Override
    public boolean apply(final Map.Entry entry)
    {
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
        out.writeDouble(m_point.getX());
        out.writeDouble(m_point.getY());
    }

    @Override
    public void readData(final ObjectDataInput in) throws IOException
    {
        super.readData(in);
        final double x = in.readDouble();
        final double y = in.readDouble();
        m_point.setXY(x, y);
        m_envelope.setCoords(x, y, x, y);
    }

    @Override
    public int compareTo(final Geometry geometry)
    {
        return 0;
    }

    @Override
    public Envelope2D getEnvelope2D()
    {
        return m_envelope;
    }

    @Override
    public boolean predicates(final Geometry geometry)
    {
        return m_contains.execute(geometry, m_point, null, null);
    }

}
