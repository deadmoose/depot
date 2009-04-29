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

package com.samskivert.depot;

import java.io.Serializable;

import com.samskivert.util.ObjectUtil;

/**
 * Convenience class that implements {@link CacheKey} as simply as possibly. This class is
 * typically used when the caller wants to cache a non-obvious query such as a collection,
 * and needs to specify their own cache key and file it under a hand-picked cache id.
 */
public class SimpleCacheKey
    implements CacheKey
{
    /**
     * Construct a {@link SimpleCacheKey} for a query that has no parameters whatsoever.
     */
    public SimpleCacheKey (String cacheId)
    {
        this(cacheId, Boolean.TRUE);
    }

    /**
     * Construct a {@link SimpleCacheKey} associated with the given persistent class with
     * the given cache key.
     */
    public SimpleCacheKey (Class<?> cacheClass, Serializable cacheKey)
    {
        this(cacheClass.getName(), cacheKey);
    }

    /**
     * Construct a {@link SimpleCacheKey} for the given cache id with the given cache key.
     */
    public SimpleCacheKey (String cacheId, Serializable cacheKey)
    {
        _cacheId = cacheId;
        _cacheKey = cacheKey;
    }

    // from CacheKey
    public String getCacheId ()
    {
        return _cacheId;
    }

    // from CacheKey
    public Serializable getCacheKey ()
    {
        return _cacheKey;
    }

    @Override
    public int hashCode ()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((_cacheId == null) ? 0 : _cacheId.hashCode());
        result = PRIME * result + ((_cacheKey == null) ? 0 : _cacheKey.hashCode());
        return result;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        SimpleCacheKey other = (SimpleCacheKey) obj;
        return ObjectUtil.equals(_cacheId, other._cacheId) &&
            ObjectUtil.equals(_cacheKey, other._cacheKey);
    }

    @Override
    public String toString ()
    {
        return "[cacheId=" + _cacheId + ", value=" + _cacheKey + "]";
    }

    protected String _cacheId;
    protected Serializable _cacheKey;
}
