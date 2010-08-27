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

/**
 * This interface uniquely identifies a single persistent entry for caching purposes.
 * Queries that are given a {@link CacheKey} consult the cache before they hit the
 * database.
 */
public interface CacheKey
{
    /**
     * Returns the id of the cache in whose scope this key makes sense.
     */
    public String getCacheId ();

    /**
     * Returns the actual opaque serializable cache key under which results are stored in the cache
     * identified by {@link #getCacheId}. The object returned by this method should <em>only</em>
     * reference system classes (not application classes). Depot takes care to ensure this and you
     * probably aren't implementing your own cache keys so this should be fine.
     */
    public Serializable getCacheKey ();
}
