//
// $Id: Exps.java 505 2009-08-07 01:58:58Z samskivert $
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

package com.samskivert.depot;

import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.function.AggregateFun.*;
import com.samskivert.depot.function.ConditionalFun.*;
import com.samskivert.depot.function.DateFun.*;
import com.samskivert.depot.function.DateFun.DatePart.Part;
import com.samskivert.depot.function.DateFun.DateTruncate.Truncation;
import com.samskivert.depot.function.NumericalFun.*;
import com.samskivert.depot.function.StringFun.*;

/**
 * Provides static methods for function construction. For example: {@link #round}, {@link
 * #count} and {@link #length}.
 */
public class Funcs
{
    /**
     * Creates an expression that computes the absolute value of the supplied expression.
     */
    public static FluentExp abs (SQLExpression expr)
    {
        return new Abs(expr);
    }

    /**
     * Creates an expression that computes the integer ceiling of the supplied expression.
     */
    public static FluentExp ceil (SQLExpression exp)
    {
        return new Ceil(exp);
    }

    /**
     * Creates an expression that computes the exponential of the supplied expression.
     */
    public static FluentExp exp (SQLExpression exp)
    {
        return new Exp(exp);
    }

    /**
     * Creates an expression that computes the integer floor of the supplied expression.
     */
    public static FluentExp floor (SQLExpression exp)
    {
        return new Floor(exp);
    }

    /**
     * Creates an expression that computes the natural logarithm of the supplied expression.
     */
    public static FluentExp ln (SQLExpression exp)
    {
        return new Ln(exp);
    }

    /**
     * Creates an expression that computes the base-10 logarithm of the supplied expression.
     */
    public static FluentExp log10 (SQLExpression value)
    {
        return new Log10(value);
    }

    /**
     * Creates an expression that evaluates to the constant PI.
     */
    public static FluentExp pi ()
    {
        return new Pi();
    }

    /**
     * Creates an expression that computes the value expression to the given power.
     */
    public static FluentExp power (SQLExpression value, SQLExpression power)
    {
        return new Power(value, power);
    }

    /**
     * Creates an expression that returns a random number between 0.0 and 1.0.
     */
    public static FluentExp random ()
    {
        return new Random();
    }

    /**
     * Creates an expression that computes the whole number nearest the supplied expression.
     */
    public static FluentExp round (SQLExpression exp)
    {
        return new Round(exp);
    }

    /**
     * Creates an expression that computes the sign of the supplied expression.
     */
    public static FluentExp sign (SQLExpression exp)
    {
        return new Sign(exp);
    }

    /**
     * Creates an expression that computes the square root of the supplied expression.
     */
    public static FluentExp sqrt (SQLExpression exp)
    {
        return new Sqrt(exp);
    }

    /**
     * Creates an expression that computes the truncation of the supplied expression,
     * i.e. the next closest whole number to zero.
     */
    public static FluentExp trunc (SQLExpression exp)
    {
        return new Trunc(exp);
    }

    /**
     * Creates an expression that evaluates to the string length of the supplied expression.
     */
    public static FluentExp length (SQLExpression exp)
    {
        return new Length(exp);
    }

    /**
     * Creates an expression that down-cases the supplied expression.
     */
    public static FluentExp lower (SQLExpression exp)
    {
        return new Lower(exp);
    }

    /**
     * Creates an expression that locates the given substring expression within the given
     * string expression and returns the index.
     */
    public static FluentExp position (SQLExpression substring, SQLExpression string)
    {
        return new Position(substring, string);
    }

    /**
     * Creates an expression that evaluates to a substring of the given string expression,
     * starting at the given index and of the given length.
     */
    public static FluentExp substring (
        SQLExpression string, SQLExpression from, SQLExpression count)
    {
        return new Substring(string, from, count);
    }

    /**
     * Creates an expression that removes whitespace from the beginning and end of the supplied
     * string expression.
     */
    public static FluentExp trim (SQLExpression exp)
    {
        return new Trim(exp);
    }

    /**
     * Creates an expression that up-cases the supplied string expression.
     */
    public static FluentExp upper (SQLExpression exp)
    {
        return new Upper(exp);
    }

    /**
     * Creates an expression that truncates the given timestamp expression to a date.
     */
    public static FluentExp truncToDay (SQLExpression exp)
    {
        return new DateTruncate(exp, Truncation.DAY);
    }

