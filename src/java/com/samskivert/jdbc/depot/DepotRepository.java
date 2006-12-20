//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2006 Michael Bayne, Pär Winzell
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
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
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
    protected <T> T load (Class<T> type, Comparable primaryKey, QueryClause... clauses)
        throws PersistenceException
    {
        clauses = ArrayUtil.append(clauses, _ctx.getMarshaller(type).makePrimaryKey(primaryKey));
        return load(type, clauses);
    }

    /**
     * Loads the first persistent object that matches the supplied key.
     */
    protected <T> T load (Class<T> type, QueryClause... clauses)
        throws PersistenceException
    {
        final DepotMarshaller<T> marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Query<T>(_ctx, type, clauses) {
            public T invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = createQuery(conn);
                try {
                    T result = null;
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        result = marsh.createObject(rs);
                    }
                    // TODO: if (rs.next()) issue warning?
                    rs.close();
                    return result;

                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Loads all persistent objects that match the specified key.
     */
    protected <T,C extends Collection<T>> Collection<T> findAll (
        Class<T> type, QueryClause... clauses)
        throws PersistenceException
    {
        final DepotMarshaller<T> marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Query<ArrayList<T>>(_ctx, type, clauses) {
            public ArrayList<T> invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = createQuery(conn);
                try {
                    ArrayList<T> results = new ArrayList<T>();
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next()) {
                        results.add(marsh.createObject(rs));
                    }
                    return results;

                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Inserts the supplied persistent object into the database, assigning its primary key (if it
     * has one) in the process.
     *
     * @return the number of rows modified by this action, this should always be one.
     */
    protected int insert (final Object record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        return _ctx.invoke(new Modifier(null) {
            public int invoke (Connection conn) throws SQLException {
                // update our modifier's key so that it can cache our results
                updateKey(marsh.assignPrimaryKey(conn, record, false));
                PreparedStatement stmt = marsh.createInsert(conn, record);
                try {
                    int mods = stmt.executeUpdate();
                    // check again in case we have a post-factum key generator
                    updateKey(marsh.assignPrimaryKey(conn, record, true));
                    return mods;
                } finally {
                    stmt.close();
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
    protected int update (final Object record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        return _ctx.invoke(new Modifier(marsh.getPrimaryKey(record)) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createUpdate(conn, record, _key);
                try {
                    return stmt.executeUpdate();
                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Updates just the specified fields of the supplied persistent object, using its primary key
     * to identify the row to be updated.
     *
     * @return the number of rows modified by this action.
     */
    protected int update (final Object record, final String... modifiedFields)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        return _ctx.invoke(new Modifier(marsh.getPrimaryKey(record)) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createUpdate(conn, record, _key, modifiedFields);
                try {
                    return stmt.executeUpdate();
                } finally {
                    stmt.close();
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
    protected <T> int updatePartial (Class<T> type, Comparable primaryKey,
                                     Map<String,Object> updates)
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
    protected <T> int updatePartial (Class<T> type, Comparable primaryKey, Object... fieldsValues)
        throws PersistenceException
    {
        return updatePartial(
            type, _ctx.getMarshaller(type).makePrimaryKey(primaryKey), fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied key.
     *
     * @param type the type of the persistent object to be modified.
     * @param key the key to match in the update.
     * @param fieldsValues an array containing the names of the fields/columns and the values to be
     * assigned, in key, value, key, value, etc. order.
     *
     * @return the number of rows modified by this action.
     */
    protected <T> int updatePartial (Class<T> type, Where key, Object... fieldsValues)
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
        return _ctx.invoke(new Modifier(key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createPartialUpdate(conn, _key, fields, values);
                try {
                    return stmt.executeUpdate();
                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update
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
    protected <T> int updateLiteral (Class<T> type, Comparable primaryKey, String... fieldsValues)
        throws PersistenceException
    {
        return updateLiteral(
            type, _ctx.getMarshaller(type).makePrimaryKey(primaryKey), fieldsValues);
    }

    /**
     * Updates the specified columns for all persistent objects matching the supplied primary
     * key. The values in this case must be literal SQL to be inserted into the update
     * statement. In general this is used when you want to do something like the following:
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
    protected <T> int updateLiteral (Class<T> type, Where key, String... fieldsValues)
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
        return _ctx.invoke(new Modifier(key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createLiteralUpdate(conn, _key, fields, values);
                try {
                    return stmt.executeUpdate();
                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Stores the supplied persisent object in the database. If it has no primary key assigned (it
     * is null or zero), it will be inserted directly. Otherwise an update will first be attempted
     * and if that matches zero rows, the object will be inserted.
     *
     * @return the number of rows modified by this action, this should always be one.
     */
    protected int store (final Object record)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(record.getClass());
        Key key = marsh.hasPrimaryKey() ? marsh.getPrimaryKey(record) : null;
        return _ctx.invoke(new Modifier(key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    // if our primary key is null or is the integer 0, assume the record has never
                    // before been persisted and insert
                    if (_key != null && !Integer.valueOf(0).equals(_key)) {
                        stmt = marsh.createUpdate(conn, record, _key);
                        int mods = stmt.executeUpdate();
                        if (mods > 0) {
                            return mods;
                        }
                        stmt.close();
                    }

                    // if the update modified zero rows or the primary key was obviously unset, do
                    // an insertion
                    updateKey(marsh.assignPrimaryKey(conn, record, false));
                    stmt = marsh.createInsert(conn, record);
                    int mods = stmt.executeUpdate();
                    updateKey(marsh.assignPrimaryKey(conn, record, true));
                    return mods;

                } finally {
                    stmt.close();
                }
            }
        });
    }

    /**
     * Deletes all persistent objects from the database with a primary key matching the primary key
     * of the supplied object.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T> int delete (T record)
        throws PersistenceException
    {
        @SuppressWarnings("unchecked") Class<T> type = (Class<T>)record.getClass();
        DepotMarshaller<T> marsh = _ctx.getMarshaller(type);
        return deleteAll(type, marsh.getPrimaryKey(record));
    }

    /**
     * Deletes all persistent objects from the database with a primary key matching the supplied
     * primary key.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T> int delete (Class<T> type, Comparable primaryKey)
        throws PersistenceException
    {
        return deleteAll(type, _ctx.getMarshaller(type).makePrimaryKey(primaryKey));
    }

    /**
     * Deletes all persistent objects from the database that match the supplied key.
     *
     * @return the number of rows deleted by this action.
     */
    protected <T> int deleteAll (Class<T> type, Where key)
        throws PersistenceException
    {
        final DepotMarshaller marsh = _ctx.getMarshaller(type);
        return _ctx.invoke(new Modifier(key) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = marsh.createDelete(conn, _key);
                try {
                    return stmt.executeUpdate();
                } finally {
                    stmt.close();
                }
            }
        });
    }

    protected static abstract class CollectionQuery<T extends Collection> extends Query<T>
    {
        public CollectionQuery (PersistenceContext ctx, Class type, Key key)
            throws PersistenceException
        {
            super(ctx, type, key);
        }

        public abstract T invoke (Connection conn) throws SQLException;
    }

    protected PersistenceContext _ctx;
}
