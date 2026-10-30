package cma;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MemoryManagerTest {

    private MemoryManager mm(long n) { return new MemoryManager(n); }

    @Test void firstFitAllocatesFromStart() {
        MemoryManager m = mm(1000);
        var b = m.allocate("P1", 300, "F");
        assertEquals(0, b.start());
        assertEquals(700, m.free());
    }

    @Test void bestAndWorstFitChooseDifferentHoles() {
        // holes: [300..499]=200, [600..999]=400 болгоно
        for (String s : new String[]{"B", "W"}) {
            MemoryManager m = mm(1000);
            m.allocate("P1", 300, "F");
            m.allocate("P2", 200, "F");
            m.allocate("P3", 100, "F");
            m.release("P2");
            var b = m.allocate("P4", 150, s);
            assertEquals(s.equals("B") ? 300 : 600, b.start()); // B: 200-ийн hole (300), W: 400-ийн hole (600)
        }
    }

    @Test void releaseMergesAdjacentHoles() {
        MemoryManager m = mm(1000);
        m.allocate("P1", 300, "F");
        m.allocate("P2", 300, "F");
        m.release("P1");
        m.release("P2");
        assertEquals(1, m.holeCount());
        assertEquals(1000, m.largestHole());
    }

    @Test void externalFragmentationThenCompaction() {
        MemoryManager m = mm(1000);
        m.allocate("P1", 300, "F");
        m.allocate("P2", 300, "F");
        m.allocate("P3", 300, "F");
        m.release("P2");
        var ex = assertThrows(IllegalArgumentException.class, () -> m.allocate("P4", 350, "F"));
        assertTrue(ex.getMessage().contains("External fragmentation"));
        assertEquals(100, m.externalFragmentation());
        m.compact();
        assertEquals(1, m.holeCount());
        assertEquals(400, m.largestHole());
        assertEquals(300, m.snapshot().get(1).start()); // P3 шилжсэн
        assertDoesNotThrow(() -> m.allocate("P4", 350, "F"));
    }

    @Test void insufficientMemory() {
        MemoryManager m = mm(1000);
        var ex = assertThrows(IllegalArgumentException.class, () -> m.allocate("P1", 1001, "F"));
        assertTrue(ex.getMessage().contains("хүрэлцэхгүй"));
    }

    @Test void invalidInputs() {
        MemoryManager m = mm(1000);
        assertThrows(IllegalArgumentException.class, () -> new MemoryManager(0));
        assertThrows(IllegalArgumentException.class, () -> new MemoryManager(-5));
        assertThrows(IllegalArgumentException.class, () -> m.allocate("P1", 0, "F"));
        assertThrows(IllegalArgumentException.class, () -> m.allocate("P1", -10, "F"));
        assertThrows(IllegalArgumentException.class, () -> m.allocate("P1", 10, "X"));
        assertThrows(IllegalArgumentException.class, () -> m.allocate("bad name", 10, "F"));
        assertThrows(IllegalArgumentException.class, () -> m.release("NOPE"));
        m.allocate("P1", 10, "F");
        assertThrows(IllegalArgumentException.class, () -> m.allocate("P1", 10, "F")); // давхар нэр
    }

    @Test void commandParsing() {
        MemoryManager m = mm(1048576);
        assertTrue(m.execute("RQ P0 40000 W").startsWith("Амжилттай"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("RQ P1 abc F"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("RQ P1"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("HELLO"));
        assertThrows(IllegalArgumentException.class, () -> m.execute(""));
        assertTrue(m.execute("STAT").contains("Process P0"));
        // STAT формат номын жишээтэй ижил: Addresses [start:end] Process P / Unused
        assertEquals("Addresses [0:39999] Process P0\nAddresses [40000:1048575] Unused", m.stat());
        assertEquals("Гарлаа", m.execute("X"));
    }
}
