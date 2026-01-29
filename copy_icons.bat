@echo off
set ICON=C:\Users\LENOVO\.gemini\antigravity\brain\235107f1-da85-4632-9b15-df3c8759c916\app_icon_launcher_1769510356019.png

copy "%ICON%" "app\src\main\res\mipmap-mdpi\ic_launcher.png"
copy "%ICON%" "app\src\main\res\mipmap-mdpi\ic_launcher_round.png"
copy "%ICON%" "app\src\main\res\mipmap-hdpi\ic_launcher.png"
copy "%ICON%" "app\src\main\res\mipmap-hdpi\ic_launcher_round.png"
copy "%ICON%" "app\src\main\res\mipmap-xhdpi\ic_launcher.png"
copy "%ICON%" "app\src\main\res\mipmap-xhdpi\ic_launcher_round.png"
copy "%ICON%" "app\src\main\res\mipmap-xxhdpi\ic_launcher.png"
copy "%ICON%" "app\src\main\res\mipmap-xxhdpi\ic_launcher_round.png"
copy "%ICON%" "app\src\main\res\mipmap-xxxhdpi\ic_launcher.png"
copy "%ICON%" "app\src\main\res\mipmap-xxxhdpi\ic_launcher_round.png"

echo Icons copied successfully!
