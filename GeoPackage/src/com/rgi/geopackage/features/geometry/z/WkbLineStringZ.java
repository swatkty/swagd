/* The MIT License (MIT)
 *
 * Copyright (c) 2015 Reinventing Geospatial, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rgi.geopackage.features.geometry.z;

import com.rgi.geopackage.features.GeometryType;
import com.rgi.geopackage.features.geometry.xy.Envelope;

import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * A Curve that connects two or more points in space.
 *
 * @see "http://www.geopackage.org/spec/#sfsql_intro"
 *
 * @author Luke Lambert
 *
 */
public class WkbLineStringZ extends WkbCurveZ
{
    public WkbLineStringZ(final CoordinateZ... points)
    {
        this(new LinearRingZ(points));
    }

    public WkbLineStringZ(final Collection<CoordinateZ> points)
    {
        this(new LinearRingZ(points));
    }

    private WkbLineStringZ(final LinearRingZ linearString)
    {
        this.linearString = linearString;
    }

    @Override
    public long getTypeCode()
    {
        return WkbGeometryZ.GeometryTypeDimensionalityBase + GeometryType.LineString.getCode();
    }

    @Override
    public String getGeometryTypeName()
    {
        return GeometryType.LineString + "Z";
    }

    @Override
    public boolean isEmpty()
    {
        return this.linearString.isEmpty();
    }

    @Override
    public Envelope createEnvelope()
    {
        return this.linearString.createEnvelope();
    }

    @Override
    public EnvelopeZ createEnvelopeZ()
    {
        return this.linearString.createEnvelope();
    }

    @Override
    public void writeWellKnownBinary(final ByteBuffer byteBuffer)
    {
        if(byteBuffer == null)
        {
            throw new IllegalArgumentException("Byte buffer may not be null");
        }

        writeByteOrder(byteBuffer);

        byteBuffer.putInt((int)GeometryType.LineString.getCode());

        this.linearString.writeWellKnownBinary(byteBuffer);
    }



    public static WkbLineStringZ readWellKnownBinary(final ByteBuffer byteBuffer)
    {
        readWellKnownBinaryHeader(byteBuffer, GeometryType.LineString.getCode());

        return new WkbLineStringZ(LinearRingZ.readWellKnownBinary(byteBuffer));
    }

    private final LinearRingZ linearString;
}
