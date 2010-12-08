//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import com.samskivert.depot.clause.*;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

/**
 * The root of a fluent mechanism for constructing queries. Obtain an instance via {@link
 * DepotRepository#from}.
 */
public class QueryBuilder<T extends PersistentRecord>
{
    public QueryBuilder (DepotRepository repo, Class<T> pclass)
    {
        _repo = repo;
        _pclass = pclass;
    }

    /** Disables the use of the cache for this query. */
    public QueryBuilder<T> noCache () {
        return cache(DepotRepository.CacheStrategy.BEST);
    }

    /** Configures the use of {@link CacheStrategy#BEST} for this query. */
    public QueryBuilder<T> cacheBest () {
        return cache(DepotRepository.CacheStrategy.BEST);
    }

    /** Configures the use of {@link CacheStrategy#RECORDS} for this query. */
    public QueryBuilder<T> cacheRecords () {
        return cache(DepotRepository.CacheStrategy.RECORDS);
    }

    /** Configures the use of {@link CacheStrategy#SHORT_KEYS} for this query. */
    public QueryBuilder<T> cacheShortKeys () {
        return cache(DepotRepository.CacheStrategy.SHORT_KEYS);
    }

    /** Configures the use of {@link CacheStrategy#LONG_KEYS} for this query. */
    public QueryBuilder<T> cacheLongKeys () {
        return cache(DepotRepository.CacheStrategy.LONG_KEYS);
    }

    /** Configures the use of {@link CacheStrategy#CONTENTS} for this query. */
    public QueryBuilder<T> cacheContents () {
        return cache(DepotRepository.CacheStrategy.CONTENTS);
    }

    /** Configures the specified caching policy this query. */
    public QueryBuilder<T> cache (DepotRepository.CacheStrategy cache) {
        _cache = cache;
        return this;
    }

    /**
     * Configures a {@link Where} clause that ANDs together all of the supplied expressions.
     */
    public QueryBuilder<T> where (SQLExpression... exprs)
    {
        return where(Arrays.asList(exprs));
    }

    /**
     * Configures a {@link Where} clause that ANDs together all of the supplied expressions.
     */
    public QueryBuilder<T> where (Iterable<? extends SQLExpression> exprs)
    {
        Iterator<? extends SQLExpression> iter = exprs.iterator();
        if (!iter.hasNext()) {
            throw new IllegalArgumentException("Must supply at least one expression.");
        }
        SQLExpression first = iter.next();
        return where(iter.hasNext() ? new Where(Ops.and(exprs)) : new Where(first));
    }

    /**
     * Configures a {@link Where} clause that selects rows where the supplied column equals the
     * supplied value.
     */
    public QueryBuilder<T> where (ColumnExp column, Comparable<?> value)
    {
        return where(new Where(column, value));
    }

    /**
     * Configures a {@link Where} clause that selects rows where both supplied columns equal both
     * supplied values.
     */
    public QueryBuilder<T> where (ColumnExp index1, Comparable<?> value1,
                                  ColumnExp index2, Comparable<?> value2)
    {
        return where(new Where(index1, value1, index2, value2));
    }

    /**
     * Configures a {@link Where} clause that selects rows where both supplied columns equal both
     * supplied values.
     */
    public QueryBuilder<T> where (WhereClause where)
    {
        requireNull(_where, "Where clause is already configured.");
        _where = where;
        return this;
    }

    /**
     * Configures a {@link Join} clause configured with the supplied left and right columns.
     */
    public QueryBuilder<T> join (ColumnExp left, ColumnExp right)
    {
        return join(new Join(left, right));
    }

    /**
     * Configures a {@link Join} clause configured with the join condition.
     */
    public QueryBuilder<T> join (Class<? extends PersistentRecord> joinClass,
                                 SQLExpression joinCondition)
    {
        return join(new Join(joinClass, joinCondition));
    }

    /**
     * Configures a {@link Join} clause configured with the supplied left and right columns and
     * join type.
     */
    public QueryBuilder<T> join (ColumnExp left, ColumnExp right, Join.Type type)
    {
        return join(new Join(left, right).setType(type));
    }

