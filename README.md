# Contiguous Memory Allocation

**Хичээл:** F.CSM302 Үйлдлийн систем, Бие даалт 1
**Хуваарилалтын код:** IV-2-14
**Төсөл:** Programming Project - Contiguous Memory Allocation (Operating System Concepts, Бүлэг 9)
**UI дүрслэл:** Memory layout (JavaFX)

**Гишүүд:**

| Код | Нэр |
|---|---|
| B210930813 | А. Энхболд |
| B222270817 | Э. Эрхэс |
| B231940150 | Б. Мөнх-Од |
| B241940074 | Д. Мөнгөнсүх |
| B242270021 | Б. Отгонсүх |
| B252270135 | Э. Ундрах |

## Зорилго
MAX байт санах ойг variable partition аргаар удирдана: хүсэх (RQ), чөлөөлөх (RL),
compaction (C), төлөв харуулах (STAT). Strategy: F (first fit), B (best fit), W (worst fit).

## Quick Start
Шаардлага: **JDK 17 ба түүнээс дээш** (21, 23 (macOS), 25 (Ubuntu) дээр туршсан), Maven 3.8+.

```bash
git clone https://github.com/Oogii852/os-bd1-contiguous-memory.git
cd os-bd1-contiguous-memory

mvn clean test          # JUnit 11 тест
mvn javafx:run          # JavaFX цонх (memory layout)

./allocator.sh 10000                                   # CLI (номын ./allocator 10000)
./allocator.sh 1048576 < examples/book_example.txt     # номын жишээ
./allocator.sh 1704 < examples/compare_best_fit.txt    # F/B/W харьцуулалт
bash tests/run_tests.sh                                # black-box 12 тест
```

Windows: `mvn compile` хийгээд `allocator.cmd 10000` (консолыг `chcp 65001` буюу UTF-8 болгоно).
Дэлгэрэнгүй заавар: [docs/MANUAL.md](docs/MANUAL.md).

## Командууд
`RQ <нэр> <хэмжээ> <F|B|W>`, `RL <нэр>`, `C`, `STAT`, `STAT -v` (+ статистик), `X` (гарах).
Команд болон strategy-ийн үсэг том/жижиг хамаарахгүй, **process-ийн нэр ялгана** (`P1` ≠ `p1`).

## Тест
- JUnit: 11 (`src/test/java/cma/MemoryManagerTest.java`)
- Black-box: 12 (`tests/cases/*.in` → `*.expected`), locale-оос хамаарахгүй (Cli гаралт UTF-8)
- Нийт: 23

## Бүтэц
- `src/main/java/cma/MemoryManager.java` - алгоритм
- `src/main/java/cma/Main.java` - JavaFX UI (memory layout)
- `src/main/java/cma/Cli.java` - command-line горим
- `src/test/java/cma/` - JUnit тест
- `tests/` - black-box тест ба `run_tests.sh`
- `examples/` - жишээ оролт (номын жишээ, тайлангийн жишээ 2, F/B/W харьцуулалт)
- `docs/` - Manual
- `contiguous-allocator.html` - браузерт ажилладаг туслах симулятор

## Бэлэн код, сан, AI
Сан: JavaFX 21, JUnit 5. Бусад бэлэн код ашиглаагүй.
AI: Claude (Anthropic)-ийг кодын бүтэц, тест, харилцан шүүмжийн (II-4-7) дагуу засвар
(UTF-8 гаралт, оролтын шалгалт, `STAT -v`, UI сайжруулалт, README/Manual) хийхэд туслуулсан.
Үр дүнг баг өөрөө ажиллуулж, тестээр шалгасан.
