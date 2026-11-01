#!/bin/bash
# Black-box тест: tests/cases/*.in -> Cli -> tests/cases/*.expected-тэй харьцуулна.
# Тестийн файлын эхний мөр "# MAX=<санах ойн хэмжээ>" байна.
cd "$(dirname "$0")/.." || exit 1
mvn -q compile 2>&1 | tail -5
pass=0; fail=0
for in in tests/cases/*.in; do
  name=$(basename "$in" .in)
  max=$(head -1 "$in" | sed 's/^# MAX=//')
  actual=$(java -cp target/classes cma.Cli "$max" < "$in" | sed 's/allocator>//g')
  expected=$(cat "tests/cases/$name.expected")
  if [ "$actual" == "$expected" ]; then
    echo "PASS  $name"; pass=$((pass+1))
  else
    echo "FAIL  $name"; diff <(echo "$expected") <(echo "$actual"); fail=$((fail+1))
  fi
done
echo "----- PASS: $pass, FAIL: $fail"
[ "$fail" -eq 0 ]
