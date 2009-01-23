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

package com.samskivert.depot.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.CacheKey;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Stats;
import com.samskivert.depot.CacheAdapter.CacheCategory;
import com.samskivert.jdbc.DatabaseLiaison;

/**
 * Encapsulates a modification of persistent objects.
 */
public abstract class Modifier implements Operation<Integer>
{
    /**
     * A simple modifier that executes a single SQL statement. No cache flushing is done as a
     * result of this operation.
     */
    public abstract static class Simple extends Modifier
    {
        public Simple () {
            super(null);
        }

        @Override // from Modifier
        protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
            Statement stmt = conn.createStatement();
            try {
                return stmt.executeUpdate(createQuery(liaison));
            } finally {
                stmt.close();
            }
        }

        protected abstract String createQuery (DatabaseLiaison liaison);
    }

    /**
     * A convenience modifier that can perform cache updates in addition to invalidation:
     * - Before {@link #invoke}, the {@link CacheInvalidator} is run, if given.
     * - After {@link #invoke}, the cache is updated with the modified object,
     * presuming both _key and _result are non-null. These variables may be set or modified
     * during execution in addition to being supplied to the constructor.
     */
    public static abstract class CachingModifier<T extends PersistentRecord> extends Modifier
    {
        /**
         * Construct a new CachingModifier with the given result, cache key, and invalidator,
         * all of which are optional, and may also be set during execution.
         */
        protected CachingModifier (T result, CacheKey key, CacheInvalidator invalidator)
        {
            super(invalidator);
            _result = result;
            _key = key;
        }

        /**
         * Update this {@link CachingModifier}'s cache key, e.g. during insertion when a
         * persistent object first receives a generated key.
         */
        protected void updateKey (CacheKey key)
        {
            if (key != null) {
                _key = key;
            }
        }

        @Override // from Modifier
        public Integer invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
            throws SQLException
        {
            Integer rows = super.invoke(ctx, conn, liaison);
            // if we have both a key and a record, cache
            if (_key != null && _result != null) {
                ctx.cacheStore(CacheCategory.RECORD, _key, _result.clone());
            }
            return rows;
        }

        protected CacheKey _key;
        protected T _result;
    }

    /**
     * Constructs a {@link Modifier} without a cache invalidator.
     */
    public Modifier ()
    {
        this(null);
    }

    /**
     * Constructs a {@link Modifier} with the given cache invalidator.
     */
    public Modifier (CacheInvalidator invalidator)
    {
        _invalidator = invalidator;
    }

    // from interface Operation
    public boolean isReadOnly ()
    {
        return false;
    }

    // from interface Operation
    public Integer invoke (PersistenceContext ctx, Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        if (_invalidator != null) {
            _invalidator.invalidate(ctx);
        }
        return invoke(conn, liaison);
    }

    // from interface Operation
    public void updateStats (Stats stats)
    {
        // nothing to update for modifiers
    }

    /**
     * Overriden to perform the actual database modifications represented by this object; should
     * return the number of modified rows.
     */
    protected abstract int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException;

    protected CacheInvalidator _invalidator;
}