    /**
     * Creates an expression to extract the day-of-month from the the supplied timestamp expression.
     */
    public static FluentExp dayOfMonth (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_MONTH);
    }

    /**
     * Creates an expression to extract the day-of-week from the the supplied timestamp expression.
     */
    public static FluentExp dayOfWeek (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_WEEK);
    }

    /**
     * Creates an expression to extract the day-of-year from the the supplied timestamp expression.
     */
    public static FluentExp dayOfYear (SQLExpression exp)
    {
        return new DatePart(exp, Part.DAY_OF_YEAR);
    }

    /**
     * Creates an expression to extract the hour of the the supplied timestamp expression.
     */
    public static FluentExp dateHour (SQLExpression exp)
    {
        return new DatePart(exp, Part.HOUR);
    }

    /**
     * Creates an expression to extract the minute of the the supplied timestamp expression.
     */
    public static FluentExp dateMinute (SQLExpression exp)
    {
        return new DatePart(exp, Part.MINUTE);
    }

    /**
     * Creates an expression to extract the month of the the supplied timestamp expression.
     */
    public static FluentExp dateMonth (SQLExpression exp)
    {
        return new DatePart(exp, Part.MONTH);
    }

    /**
     * Creates an expression to extract the second of the the supplied timestamp expression.
     */
    public static FluentExp dateSecond (SQLExpression exp)
    {
        return new DatePart(exp, Part.SECOND);
    }

    /**
     * Creates an expression to extract the week of the the supplied timestamp expression.
     */
    public static FluentExp dateWeek (SQLExpression exp)
    {
        return new DatePart(exp, Part.WEEK);
    }

    /**
     * Creates an expression to extract the year of the the supplied timestamp expression.
     */
    public static FluentExp dateYear (SQLExpression exp)
    {
        return new DatePart(exp, Part.YEAR);
    }

    /**
     * Creates an expression to extract the epoch (aka unix timestamp, aka seconds passed since
     * 1970-01-01) of the the supplied timestamp expression.
     */
    public static FluentExp dateEpoch (SQLExpression exp)
    {
        return new DatePart(exp, Part.EPOCH);
    }

    /**
     * Creates an expression for the current timestamp.
     */
    public static FluentExp now ()
    {
        return new Now();
    }

    /**
     * Creates an aggregate expression that averages all values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp average (SQLExpression expr)
    {
        return new Average(expr);
    }

    /**
     * Creates an expression that averages all distinct values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp averageDistinct (SQLExpression expr)
    {
        return new Average(expr, true);
    }

    /**
     * Creates an aggregate expression that counts the number of rows from the supplied
     * expression. This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp count (SQLExpression expr)
    {
        return new Count(expr);
    }

    /**
     * Creates an aggregate expression that counts the number of distinct values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with a
     * ColumnExp.
     */
    public static FluentExp countDistinct (SQLExpression expr)
    {
        return new Count(expr, true);
    }

    /**
     * Creates an aggregate expression that evaluates to true iff every value from the supplied
     * expression is also true. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp every (SQLExpression expr)
    {
        return new Every(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp max (SQLExpression expr)
    {
        return new Max(expr);
    }

    /**
     * Creates an aggregate expression that finds the largest value in the values from the
     * supplied expression. This would usually be used in a FieldOverride and supplied with
     * a ColumnExp.
     */
    public static FluentExp min (SQLExpression expr)
    {
        return new Min(expr);
    }

    /**
     * Creates an aggregate expression that sums all the values from the supplied expression.
     * This would usually be used in a FieldOverride and supplied with a ColumnExp.
     */
    public static FluentExp sum (SQLExpression expr)
    {
        return new Sum(expr);
    }

    /**
     * Creates an expression that evaluates to the first supplied expression that is not null.
     */
    public static FluentExp coalesce (SQLExpression... args)
    {
        return new Coalesce(args);
    }

    /**
     * Creates an expression that evaluates to the largest of the given expressions.
     */
    public static FluentExp greatest (SQLExpression... args)
    {
        return new Greatest(args);
    }

    /**
     * Creates an expression that evaluates to the smallest of the given expressions.
     */
    public static FluentExp least (SQLExpression... args)
    {
        return new Least(args);
    }
}
