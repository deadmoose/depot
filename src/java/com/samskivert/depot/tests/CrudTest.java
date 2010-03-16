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

package com.samskivert.depot.tests;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests simple create/read/update/delete behaviors. 
 */
public class CrudTest extends TestBase
{
    @Test public void testCreateReadDelete ()
    {
        TestRecord in = createTestRecord(1);
        _repo.insert(in);

        TestRecord out = _repo.loadNoCache(in.recordId);
        assertNotNull(out != null); // we got a result
        assertTrue(in != out); // it didn't come from the cache

        // make sure all of the fields were marshalled and unmarshalled correctly
        assertTestRecordEquals(in, out);

        // finally clean up after ourselves
        _repo.delete(TestRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    @Test public void testUpdateDelete ()
    {
        TestRecord in = createTestRecord(1);
        _repo.insert(in);

        // first try updating using the whole-record update mechanism
        in.homeTown = "Funky Town";
        _repo.update(in);
        assertTestRecordEquals(in, _repo.loadNoCache(in.recordId));

        // then try update partial
        String name = "Bob";
        int age = 25;
        int[] numbers = { 1, 2, 3, 4, 5 };
        _repo.updatePartial(TestRecord.getKey(in.recordId),
                            ImmutableMap.of(TestRecord.NAME, name, TestRecord.AGE, age,
                                            TestRecord.NUMBERS, numbers));
        TestRecord up = _repo.loadNoCache(in.recordId);
        assertEquals(name, up.name);
        assertEquals(age, up.age);
        assertTrue(Arrays.equals(numbers, up.numbers));

        // finally clean up after ourselves
        _repo.delete(TestRecord.getKey(in.recordId));
        assertNull(_repo.loadNoCache(in.recordId));
    }

    // the HSQL in-memory database persists for the lifetime of the VM, which means we have to
    // clean up after ourselves in every test; thus we go ahead and share a repository
    protected TestRepository _repo = createTestRepository();
}
