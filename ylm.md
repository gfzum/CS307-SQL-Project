SQL建库相关内容：

json文件中，major和department是有ID的，但是传入函数中并没有提到该id。
所以之前建库的时候以为是传入id，但事实上要用自增id。
此外，要使用自增id的还有section_id,semester_id,class_id

之前理解day_of_week是一个集合，因为一周可能有多个时间出现一个class，但是这个数据中day_of_week就只有一个可能。
所以将其由integer[]改成单个。
然后发现week_list同理 所以也改成单个吧。
!!!然后发现只有把这个int数组改为单个才能符合范式的要求，所以就按照int来吧。
然后考虑到这会出现一些一样的classId，于是我们将classes表的主键改成了classId并weekNum
然后就发现了插入数据的时候的问题。于是我们就要分成两块插入，第一块是插入第一条数据获得一个自增id，后面都把classId定义成这个自增id，就可以达成目的了。


优化方法：
或许可以用传入connection的方式做一些优化？


好不容易写完了所有内容，然后一跑发现居然需要跑整整20分钟，1200秒。这个结果显然是不可以被接受的
然后一直在思考是什么问题
后来发现是建库的时候没有加index索引，直接导致了delete这个操作巨慢无比。

然后发现dropCourse的得分是零分，左思右想也没有想明白为什么是0，然后ProjectJudge也并没有给出原因和错误信息。只好回去看一下文档了

好的 然后发现dropCourse是一个所有点都需要报错的测试，好吧，理解错题意了

然后发现我们的enrollCourse2，甚至于有了一个每次运行都不一样的正确点数。
然后事实上我在写判断是否AlreadyPassCourse的时候只判断了其中某一节的section是否大于六十分，实际上应该判断是否每一节section都小于六十分，不然就是算通过的。
另一种方法就是在写sql的时候返回值按照grade大小排序也可以。

于是enrollCourse2稳定在了787个点，开始de剩下的bug

然后发现bug出现在COURSE_CONFLICT这个点上。
COURSE_CONFLICT其实是比较复杂的，sql语句也比较长。后来发现是sql里面判断了一个grade=null的判断句，
来验证这个课程是否是这个学期选的。结果发现逻辑上是有问题的，因为原来这个学期选得课程也可能被通过
AddCourseGrade被手动添加进来，结果这个学期选的课也可以有grade！！
最后把判断语句改成了semester相等就通过了所有测试点。

然后开始进行优化：
1.测试autocommit(false)
    结果发现关闭autocommit然后手动commit的话反而还更慢。

autocommit（original）：

    Import student courses time: 20.47s
    Test drop course time: 5.40s
    Test enroll course 2 time: 1.03s

关闭autocommit：

    Import student courses time: 25.11s
    Test drop course time: 10.04s
    Test enroll course 2 time: 1.10s



