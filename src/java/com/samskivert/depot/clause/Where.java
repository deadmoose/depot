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

import java.util.Collection;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.depot.impl.expression.ValueExp;
import com.samskivert.depot.impl.operator.And;
import com.samskivert.depot.impl.operator.Equals;
import com.samskivert.depot.impl.operator.IsNull;

/**
 * Represents a where clause: the condition can be any comparison operator or logical combination
 * thereof.
 */
public class Where extends WhereClause
{
    public Where (ColumnExp column, Comparable<?> value)
    {
        this(new ColumnExp[] { column }, new Comparable<?>[] { value });
    }

    public Where (ColumnExp index1, Comparable<?> value1,
                ColumnExp index2, Comparable<?> value2)
    {
        this(new ColumnExp[] { index1, index2 }, new Comparable<?>[] { value1, value2 });
    }

    public Where (ColumnExp index1, Comparable<?> value1,
                ColumnExp index2, Comparable<?> value2,
                ColumnExp index3, Comparable<?> value3)
    {
        this(new ColumnExp[] { index1, index2, index3 },
             new Comparable<?>[] { value1, value2, value3 });
    }

    public Where (ColumnExp[] columns, Comparable<?>[] values)
    {
        this(toCondition(columns, values));
    }

    public Where (SQLExpression condition)
    {
        _condition = condition;
    }

    @Override // from WhereClause
    public SQLExpression getWhereExpression ()
    {
        return _condition;
    }

    // from SQLExpression
    public Object accept (ExpressionVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _condition.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        return String.valueOf(_condition);
    }

    protected static SQLExpression toCondition (ColumnExp[] columns, Comparable<?>[] values)
    {
        SQLExpression[] comparisons = new SQLExpression[columns.length];
        for (int ii = 0; ii < columns.length; ii ++) {
            comparisons[ii] = (values[ii] == null) ? new IsNull(columns[ii]) :
                new Equals(columns[ii], new ValueExp(values[ii]));
        }
        return new And(comparisons);
    }

    protected SQLExpression _condition;
}
