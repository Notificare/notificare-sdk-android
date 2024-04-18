# Development guide

## Getting started

### IDE Plugins

#### KtLint

Even though KtLint is implemented as a dependency, there is a recommended Android Studio KtLint plugin that formats as you write.
It is recommended its download, configuring it in Distract Free mode.

### Git hooks

To ensure correct linting, formatting, and that the commit messages are up to convention, 2 git hooks are present in the .hooks folder.
To install them, please run the following command in root folder:

```shell
git config core.hooksPath .hooks
```
