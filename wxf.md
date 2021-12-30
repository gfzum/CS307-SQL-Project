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
之后仅需判断选中课程是否与这两个set里的元素冲突即可

conflict list的具体思路：在进行搜索时，由于在sql使用了distinct，故最后的返回表中会分出单独的class。
于是，在每次的while(rs.next())循环里，创建CourseSearchEntry并添加course和section，进行courseConflict判定，
然后在classSet中添加当前class并进行timeConflict判定。
随后，在当前的while循环里嵌套另一个while(rs.next())循环，判断下一行中的course与section是否与当前相同，
即是否还在当前CourseSearchEntry的范围内，若是，则继续添加class并进行timeConflict判定，
直到出现不同的course时执行rs.previous或rs.next为null。
需要注意的是，在这个嵌套的while中，如果已经有timeConflict标签，则不需再进行timeConflict判定；
如果同时有timeConflict和ignoreConfilct标签，则class的添加也不需执行，
只需让rs不断next到下一个不同的CourseSearchEntry即可，最后该CourseSearchEntry也不会添加到result中。

由于需要令rs回滚，我们将rs的类型指定为
ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY


接下来，考虑搜索课程的过程，对于简单参数仅需进行等于或like条件判定即可，对于locations，使用了`Any()`方法。
另外，由于传入时参数仅仅为”一教“、“荔园”等字样，而课程的实际地点为“一教406”、“荔园207”这样的字样，
而any()又意外地像坨屎，所以对原location list的每个元素后面加了%传入，然后用like匹配。
对于courseType，考虑根据传入的参数使用不同的sql语句进行查询。
ALL：不关联
MAJOR_COMPULSORY：student_major_id = course_major_id, type = comp
MAJOR_ELECTIVE：=, type = elective
CROSS_MAJOR：<>
PUBLIC：关联，course_id <> major_course_id

接下来，关于四个ignore标签的实现，只需在其为true的时候进行对应条件的判断，当条件符合要求时再往答案list里添加即可

考虑到狗屎的order by，把conflict改为了用sql去查找并order by
注意到文档里“Matches *any* class in the section”的说法，故直接搜索得到的section id下的class，删掉了优雅的写法。



关于page