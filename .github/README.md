# quest-x11

Termux:X11 for Meta Quest headsets. It is [Termux:X11](https://github.com/termux/termux-x11) with an **XR pointer** touch mode, so the controller ray works as a mouse in a Linux desktop running inside the headset: hovering moves the cursor, a click stays where it was aimed even though the ray shakes, and no button gets stuck.

> [!IMPORTANT]
> quest-x11 is **not** an official Termux project and is not endorsed by the Termux maintainers. Report its bugs [here](https://github.com/ixyzdev/quest-x11/issues), not to Termux.

## Why it exists

The Quest controller ray reports hover as finger touches. In Termux:X11's `Direct touch` mode that opens a touch that never ends, so the left button stays pressed until the next click, and every click behaves like a double click. None of the existing touch modes turns the ray's hover into cursor movement.

The XR pointer mode is also proposed to Termux:X11 in [termux/termux-x11#1176](https://github.com/termux/termux-x11/pull/1176). If Termux merges it, the official app is enough.

## Install

quest-x11 installs under its own package, `dev.ixyz.questx11`, next to the official Termux:X11 if you have it.

1. Download from the [latest release](https://github.com/ixyzdev/quest-x11/releases/latest) the APK and the companion `.deb` package.
2. Install the APK on the headset, with `adb install quest-x11-<version>-universal.apk` from a PC or with SideQuest.
3. In Termux, install the companion package, which provides the `termux-x11` and `termux-x11-preference` commands for this app. It replaces Termux's `termux-x11-nightly` package:

   ```sh
   apt install ./quest-x11-<version>-all.deb
   ```

4. Start your desktop with `termux-x11` as usual, then pick the mode in the app's preferences (Input, Touch mode, XR pointer), or from Termux:

   ```sh
   echo '"touchMode"="XR pointer"' | termux-x11-preference
   ```

To go back to the official app, install `termux-x11-nightly` again with `pkg install termux-x11-nightly`. Everything else about using the X server, from starting sessions to the preferences, is in [Termux:X11's documentation](../README.md).

Those docs name the app by the official package, `com.termux.x11`. For quest-x11 change only the package to `dev.ixyz.questx11`; class and action names stay as they are:

```sh
am start --user 0 -n dev.ixyz.questx11/com.termux.x11.MainActivity
am broadcast -a com.termux.x11.ACTION_STOP -p dev.ixyz.questx11
export CLASSPATH=$(/system/bin/pm path dev.ixyz.questx11 | cut -d: -f2)
```

## Changes from Termux:X11

quest-x11 follows Termux:X11's `master`, merged in every week through a pull request. These are its own changes, as GPL-3.0 section 5a asks of a modified version:

| Date | Change |
|---|---|
| 2026-11-21 | Added the XR pointer touch mode ([#1](https://github.com/ixyzdev/quest-x11/issues/1)). |
| 2026-11-21 | Added this README, an issue template and a security policy for this repository, and turned off the automation inherited from Termux ([#2](https://github.com/ixyzdev/quest-x11/issues/2)). |
| 2026-11-21 | Moved the app to its own package and name, with its own companion package ([#3](https://github.com/ixyzdev/quest-x11/issues/3)). |
| 2026-11-21 | Added the weekly sync with Termux:X11 ([#4](https://github.com/ixyzdev/quest-x11/issues/4)) and releases with the APK attached ([#5](https://github.com/ixyzdev/quest-x11/issues/5)). |

## Releases

Every merge to `main` publishes a new version with the shared release action of [ixyzdev/gitops-public](https://github.com/ixyzdev/gitops-public). Each release carries the APK and companion packages built from its tag, and its source is the tagged commit of this repository.

## Credits and license

- [Termux:X11](https://github.com/termux/termux-x11), by Twaik Yont and the Termux contributors, is the base of this project. Its sponsor links are kept in this repository's Sponsor button: support them.
- The XR pointer's rules for hover, press stability and double clicks come from [MesmerPrism/quest-termux-lab](https://github.com/MesmerPrism/quest-termux-lab) (MIT).
- quest-x11 is distributed under the [GNU General Public License v3.0](../LICENSE), like Termux:X11. "Termux" is the name of the Termux project; quest-x11 uses it only to describe where it comes from.