    /**
     * Configures the query with the supplied {@link Join} clause.
     */
    public QueryBuilder<T> join (Join join)
    {
        requireNull(_join, "Join clause is already configured.");
        _join = join;
        return this;
    }

    /**
     * Configures a {@link GroupBy} clause on the supplied group expressions.
     */
    public QueryBuilder<T> groupBy (SQLExpression... exprs)
    {
        requireNull(_groupBy, "GroupBy clause is already configured.");
        _groupBy = new GroupBy(exprs);
        return this;
    }

    /**
     * Configures an {@link OrderBy} clause configured to randomly order the results.
     */
    public QueryBuilder<T> randomOrder ()
    {
        return orderBy(OrderBy.random());
    }

    /**
     * Configures an {@link OrderBy} clause that ascends on the supplied expression.
     */
    public QueryBuilder<T> ascending (SQLExpression value)
    {
        return orderBy(OrderBy.ascending(value));
    }

    /**
     * Configures an {@link OrderBy} clause that descends on the supplied expression.
     */
    public QueryBuilder<T> descending (SQLExpression value)
    {
        return orderBy(OrderBy.descending(value));
    }

    /**
     * Configures the query with the supplied {@link OrderBy} clause.
     */
    public QueryBuilder<T> orderBy (OrderBy orderBy)
    {
        requireNull(_orderBy, "OrderBy clause is already configured.");
        _orderBy = orderBy;
        return this;
    }

    /**
     * Configures a {@link Limit} clause configured with the supplied count.
     */
    public QueryBuilder<T> limit (int count)
    {
        requireNull(_limit, "Limit clause is already configured.");
        _limit = new Limit(0, count);
        return this;
    }

    /**
     * Configures a {@link Limit} clause configured with the supplied offset and count.
     */
    public QueryBuilder<T> limit (int offset, int count)
    {
        requireNull(_limit, "Limit clause is already configured.");
        _limit = new Limit(offset, count);
        return this;
    }

    /**
     * Configures a {@link FromOverride} clause configured with the supplied override class.
     */
    public QueryBuilder<T> override (Class<? extends PersistentRecord> fromClass)
    {
        return override(new FromOverride(fromClass));
    }

    /**
     * Configures a {@link FromOverride} clause configured with the supplied override classes.
     */
    public QueryBuilder<T> override (Class<? extends PersistentRecord> fromClass1,
                                     Class<? extends PersistentRecord> fromClass2)
    {
        return override(new FromOverride(fromClass1, fromClass2));
    }

    /**
     * Configures the query with the supplied {@link FromOverride} clause.
     */
    public QueryBuilder<T> override (FromOverride fromOverride)
    {
        requireNull(_fromOverride, "FromOverride clause is already configured.");
        _fromOverride = fromOverride;
        return this;
    }

    /**
     * Configures a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (String field, String value)
    {
        return fieldDef(new FieldDefinition(field, value));
    }

    /**
     * Configures a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (String field, SQLExpression override)
    {
        return fieldDef(new FieldDefinition(field, override));
    }

    /**
     * Configures a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (ColumnExp field, SQLExpression override)
    {
        return fieldDef(new FieldDefinition(field, override));
    }

    /**
     * Configures a {@link FieldDefinition} clause.
     */
    public QueryBuilder<T> fieldDef (FieldDefinition fieldDef)
    {
        if (_fieldDefs == null) {
            _fieldDefs = Lists.newArrayList();
        }
        _fieldDefs.add(fieldDef);
        return this;
    }

    /**
     * Configures a {@link ForUpdate} clause which marks this query as selecting for update.
     */
    public QueryBuilder<T> forUpdate ()
    {
        requireNull(_forUpdate, "ForUpdate clause is already configured.");
        _forUpdate = new ForUpdate();
        return this;
    }

    /**
     * Loads the first persistent object that matches the configured query clauses.
     */
    public T load ()
    {
        return _repo.load(_pclass, _cache, getClauseArray());
    }

