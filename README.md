# qmk-hid-display-kotlin

I build this to display my current playing spotify song and the current time on the display of my qmk keyboard. Refer to my [layout](https://github.com/tobiasarndt/qmk_firmware/tree/7a128315ccd742d1f681d4313462be271a65b88e/keyboards/jorne/keymaps/tobiasarndt)
on how to implement this in your keymap. For the font i used for the clock it is necessary to merge thi [PR](https://github.com/qmk/qmk_firmware/pull/18406)
to allow for bitmaps with more than 255 characters.

This utility works by sending the strings to be displayed on the display line by line. Once four lines are recieved the keyboard updates the display.
Currently it is possitle to display the time and Spotify information. The weather monitor is sadly broken, as yahoo no longer supplis the data as they did. To add a new custom monitor implement a new class with the Monitor interface and add it to the monitor list in monitorList in the main.

![image](https://github.com/tobiasarndt/qmk-hid-display-kotlin/assets/54204861/31a08b7e-8221-4dca-99b2-149c9e7cd61a) ![image](https://github.com/tobiasarndt/qmk-hid-display-kotlin/assets/54204861/a63f6f9c-39c1-4681-a66c-f65b0b4f1ede)
