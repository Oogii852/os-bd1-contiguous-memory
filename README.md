# Contiguous Memory Allocation

**Хичээл:** F.CSM302 Үйлдлийн систем, Бие даалт 1
**Хуваарилалтын код:** IV-2-14
**Төсөл:** Programming Project - Contiguous Memory Allocation (Operating System Concepts, Бүлэг 9)
**UI дүрслэл:** Memory layout (JavaFX)
**Гишүүд:** (нэмнэ)

## Зорилго
MAX байт санах ойг variable partition аргаар удирдана: хүсэх (RQ), чөлөөлөх (RL),
compaction (C), төлөв харуулах (STAT). Strategy: F (first fit), B (best fit), W (worst fit).

## Quick Start
Шаардлага: JDK 17+, Maven 3.8+ (Ubuntu, Java 25, Maven 3.9.12 дээр шалгасан)

## Командууд
`RQ <нэр> <хэмжээ> <F|B|W>`, `RL <нэр>`, `C`, `STAT`, `X` (гарах)

## Бүтэц
- `src/main/java/cma/MemoryManager.java` - алгоритм
- `src/main/java/cma/Main.java` - JavaFX UI (memory layout)
- `src/main/java/cma/Cli.java` - command-line горим
- `src/test/java/cma/` - JUnit тест
- `examples/` - жишээ оролт
- `docs/` - Manual, зураг

## Бэлэн код, сан, AI
Сан: JavaFX, JUnit 5. AI (Claude) ашигласан хэсгийг энд тодорхой тэмдэглэнэ.
