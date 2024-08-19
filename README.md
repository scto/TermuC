# TermuC

[中文 README](./README_zh.md)

TermuC is a simple C/C++ IDE backed on Termux. Based on [MrIkso/CodeEditor](//github.com/MrIkso/CodeEditor)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/cn.rbc.termuc)

## Screenshot

![1](fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg)

## Technology

This app use `com.termux.RUN_COMMAND` to call Termux to run command, and run `clangd` language server with `nc`, which builds an insistant I/O channel, offering functions as diagnosing and compilation.

## Features

- [x] Highighting
- [x] Autocompletion
- [x] Formatting
- [x] Diagnose
- [x] Compile flags
- [x] Dark mode
- [ ] Debug
- [ ] Project manage
- [ ] Workspace

## Setup

To support the full functions as an IDE, please follow the setup instruction.

1. Install Termux([Github Releases](https://github.com/termux/termux-app/releases) or [F-Droid](https://f-droid.org/packages/com.termux)) first.
2. Run `pkg install clang` to install the clang compiler & clangd language server.
3. See [RUN_COMMAND Intent](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent#setup-instructions) to enable calls from 3rd-party apps.
4. Then install TermuC.

Otherwise you can also copy these commands and paste to Termux to initialize Termux automatically.

```bash
echo Setup storage
termux-setup-storage
DATA=package:com.termux
echo -n Setup draw over apps \(Press Enter\);read _
am start -a android.settings.action.MANAGE_OVERLAY_PERMISSION -d $DATA>/dev/null
echo -n Setup ignore battery optimizations \(Press Enter\);read _
am start -a android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS>/dev/null
echo -n Setup Startup \& Background pop-ups permissions \(Press Enter\);read _
am start -a android.settings.APPLICATION_DETAILS_SETTINGS -d $DATA>/dev/null
echo Setup allow-external-app to \`true\'
fl=/data/data/com.termux/files/home/.termux/termux.properties
if [ -f $fl ];then
awk '/^#/{print;next }/^\s*allow-external-apps/{gsub(/allow-external-apps.*/,"allow-external-apps=true");found=1}{print $0}END{if(!found)print "allow-external-apps=true"}' $fl>$TMPDIR/a.tmp && mv $TMPDIR/a.tmp $fl
else
mkdir -p `dirname $fl`
echo 'allow-external-apps=true'>$fl
fi
echo Install Clang
pkg i clang -y
apt autoremove --purge
apt clean
echo ok
```