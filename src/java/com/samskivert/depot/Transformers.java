//
// $Id$
//
// Depot library - a Java relational persistence library
// Copyright (C) 2006-2010 Michael Bayne and Pär Winzell
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.samskivert.depot.annotation.Column;

//import com.google.common.annotations.Beta;

import com.google.common.base.Preconditions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Contains various generally useful {@link Transformer} implementations. To use a transformer, you
 * specify it via a {@link Column} annotation. For example:
 * <pre>
 * public class MyRecord extends PersistentRecord {
 *     @Transform(Transformers.CommaSeparatedString.class)
 *     public String[] cities;
 * }
 * </pre>
 */
public class Transformers
{
    /**
     * Combines the contents of a String[] column into a single string, separated by tabs. Any tabs
     * in the strings will be escaped.
     */
    public static class TabSeparatedString implements Transformer<String[], String> {
        public String toPersistent (String[] values) {
            StringBuffer buf = new StringBuffer();
            for (String value : values) {
                if (buf.length() > 0) {
                    buf.append("\t");
                }
                buf.append(value.replace("\t", "\\\t"));
            }
            return buf.toString();
        }
        public String[] fromPersistent (Type ftype, String value) {
            String[] values = value.replace("\\\t", "%%ESCTAB%%").split("\t");
            for (int ii = 0; ii < values.length; ii++) {
                values[ii] = values[ii].replace("%%ESCTAB%%", "\t");
            }
            return values;
        }
    }

    //@Beta
    public static class StringArray implements Transformer<String[], String>
    {
        public String toPersistent (String[] value)
        {
            if (value == null) {
                return null;
            }
            return StringIterable.toPersistent0(Arrays.asList(value));
        }

        public String[] fromPersistent (Type ftype, String encoded)
        {
            if (encoded == null) {
                return null;
            }
            return Iterables.toArray(StringIterable.fromPersistent0(encoded), String.class);
        }
    }

    //@Beta
    public static class StringIterable implements Transformer<Iterable<String>, String>
    {
        public String toPersistent (Iterable<String> value)
        {
            return toPersistent0(value);
        }

        public Iterable<String> fromPersistent (Type ftype, String encoded)
        {
            ArrayList<String> value = fromPersistent0(encoded);
            Type fclass = (ftype instanceof ParameterizedType) ?
                ((ParameterizedType)ftype).getRawType() : ftype;
            if (value == null ||
                    fclass == ArrayList.class || fclass == List.class ||
                    fclass == Collection.class || fclass == Iterable.class) {
                return value;
            }
            if (fclass == LinkedList.class) {
                return Lists.newLinkedList(value);
            }
            if (fclass == HashSet.class || fclass == Set.class) {
                return Sets.newHashSet(value);
            }
            // else: reflection? See if it's a collection, call the 0-arg constructor, add all
            // and return? Something?
            return value;
        }

        protected static String toPersistent0 (Iterable<String> value)
        {
            if (value == null) {
                return null;
            }
            StringBuilder buf = new StringBuilder();
            for (String s : value) {
                if (s == null) {
                    buf.append("\\0"); // encode nulls as "\0" (with no terminator)
                } else {
                    s = s.replace("\\", "\\\\"); // turn \ into \\ 
                    s = s.replace("\n", "\\n");  // turn a newline in a String to "\n"
                    buf.append(s).append('\n');
                }
            }
            return buf.toString();
        }

        protected static ArrayList<String> fromPersistent0 (String encoded)
        {
            if (encoded == null) {
                return null;
            }
            ArrayList<String> value = Lists.newArrayList();
            StringBuilder buf = new StringBuilder(encoded.length());
            for (int ii = 0, nn = encoded.length(); ii < nn; ii++) {
                char c = encoded.charAt(ii);
                switch (c) {
                case '\n':
                    value.add(buf.toString()); // TODO: intern?
                    buf.setLength(0);
                    break;

                case '\\':
                    Preconditions.checkArgument(++ii < nn, "Invalid encoded string");
                    char slashed = encoded.charAt(ii);
                    switch (slashed) {
                    case '0': // turn \0 into a null element
                        Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
                        value.add(null);
                        break;

                    case 'n': // turn \n back into a newline
                        buf.append('\n');
                        break;

                    default: // this should only be a slash...
                        buf.append(slashed);
                        break;
                    }
                    break;

                default:
                    buf.append(c);
                    break;
                }
            }
            // make sure the last element was terminated
            Preconditions.checkArgument(buf.length() == 0, "Invalid encoded string");
            return value;
        }
    }
}
