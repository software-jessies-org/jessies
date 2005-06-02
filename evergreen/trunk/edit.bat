@echo off
set EDIT_HOME=u:\Projects\edit
java -Xmx1g -cp %EDIT_HOME%\classes;%EDIT_HOME%\..\salma-hayek\classes;%EDIT_HOME%\..\salma-hayek\MRJ141Stubs.jar -DpreferencesDirectory=%EDIT_HOME%\preferences e.edit.Edit %* > %EDIT_HOME%\edit.log
