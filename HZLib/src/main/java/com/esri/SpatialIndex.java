package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.QuadTree;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.query.impl.ComparisonType;
import com.hazelcast.query.impl.Index;
import com.hazelcast.query.impl.QueryException;
import com.hazelcast.query.impl.QueryableEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class SpatialIndex implements Index
{

    private final static ILogger LOG = Logger.getLogger(SpatialIndex.class);

    private final String m_attribute;
    private final boolean m_ordered;
    private final QuadTree.QuadTreeIterator m_iterator;
    private Envelope2D m_envelope2D;
    private int m_height;
    private QuadTree m_spatialIndex;
    private List<QueryableEntry> m_list = new ArrayList<QueryableEntry>();

    public SpatialIndex(
            final String attribute,
            final boolean ordered,
            final String properties)
    {
        m_attribute = attribute;
        m_ordered = ordered;
        double xmin, ymin, xmax, ymax;
        m_height = 16;
        final String[] tokens = properties.split(",");
        switch (tokens.length)
        {
            case 5:
                m_height = Integer.parseInt(tokens[4]);
                // NO BREAK - FALL THROUGH !!
            case 4:
                xmin = Double.parseDouble(tokens[0]);
                ymin = Double.parseDouble(tokens[1]);
                xmax = Double.parseDouble(tokens[2]);
                ymax = Double.parseDouble(tokens[3]);
                break;
            default:
                xmin = -180.0;
                ymin = -90.0;
                xmax = 180.0;
                ymax = 90.0;
        }
        m_envelope2D = new Envelope2D(xmin, ymin, xmax, ymax);
        m_spatialIndex = new QuadTree(m_envelope2D, m_height);
        m_iterator = m_spatialIndex.getIterator();
    }

    @Override
    public void saveEntryIndex(QueryableEntry e) throws QueryException
    {
        if (e.getValue() instanceof SpatialValue)
        {
            final SpatialValue spatialValue = (SpatialValue) e.getValue();
            try
            {
                final Envelope2D envelope2D = spatialValue.getEnvelope2D();
                if (envelope2D != null)
                {
                    final int pos = m_list.size();
                    m_list.add(e);
                    m_spatialIndex.insert(pos, envelope2D);
                }
                else
                {
                    LOG.warning("Inserting null envelope2D for key " + e.getKey());
                }
            }
            catch (Throwable t)
            {
                LOG.warning("Error in saving entry with key " + e.getKey(), t);
            }
        }
    }

    @Override
    public void clear()
    {
        m_spatialIndex = new QuadTree(m_envelope2D, m_height);
        m_list.clear();
    }

    @Override
    public void removeEntryIndex(Data indexKey)
    {
        // LOG.info(String.format("-----removeEntryIndex %d", indexKey.getId()));
    }

    @Override
    public Set<QueryableEntry> getRecords(Comparable[] values)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<QueryableEntry> getRecords(final Comparable predicate)
    {
        final Set<QueryableEntry> result = new HashSet<QueryableEntry>();
        if (predicate instanceof SpatialPredicate)
        {
            final SpatialPredicate spatialPredicate = (SpatialPredicate) predicate;
            final Envelope2D envelope2D = spatialPredicate.getEnvelope2D();
            // TODO make new iterator instance
            m_iterator.resetIterator(envelope2D, 0.000001);
            int elementId = m_iterator.next();
            while (elementId != -1)
            {
                final int index = m_spatialIndex.getElement(elementId);
                final QueryableEntry queryableEntry = m_list.get(index);
                final Object value = queryableEntry.getValue();
                if (value instanceof SpatialValue)
                {
                    final SpatialValue spatialValue = (SpatialValue) value;
                    // TODO - keep here list of accelerated geometries !!!
                    if (spatialPredicate.predicates(spatialValue.getGeometry()))
                    {
                        result.add(queryableEntry);
                    }
                }
                elementId = m_iterator.next();
            }
        }
        else
        {
            for (final QueryableEntry queryableEntry : m_list)
            {
                if (predicate.compareTo(queryableEntry.getAttribute(this.m_attribute)) == 0)
                {
                    result.add(queryableEntry);
                }
            }
        }
        return result;
    }

    @Override
    public Set<QueryableEntry> getSubRecordsBetween(
            Comparable from,
            Comparable to)
    {
        return Collections.emptySet();
    }

    @Override
    public Set<QueryableEntry> getSubRecords(
            ComparisonType comparisonType,
            Comparable searchedValue)
    {
        return Collections.emptySet();
    }

    @Override
    public String getAttributeName()
    {
        return m_attribute;
    }

    @Override
    public boolean isOrdered()
    {
        return m_ordered;
    }
}
