//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2006-2007 Michael Bayne, Pär Winzell
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.Modifier.*;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.util.ArrayUtil;

/**
 * Provides a base for classes that provide access to persistent objects. Also defines the
 * mechanism by which all persistent queries and updates are routed through the distributed cache.
 */
public class DepotRepository
{
    /**
     * Creates a repository with the supplied connection provider and its own private persistence
     * context.
     */
    protected DepotRepository (ConnectionProvider conprov)
    {
        _ctx = new PersistenceContext(getClass().getName(), conprov);
    }

    /**
     * Creates a repository with the supplied persistence context.
     */
    protected DepotRepository (PersistenceContext context)
    {
        _ctx = context;
    }

    /**
     * Loads the persistent object that matches the specified primary key.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, Comparable primaryKey,
                                                   QueryClause... clauses)
        throws PersistenceException
    {
        clauses = ArrayUtil.append(clauses, _ctx.getMarshaller(type).makePrimaryKey(primaryKey));
        return load(type, clauses);
    }

    /**
     * Loads the persistent object that matches the specified primary key.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, String ix, Comparable val,
                                                   QueryClause... clauses)
        throws PersistenceException
    {
        clauses = ArrayUtil.append(clauses, new Key<T>(type, ix, val));
        return load(type, clauses);
    }

    /**
     * Loads the persistent object that matches the specified two-column primary key.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, String ix1, Comparable val1,
                                                   String ix2, Comparable val2,
                                                   QueryClause... clauses)
        throws PersistenceException
    {
        clauses = ArrayUtil.append(clauses, new Key<T>(type, ix1, val1, ix2, val2));
        return load(type, clauses);
    }

    /**
     * Loads the persistent object that matches the specified three-column primary key.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, String ix1, Comparable val1,
                                                   String ix2, Comparable val2, String ix3,
                                                   Comparable val3, QueryClause... clauses)
        throws PersistenceException
    {
        clauses = ArrayUtil.append(clauses, new Key<T>(type, ix1, val1, ix2, val2, ix3, val3));
        return load(type, clauses);
    }

    /**
     * Loads the first persistent object that matches the supplied key.
     */
    protected <T extends PersistentRecord> T load (Class<T> type, QueryClause... clauses)
        throws PersistenceException
    {
        return _ctx.invoke(new FindOneQuery<T>(_ctx, type, clauses));
    }

    /**
     * Loads all persistent objects that match the specified clauses.
     *
     * We have two strategies for doing this: one performs the query as-is, the second executes
     * two passes: first fetching only key columns and consulting the cache for each such key;
     * then, in the second pass, fetching the full entity only for keys that were not found in
     * the cache.
     *
     * The more complex strategy could save a lot of data shuffling. On the other hand, its
     * complexity is an inherent drawback, and it does execute two separate database queries
     * for what the simple method does in one.
     */
    protected <T extends PersistentRecord> List<T> findAll (Class<T> type, QueryClause... clauses)
        throws PersistenceException
    {
        DepotMarshaller<T> marsh = _ctx.getMarshaller(type);
        boolean useExplicit = (marsh.getTableName() == null) || !marsh.hasPrimaryKey();

        // TODO: This is a very conservative approach where all complicated queries are handled
        // TODO: with the traditional single-pass query. The double pass algorithm can handle a
        // TODO: much larger class of queries, but only after some engine upgrades: specifically,
        // TODO: the field references used in the WHERE clause of the Entity query in WithCache
        // TODO: cannot be the trivial expansion currently used in the Key class, but must rather
        // TODO: undergo the same treatment SELECT fields currently do in SQLQueryBuilder. This
        // TODO: is probably best done with an additional method in QueryBuilderContext, or perhaps
        // TODO: a much larger switch to using a visitor pattern for generating the SQL of the
        // TODO: clause hierarchies.
        for (QueryClause clause : clauses) {
            useExplicit |= (clause instanceof FieldOverride ||
                            clause instanceof Join ||
                            clause instanceof FromOverride);
        }
        return _ctx.invoke(useExplicit ?
            new FindAllQuery.Explicitly<T>(_ctx, type, clauses) :
            new FindAllQuery.WithCache<T>(_ctx, type, clauses));
    }

