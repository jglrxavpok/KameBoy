KameBoy
=======

Original GameBoy and GameBoy Color emulator written in Kotlin

Summary
=======
* CPU instructions have been tested thanks to gblargg's tests and all tests pass
* Instruction timings have been tested thanks to gblargg's tests and all tests pass
* Supports playing with a gamepad! Mappings are provided for XBox360 controllers only ATM.
* Sound emulation sounds okay-ish but is far from perfect (the emulator does not pass a single dmg_sound test atm)

How do I install it ?
---------------------
The emulator has no binary release yet so you will need to run it from sources or compile it.

How to compile
--------------
Import the Gradle project into your favorite IDE and launch `gradle build`

How to run from sources
-----------------------
Import the Gradle project into your IDE, open 'org.jglrxavpok.kameboy.KameboyCore' and change:
```kotlin
val cartridge = _DEV_cart("<Your file name here>", useBootRom = <do you want to provide a bootrom?>)
```
Roms are fetched from `/roms/` in the classpath for the moment.

Is it accurate ?
----------------
No, absolutely not.

Does it support save states?
----------------------------
Yes: Press a key between 1-9 (or 0) on your keyboard to create a save state indexed by the number on the key (eg. pressing '4' saves a save state into slot #4).

Hold shift and press the same key to reload the save state!

Does it support netplay?
------------------------
Kind of, it is still experimental and may very well crash the emulator!


