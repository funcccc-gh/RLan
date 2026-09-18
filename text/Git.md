# Git 使用手册

## 命令

1. `git --version` 查看 Git 版本，验证是否安装成功
2. `git config --global user.name <name>` 设置全局用户名
3. `git config --global user.email <email>` 设置全局邮箱
4. `git config --global user.name` 查看用户名
5. `git config --global user.email` 查看邮箱
6. `cd <path>` 进入指定文件夹
7. `git init` 初始化本地仓库
8. `git status` 查看文件状态,获取文件管理信息
9. `git add <filename>`把文件加入暂存区
10. `git add .` 加入所有修改/未跟踪文件
11. `git commit -m <description>` 提交暂存区文件到本地仓库
12. `git log` 查看详细提交记录
13. `git log --oneline` 查看精简提交记录
14. `git remote add <repository-name> <repository-link>` 关联远程仓库
15. `git remote -v` 查看远程仓库关联信息
16. `git remote remove <repository-name>` 删除关联的远程仓库
17. `git push -u <repository-name> <branch-name>` 第一次推代码到 GitHub 把本地代码传到云端
18. `git push` 推代码到 GitHub 后续更新代码时用
19. `git pull <repository-name> <branch-name>` 从 GitHub 拉代码到本地
20. `git commit --amend -m <new-description>` 修正上一次提交/补加漏传的文件或改提交信息

## 其他

- git bash 可以使用`Shift+Ins+(Fn)` 粘贴
- github网速慢，命令执行失败可能是网络不稳定，可以**多运行几遍**

## 我的仓库

- `https://codeup.aliyun.com/6311f6c395064d67d44681ff/JYH.git`
- `https://github.com/funcccc-gh/Project.git`
