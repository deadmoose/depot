package com.samskivert.depot;

import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

public class AllGeneratedRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AllGeneratedRecord> _R = AllGeneratedRecord.class;
    public static final ColumnExp<Integer> RECORD_ID = colexp(_R, "recordId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    @Id @GeneratedValue public int recordId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AllGeneratedRecord}
     * with the supplied key values.
     */
    public static Key<AllGeneratedRecord> getKey (int recordId)
    {
        return newKey(_R, recordId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(RECORD_ID); }
    // AUTO-GENERATED: METHODS END
}
