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

package com.samskivert.jdbc.depot.clause;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.samskivert.jdbc.depot.ConstructedQuery;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;

/**
 *  Represents an ORDER BY clause.
 */
public class OrderBy extends QueryClause
{
    /** Indicates the order of the clause. */
    public enum Order { ASC, DESC };

    /**
     * Creates and returns a random order by clause.
     */
    public static OrderBy random ()
    {
        return ascending(new LiteralExp("rand()"));
    }

    /**
     * Creates and returns an ascending order by clause on the supplied column.
     */
    public static OrderBy ascending (String column)
    {
        return ascending(new ColumnExp(null, column));
    }

    /**
     * Creates and returns a descending order by clause on the supplied column.
     */
    public static OrderBy descending (String column)
    {
        return descending(new ColumnExp(null, column));
    }

    /**
     * Creates and returns an ascending order by clause on the supplied expression.
     */
    public static OrderBy ascending (SQLExpression value)
    {
        return new OrderBy(new SQLExpression[] { value } , new Order[] { Order.ASC });
    }

    /**
     * Creates and returns a descending order by clause on the supplied expression.
     */
    public static OrderBy descending (SQLExpression value)
    {
        return new OrderBy(new SQLExpression[] { value }, new Order[] { Order.DESC });
    }

    public OrderBy (SQLExpression[] values, Order[] orders)
    {
        _values = values;
        _orders = orders;
    }

    // from QueryClause
    public void appendClause (ConstructedQuery<?> query, StringBuilder builder)
    {
        builder.append(" order by ");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            _values[ii].appendExpression(query, builder);
            builder.append(" ").append(_orders[ii]);
        }
    }

    // from QueryClause
    public int bindArguments (PreparedStatement pstmt, int argIdx)
        throws SQLException
    {
        for (int ii = 0; ii < _values.length; ii++) {
            argIdx = _values[ii].bindArguments(pstmt, argIdx);
        }
        return argIdx;
    }

    /** The expressions that are generated for the clause. */
    protected SQLExpression[] _values;

    /** Whether the ordering is to be ascending or descending. */
    protected Order[] _orders;
}
