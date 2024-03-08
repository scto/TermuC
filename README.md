# TermuC

[中文 README](./README_zh.md)

TermuC is a simple C/C++ IDE backed on Termux. Based on [MrIkso/CodeEditor](//github.com/MrIkso/CodeEditor)

## Screenshot

![Screenshot_20240306_125511](_res/Screenshot_20240306_125511.jpg)

## Technology

This app use `com.termux.RUN_COMMAND` to call Termux to run command, and run `clangd` language server with `nc`, which builds an insistant I/O channel, offering functions as diagnosing and compilation.

## Features

- [x] Highighting
- [x] Autocompletion
- [x] Diagnose
- [ ] Compile flags
- [ ] Debug
- [ ] Project manage
- [ ] Workspace
- [ ] Dark mode
