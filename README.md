KameBoy
=======

Original GameBoy and GameBoy Color emulator written in Kotlin

![](images/PokemonOr.png)

Summary
=======
* [x] Sound emulation (not perfect but works)
* [x] CPU emulation
* [x] Serial port emulation
* [x] Save states
* [x] Game saves
* [x] RTC emulation
* [x] Playable with gamepad
* [ ] Rumble emulation: only need interfacing with controllers
* [ ] Easy to use UI
* [ ] Netplay: Work in Progress
* [ ] Bug free

Infos
-----
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
***WARNING***: **Press all direction (arrows) and button (Q,W on qwerty, Enter and Backspace) keys when launching a game: the emulator thinks they are all pressed (known issue).**

Import the Gradle project into your IDE. Then, run the main method from `org.jglrxavpok.kameboy.KameboyMain`

Click on 'Insert cartridge' and select your ROM file!

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

Does it support saves?
----------------------
Yes, games are saved to a `<rom name>.sav` file in a folder named 'saves' located inside the run directory of the emulator.
