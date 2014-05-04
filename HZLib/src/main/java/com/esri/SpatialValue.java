package com.esri;

import com.esri.core.geometry.Envelope2D;
import com.esri.core.geometry.Geometry;

/**
 */
public interface SpatialValue
{
    public Envelope2D getEnvelope2D();

    public Geometry getGeometry();
}
