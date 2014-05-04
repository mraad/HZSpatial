package com.esri;

import com.esri.core.geometry.Point;
import com.esri.dbf.DBFReader;
import com.esri.shp.ShpReader;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 */
public class Shapefile
{
    private final FileInputStream m_shpInputStream;
    private final FileInputStream m_dbfInputStream;
    private final ShpReader m_shpReader;
    private final DBFReader m_dbfReader;

    public Shapefile(final String filename) throws IOException
    {
        m_shpInputStream = new FileInputStream(filename);
        m_shpReader = new ShpReader(new DataInputStream(m_shpInputStream));
        m_dbfInputStream = new FileInputStream(filename.replace(".shp", ".dbf"));
        m_dbfReader = new DBFReader(new DataInputStream(m_dbfInputStream));
    }

    public boolean hasNext() throws IOException
    {
        return m_shpReader.hasMore();
    }

    public void readNext(
            final Point point,
            final Map<String, Object> map) throws IOException
    {
        m_shpReader.queryPoint(point);
        m_dbfReader.readRecordAsMap(map);
    }

    public void close() throws IOException
    {
        if (m_dbfInputStream != null)
        {
            m_dbfInputStream.close();
        }
        if (m_shpInputStream != null)
        {
            m_shpInputStream.close();
        }
    }
}
