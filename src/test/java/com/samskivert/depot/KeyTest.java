//
// $Id$
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

import org.junit.Test;
import static org.junit.Assert.*;

import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.DepotUtil;

/**
 * Tests some super basic {@link Key} stuff.
 */
public class KeyTest
{
    @Test public void testSlowConstructor ()
    {
        int species = 10, monkeyId = 15;
        Key<MonkeyRecord> key = MonkeyRecord.getKey(species, monkeyId);

        // make sure that the arguments we passed in got assigned in the right positions
        ColumnExp[] kfs = DepotUtil.getKeyFields(MonkeyRecord.class);
        int kspecies = 0, kmonkeyId = 0;
        for (int ii = 0; ii < kfs.length; ii++) {
            if (MonkeyRecord.SPECIES.equals(kfs[ii])) {
                kspecies = (Integer)(key.getValues()[ii]);
            } else if (MonkeyRecord.MONKEY_ID.equals(kfs[ii])) {
                kmonkeyId = (Integer)(key.getValues()[ii]);
            }
        }
        assertEquals(species, kspecies);
        assertEquals(monkeyId, kmonkeyId);
    }

    @Test public void testFastConstructor ()
    {
        int recordId = 10;
        Key<TestRecord> key = TestRecord.getKey(recordId);

        // make sure that the arguments we passed in got assigned in the right positions
        ColumnExp[] kfs = DepotUtil.getKeyFields(TestRecord.class);
        int krecordId = 0;
        for (int ii = 0; ii < kfs.length; ii++) {
            if (TestRecord.RECORD_ID.equals(kfs[ii])) {
                krecordId = (Integer)(key.getValues()[ii]);
            }
        }
        assertEquals(recordId, krecordId);
    }
}