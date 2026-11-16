package cma;

import java.util.ArrayList;
import java.util.List;

/**
 * Contiguous memory allocation (variable partitions).
 * Санах ойг hole (чөлөөт) ба process блокуудын жагсаалтаар хадгална.
 * Strategy: F = first fit, B = best fit, W = worst fit.
 * Бүх алдааг IllegalArgumentException-ээр ойлгомжтой мэдээлнэ.
 */
public class MemoryManager {

    /** Нэг блок: owner == null бол hole. */
    public record Block(long start, long size, String owner) {
        public boolean isHole() { return owner == null; }
        public long end() { return start + size - 1; }
    }

    private final long total;
    private final List<Block> blocks = new ArrayList<>();
    /** Сүүлийн амжилттай RQ-д сонгогдсон hole (хуваахаас өмнөх байдлаар). UI тайлбарт ашиглана. */
    private Block lastChosenHole;

    /**
     * Санах ойн хэмжээг текстээс уншина (Cli болон UI хоёулаа үүнийг ашиглана).
     * Тоо биш, хэт том, 0 эсвэл сөрөг бол ижил Монгол алдаа өгнө.
     */
    public static long parseMemorySize(String text) {
        String s = text == null ? "" : text.trim();
        String msg = "Санах ойн хэмжээ 1-ээс " + Long.MAX_VALUE + " хүртэлх бүхэл тоо байх ёстой: '" + s + "'";
        if (!s.matches("[+-]?\\d+")) throw new IllegalArgumentException(msg);
        long v;
        try { v = Long.parseLong(s); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(msg); } // хэт урт тоо
        if (v <= 0) throw new IllegalArgumentException(msg);
        return v;
    }

