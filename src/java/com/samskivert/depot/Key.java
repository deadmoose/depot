//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2008 Michael Bayne and Pär Winzell
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

package com.samskivert.depot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import com.samskivert.depot.clause.WhereClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.DepotMarshaller;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.util.StringUtil;

/**
 * A special form of {@link WhereClause} that uniquely specifies a single database row and thus
 * also a single persistent object. Because it implements both {@link CacheKey} and {@link
 * CacheInvalidator} it also uniquely indexes into the cache and knows how to invalidate itself
 * upon modification. This class is created by many {@link DepotMarshaller} methods as a
 * convenience, and may also be instantiated explicitly.
 */
public class Key<T extends PersistentRecord> extends WhereClause
    implements SQLExpression, CacheKey, ValidatingCacheInvalidator, Serializable
{
    /** Handles the matching of the key columns to its bound values. This is needed so that we can
     * combine a bunch of keys into a {@link KeySet}. */
    public static class Expression implements SQLExpression
    {
        public Expression (Class<? extends PersistentRecord> pClass, Comparable<?>[] values) {
            _pClass = pClass;
            _values = values;
        }
        public Class<? extends PersistentRecord> getPersistentClass () {
            return _pClass;
        }
        public Comparable<?>[] getValues () {
            return _values;
        }
        public Object accept (ExpressionVisitor<?> builder) {
            return builder.visit(this);
        }
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet) {
            classSet.add(getPersistentClass());
        }
        protected Class<? extends PersistentRecord> _pClass;
        protected Comparable<?>[] _values;
    }

    /**
     * Creates a single column key.
     */
    public static <T extends PersistentRecord> Key<T> newKey (
        Class<T> pClass, ColumnExp ix, Comparable<?> val)
    {
        return new Key<T>(pClass, new ColumnExp[] { ix }, new Comparable[] { val });
    }

    /**
     * Creates a two column key.
     */
    public static <T extends PersistentRecord> Key<T> newKey (
        Class<T> pClass, ColumnExp ix1, Comparable<?> val1, ColumnExp ix2, Comparable<?> val2)
    {
        return new Key<T>(pClass, new ColumnExp[] { ix1, ix2 }, new Comparable[] { val1, val2 });
    }

    /**
     * Creates a three column key.
     */
    public static <T extends PersistentRecord> Key<T> newKey (
        Class<T> pClass, ColumnExp ix1, Comparable<?> val1, ColumnExp ix2, Comparable<?> val2,
        ColumnExp ix3, Comparable<?> val3)
    {
        return new Key<T>(pClass, new ColumnExp[] { ix1, ix2, ix3 },
                          new Comparable[] { val1, val2, val3 });
    }

    /**
     * Extracts an integer key from a record's {@link Key}. This should only be used on records
     * whose primary key is a single integer.
     */
    public static <T extends PersistentRecord> Function<Key<T>,Integer> toInt ()
    {
        return new Function<Key<T>,Integer>() {
            public Integer apply (Key<T> key) {
                return (Integer)key.getValues()[0];
            }
        };
    }

    /**
     * Constructs a new multi-column {@code Key} with the given values.
     */
    public Key (Class<T> pClass, ColumnExp[] fields, Comparable<?>[] values)
    {
        if (fields.length != values.length) {
            throw new IllegalArgumentException("Field and Value arrays must be of equal length.");
        }

        // keep this for posterity
        _pClass = pClass;

        // build a local map of field name -> field value
        Map<ColumnExp, Comparable<?>> map = Maps.newHashMap();
        for (int i = 0; i < fields.length; i ++) {
            map.put(fields[i], values[i]);
        }

        // look up the cached primary key fields for this object
        ColumnExp[] keyFields = DepotUtil.getKeyFields(pClass);

        // now extract the values in field order and ensure none are extra or missing
        _values = new Comparable<?>[values.length];
        for (int ii = 0; ii < keyFields.length; ii++) {
            Comparable<?> value = map.remove(keyFields[ii]);
            if (value == null) {
                // make sure we were provided with a value for this primary key field
                throw new IllegalArgumentException("Missing value for key field: " + keyFields[ii]);
            }
            if (!(value instanceof Serializable)) {
                throw new IllegalArgumentException(
                    "Non-serializable argument [key=" + keyFields[ii] + ", value=" + value + "]");
            }
            _values[ii] = value;
        }

        // finally make sure we were not given any fields that are not in fact primary key fields
        if (map.size() > 0) {
            throw new IllegalArgumentException(
                "Non-key columns given: " +  StringUtil.join(map.keySet().toArray(), ", "));
        }
    }

    /**
     * Used to create a key when you know you have the canonical values array. Don't call this
     * unless you know what you're doing!
     */
    public Key (Class<T> pClass, Comparable<?>[] values)
    {
        _pClass = pClass;
        _values = values;
    }

    /**
     * Returns the persistent class for which we represent a key.
     */
    public Class<T> getPersistentClass ()
    {
        return _pClass;
    }

    /**
     * Returns the values bound to this key.
     */
    public Comparable<?>[] getValues ()
    {
        return _values;
    }

    @Override // from WhereClause
    public SQLExpression getWhereExpression ()
    {
        return new Expression(_pClass, _values);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from CacheKey
    public String getCacheId ()
    {
        return _pClass.getName();
    }

    // from CacheKey
    public Serializable getCacheKey ()
    {
        return this;
    }

    // from ValidatingCacheInvalidator
    public void validateFlushType (Class<?> pClass)
    {
        if (!pClass.equals(_pClass)) {
            throw new IllegalArgumentException(
                "Class mismatch between persistent record and cache invalidator " +
                "[record=" + pClass.getSimpleName() +
                ", invtype=" + _pClass.getSimpleName() + "].");
        }
    }

    // from CacheInvalidator
    public void invalidate (PersistenceContext ctx)
    {
        ctx.cacheInvalidate(this);
    }

    /**
     * Appends just the key=value portion of our {@link #toString} to the supplied buffer.
     */
    public void toShortString (StringBuilder builder)
    {
        ColumnExp[] keyFields = DepotUtil.getKeyFields(_pClass);
        for (int ii = 0; ii < keyFields.length; ii ++) {
            if (ii > 0) {
                builder.append(":");
            }
            builder.append(keyFields[ii].name).append("=").append(_values[ii]);
        }
    }

    @Override // from WhereClause
    public void validateQueryType (Class<?> pClass)
    {
        super.validateQueryType(pClass);
        validateTypesMatch(pClass, _pClass);
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return Arrays.equals(_values, ((Key<?>) obj)._values);
    }

    @Override
    public int hashCode ()
    {
        return Arrays.hashCode(_values);
    }

    @Override
    public String toString ()
    {
        StringBuilder builder = new StringBuilder(_pClass.getSimpleName());
        builder.append("(");
        toShortString(builder);
        builder.append(")");
        return builder.toString();
    }

    /** The persistent record type for which we are a key. */
    protected final Class<T> _pClass;

    /** The expression that identifies our row. */
    protected final Comparable<?>[] _values;
}
