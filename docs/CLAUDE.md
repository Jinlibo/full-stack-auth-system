## Language Rules

* 所有回复必须使用 **简体中文**。
* 所有分析、计划、解释、代码说明均使用中文。
* 只有代码内容可以使用英文。
* 不要在回复中切换为英文说明。

## Repository Navigation Rules

在分析代码仓库、搜索文件、读取文件或修改代码时，必须忽略以下目录（所有层级）：

* target
* node_modules
* .idea

忽略规则：

```
**/target/**
**/node_modules/**
**/.idea/**
```

要求：

* 不要读取这些目录中的文件
* 不要搜索这些目录
* 不要修改这些目录
* 不要在任务计划中包含这些目录
* 不要将这些目录中的文件作为代码分析依据

## Code Analysis Rules

在分析项目结构时：

优先阅读以下目录：

* src/
* app/
* lib/
* config/

避免读取：

* target/
* node_modules/
* .idea/
* dist/
* build/
* .git/

## Workflow Rules

执行复杂任务时遵循以下流程：

1. 先分析项目结构
2. 提出实现计划
3. 等待确认
4. 再开始修改代码
5. 修改后执行构建或测试

## Performance Rules

为了减少上下文消耗：

* 不要读取大型生成文件
* 不要读取编译产物
* 不要读取依赖库目录
* 只关注源码目录
