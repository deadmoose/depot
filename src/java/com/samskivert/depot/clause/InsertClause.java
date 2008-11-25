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
import java.util.Set;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.impl.ExpressionVisitor;

/**
 * Builds actual SQL given a main persistent type and some {@link QueryClause} objects.
 */
public class InsertClause<T extends PersistentRecord> implements QueryClause
{
    public InsertClause (
        Class<? extends PersistentRecord> pClass, Object pojo, Set<String> identityFields)
    {
        _pClass = pClass;
        _pojo = pojo;
        _idFields = identityFields;
    }

    public Class<? extends PersistentRecord> getPersistentClass ()
    {
        return _pClass;
    }

    public Object getPojo ()
    {
        return _pojo;
    }

    public Set<String> getIdentityFields ()
    {
        return _idFields;
    }

    // from SQLExpression
    public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
    {
        classSet.add(_pClass);
        // If we add SQLExpression[] values INSERT, remember to recurse into them here.
    }

    // from SQLExpression
    public void accept (ExpressionVisitor builder)
    {
        builder.visit(this);
    }

    protected Class<? extends PersistentRecord> _pClass;

    /** The object from which to fetch values, or null. */
    protected Object _pojo;

    protected Set<String> _idFields;
}
