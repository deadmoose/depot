//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2009 Michael Bayne and Pär Winzell
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

package com.samskivert.depot.impl.operator;

import java.util.Collection;

import com.google.common.collect.Iterables;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.FragmentVisitor;

/**
 * The SQL 'in (...)' operator.
 */
public class In
    implements SQLExpression<Boolean>
{
    /** The maximum number of keys allowed in an IN() clause. */
    public static final int MAX_KEYS = Short.MAX_VALUE;

    public In (SQLExpression<?> expression, Comparable<?>... values)
    {
        _expression = expression;
        _values = values;
    }

    public In (SQLExpression<?> pColumn, Iterable<? extends Comparable<?>> values)
    {
        this(pColumn, Iterables.toArray(values, Comparable.class));
    }

    public SQLExpression<?> getExpression ()
    {
        return _expression;
    }

    public Comparable<?>[] getValues ()
    {
        return _values;
    }

    // from SQLFragment
    public Object accept (FragmentVisitor<?> builder)
    {
        return builder.visit(this);
    }

    // from SQLFragment
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        _expression.addClasses(classSet);
    }

    @Override // from Object
    public String toString ()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(_expression).append(" in (");
        for (int ii = 0; ii < _values.length; ii++) {
            if (ii > 0) {
                builder.append(", ");
            }
            builder.append((_values[ii] instanceof Number) ?
                String.valueOf(_values[ii]) : ("'" + _values[ii] + "'"));
        }
        return builder.append(")").toString();
    }

    protected SQLExpression<?> _expression;
    protected Comparable<?>[] _values;
}
