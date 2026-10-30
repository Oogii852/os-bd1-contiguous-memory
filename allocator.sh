#!/bin/sh
# Номын ./allocator <MAX> -тэй дүйцэх launcher. Урьдчилан: mvn compile
exec java -cp "$(dirname "$0")/target/classes" cma.Cli "$@"
