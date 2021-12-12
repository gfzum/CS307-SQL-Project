
# 2021 Fall CS307 Project 2 补充文档



## 1. 提交相关

BlackBoard 源代码&小组报告 提交 deadline: **December 31, 18:30**

lab课答辩: **December 31, lab课**



### 源代码提交格式：

请尽量遵循以下格式，这会尽可能减轻SA的工作量

1. 每组只交一份

2. 单个Zip文件（不要使用rar,tar,gz等其他格式，推荐使用自由软件 7-Zip）

3. 文件结构：

   1. SQL文件放在项目根目录下 cs307.sql ，使用小写
   2. 删除项目中的文档 project_readme.md, README.md, document_assets
   3. 选中在项目根目录下的所有文件，7-Zip -> Add to "xxx.zip"

   <img src="/home/luvletter/.config/Typora/typora-user-images/image-20211209163514842.png" alt="image-20211209163514842" style="zoom:67%;" />

4. 压缩包文件名：提交者学号.zip

5. 请勿手动导入jar包，请使用maven

   In this project, we use Maven to manage dependent libraries. If you want to introduce a new library, you need to record it in pom.xml. Your dependencies should be downloadable from the Maven Central repository.



### 评分标准

|                   | 占比 |
| ----------------- | ---- |
| 小组报告&课堂答辩 | 20%  |
| 样例得分          | 80%  |

样例分为多组，每组独立计分，最后加权平均

对于每组样例，满分100%：

|                                     | 占比  |
| ----------------------------------- | ---- |
| 正确率 =  正确样例个数 / 总样例个数      | 75%  |
| 运行速度 = 相对运行速度 * 正确率         | 25%  |

- 相对运行速度换算表：

  | 相对运行时间 (倍率) | 相对运行速度 |
  | ------------------- | ------------ |
  | <= 1x               | 100%         |
  | <= 5x               | 60%          |
  | <= 10x              | 50%          |
  | <= 25x              | 0%           |

- 每档之间线性计算

- sampleResult.txt 中的绝对用时可以不做参考（机子性能不同），但是可以参考每组测试样例之间的相对速度




### 评测环境

Ubuntu 18.04.6
CPU: Intel Xeon Gold 6240 (16) @ 2.600GHz
RAM: 32G
IO: Unknown
PostgreSQL: 14 (docker)
OpenJDK 17

>java -version 
>openjdk version "11.0.11" 2021-04-20 
>OpenJDK Runtime Environment (build 11.0.11+9-Ubuntu-0ubuntu2.18.04) 
>OpenJDK 64-Bit Server VM (build 11.0.11+9-Ubuntu-0ubuntu2.18.04, mixed mode, sharing)



### FAQ

1. cn.sustech.** 框架里哪些东西可以改？

   在测评的时候会把 cn.sustech.** 整个包替换为初始状态，你只需要确保你的程序能正常运行即可

   例如：

   - 不能改dto的constructor
   - 可以加toString函数方便调试
   - 可以修改本地测评程序ProjectJudge，以此显示正确和错误的数据点
   - 不要给接口添加 throw xxxException，SQLException请自行处理

   

2. 如何确保我的数据库能在服务器上正确构建

   按照 Tips 中导出数据库结构，然后在新建一个数据库，将表结构导入新库中，修改 config.properties，让程序在新库上正常跑

