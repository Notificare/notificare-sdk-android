# Development guide

## Getting started

### Configuring auto registration

You have the option to set up a static user for automatic registration on your device. This feature helps the process of targeting your devices during development across various app installations.
Configure the following variables in `local.properties`

```
userId=
userName=
```

### IDE Plugins

#### KtLint

Even though KtLint is implemented as a dependency, there is a recommended Android Studio KtLint plugin that formats as you write.
It is recommended its download, configuring it in Distract Free mode.

#### Detekt

Even though KtLint is implemented as a dependency, there is a recommended Android Studio Detekt plugin that points out problems in code as you write.
It is recommended its download, configuring to use the Detekt configuration file present in `config/detekt`.


### Git hooks

To ensure correct linting, formatting, and that the commit messages are up to convention, 2 git hooks are present in the .hooks folder.
To install them, please run the following command in root folder:

```shell
git config core.hooksPath .hooks
```

#### Commitlint

Our commit-msg hook uses Commitlint to ensure the messages of our commits are in order. 
This hook requires the installation of Commitlint. To do so, global installation is recommended

```shell
npm install -g --save-dev @commitlint/{cli,config-conventional}
```
