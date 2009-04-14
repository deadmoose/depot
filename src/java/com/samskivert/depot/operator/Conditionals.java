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

package com.samskivert.depot.operator;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.impl.ExpressionVisitor;
import com.samskivert.util.Tuple;

import static com.samskivert.Log.log;

/**
 * A convenient container for implementations of conditional operators.  Classes that value brevity
 * classes that feel otherwise will use Conditionals.Equals() and Conditionals.In().
 */
public abstract class Conditionals
{
    /** The SQL 'is null' operator. */
    public static class IsNull
        implements SQLOperator
    {
        public IsNull (ColumnExp column)
        {
            _column = column;
        }

        public ColumnExp getColumn()
        {
            return _column;
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
        }

        @Override // from Object
        public String toString ()
        {
            return "IsNull(" + _column + ")";
        }

        protected ColumnExp _column;
    }

    /** The SQL '=' operator. */
    public static class Equals extends SQLOperator.BinaryOperator
    {
        public Equals (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public Equals (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return "=";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (left == null || right == null) {
                return new NoValue("Null operand to '=': (" + left + ", " + right + ")");
            }
            return left.equals(right);
        }
    }

    /** The SQL '!=' operator. */
    public static class NotEquals extends SQLOperator.BinaryOperator
    {
        public NotEquals (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public NotEquals (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return "!=";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (left == null || right == null) {
                return new NoValue("Null operand to '!=': (" + left + ", " + right + ")");
            }
            return !left.equals(right);
        }
    }

    /** The SQL '<' operator. */
    public static class LessThan extends SQLOperator.BinaryOperator
    {
        public LessThan (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public LessThan (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return "<";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (all(NUMERICAL, left, right)) {
                return NUMERICAL.apply(left) < NUMERICAL.apply(right);
            }
            if (all(STRING, left, right) || all(DATE, left, right)) {
                return compare(STRING, left, right) < 0;
            }
            return new NoValue("Non-comparable operand to '<': (" + left + ", " + right + ")");
        }
    }

    /** The SQL '<=' operator. */
    public static class LessThanEquals extends SQLOperator.BinaryOperator
    {
        public LessThanEquals (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public LessThanEquals (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return "<=";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (all(NUMERICAL, left, right)) {
                return NUMERICAL.apply(left) <= NUMERICAL.apply(right);
            }
            if (all(STRING, left, right) || all(DATE, left, right)) {
                return compare(STRING, left, right) <= 0;
            }
            return new NoValue("Non-comparable operand to '<=': (" + left + ", " + right + ")");
        }
}

    /** The SQL '>' operator. */
    public static class GreaterThan extends SQLOperator.BinaryOperator
    {
        public GreaterThan (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public GreaterThan (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return ">";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (all(NUMERICAL, left, right)) {
                return NUMERICAL.apply(left) > NUMERICAL.apply(right);
            }
            if (all(STRING, left, right) || all(DATE, left, right)) {
                return compare(STRING, left, right) > 0;
            }
            return new NoValue("Non-comparable operand to '>': (" + left + ", " + right + ")");
        }
    }

    /** The SQL '>=' operator. */
    public static class GreaterThanEquals extends SQLOperator.BinaryOperator
    {
        public GreaterThanEquals (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public GreaterThanEquals (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return ">=";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            if (all(NUMERICAL, left, right)) {
                return NUMERICAL.apply(left) >= NUMERICAL.apply(right);
            }
            if (all(STRING, left, right) || all(DATE, left, right)) {
                return compare(STRING, left, right) >= 0;
            }
            return new NoValue("Non-comparable operand to '>=': (" + left + ", " + right + ")");
        }
    }

    /** The SQL 'in (...)' operator. */
    public static class In
        implements SQLOperator
    {
        /** The maximum number of keys allowed in an IN() clause. */
        public static final int MAX_KEYS = Short.MAX_VALUE;

        public In (ColumnExp column, Comparable<?>... values)
        {
            if (values.length == 0) {
                log.warning("Grouchily allowing empty In() operator", "column", column.name,
                            new Exception());
            }
            _column = column;
            _values = values;
        }

        public In (ColumnExp pColumn, Collection<? extends Comparable<?>> values)
        {
            this(pColumn, values.toArray(new Comparable<?>[values.size()]));
        }

        public ColumnExp getColumn ()
        {
            return _column;
        }

        public Comparable<?>[] getValues ()
        {
            return _values;
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _column.addClasses(classSet);
        }

        @Override // from Object
        public String toString ()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(_column).append(" in (");
            for (int ii = 0; ii < _values.length; ii++) {
                if (ii > 0) {
                    builder.append(", ");
                }
                builder.append(_values[ii]);
            }
            return builder.append(")").toString();
        }

