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

package com.samskivert.jdbc.depot.operator;

import java.util.Collection;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ExpressionVisitor;
import com.samskivert.jdbc.depot.expression.SQLExpression;

/**
 * A convenient container for implementations of logical operators.  Classes that value brevity
 * over disambiguation will import Logic.* and construct queries with And() and Not(); classes that
 * feel otherwise will use Logic.And() and Logic.Not().
 */
public abstract class Logic
{
    /**
     * Represents a condition that is false iff all its subconditions are false.
     */
    public static class Or extends SQLOperator.MultiOperator
    {
        public Or (SQLExpression... conditions)
        {
            super(conditions);
        }

        @Override
        public String operator()
        {
            return "or";
        }
    }

    /**
     * Represents a condition that is true iff all its subconditions are true.
     */
    public static class And extends SQLOperator.MultiOperator
    {
        public And (SQLExpression... conditions)
        {
            super(conditions);
        }

        @Override
        public String operator()
        {
            return "and";
        }
    }

    /**
     * Represents the truth negation of another conditon.
     */
    public static class Not
        implements SQLOperator
    {
        public Not (SQLExpression condition)
        {
            _condition = condition;
        }
        
        public SQLExpression getCondition ()
        {
            return _condition;
        }

        // from SQLExpression
        public void accept (ExpressionVisitor builder) throws Exception
        {
            builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _condition.addClasses(classSet);
        }
        
        protected SQLExpression _condition;
    }
}
