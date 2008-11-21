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

package com.samskivert.depot.clause;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.WhereClause;
import com.samskivert.depot.expression.ExpressionVisitor;

/**
 * Builds actual SQL given a main persistent type and some {@link QueryClause} objects.
 */
public class SelectClause<T extends PersistentRecord> extends QueryClause
{
    /**
     * Creates a new Query object to generate one or more instances of the specified persistent
     * class, as dictated by the key and query clauses.  A persistence context is supplied for
     * instantiation of marshallers, which may trigger table creations and schema migrations.
     */
    public SelectClause (Class<T> pClass, String[] fields, Collection<? extends QueryClause> clauses)
    {
        _pClass = pClass;
        _fields = fields;

        // iterate over the clauses and sort them into the different types we understand
        for (QueryClause clause : clauses) {
            if (clause == null) {
                continue;
            }
            if (clause instanceof WhereClause) {
                if (_where != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple Where clauses.");
                }
                _where = (WhereClause) clause;

            } else if (clause instanceof FromOverride) {
                if (_fromOverride != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple FromOverride clauses.");
                }
                _fromOverride = (FromOverride) clause;

            } else if (clause instanceof Join) {
                _joinClauses.add((Join) clause);

            } else if (clause instanceof FieldDefinition) {
                _disMap.put(((FieldDefinition) clause).getField(), ((FieldDefinition) clause));

            } else if (clause instanceof OrderBy) {
                if (_orderBy != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple OrderBy clauses.");
                }
                _orderBy = (OrderBy) clause;

            } else if (clause instanceof GroupBy) {
                if (_groupBy != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple GroupBy clauses.");
                }
                _groupBy = (GroupBy) clause;

            } else if (clause instanceof Limit) {
                if (_limit != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple Limit clauses.");
                }
                _limit = (Limit) clause;

            } else if (clause instanceof ForUpdate) {
                if (_forUpdate != null) {
                    throw new IllegalArgumentException(
                        "Query can't contain multiple For Update clauses.");
                }
                _forUpdate = (ForUpdate) clause;

            } else {
                throw new IllegalArgumentException(
                    "Unknown clause provided in select " + clause + ".");
            }
        }
    }

    /**
     * A varargs version of the constructor.
     */
    public SelectClause (Class<T> pClass, String[] fields, QueryClause... clauses)
    {
        this(pClass, fields, Arrays.asList(clauses));
    }

    public FieldDefinition lookupDefinition (String field)
    {
        return _disMap.get(field);
    }

    public Collection<FieldDefinition> getFieldDefinitions ()
    {
        return _disMap.values();
    }

    public Class<T> getPersistentClass ()
    {
        return _pClass;
    }

    public String[] getFields ()
    {
        return _fields;
    }

    public FromOverride getFromOverride ()
    {
        return _fromOverride;
    }

    public WhereClause getWhereClause ()
    {
        return _where;
    }

    public List<Join> getJoinClauses ()
    {
        return _joinClauses;
    }

    public OrderBy getOrderBy ()
    {
        return _orderBy;
    }

    public GroupBy getGroupBy ()
    {
        return _groupBy;
    }

    public Limit getLimit ()
    {
        return _limit;
    }

    public ForUpdate getForUpdate ()
    {
        return _forUpdate;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);

        if (_fromOverride != null) {
            _fromOverride.addClasses(classSet);
        }
        if (_where != null) {
            _where.addClasses(classSet);
        }
        for (Join join : _joinClauses) {
            join.addClasses(classSet);
        }
        for (FieldDefinition override : _disMap.values()) {
            override.addClasses(classSet);
        }
    }

    // from SQLExpression
    public void accept (ExpressionVisitor builder)
    {
        builder.visit(this);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("(where=").append(_where);
        if (_fromOverride != null) {
            builder.append(", from=").append(_fromOverride);
        }
        if (!_joinClauses.isEmpty()) {
            builder.append(", join=").append(_joinClauses);
        }
        if (_orderBy != null) {
            builder.append(", orderBy=").append(_orderBy);
        }
        if (_groupBy != null) {
            builder.append(", groupBy=").append(_groupBy);
        }
        if (_limit != null) {
            builder.append(", limit=").append(_limit);
        }
        if (_forUpdate != null) {
            builder.append(", forUpdate=").append(_forUpdate);
        }
        return builder.append(")").toString();
    }

    /** Persistent class fields mapped to field override clauses. */
    protected Map<String, FieldDefinition> _disMap = Maps.newHashMap();

    /** The persistent class this select defines. */
    protected Class<T> _pClass;

    /** The persistent fields to select. */
    protected String[] _fields;

    /** The from override clause, if any. */
    protected FromOverride _fromOverride;

    /** The where clause. */
    protected WhereClause _where;

    /** A list of join clauses, each potentially referencing a new class. */
    protected List<Join> _joinClauses = Lists.newArrayList();

    /** The order by clause, if any. */
    protected OrderBy _orderBy;

    /** The group by clause, if any. */
    protected GroupBy _groupBy;

    /** The limit clause, if any. */
    protected Limit _limit;

    /** The For Update clause, if any. */
    protected ForUpdate _forUpdate;
}
