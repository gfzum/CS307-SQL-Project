department

addDepartment中，发现接口中定义的方法没有传入id参数，而json文件中department数据中包含id属性，
且其从1开始递增，故在建表时将dept_id设置为自增。
同样的，major_id,semester_id也是自增的。
另外，考虑在查找返回时加入异常处理。

dept_name 须添加unique约束，并在addDepartment中根据该约束可能出现的错误进行IntegrityViolationException的抛出
但对于 instructor_name，由于可能出现同名老师，故不须进行unique约束和异常抛出

MyUserService 中的 removeUser 与 getUser方法，UserId应该同时面向student及instructor
但由于建表时两者分开，故分别使用两条语句在两个表中进行对应id的检索。
必然的，其中一条会因为找不到id而报错，此时entityNotFound exception将会对其进行处理。（可以吗？）
是否会因为第一条语句throw exception后导致第二条语句不执行？

对于User中的fullName要求，利用正则表达式构建fullName方法，写在MyUserService类中，添加public static关键字供全局调用

关于searchCourse的思路：
首先，考虑到之后需要列出并过滤冲突课程列表，所以先把学生已经选了的课选出，并将已选section的course_id存入一个Hashset，
把已选class的{周、日、时间段}建立一个新的Schedule类对象并存入另一个Hashset。需要注意的是，这里重写了Schedule类的
equals方法，认为只要两个schedule的周、日相同且时间段有冲突，则为相同的Schedule。
由于在sql中对除了weeknum的值使用了group by，故最后的返回表中只会有一个单独的class。
之后仅需判断选中课程是否与这两个set里的元素冲突即可

接下来，考虑搜索课程的过程，对于简单参数仅需进行等于或like条件判定即可，对于locations，使用了`Any()`方法。
对于courseType，考虑根据传入的参数使用不同的sql语句进行查询。
ALL：不关联
MAJOR_COMPULSORY：student_major_id = course_major_id, type = comp
MAJOR_ELECTIVE：=, type = elective
CROSS_MAJOR：<>
PUBLIC：关联，course_id <> major_course_id

接下来，关于四个ignore标签的实现，只需在其为true的时候进行对应条件的判断，当条件符合要求时再往答案list里添加即可

关于page