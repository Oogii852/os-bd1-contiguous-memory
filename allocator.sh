#!/bin/sh
# Номын ./allocator <MAX> -тэй дүйцэх launcher. Жнь: ./allocator.sh 10000
# target/classes байхгүй бол автоматаар хөрвүүлнэ (mvn, эсвэл mvn байхгүй бол javac).
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ ! -f "$DIR/target/classes/cma/Cli.class" ]; then
  if command -v mvn >/dev/null 2>&1; then
    (cd "$DIR" && mvn -q compile) >&2
  fi
  if [ ! -f "$DIR/target/classes/cma/Cli.class" ]; then # mvn байхгүй/амжилтгүй бол javac
    mkdir -p "$DIR/target/classes"
    javac -encoding UTF-8 -d "$DIR/target/classes" "$DIR/src/main/java/cma/MemoryManager.java" "$DIR/src/main/java/cma/Cli.java" || exit 1
  fi
fi
exec java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "$DIR/target/classes" cma.Cli "$@"
