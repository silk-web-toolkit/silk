Name "Silk"

OutFile "setup.exe"

InstallDir "$PROGRAMFILES\Silk"

DirText "Please choose a directory to which you'd like to install Silk."

; ----------------------------------------------------------------------------------
; *************************** SECTION FOR INSTALLING *******************************
; ----------------------------------------------------------------------------------

Section ""

SetOutPath $INSTDIR\

File ffmpegsumo.dll
File icudtl.dat
File libEGL.dll
File libGLESv2.dll
File nw.pak
File silk-gui.exe
File /r bin

WriteUninstaller $INSTDIR\uninstall.exe

CreateDirectory "$SMPROGRAMS\Silk"
CreateDirectory "$SMPROGRAMS\Silk\bin"
CreateShortCut "$SMPROGRAMS\Silk\Run Silk GUI.lnk" "$INSTDIR\silk-gui.exe"
CreateShortCut "$SMPROGRAMS\Silk\Uninstall Silk.lnk" "$INSTDIR\uninstall.exe"

Exec 'setx PATH "%PATH%;$INSTDIR\bin\"'

WriteRegStr HKLM "Software\Microsoft\Windows\0.6.0\Uninstall\Silk" "DisplayName" "Silk (remove only)"
WriteRegStr HKLM "Software\Microsoft\Windows\0.6.0\Uninstall\Silk" "Uninstall" "$INSTDIR\uninstall.exe"

MessageBox MB_OK "Silk was successfully installed."

SectionEnd

; ----------------------------------------------------------------------------------
; ************************** SECTION FOR UNINSTALLING ******************************
; ----------------------------------------------------------------------------------

Section "Uninstall"

Delete $INSTDIR\uninstall.exe
Delete $INSTDIR\ffmpegsumo.dll
Delete $INSTDIR\icudtl.dat
Delete $INSTDIR\libEGL.dll
Delete $INSTDIR\libGLESv2.dll
Delete $INSTDIR\nw.pak
Delete $INSTDIR\silk-gui.exe
Delete $INSTDIR\bin\silk.exe

Exec 'setx PATH "%PATH:;$INSTDIR\bin\="'

RMDir $INSTDIR\bin
RMDir $INSTDIR

Delete "$SMPROGRAMS\Silk\Run Silk GUI.lnk"
Delete "$SMPROGRAMS\Silk\Uninstall Silk.lnk"

RMDIR "$SMPROGRAMS\Silk"

DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Silk"
DeleteRegKey HKEY_LOCAL_MACHINE "SOFTWARE\Microsoft\Windows\0.6.0\Uninstall\Silk"

SectionEnd
