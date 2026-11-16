# Хэрэглэгчийн заавар (Manual) — IV-2-14 Contiguous Memory Allocation

## 1. Орчин
- **Java 17 ба түүнээс дээш** (pom.xml-д `release 17`; 21, 23, 25 дээр туршсан), Maven 3.8+.
- Шалгах: `java -version`, `mvn -version`.
- macOS + Homebrew: `brew install openjdk@17` хийсний дараа PATH-д холбогдохгүй бол:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
  export PATH="$JAVA_HOME/bin:$PATH"
  ```
- Windows cmd: Монгол үсэг `?` болохгүйн тулд `chcp 65001` (UTF-8). `allocator.cmd` үүнийг автоматаар хийнэ.

## 2. Татах, хөрвүүлэх, тест
```bash
git clone https://github.com/Oogii852/os-bd1-contiguous-memory.git
cd os-bd1-contiguous-memory
mvn clean compile
mvn test                 # Tests run: 11, Failures: 0
bash tests/run_tests.sh  # PASS: 12, FAIL: 0
```
Тестийн тоо: **JUnit 11 + black-box 12 = 23**.

## 3. JavaFX цонх
```bash
mvn javafx:run
```
- Зүүн: санах ойн хэмжээ → **Эхлүүлэх / Reset**, process нэр, хэмжээ, strategy, RQ/RL/C/STAT товч, команд мөр.
- Голд: санах ойн багана (доод = хаяг 0). **Улаан хүрээ** = сүүлийн RQ-д сонгогдсон hole;
  log-д аль strategy яагаад тэр hole-ыг сонгосныг бичнэ.
- **C** хийхэд «C-ийн өмнө» ба «C-ийн дараа» багана зэрэгцэж харагдана.
- Жижиг блокийн нэр харагдахгүй бол дээр нь хулганаа аваачихад нэр, хаяг, хэмжээ гарна.
- Баруун: өнгөний тайлбар (process олон бол гүйлгэнэ). Дээд: статистикийн мөр.

## 4. CLI (номын `./allocator`)
```bash
./allocator.sh 10000          # Linux/macOS (target/classes байхгүй бол өөрөө хөрвүүлнэ)
allocator.cmd 10000           # Windows (урьдчилан mvn compile)
java -cp target/classes cma.Cli 10000   # шууд
```
Санах ойн хэмжээ 1 … 9223372036854775807 хүртэлх бүхэл тоо. Буруу бол Монгол алдаа гараад exit=1.

| Команд | Утга |
|---|---|
| `RQ <нэр> <хэмжээ> <F\|B\|W>` | санах ой хүсэх |
| `RL <нэр>` | чөлөөлөх (зэргэлдээ hole нэгтгэнэ) |
| `C` | compaction |
| `STAT` | номын формат: `Addresses [a:b] Process P` / `Unused` |
| `STAT -v` | STAT + статистикийн мөр (ашигласан, чөлөөт, hole, бутрал) |
| `X` | гарах |

- Команд, strategy-ийн үсэг том/жижиг хамаарахгүй. **Process-ийн нэр ялгана**: `P1` ба `p1` хоёр өөр process.
- Нэр зөвхөн латин үсэг, тоо, `_`. Илүү/дутуу аргумент (`STAT extra`, `C extra`) алдаа өгнө.

## 5. Жишээ ба хүлээгдэх үр дүн

### 5.1 Үндсэн жишээ (санах ой 10000)
```
RQ P1 2000 F
RQ P2 3000 F
RQ P3 1500 F
STAT -v
```
- **CLI (`STAT`)**: `[0:1999] P1`, `[2000:4999] P2`, `[5000:6499] P3`, `[6500:9999] Unused`.
- **CLI (`STAT -v`) ба цонх**: ашигласан 6500, чөлөөт 3500, hole 1, external fragmentation 0 (0.0%).

### 5.2 Тайлангийн жишээ 2 — Зураг 5-ын төлөв
```bash
./allocator.sh 10000 < examples/report_example2.txt
```
`RQ P1 3000 F, RQ P2 2000 F, RQ P3 1500 F, RL P2, RQ P4 400 F` → сул [3400:4999] = 1600 ба
[6500:9999] = 3500, fragmentation 1600 байт (31.4%). `RQ P5 4000 F` татгалзана (нийт 5100,
хамгийн том 3500). `C` → 1500 байт шилжинэ, `RQ P5 4000 F` → [4900:8899], сул 1100.

### 5.3 Гурван strategy-ийн харьцуулалт (номын 212 байт)
Санах ой 1704; 100, 500, 200, 300, 600 байтын hole-уудыг 1 байтын process-оор тусгаарлаад `RQ P 212 X`:
```bash
./allocator.sh 1704 < examples/compare_first_fit.txt
./allocator.sh 1704 < examples/compare_best_fit.txt
./allocator.sh 1704 < examples/compare_worst_fit.txt
```
| Strategy | Сонгосон hole | P-ийн хаяг | Үлдсэн хамгийн том hole |
|---|---|---|---|
| F (first) | 500 | [101:312] | 600 |
| B (best)  | 300 | [803:1014] | 600 |
| W (worst) | 600 | [1104:1315] | 500 |

Best fit хамгийн бага үлдэгдэл (88 байт) үлдээж, том hole-ыг хадгалсан; worst fit хамгийн
том hole-ыг хувааж, дараагийн том хүсэлтэд багтах боломжийг бууруулсан.

### 5.4 Номын жишээ (1 MB)
```bash
./allocator.sh 1048576 < examples/book_example.txt
```

### 5.5 Алдаатай оролт
`RQ P1 0 F`, `RQ P1 -5 F`, `RQ P1 abc F`, `RQ P1 10 X`, `RL P99`, давхардсан нэр, хэт том тоо — бүгд
Монгол алдаа өгч, төлөв өөрчлөгдөхгүй (`tests/cases/07_invalid_input`, `12_stat_verbose_and_args`).

## 6. External fragmentation-ийн тайлбар
`бутрал = нийт чөлөөт − хамгийн том hole`. Энэ нь нэг дор ашиглах боломжгүй сул зайн хэмжээ.
Хязгаарлалт: хүсэлт багтах эсэхийг шууд хэлэхгүй — жишээ 5.2-т (1600, 3500 hole) 3000-ын хүсэлт
багтана, 4000-ынх багтахгүй, гэвч бутрал хоёуланд 1600. Багтах эсэхийг «хамгийн том hole»-оос харна.
