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

package com.samskivert.depot.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines an index on an entity table.
 */
@Target(value={})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Index
{
    /** Defines the name of the index. */
    String name ();

    /** Does this index enforce a uniqueness constraint? */
    boolean unique () default false;

    /** Defines the fields on which the index operates. */
    String[] fields () default {};

    /** Whether or not this is a complex index. If true, a static method
     * must be defined on the record that declares this index of the signature:
     * <pre>public static List<Tuple<SQLExpression, OrderBy.Order>> indexNameExpression ()</pre>
     * which should return the index's defining expressions and whether each one is ascending
     * or descending. */
    boolean complex () default false;
}
