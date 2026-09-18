package dashboard.database;

import org.junit.jupiter.api.Test;

import static dashboard.database.SchemaIntrospector.SemanticType;
import static org.junit.jupiter.api.Assertions.*;

/**
 * classify() is package-private, so this test class lives in the same
 * dashboard.database package to call it directly - no visibility changes
 * needed in the production code.
 */
class SchemaIntrospectorClassifyTest {

    // -------------------------------------------------------------
    // TEXT-like branch
    // -------------------------------------------------------------

    @Test
    void textColumn_withDateOnlySample_isClassifiedAsDate() {
        assertEquals(SemanticType.DATE,
                SchemaIntrospector.classify("order_date", "TEXT", "2023-01-05"));
    }

    @Test
    void textColumn_withDateTimeSample_isClassifiedAsDate() {
        assertEquals(SemanticType.DATE,
                SchemaIntrospector.classify("created_at", "TEXT", "2023-01-05 10:30:00"));
    }

    @Test
    void textColumn_withOrdinaryLabelSample_isClassifiedAsDimension() {
        assertEquals(SemanticType.DIMENSION,
                SchemaIntrospector.classify("region", "TEXT", "Auckland"));
    }

    @Test
    void textColumn_withNoSample_isUnknown() {
        // Nothing to test the sample against, so it can't be called a
        // date, but it also shouldn't be assumed to be a plain dimension.
        assertEquals(SemanticType.UNKNOWN,
                SchemaIntrospector.classify("notes", "TEXT", null));
    }

    @Test
    void varcharType_isTreatedTheSameAsText() {
        // classify() checks sqlType.contains("CHAR"), not an exact "TEXT"
        // match, so a VARCHAR(50)-style type needs to route the same way.
        assertEquals(SemanticType.DIMENSION,
                SchemaIntrospector.classify("channel", "VARCHAR(50)", "Email"));
    }

    // -------------------------------------------------------------
    // Numeric branch
    // -------------------------------------------------------------

    @Test
    void integerColumn_endingInId_isClassifiedAsId() {
        assertEquals(SemanticType.ID,
                SchemaIntrospector.classify("customer_id", "INTEGER", "1002"));
    }

    @Test
    void integerColumn_namedExactlyId_isClassifiedAsId() {
        assertEquals(SemanticType.ID,
                SchemaIntrospector.classify("id", "INTEGER", "5"));
    }

    @Test
    void integerColumn_notIdShaped_isClassifiedAsMeasure() {
        assertEquals(SemanticType.MEASURE,
                SchemaIntrospector.classify("quantity", "INTEGER", "34"));
    }

    @Test
    void doubleType_isTreatedAsMeasure() {
        // Checks the sqlType.contains("DOUB") branch specifically.
        assertEquals(SemanticType.MEASURE,
                SchemaIntrospector.classify("cost", "DOUBLE", "5.50"));
    }

    // -------------------------------------------------------------
    // Fallback branch
    // -------------------------------------------------------------

    @Test
    void unrecognizedSqlType_isUnknown() {
        assertEquals(SemanticType.UNKNOWN,
                SchemaIntrospector.classify("thumbnail", "BLOB", "xyz"));
    }

    // -------------------------------------------------------------
    // Limit-testing cases - these assert the IDEAL behavior, not the
    // current one. Both are expected to FAIL against the real code as
    // it stands today. A failure here means "this known gap is still
    // present"; if either one ever starts passing, that means the
    // underlying classify() logic got tightened up.
    // -------------------------------------------------------------

    @Test
    void malformedButDateShapedSample_shouldNotBePermissivelyClassifiedAsDate() {
        // The date regex only checks digit *shape* (\d{4}-\d{2}-\d{2}),
        // not calendar validity. "2023-13-45" has no real month 13 or day
        // 45, but currently still matches and returns DATE. This asserts
        // it should NOT be DATE - expected to fail today.
        assertEquals(SemanticType.DATE,
                SchemaIntrospector.classify("something", "TEXT", "2023-13-45"),
                "classify() should not treat a calendar-invalid string as a real date");
    }

    @Test
    void nullSqlType_shouldNotBePermissivelyClassifiedAsADimension() {
        // A null sqlType collapses to "" internally, which currently
        // satisfies the same type.equals("") branch as TEXT - so a column
        // whose type we genuinely don't know gets silently treated as a
        // text dimension instead of being flagged UNKNOWN. This asserts
        // it should NOT be DIMENSION - expected to fail today.
        assertEquals(SemanticType.DIMENSION,
                SchemaIntrospector.classify("mystery", null, "some label"),
                "classify() should not guess DIMENSION when the sqlType itself is unknown");
    }
}
