@echo off
java -jar convex-cli/target/convex-cli-0.7.0-SNAPSHOT-jar-with-dependencies.jar %*
exit /b %errorlevel%