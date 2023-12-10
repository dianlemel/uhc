//https://www.spigotmc.org/wiki/buildtools/#1-18-2
//set java8=
//set java16=
//set java17=
@echo off
title SpigotMC BuildTools Builder
IF NOT EXIST BuildTools (
    mkdir BuildTools
)
cd BuildTools
curl -z BuildTools.jar -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
set /p Input=Enter the version: || set Input=latest
set /p Java=Java 8 or Java 16 (for 1.17.1 only) or Java 17? || set Java=17
if %Java%==8 "%java8%\bin\java" -jar BuildTools.jar --remappe --rev %Input%
if %Java%==16 "%java16%\bin\java" -jar BuildTools.jar --remappe --rev 1.17.1
if %Java%==17 "%java17%\bin\java" -jar BuildTools.jar --remappe --rev %Input%
if %Java%==18 "%java18%\bin\java" -jar BuildTools.jar --remappe --rev %Input%

echo "Done!"
pause