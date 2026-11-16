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

    /** Номын 212 байтын жишээ: hole 100, 500, 200, 300, 600 (1 байтын process-оор тусгаарласан). */
    private MemoryManager bookHoles() {
        MemoryManager m = mm(1704);
        long[] holes = {100, 500, 200, 300, 600};
        for (int i = 0; i < holes.length; i++) {
            m.allocate("H" + i, holes[i], "F");
            if (i < holes.length - 1) m.allocate("S" + i, 1, "F");
        }
        for (int i = 0; i < holes.length; i++) m.release("H" + i);
        return m;
    }

    @Test void threeStrategiesOnBookExample() {
        // F: эхний багтах (500), B: хамгийн жижиг багтах (300), W: хамгийн том (600)
        assertEquals(101, bookHoles().allocate("P", 212, "F").start());
        assertEquals(803, bookHoles().allocate("P", 212, "B").start());
        assertEquals(1104, bookHoles().allocate("P", 212, "W").start());
        MemoryManager m = bookHoles();
        m.allocate("P", 212, "B");
        assertEquals(300, m.lastChosenHole().size()); // UI-д сонгосон hole-ыг тэмдэглэнэ
    }

    @Test void statVerboseAndArgumentChecks() {
        MemoryManager m = mm(10000);
        m.execute("RQ P1 3000 F");
        m.execute("RQ P2 2000 F");
        m.execute("RQ P3 1500 F");
        m.execute("RL P2");
        m.execute("RQ P4 400 F");
        String v = m.execute("STAT -v");
        assertTrue(v.startsWith(m.stat()));
        assertTrue(v.endsWith("External fragmentation: 1600 байт (31.4%)"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("STAT extra"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("C extra"));
        assertThrows(IllegalArgumentException.class, () -> m.execute("X extra"));
        var big = assertThrows(IllegalArgumentException.class, () -> m.execute("RQ P9 99999999999999999999 F"));
        assertTrue(big.getMessage().contains("хэт их"));
    }

    @Test void parseMemorySizeGivesSameMongolianError() {
        assertEquals(10000, MemoryManager.parseMemorySize(" 10000 "));
        assertEquals(Long.MAX_VALUE, MemoryManager.parseMemorySize("9223372036854775807"));
        for (String bad : new String[]{"abc", "1.5", "", "0", "-5", "9223372036854775808"}) {
            var ex = assertThrows(IllegalArgumentException.class, () -> MemoryManager.parseMemorySize(bad));
            assertTrue(ex.getMessage().startsWith("Санах ойн хэмжээ 1-ээс"), bad);
        }
    }

    @Test void commandsCaseInsensitiveButNamesCaseSensitive() {
        MemoryManager m = mm(100);
        m.execute("RQ P1 10 F");
        assertDoesNotThrow(() -> m.execute("rq p1 10 f")); // p1 нь P1-ээс өөр process
        m.execute("rl p1");
        assertTrue(m.stat().contains("Process P1"));
    }
}
