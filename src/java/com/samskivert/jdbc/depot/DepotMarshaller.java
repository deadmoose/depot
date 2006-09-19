//
// samskivert library - useful routines for java programs
// Copyright (C) 2001-2006 Michael Bayne
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.util.StringUtil;

import static com.samskivert.Log.log;

/**
 * Handles the marshalling and unmarshalling of persistent instances to JDBC
 * primitives ({@link PreparedStatement} and {@link ResultSet}).
 */
public class DepotMarshaller<T>
{
    /** The name of the private static field that must be defined for all
     * persistent object classes which is used to handle schema migration. If
     * automatic schema migration is not desired, define this field and set its
     * value to -1.  */
    public static final String SCHEMA_VERSION_FIELD = "SCHEMA_VERSION";

    /**
     * Creates a marshaller for the specified persistent object class.
     */
    public DepotMarshaller (Class<T> pclass)
    {
        _pclass = pclass;

        // determine our table name
        _tableName = _pclass.getName();
        _tableName = _tableName.substring(_tableName.lastIndexOf(".")+1);

        // introspect on the class and create marshallers for persistent fields
        ArrayList<String> fields = new ArrayList<String>();
        for (Field field : _pclass.getFields()) {
            int mods = field.getModifiers();

            // check for a static constant schema version
            if ((mods & Modifier.STATIC) != 0 &&
                field.getName().equals(SCHEMA_VERSION_FIELD)) {
                try {
                    _schemaVersion = (Integer)field.get(null);
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to read schema version " +
                        "[class=" + _pclass + "].", e);
                }
            }

            // the field must be public, non-static and non-transient
            if (((mods & Modifier.PUBLIC) == 0) ||
                ((mods & Modifier.STATIC) != 0) ||
                field.getAnnotation(Transient.class) != null) {
                continue;
            }

            FieldMarshaller fm = FieldMarshaller.createMarshaller(field);
            _fields.put(fm.getColumnName(), fm);
            fields.add(fm.getColumnName());

            // check to see if this is our primary key
            if (field.getAnnotation(Id.class) != null) {
                // TODO: handle multiple field primary keys
                _primaryKey = fm;
            }
        }

        // generate our full list of fields/columns for use in queries
        _allFields = fields.toArray(new String[fields.size()]);
        _fullColumnList = StringUtil.join(_allFields, ",");

        // create the SQL used to create and migrate our table
        _columnDefinitions = new String[_allFields.length];
        for (int ii = 0; ii < _allFields.length; ii++) {
            _columnDefinitions[ii] = 
                _fields.get(_allFields[ii]).getColumnDefinition();
        }
        _postamble = ""; // TODO: add annotations for the postamble

