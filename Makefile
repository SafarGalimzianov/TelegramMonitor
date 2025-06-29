b:
	./gradlew assembleDebug
	# app/build/outputs/apk/debug/app-debug.apk
	# adb install app/build/outputs/apk/debug/app-debug.apk

i:
	adb install -r ~/Programming/MyProjects/TelegramMonitor/app/build/outputs/apk/debug/app-debug.apk