    public MemoryManager(long total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Санах ойн хэмжээ 0-ээс их байх ёстой: " + total);
        }
        this.total = total;
        blocks.add(new Block(0, total, null)); // эхэндээ нэг том hole
    }

    public long total() { return total; }

    public List<Block> snapshot() { return List.copyOf(blocks); }

    /** Сүүлийн RQ-д аль hole сонгогдсон (байхгүй бол null). */
    public Block lastChosenHole() { return lastChosenHole; }

    /** Санах ой хүсэх. */
    public Block allocate(String name, long size, String strategy) {
        if (name == null || !name.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Process-ийн нэр зөвхөн үсэг, тоо, _ агуулна: '" + name + "'");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Хүсэх хэмжээ 0-ээс их байх ёстой: " + size);
        }
        if (strategy == null || !strategy.toUpperCase().matches("[FBW]")) {
            throw new IllegalArgumentException("Strategy нь F, B, W-ийн нэг байх ёстой: '" + strategy + "'");
        }
        if (find(name) >= 0) {
            throw new IllegalArgumentException("Process '" + name + "' аль хэдийн санах ойд байна");
        }
        char s = strategy.toUpperCase().charAt(0);

        int idx = -1;
        for (int i = 0; i < blocks.size(); i++) {
            Block b = blocks.get(i);
            if (!b.isHole() || b.size() < size) continue;
            if (s == 'F') { idx = i; break; }
            if (idx < 0
                || (s == 'B' && b.size() < blocks.get(idx).size())
                || (s == 'W' && b.size() > blocks.get(idx).size())) {
                idx = i;
            }
        }
        if (idx < 0) {
            if (free() >= size) {
                throw new IllegalArgumentException("External fragmentation: нийт чөлөөт зай " + free()
                    + " байт хүрэлцэх боловч хамгийн том hole " + largestHole()
                    + " байт. Compaction (C) хийнэ үү.");
            }
            throw new IllegalArgumentException("Санах ой хүрэлцэхгүй: хүссэн " + size
                + " байт, чөлөөт нийт " + free() + " байт.");
        }
        Block h = blocks.get(idx);
        lastChosenHole = h;
        Block alloc = new Block(h.start(), size, name);
        blocks.set(idx, alloc);
        if (h.size() > size) { // hole-ыг хоёр хуваана
            blocks.add(idx + 1, new Block(h.start() + size, h.size() - size, null));
        }
        return alloc;
    }

    /** Process-ийн санах ойг чөлөөлөх, зэргэлдээ hole-уудыг нэгтгэх. */
    public void release(String name) {
        int i = find(name);
        if (i < 0) {
            throw new IllegalArgumentException("Process '" + name + "' санах ойд олдсонгүй");
        }
        Block b = blocks.get(i);
        blocks.set(i, new Block(b.start(), b.size(), null));
        mergeHoles();
    }

    /** Compaction: бүх process-ыг доош нь шилжүүлж, нэг том hole үлдээнэ. Шилжүүлсэн байтыг буцаана. */
    public long compact() {
        List<Block> out = new ArrayList<>();
        long pos = 0, moved = 0;
        for (Block b : blocks) {
            if (b.isHole()) continue;
            if (b.start() != pos) moved += b.size();
            out.add(new Block(pos, b.size(), b.owner()));
            pos += b.size();
        }
        if (pos < total) out.add(new Block(pos, total - pos, null));
        blocks.clear();
        blocks.addAll(out);
        return moved;
    }

    private void mergeHoles() {
        int i = 0;
        while (i < blocks.size() - 1) {
            Block a = blocks.get(i), b = blocks.get(i + 1);
            if (a.isHole() && b.isHole()) {
                blocks.set(i, new Block(a.start(), a.size() + b.size(), null));
                blocks.remove(i + 1);
            } else i++;
        }
    }

    private int find(String name) {
        for (int i = 0; i < blocks.size(); i++) {
            if (name != null && name.equals(blocks.get(i).owner())) return i;
        }
        return -1;
    }

    // ---- Статистик ----
    public long free() { return blocks.stream().filter(Block::isHole).mapToLong(Block::size).sum(); }
    public long used() { return total - free(); }
    public long largestHole() { return blocks.stream().filter(Block::isHole).mapToLong(Block::size).max().orElse(0); }
    public int holeCount() { return (int) blocks.stream().filter(Block::isHole).count(); }
    /** External fragmentation = нийт чөлөөт - хамгийн том hole (нэг дор ашиглах боломжгүй хэсэг). */
    public long externalFragmentation() { return free() - largestHole(); }

    /** STAT командын гаралт. */
    public String stat() {
        StringBuilder sb = new StringBuilder();
        for (Block b : blocks) {
            sb.append("Addresses [").append(b.start()).append(":").append(b.end()).append("] ")
              .append(b.isHole() ? "Unused" : "Process " + b.owner())
              .append("\n");
        }
        return sb.toString().stripTrailing();
    }

    /** Статистикийн нэг мөр (STAT -v болон JavaFX цонх ижил текст харуулна). */
    public String summary() {
        long free = free();
        double pct = free == 0 ? 0 : 100.0 * externalFragmentation() / free;
        return String.format(java.util.Locale.ROOT,
            "Нийт: %d | Ашигласан: %d | Чөлөөт: %d | Hole: %d | Хамгийн том hole: %d | External fragmentation: %d байт (%.1f%%)",
            total, used(), free, holeCount(), largestHole(), externalFragmentation(), pct);
    }

    /** Номын командыг ажиллуулна: RQ, RL, C, STAT (STAT -v), X. Команд, strategy том/жижиг үсэг хамаарахгүй, process нэр ялгана. */
    public String execute(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Хоосон команд");
        }
        String[] t = line.trim().split("\\s+");
        switch (t[0].toUpperCase()) {
            case "RQ": {
                if (t.length != 4) throw new IllegalArgumentException("Формат: RQ <нэр> <хэмжээ> <F|B|W>");
                long sz;
                if (!t[2].matches("[+-]?\\d+")) {
                    throw new IllegalArgumentException("Хэмжээ бүхэл тоо байх ёстой: '" + t[2] + "'");
                }
                try { sz = Long.parseLong(t[2]); }
                catch (NumberFormatException e) { // бүхэл тоо мөн боловч long-д багтахгүй
                    throw new IllegalArgumentException("Хэмжээ хэт их: '" + t[2] + "' (хамгийн ихдээ " + Long.MAX_VALUE + ")");
                }
                Block b = allocate(t[1], sz, t[3]);
                return "Амжилттай: " + b.owner() + " -> [" + b.start() + " : " + b.end() + "]";
            }
            case "RL": {
                if (t.length != 2) throw new IllegalArgumentException("Формат: RL <нэр>");
                release(t[1]);
                return "Чөлөөлсөн: " + t[1];
            }
            case "C": {
                if (t.length != 1) throw new IllegalArgumentException("Формат: C");
                return "Compaction дууслаа, шилжүүлсэн: " + compact() + " байт";
            }
            case "STAT": {
                if (t.length == 1) return stat();
                if (t.length == 2 && t[1].equalsIgnoreCase("-v")) return stat() + "\n" + summary(); // нэмэлт статистик
                throw new IllegalArgumentException("Формат: STAT эсвэл STAT -v");
            }
            case "X": {
                if (t.length != 1) throw new IllegalArgumentException("Формат: X");
                return "Гарлаа";
            }
            default:
                throw new IllegalArgumentException("Үл мэдэгдэх команд: '" + t[0] + "'. Боломжит: RQ, RL, C, STAT, X");
        }
    }
}