        // if we did not find a schema version field, complain
        if (_schemaVersion < 0) {
            log.warning("Unable to read " + _pclass.getName() + "." +
                SCHEMA_VERSION_FIELD + ". Schema migration disabled.");
        }
    }

    /**
     * Returns the name of the table in which persistence instances of this
     * class are stored. By default this is the classname of the persistent
     * object without the package.
     */
    public String getTableName ()
    {
        return _tableName;
    }

    /**
     * Returns a key configured with the primary key of the supplied object.
     * Throws an exception if the persistent object did not declare a primary
     * key.
     */
    public DepotRepository.Key getPrimaryKey (Object object)
    {
        if (_primaryKey == null) {
            throw new UnsupportedOperationException(
                getClass().getName() + " does not define a primary key");
        }
        try {
            return new DepotRepository.Key(_primaryKey.getColumnName(),
                (Comparable)_primaryKey.getField().get(object));
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }

    /**
     * Creates a primary key record for the type of object handled by this
     * marshaller, using the supplied primary key vlaue.
     */
    public DepotRepository.Key makePrimaryKey (Comparable value)
    {
        return new DepotRepository.Key(_primaryKey.getColumnName(), value);
    }

    /**
     * Initializes the table used by this marshaller. If the table does not
     * exist, it will be created. If the schema version specified by the
     * persistent object is newer than the database schema, it will be
     * migrated.
     */
    public void init (Connection conn)
        throws SQLException
    {
        // check to see if our schema version table exists, create it if not
        JDBCUtil.createTableIfMissing(conn, SCHEMA_VERSION_TABLE,
            new String[] { "persistentClass VARCHAR(255) NOT NULL",
                           "version INTEGER NOT NULL" },
            "");

        // now create the table for our persistent class if it does not exist
        if (!JDBCUtil.tableExists(conn, getTableName())) {
            JDBCUtil.createTableIfMissing(
                conn, getTableName(), _columnDefinitions, _postamble);
            // TODO: insert current version into version table
            return;
        }

        // if schema versioning is disabled, stop now
        if (_schemaVersion < 0) {
            return;
        }

        // otherwise, then make sure the versions match and do some migration
        // if they don't
        Statement stmt = conn.createStatement();
        try {
            // TODO: magical migration; execute registered migration actions
        } finally {
            stmt.close();
        }
    }

    /**
     * Creates a query for instances of this persistent object type using the
     * supplied key.
     */
    public PreparedStatement createQuery (
        Connection conn, DepotRepository.Key key)
        throws SQLException
    {
        String query = "select " + _fullColumnList + " from " + getTableName() +
            " where " + key.toWhereClause();
        PreparedStatement pstmt = conn.prepareStatement(query);
        key.bindArguments(pstmt, 1);
        return pstmt;
    }

    /**
     * Creates a persistent object from the supplied result set. The result set
     * must have come from a query provided by {@link #createQuery}.
     */
    public T createObject (ResultSet rs)
        throws SQLException
    {
        try {
            T po = (T)_pclass.newInstance();
            for (FieldMarshaller fm : _fields.values()) {
                fm.getValue(rs, po);
            }
            return po;

        } catch (SQLException sqe) {
            // pass this on through
            throw sqe;

        } catch (Exception e) {
            String errmsg = "Failed to unmarshall persistent object " +
                "[pclass=" + _pclass.getName() + "]";
            throw (SQLException)new SQLException(errmsg).initCause(e);
        }
    }

    /**
     * Creates a statement that will insert the supplied persistent object into
     * the database.
     */
    public PreparedStatement createInsert (Connection conn, Object po)
        throws SQLException
    {
        try {
            StringBuilder insert = new StringBuilder();
            insert.append("insert into ").append(getTableName());
            insert.append(" (").append(_fullColumnList).append(")");
            insert.append(" values(");
            for (int ii = 0; ii < _allFields.length; ii++) {
                if (ii > 0) {
                    insert.append(", ");
                }
                insert.append("?");
            }
            insert.append(")");

            // TODO: handle primary key, nullable fields specially?
            PreparedStatement pstmt = conn.prepareStatement(insert.toString());
            int idx = 0;
            for (String field :  _allFields) {
                _fields.get(field).setValue(po, pstmt, ++idx);
            }
            return pstmt;

        } catch (SQLException sqe) {
            // pass this on through
            throw sqe;

        } catch (Exception e) {
            String errmsg = "Failed to marshall persistent object " +
                "[pclass=" + _pclass.getName() + "]";
            throw (SQLException)new SQLException(errmsg).initCause(e);
        }
    }

    /**
     * Fills in the primary key just assigned to the supplied persistence
     * object by the execution of the results of {@link #createInsert}. The
     * structure of primary key assignment will probably have to change when we
     * support other databases.
     */
    public void assignPrimaryKey (Connection conn, Object po)
        throws SQLException
    {
        // no primary key, no problem!
        if (_primaryKey == null) {
            return;
        }

        // if the primary key is non-numeric, we can't auto-assign it
        Class<?> ftype = _primaryKey.getField().getType();
        if (!ftype.equals(Byte.TYPE) && !ftype.equals(Byte.class) &&
            !ftype.equals(Short.TYPE) && !ftype.equals(Short.class) &&
            !ftype.equals(Integer.TYPE) && !ftype.equals(Integer.class) &&
            !ftype.equals(Long.TYPE) && !ftype.equals(Long.class)) {
            return;
        }

        // load up the last inserted ID mysql style; eventually this will be
        // fancier and pluggable
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select LAST_INSERT_ID()");
            if (rs.next()) {
                _primaryKey.getField().set(po, rs.getInt(1));
            }

        } catch (IllegalAccessException iae) {
            String errmsg = "Failed to assign primary key " +
                "[type=" + _pclass + "]";
            throw (SQLException)new SQLException(errmsg).initCause(iae);

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Creates a statement that will update the supplied persistent object
     * using the supplied key.
     */
    public PreparedStatement createUpdate (
        Connection conn, Object po, DepotRepository.Key key)
        throws SQLException
    {
        return createUpdate(conn, po, key, _allFields);
    }

    /**
     * Creates a statement that will update the supplied persistent object
     * using the supplied key.
     */
    public PreparedStatement createUpdate (
        Connection conn, Object po, DepotRepository.Key key,
        String[] modifiedFields)
        throws SQLException
    {
        StringBuilder update = new StringBuilder();
        update.append("update ").append(getTableName()).append(" set ");
        int idx = 0;
        for (String field : modifiedFields) {
            if (idx++ > 0) {
                update.append(", ");
            }
            update.append(field).append(" = ?");
        }
        update.append(" where ").append(key.toWhereClause());

        try {
            PreparedStatement pstmt = conn.prepareStatement(update.toString());
            idx = 0;
            // bind the update arguments
            for (String field : modifiedFields) {
                _fields.get(field).setValue(po, pstmt, ++idx);
            }
            // now bind the key arguments
            key.bindArguments(pstmt, ++idx);
            return pstmt;

        } catch (SQLException sqe) {
            // pass this on through
            throw sqe;

        } catch (Exception e) {
            String errmsg = "Failed to marshall persistent object " +
                "[pclass=" + _pclass.getName() + "]";
            throw (SQLException)new SQLException(errmsg).initCause(e);
        }
    }

    /**
     * Creates a statement that will update the specified set of fields for all
     * persistent objects that match the supplied key.
     */
    public PreparedStatement createPartialUpdate (
        Connection conn, DepotRepository.Key key,
        String[] modifiedFields, Object[] modifiedValues)
        throws SQLException
    {
        StringBuilder update = new StringBuilder();
        update.append("update ").append(getTableName()).append(" set ");
        int idx = 0;
        for (String field : modifiedFields) {
            if (idx++ > 0) {
                update.append(", ");
            }
            update.append(field).append(" = ?");
        }
        update.append(" where ").append(key.toWhereClause());

        PreparedStatement pstmt = conn.prepareStatement(update.toString());
        idx = 0;
        // bind the update arguments
        for (Object value : modifiedValues) {
            // TODO: use the field marshaller?
            pstmt.setObject(++idx, value);
        }
        // now bind the key arguments
        key.bindArguments(pstmt, ++idx);
        return pstmt;
    }

    /**
     * Creates a statement that will update the specified set of fields, using
     * the supplied literal SQL values, for all persistent objects that match
     * the supplied key.
     */
    public PreparedStatement createLiteralUpdate (
        Connection conn, DepotRepository.Key key,
        String[] modifiedFields, Object[] modifiedValues)
        throws SQLException
    {
        StringBuilder update = new StringBuilder();
        update.append("update ").append(getTableName()).append(" set ");
        for (int ii = 0; ii < modifiedFields.length; ii++) {
            if (ii > 0) {
                update.append(", ");
            }
            update.append(modifiedFields[ii]).append(" = ");
            update.append(modifiedValues[ii]);
        }
        update.append(" where ").append(key.toWhereClause());

        PreparedStatement pstmt = conn.prepareStatement(update.toString());
        key.bindArguments(pstmt, 1);
        return pstmt;
    }

    /**
     * Creates a statement that will delete all rows matching the supplied key.
     */
    public PreparedStatement createDelete (
        Connection conn, DepotRepository.Key key)
        throws SQLException
    {
        String query = "delete from " + getTableName() +
            " where " + key.toWhereClause();
        PreparedStatement pstmt = conn.prepareStatement(query);
        key.bindArguments(pstmt, 1);
        return pstmt;
    }

    /** The persistent object class that we manage. */
    protected Class<T> _pclass;

    /** A field marshaller for each persistent field in our object. */
    protected HashMap<String, FieldMarshaller> _fields =
        new HashMap<String, FieldMarshaller>();

    /** The field marshaller for our persistent object's primary key or null if
     * it did not define a primary key. */
    protected FieldMarshaller _primaryKey;

    /** The name of our persistent object table. */
    protected String _tableName;

    /** The persisent fields of our object, in definition order, separated by
     * commas for easy use in a select statement. */
    protected String _fullColumnList;

    /** The persisent fields of our object, in definition order. */
    protected String[] _allFields;

    /** The version of our persistent object schema as specified in the class
     * definition. */
    protected int _schemaVersion = -1;

    /** Used when creating and migrating our table schema. */
    protected String[] _columnDefinitions;

    /** Used when creating and migrating our table schema. */
    protected String _postamble;

    /** The name of the table we use to track schema versions. */
    protected static final String SCHEMA_VERSION_TABLE = "DepotSchemaVersion";
}
