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

package com.samskivert.jdbc.depot;

import java.io.Serializable;

/**
 * The base class for all persistent records used in Depot. Persistent records must be cloneable
 * and serializable; this class is used to enforce those requirements.
 */
public class PersistentRecord
    implements Cloneable, Serializable
{
    @Override // from Object
    public PersistentRecord clone ()
    {
        try {
            return (PersistentRecord) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse); // this should never happen
        }
    }
}