    /**
     * Inserts the supplied persistent object into the database, assigning its primary key (if it
     * has one) in the process.
     *
     * @return the number of rows modified by this action, this should always be one.
     */
    protected <T extends PersistentRecord> int insert (T record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        final Key key = marsh.getPrimaryKey(record, false);
        // key will be null if record was supplied without a primary key
        return _ctx.invoke(new CachingModifier<T>(record, key, key) {
            public int invoke (Connection conn) throws SQLException {
                // update our modifier's key so that it can cache our results
                updateKey(marsh.assignPrimaryKey(conn, _result, false));
                PreparedStatement stmt = marsh.createInsert(conn, _result);
                try {
                    int mods = stmt.executeUpdate();
                    // check again in case we have a post-factum key generator
                    updateKey(marsh.assignPrimaryKey(conn, _result, true));
                    return mods;
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Updates all fields of the supplied persistent object, using its primary key to identify the
     * row to be updated.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int update (T record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        final Key key = marsh.getPrimaryKey(record);
        if (key == null) {
            throw new IllegalArgumentException("Can't update record with null primary key.");
        }
        return _ctx.invoke(new CachingModifier<T>(record, key, key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createUpdate(conn, _result, key);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Updates just the specified fields of the supplied persistent object, using its primary key
     * to identify the row to be updated. This method currently flushes the associated record from
     * the cache, but in the future it should be modified to update the modified fields in the
     * cached value iff the record exists in the cache.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int update (T record, final String... modifiedFields)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        final Key key = marsh.getPrimaryKey(record);
        if (key == null) {
            throw new IllegalArgumentException("Can't update record with null primary key.");
        }
        return _ctx.invoke(new CachingModifier<T>(record, key, key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createUpdate(conn, _result, key, modifiedFields);
                // clear out _result so that we don't rewrite this partial record to the cache
                _result = null;
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary key.
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the primary key to match in the update.
     * @param fieldsValues an mapping from the names of the fields/columns ti the values to be
     * assigned.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, Comparable primaryKey, Map<String,Object> updates)
        throws PersistenceException
    {
        Object[] fieldsValues = new Object[updates.size()*2];
        int idx = 0;
        for (Map.Entry<String,Object> entry : updates.entrySet()) {
            fieldsValues[idx++] = entry.getKey();
            fieldsValues[idx++] = entry.getValue();
        }
        return updatePartial(type, primaryKey, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary key.
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the primary key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, Comparable primaryKey, Object... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = _ctx.getMarshaller(type).makePrimaryKey(primaryKey);
        return updatePartial(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied two-column
     * primary key.
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the primary key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, String ix1, Comparable val1, String ix2, Comparable val2,
        Object... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = new Key<T>(type, ix1, val1, ix2, val2);
        return updatePartial(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied three-column
     * primary key.
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the primary key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, String ix1, Comparable val1, String ix2, Comparable val2,
        String ix3, Comparable val3, Object... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = new Key<T>(type, ix1, val1, ix2, val2, ix3, val3);
        return updatePartial(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key. This
     * method currently flushes the associated record from the cache, but in the future it should
     * be modified to update the modified fields in the cached value iff the record exists in the
     * cache.
     *
     * @param type the type of the persistent object to be modified.
     * @param key the key to match in the update.
     * @param invalidator a cache invalidator that will be prior to the update to flush the
     * relevant persistent objects from the cache.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updatePartial (
        Class<T> type, final Where key, CacheInvalidator invalidator, Object... fieldsValues)
        throws PersistenceException
    {
        // separate the arguments into keys and values
        final String[] fields = new String[fieldsValues.length/2];
        final Object[] values = new Object[fields.length];
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            fields[ii] = (String)fieldsValues[idx++];
            values[ii] = fieldsValues[idx++];
        }

        final DepotMarshaller marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Modifier(invalidator) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createPartialUpdate(conn, key, fields, values);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update statement.
     * In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, literal value, key, literal value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Class<T> type, Comparable primaryKey, String... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = _ctx.getMarshaller(type).makePrimaryKey(primaryKey);
        return updateLiteral(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied two-column
     * primary key. The values in this case must be literal SQL to be inserted into the update
     * statement. In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, literal value, key, literal value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Class<T> type, String ix1, Comparable val1, String ix2, Comparable val2,
        String... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = new Key<T>(type, ix1, val1, ix2, val2);
        return updateLiteral(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied three-column
     * primary key. The values in this case must be literal SQL to be inserted into the update
     * statement. In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param type the type of the persistent object to be modified.
     * @param primaryKey the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, literal value, key, literal value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Class<T> type, String ix1, Comparable val1, String ix2, Comparable val2,
        String ix3, Comparable val3, String... fieldsValues)
        throws PersistenceException
    {
        Key<T> key = new Key<T>(type, ix1, val1, ix2, val2, ix3, val3);
        return updateLiteral(type, key, key, fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update statement.
     * In general this is used when you want to do something like the following:
     *
     * <pre>
     * update FOO set BAR = BAR + 1;
     * update BAZ set BIF = NOW();
     * </pre>
     *
     * @param type the type of the persistent object to be modified.
     * @param key the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, literal value, key, literal value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T extends PersistentRecord> int updateLiteral (
        Class<T> type, final Where key, CacheInvalidator invalidator, String... fieldsValues)
        throws PersistenceException
    {
        // separate the arguments into keys and values
        final String[] fields = new String[fieldsValues.length/2];
        final String[] values = new String[fields.length];
        for (int ii = 0, idx = 0; ii < fields.length; ii++) {
            fields[ii] = fieldsValues[idx++];
            values[ii] = fieldsValues[idx++];
        }

        final DepotMarshaller marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Modifier(invalidator) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createLiteralUpdate(conn, key, fields, values);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    /**
     * Stores the supplied persisent object in the database. If it has no primary key assigned (it
     * is null or zero), it will be inserted directly. Otherwise an update will first be attempted
     * and if that matches zero rows, the object will be inserted.
     *
     * @return true if the record was created, false if it was updated.
     */
    protected <T extends PersistentRecord> boolean store (T record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        final Key key = marsh.hasPrimaryKey() ? marsh.getPrimaryKey(record) : null;
        final boolean[] created = new boolean[1];
        _ctx.invoke(new CachingModifier<T>(record, key, key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    // if our primary key isn't null, update rather than insert the record
                    // before been persisted and insert
                    if (key != null) {
                        stmt = marsh.createUpdate(conn, _result, key);
                        int mods = stmt.executeUpdate();
                        if (mods > 0) {
                            return mods;
                        }
                        JDBCUtil.close(stmt);
                    }

                    // if the update modified zero rows or the primary key was obviously unset, do
                    // an insertion
                    updateKey(marsh.assignPrimaryKey(conn, _result, false));
                    stmt = marsh.createInsert(conn, _result);
                    int mods = stmt.executeUpdate();
                    updateKey(marsh.assignPrimaryKey(conn, _result, true));
                    created[0] = true;
                    return mods;

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
        return created[0];
    }

    /**
     * Deletes all persistent objects from the database with a primary key matching the primary key
     * of the supplied object.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T extends PersistentRecord> int delete (T record)
        throws PersistenceException
    {
        @SuppressWarnings("unchecked") Class<T> type = (Class<T>)record.getClass();
        Key<T> primaryKey = _ctx.getMarshaller(type).getPrimaryKey(record);
        if (primaryKey == null) {
            throw new IllegalArgumentException("Can't delete record with null primary key.");
        }
        return deleteAll(type, primaryKey, primaryKey);
    }

    /**
     * Deletes all persistent objects from the database with a primary key matching the supplied
     * primary key.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T extends PersistentRecord> int delete (Class<T> type, Comparable primaryKeyValue)
        throws PersistenceException
    {
        return delete(type, _ctx.getMarshaller(type).makePrimaryKey(primaryKeyValue));
    }

    /**
     * Deletes all persistent objects from the database with a primary key matching the supplied
     * primary key.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T extends PersistentRecord> int delete (Class<T> type, Key<T> primaryKey)
        throws PersistenceException
    {
        return deleteAll(type, primaryKey, primaryKey);
    }

    /**
     * Deletes all persistent objects from the database that match the supplied key.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T extends PersistentRecord> int deleteAll (
        Class<T> type, final Where key, CacheInvalidator invalidator)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Modifier(invalidator) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createDelete(conn, key);
                try {
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });
    }

    protected static abstract class CollectionQuery<T extends Collection> implements Query<T>
    {
        public CollectionQuery (CacheKey key)
            throws PersistenceException
        {
            _key = key;
        }

        public CacheKey getCacheKey ()
        {
            return _key;
        }

        public void updateCache (PersistenceContext ctx, T result)
        {
            ctx.cacheStore(_key, result);
        }

        protected CacheKey _key;
    }

    protected PersistenceContext _ctx;
}
