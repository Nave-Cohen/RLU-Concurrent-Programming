import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RLUEdgeCaseTest {
    @Test
    public void testDuplicatewrite() {
        RLU<Integer> set = new RLU<>();
        assertTrue(set.write(1, 100));
        assertTrue(set.write(1, 200)); // Overwrites before commit
        set.commit();
        assertEquals(200, set.read(1)); // Final value should be 200
    }

    @Test
    public void testRemoveNonExistent() {
        RLU<Integer> set = new RLU<>();
        assertFalse(set.remove(99)); // Should not crash
        set.commit();
        assertNull(set.read(99)); // Should remain null
    }
}
