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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Is used to specify a mapped column for a persistent property or field. If no Column annotation
 * is specified, the default values are applied.
 */
@Target(value=ElementType.FIELD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Column
{
    /**
     * The name of the column. Defaults to the field name.
     */
    String name () default "";

    /**
     * Whether the property is a unique key. This is a shortcut for the UniqueConstraint annotation
     * at the table level and is useful for when the unique key constraint is only a single
     * field. This constraint applies in addition to any constraint entailed by primary key mapping
     * and to constraints specified at the table level.
     */
    boolean unique () default false;

    /**
     * Whether the database column is nullable. <em>Note:</em> this default differs from the value
     * used by the EJB3 persistence framework.
     */
    boolean nullable () default false;

    /**
     * The column length. (Applies to String and byte[] columns.)
     */
    int length () default 255;

    /**
     * The SQL literal value to be used when defining this column's default value. The value must
     * be quoted and escaped if it is not a SQL primitive datatype. For example:
     * <code>'2006-01-01'</code> or <code>25</code> or <code>NULL</code>.
     */
    String defaultValue () default "";
}
