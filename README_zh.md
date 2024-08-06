# TermuC

TermuC 是一个简单的 C/C++ IDE，采用 Termux 作为后台。项目基于 [MrIkso/CodeEditor](//github.com/MrIkso/CodeEditor)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/cn.rbc.termuc)

## 截图

![1](fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg)

## 技术原理

本应用利用 `com.termux.RUN_COMMAND` 调用 Termux 执行编译命令，并利用 `nc` 运行 `clangd` 语言服务器，通过 TCP Socket 建立持续的 I/O 通道，提供代码诊断和补全功能。

## 特性

- [x] 代码高亮
- [x] 自动补全
- [x] 格式化
- [x] 代码诊断
- [x] 编译选项
- [x] 暗主题
- [ ] 调试
- [ ] 项目管理
- [ ] 工作空间

## 初始化

为了支持 Termux 提供的所有 IDE 功能，请根据以下步骤进行配置：

1. 安装 Termux([Github Releases](https://github.com/termux/termux-app/releases) 或 [F-Droid](https://f-droid.org/packages/com.termux))；
2. 运行 `pkg install clang`，安装 clang 编译器和 clangd 语言服务器；
3. 按照 [RUN_COMMAND Intent](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent#setup-instructions) 以允许第三方 App 调用 Termux 执行命令；
4. 最后安装 TermuC。