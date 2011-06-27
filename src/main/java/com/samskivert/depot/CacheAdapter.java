//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot;

import java.io.Serializable;

import com.samskivert.depot.impl.FindAllQuery;

/**
 * Implementations of this interface are responsible for all the caching needs of Depot.
 *
 * From the point of view of this interface, there are a potentially very large number of
 * caches available, each idenfided by a unique cacheId. Currently Depot creates up to three
 * caches for each record type:
 *
 * Any record type with a primary key has a {@link CacheCategory#RECORD} cache, for storing
 * record instances by primary key.
 *
 * Record types with primary keys may also have a {@link CacheCategory#SHORT_KEYSET} cache wherein
 * {@link KeySet} instances are stored, identified by query strings. See {@link FindAllQuery}
 * for more on this.
 *
 * Finally, clients may request {@link CacheCategory#RESULT} caching of entire result sets of
 * some record type -- which does not need to have a primary key, in contrast to the other two
 * categories. These are also identified by query strings, and end up in a third cache.
 */
public interface CacheAdapter
{
    public enum CacheCategory { RECORD, SHORT_KEYSET, LONG_KEYSET, RESULT }

    /** The encapsulated result of a cache lookup. */
    public interface CachedValue<T>
    {
        /** Returns the cached value, which can be null. */
        public T getValue ();
    }

    /**
     * Searches the given cache using the given key and returns the resulting {@link CachedValue},
     * or null if nothing exists in the cache for this key.
     */
    public <T> CachedValue<T> lookup (String cacheId, Serializable key);

    /**
     * Stores a new value in the given cache under the given key.
     */
    public <T> void store (CacheCategory category, String cacheId, Serializable key, T value);

    /**
     * Removes the cache entry, if any, associated with the given key.
     */
    public void remove (String cacheId, Serializable key);

    /**
     * Provides a way to enumerate the currently cached entries for the given cache.
     */
    public <T> Iterable<Serializable> enumerate (String cacheId);

    /**
     * Clears the cache with the specified name.
     *
     * @param localOnly if true, only clear the cache on the local node, do not broadcast
     * instructions for all distributed nodes to clear this cache as well.
     */
    public void clear (String cacheId, boolean localOnly);

    /**
     * Shut down all operations, e.g. persisting memory contents to disk.
     */
    public void shutdown ();
}
