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

package com.samskivert.jdbc.depot.clause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ExpressionVisitor;

/**
 *  Completely overrides the FROM clause, if it exists.
 */
public class FromOverride extends QueryClause
{
    public FromOverride (Class<? extends PersistentRecord> fromClass)
    {
        _fromClasses.add(fromClass);
    }

    public FromOverride (Class<? extends PersistentRecord> fromClass1,
                         Class<? extends PersistentRecord> fromClass2)
    {
        _fromClasses.add(fromClass1);
        _fromClasses.add(fromClass2);
    }

    public FromOverride (Collection<Class<? extends PersistentRecord>> fromClasses)
    {
        _fromClasses.addAll(fromClasses);
    }

    public List<Class<? extends PersistentRecord>> getFromClasses ()
    {
        return _fromClasses;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.addAll(getFromClasses());
    }

    // from SQLExpression
    public void accept (ExpressionVisitor builder)
        throws Exception
    {
        builder.visit(this);
    }

    /** The classes of the tables we're selecting from. */
    protected List<Class<? extends PersistentRecord>> _fromClasses =
        new ArrayList<Class<? extends PersistentRecord>>();
}
