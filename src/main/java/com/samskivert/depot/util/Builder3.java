//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.util;

/**
 * A type-safe builder used to construct objects from the columns selected from Depot queries.
 */
public interface Builder3<T, A, B, C>
{
    /**
     * Builds an instance, using the supplied data.
     */
    public T build (A a, B b, C c);
}
