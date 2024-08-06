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