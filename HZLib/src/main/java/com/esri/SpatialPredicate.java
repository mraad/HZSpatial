package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;

/**
 */
public interface SpatialPredicate extends Comparable<Geometry>
{
    public Envelope2D getEnvelope2D();

    public boolean predicates(final Geometry geometry);
}
