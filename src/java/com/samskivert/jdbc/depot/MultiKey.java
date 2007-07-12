//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2006 Michael Bayne
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.jdbc.depot;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.jdbc.depot.clause.Where;

/**
 * A special form of {@link Where} clause that specifies an explicit range of database rows. It
 * does not implement {@link CacheKey} but it does implement {@link CacheInvalidator} which means
 * it can be sent into e.g. {@link DepotRepository#deleteAll) and have it clean up after itself.
 */
public class MultiKey<T extends PersistentRecord> extends Where
    implements CacheInvalidator
{
    /**
     * Constructs a new single-column {@code MultiKey} with the given value range.
     */
    public MultiKey (Class<T> pClass, String ix, Comparable... val)
    {
        this(pClass, new String[0], new Comparable[0], ix, val);
    }

    /**
     * Constructs a new two-column {@code MultiKey} with the given value range.
     */
    public MultiKey (Class<T> pClass, String ix1, Comparable val1, String ix2, Comparable... val2)
    {
        this(pClass, new String[] { ix1 }, new Comparable[] { val1 }, ix2, val2);
    }

    /**
     * Constructs a new three-column {@code MultiKey} with the given value range.
     */
    public MultiKey (Class<T> pClass, String ix1, Comparable val1, String ix2, Comparable val2,
                     String ix3, Comparable... val3)
    {
        this(pClass, new String[] { ix1, ix2 }, new Comparable[] { val1, val2 }, ix3, val3);
    }

    /**
     * Constructs a new multi-column {@code MultiKey} with the given value range.
     * @TODO: See {@link Key#Key(Class, String[], Comparable[]) for somewhat relevant comments.
     */
    public MultiKey (Class<T> pClass, String[] sFields, Comparable[] sValues,
                     String mField, Comparable[] mValues)
    {
        // TODO
        super(null);
        if (sFields.length != sValues.length) {
            throw new IllegalArgumentException(
                "Key field and values arrays must be of equal length.");
        }
        _pClass = pClass;
        _mField = mField;
        _mValues = mValues;
        _map = new HashMap<String, Comparable>();
        for (int i = 0; i < sFields.length; i ++) {
            _map.put(sFields[i], sValues[i]);
        }
    }

    // from QueryClause
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    // from QueryClause
    public void appendClause (QueryBuilderContext<?> query, StringBuilder builder)
    {
        builder.append(" where ");
        boolean first = true;
        for (Map.Entry entry : _map.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(" and ");
            }
            builder.append(entry.getKey());
            builder.append(entry.getValue() == null ? " is null " : " = ? ");
        }
        if (!first) {
            builder.append(" and ");
        }
        builder.append(_mField).append(" in (");
        for (int ii = 0; ii < _mValues.length; ii ++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append("?");
        }
        builder.append(")");
    }

    // from QueryClause
    public int bindExpressionArguments (PreparedStatement pstmt, int argIdx)
        throws SQLException
    {
        for (Map.Entry entry : _map.entrySet()) {
            if (entry.getValue() != null) {
                pstmt.setObject(argIdx ++, entry.getValue());
            }
        }
        for (int ii = 0; ii < _mValues.length; ii++) {
            pstmt.setObject(argIdx ++, _mValues[ii]);
        }
        return argIdx;
    }

    // from CacheInvalidator
    public void invalidate (PersistenceContext ctx)
    {
        HashMap<String, Comparable> newMap = new HashMap<String, Comparable>(_map);
        for (int i = 0; i < _mValues.length; i ++) {
            newMap.put(_mField, _mValues[i]);
            ctx.cacheInvalidate(new SimpleCacheKey(_pClass, newMap));
        }
    }

    protected Class<T> _pClass;
    protected HashMap<String, Comparable> _map;
    protected String _mField;
    protected Comparable[] _mValues;
}
