#!/bin/bash
# Black-box тест: tests/cases/*.in -> Cli -> tests/cases/*.expected-тэй харьцуулна.
# Тестийн файлын эхний мөр "# MAX=<санах ойн хэмжээ>" байна.
# Cli гаралтаа UTF-8-аар хэвлэдэг тул компьютерийн locale (POSIX, C, UTF-8)-оос хамаарахгүй.
cd "$(dirname "$0")/.." || exit 1
if command -v mvn >/dev/null 2>&1 && mvn -q compile 2>&1 | tail -5 && [ -f target/classes/cma/Cli.class ]; then
  :
else # mvn байхгүй/ажиллахгүй үед зөвхөн CLI-ийн 2 классыг javac-аар хөрвүүлнэ
  mkdir -p target/classes
  javac -encoding UTF-8 -d target/classes src/main/java/cma/MemoryManager.java src/main/java/cma/Cli.java || exit 1
fi
pass=0; fail=0
for in in tests/cases/*.in; do
  name=$(basename "$in" .in)
  max=$(head -1 "$in" | sed 's/^# MAX=//')
  actual=$(java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp target/classes cma.Cli "$max" < "$in" | sed 's/allocator>//g')
  expected=$(cat "tests/cases/$name.expected")
  if [ "$actual" == "$expected" ]; then
    echo "PASS  $name"; pass=$((pass+1))
  else
    echo "FAIL  $name"; diff <(echo "$expected") <(echo "$actual"); fail=$((fail+1))
  fi
done
echo "----- PASS: $pass, FAIL: $fail"
[ "$fail" -eq 0 ]