    /**
     * Loads all persistent objects that match the configured query clauses.
     */
    public List<T> select ()
        throws DatabaseException
    {
        return _repo.findAll(_pclass, _cache, getClauses());
    }

    /**
     * Loads the keys of all persistent objects that match the configured query clauses. Note that
     * cache configuration is ignored for key-only queries.
     *
     * @param useMaster if true, the query will be run using a read-write connection to ensure that
     * it talks to the master database, if false, the query will be run on a read-only connection
     * and may load keys from a slave. For performance reasons, you should always pass false unless
     * you know you will be modifying the database as a result of this query and absolutely need
     * the latest data.
     */
    public List<Key<T>> selectKeys (boolean useMaster)
        throws DatabaseException
    {
        return _repo.findAllKeys(_pclass, useMaster, getClauses());
    }

    /**
     * Returns the count of rows that match the configured query clauses.
     */
    public int selectCount ()
    {
        _fromOverride = new FromOverride(_pclass);
        return _repo.load(CountRecord.class, _cache, getClauseArray()).count;
    }

    /**
     * Deletes the records that match the configured query clauses. Note that only the where
     * clauses are used to evaluate a deletion. Attempts to use other clauses will result in
     * failure.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public int delete ()
    {
        assertValidDelete();
        return _repo.deleteAll(_pclass, _where);
    }

    /**
     * Deletes the records that match the configured query clauses. The supplied cache invalidator
     * is used to remove deleted records from the cache.
     *
     * @return the number of rows deleted by this action.
     *
     * @throws DatabaseException if any problem is encountered communicating with the database.
     */
    public int delete (CacheInvalidator invalidator)
    {
        assertValidDelete();
        return _repo.deleteAll(_pclass, _where, invalidator);
    }

    protected List<QueryClause> getClauses ()
    {
        List<QueryClause> clauses = Lists.newArrayList();
        addIfNotNull(clauses, _where);
        addIfNotNull(clauses, _join);
        addIfNotNull(clauses, _orderBy);
        addIfNotNull(clauses, _groupBy);
        addIfNotNull(clauses, _limit);
        addIfNotNull(clauses, _fromOverride);
        if (_fieldDefs != null) {
            clauses.addAll(_fieldDefs);
        }
        addIfNotNull(clauses, _forUpdate);
        return clauses;
    }

    protected QueryClause[] getClauseArray ()
    {
        List<QueryClause> clauses = getClauses();
        return clauses.toArray(new QueryClause[clauses.size()]);
    }

    protected void addIfNotNull (List<QueryClause> clauses, QueryClause clause)
    {
        if (clause != null) {
            clauses.add(clause);
        }
    }

    protected void requireNotNull (Object value, String message)
    {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    protected void requireNull (Object value, String message)
    {
        if (value != null) {
            throw new AssertionError(message);
        }
    }

    protected void assertValidDelete ()
    {
        requireNotNull(_where, "Where clause must be specified for delete.");
        requireNull(_join, "Join clause not supported by delete.");
        requireNull(_orderBy, "OrderBy clause not applicable for delete.");
        requireNull(_groupBy, "GroupBy clause not applicable for delete.");
        requireNull(_limit, "Limit clause not supported by delete.");
        requireNull(_fromOverride, "FromOverride clause not applicable for delete.");
        requireNull(_fieldDefs, "FieldDefinition clauses not applicable for delete.");
        requireNull(_forUpdate, "ForUpdate clause not supported by delete.");
    }

    protected final DepotRepository _repo;
    protected final Class<T> _pclass;

    protected DepotRepository.CacheStrategy _cache = DepotRepository.CacheStrategy.BEST;

    protected WhereClause _where;
    protected Join _join;
    protected OrderBy _orderBy;
    protected GroupBy _groupBy;
    protected Limit _limit;
    protected FromOverride _fromOverride;
    protected ForUpdate _forUpdate;
    protected List<FieldDefinition> _fieldDefs;
}
