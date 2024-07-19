# TermuC

[中文 README](./README_zh.md)

TermuC is a simple C/C++ IDE backed on Termux. Based on [MrIkso/CodeEditor](//github.com/MrIkso/CodeEditor)

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
