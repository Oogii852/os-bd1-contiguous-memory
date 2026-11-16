@echo off
rem Windows launcher: allocator.cmd 10000   (урьдчилан: mvn compile)
rem Монгол үсэг зөв харагдуулахын тулд консолыг UTF-8 болгоно
chcp 65001 >nul
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp "%~dp0target\classes" cma.Cli %*
