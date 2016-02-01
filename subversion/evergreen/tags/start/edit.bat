@echo off
set EDIT_HOME=v:\edit
java -Xmx128m -cp %EDIT_HOME%\classes;MRJ141Stubs.jar -DpreferencesDirectory=%EDIT_HOME%\preferences  e.edit.Edit %* > %EDIT_HOME%\edit.log