        protected ColumnExp _column;
        protected Comparable<?>[] _values;
    }

    /** The SQL ' like ' operator. */
    public static class Like extends SQLOperator.BinaryOperator
    {
        public Like (SQLExpression column, Comparable<?> value)
        {
            super(column, value);
        }

        public Like (SQLExpression column, SQLExpression value)
        {
            super(column, value);
        }

        @Override public String operator()
        {
            return " like ";
        }

        @Override
        public Object evaluate (Object left, Object right)
        {
            return new NoValue("Like operator not implemented");
        }
    }

    /** The SQL ' exists' operator. */
    public static class Exists<T extends PersistentRecord> implements SQLOperator
    {
        public Exists (SelectClause<T> clause)
        {
            _clause = clause;
        }

        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            _clause.addClasses(classSet);
        }

        public SelectClause<T> getSubClause ()
        {
            return _clause;
        }

        @Override // from Object
        public String toString ()
        {
            return "Exists(" + _clause + ")";
        }

        protected SelectClause<T> _clause;
    }

    public static class Case implements SQLOperator
    {
        public Case (SQLExpression... exps)
        {
            int i = 0;
            while (i+1 < exps.length) {
                _whenExps.add(Tuple.newTuple(exps[i], exps[i+1]));
                i += 2;
            }
            _elseExp = (i < exps.length) ? exps[i] : null;
        }

        // from SQLExpression
        public Object accept (ExpressionVisitor<?> builder)
        {
            return builder.visit(this);
        }

        // from SQLExpression
        public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
        {
            for (Tuple<SQLExpression, SQLExpression> tuple : _whenExps) {
                tuple.left.addClasses(classSet);
                tuple.right.addClasses(classSet);
            }
            if (_elseExp != null) {
                _elseExp.addClasses(classSet);
            }
        }

        public List<Tuple<SQLExpression, SQLExpression>> getWhenExps ()
        {
            return _whenExps;
        }

        public SQLExpression getElseExp ()
        {
            return _elseExp;
        }

        @Override // from Object
        public String toString ()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Case(");
            for (Tuple<SQLExpression, SQLExpression> tuple : _whenExps) {
                builder.append(tuple.left.toString()).append("->");
                builder.append(tuple.right.toString()).append(",");
            }
            if (_elseExp != null) {
                builder.append(_elseExp.toString()).append(")");
            }
            return builder.toString();
        }

        protected List<Tuple<SQLExpression, SQLExpression>> _whenExps = Lists.newArrayList();
        protected SQLExpression _elseExp;
    }

    /**
     * An attempt at a dialect-agnostic full-text search condition, such as MySQL's MATCH() and
     * PostgreSQL's @@ TO_TSQUERY(...) abilities.
     */
    public static class FullText
    {
        public class Rank
            implements SQLOperator
        {
           // from SQLExpression
            public Object accept (ExpressionVisitor<?> builder)
            {
                return builder.visit(this);
            }

            // from SQLExpression
            public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
            {
            }

            @Override // from Object
            public String toString ()
            {
                return FullText.this.toString("Rank");
            }

            public FullText getDefinition ()
            {
                return FullText.this;
            }
        }

        public class Match
            implements SQLOperator
        {
            // from SQLExpression
            public Object accept (ExpressionVisitor<?> builder)
            {
                return builder.visit(this);
            }

            public FullText getDefinition ()
            {
                return FullText.this;
            }

            // from SQLExpression
            public void addClasses (Collection<Class<? extends PersistentRecord>> classSet)
            {
            }

            @Override // from Object
            public String toString ()
            {
                return FullText.this.toString("Match");
            }
        }

        public FullText (Class<? extends PersistentRecord> pClass, String name, String query)
        {
            _pClass = pClass;
            _name = name;
            _query = query;
        }

        public SQLOperator match ()
        {
            return new Match();
        }

        public SQLOperator rank ()
        {
            return new Rank();
        }

        public Class<? extends PersistentRecord> getPersistentClass ()
        {
            return _pClass;
        }

        public String getName ()
        {
            return _name;
        }

        public String getQuery ()
        {
            return _query;
        }

        protected String toString (String subType)
        {
            return "FullText." + subType + "(" + _name + "=" + _query + ")";
        }

        protected Class<? extends PersistentRecord> _pClass;
        protected String _name;
        protected String _query;
    }
}
