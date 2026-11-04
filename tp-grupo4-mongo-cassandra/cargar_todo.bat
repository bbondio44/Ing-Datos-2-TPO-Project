@echo off
REM Doble clic para cargar MongoDB y Cassandra (ver cargar_todo.ps1)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0cargar_todo.ps1" %*
