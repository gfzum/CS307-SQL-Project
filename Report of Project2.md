# Report of Project2

*12011411 吴笑丰	12011412 杨鹭鸣	12011216 刘锦润*

# content

- 0.小组成员分工与贡献
- 1.数据库介绍
- 2.各个接口功能的实现
- 3.本地测试样例完成情况
- 4.可进行的优化
- 5.总结与反思

[TOC]

# 0 小组成员分工与贡献

| 姓名   | 学号     | 完成的任务                       | 贡献比 |
| ------ | -------- | -------------------------------- | ------ |
| 吴笑丰 | 12011411 | 实现部分接口+建立git仓库         | 33%    |
| 杨鹭鸣 | 12011412 | 实现部分接口+建立数据库+效率优化 | 34%    |
| 刘锦润 | 12011216 | 实现部分接口+制作报告与ppt       | 33%    |



# 1 数据库介绍

## 1.0 Diagram

![image-20211229025838256](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211229025838256.png)

## 1.1 表的结构与功能

 - instructor表中储存教师的身份信息，包括主键id，first_name和last_name。

 - semester表中为每个学期对应的信息，包括主键id，学期名sem_name（如2018-2019-1，表示2018到2019学年的夏季学期），学期开始的时间sem_begin（如2018-09-03）以及学期结束的时间sem_end（如2019-02-03）。
 - department表中存储各院系的信息，仅包括主键id和对应的名称。
 - prerequisite表中存储每个院系进专业前需满足的先修课要求，它仅包括id和course_id两列，且只通过couse_id进行外键约束与course相关。
 - major表中是所有专业的信息，每个专业有属于自己的主键id，专业名称major_name和该专业隶属于哪个院系，以department_id作为外键约束与对应的院系相关。同时，与major相连的表有student和course_majors，表示每个学生选择的专业以及该专业所包含的专业课程。
 - major表中是所有专业的信息，每个专业有属于自己的主键id，专业名称major_name和该专业隶属于哪个院系，以department_id作为外键约束与对应的院系相关。同时，与major相连的表有student和course_majors，表示每个学生选择的专业以及该专业所包含的专业课程。
 - course_majors表中以course_id和major_id组合起来作为双主键，并且以course_type记录该专业课程的类型是专业选修课还是专业必修课。
 - course_section表记录的是每节课程的id，名字section_name(例如英文1班)，该节课程的课程id——course_id，该节课程的总容量total_capacity和目前剩余的课容量left_capacity以及该节课程对应的学期的id——semester_id。
 - classes表中包含了每门课程中各个班级的信息。week_num是该班级上课时间在学期的第几周，它和class_id组合构成双主键。instructor_id作为外键和instructor表相关联，记录了该班级教师的id。day_of_week记录了该班级在每周的周几上课，location记录了该班级的上课地点。class_begin和class_end是该班每天上课的开始和结束时间对应的课节。它以section_id作为外键约束与section表关联，表示该班级从属于该节课。
 - student_selections表表示学生的选课情况。主键id为自增id，外键student_id和section_id表示某student选择了某section，并以grade记录该学生该门课程的成绩。
 - course表便是所有课程所对应的信息。包括每门课程独有的course_id，并将它作为主键；课程名称course_name，学分credit，学时class_hour，该门课程的评分方式（若为百分制，则记录为HUNDRED_MARK_SCORE，若为pf制，则记录为PASS_OR_FAIL）以及此课程的先修课信息。通过course_id我们可以找到任何一门课程从属的专业、先修课的内容以及对应的course_section。值得一提的是，我们将读入的先修课信息的格式标准为逻辑表达式的形式，包含课程，括号以及&、|，其中"&"表示先修课间的“且”关系，"|"表示先修课间的“或”关系。我们将格式化的方法实现在MyCourseService中，处理先修课信息的方法实现在MyStudentService中，稍后我们会详细讲到。

​		

## 1.2 三大范式

>**1**.**第一范式(确保每列保持原子性)**

第一范式是最基本的范式。如果数据库表中的所有字段值都是不可分解的原子值，就说明该数据库表满足了第一范式。

>**2**.**第二范式(确保表中的每列都和主键相关)**

第二范式在第一范式的基础之上更进一层。第二范式需要确保数据库表中的每一列都和主键相关，而不能只与主键的某一部分相关（主要针对联合主键而言）。

也就是说在一个数据库表中，一个表中只能保存一种数据，不可以把多种数据保存在同一张数据库表中。

>**3**.**第三范式(确保每列都和主键列直接相关,而不是间接相关)**

第三范式需要确保数据表中的每一列数据都和主键直接相关，而不能间接相关。



在建造数据库的时候，许多数据不能简单直接地存入数据库，因为这样会违反三大范式。

因此，我们发现了不少问题并找到了解决方案，以下：

### （1）自增id

在json文件中，major和department类型的数据是自带id的，但是dto文件中的参数类型说明中并没有提到这类id，因此在传入函数里并没有关于该id的参数。s

因此，在最初版本的数据库中，建库的时major表格以及department表格中的id我们都是当作传入的id来处理的，但事实上要用自增id作为主键，这样表中的每一列都与同行所在的主键自增id相关，符合第二范式和第三范式。

需要补充的是，此外，要使用自增id的还有section_id, semester_id和class_id等表格

### （2）将数组拆分为单个元素存储

起初建立数据库时，我们将week_list以数组的形式存储在classes中。但是这样很明显违反了第一范式，即表格中每一列都应时刻保持着不可分割的原子性。

因此，我们将week_list中的每一个元素都单独取出，以“week_num”存储在新版classes表格中。对于一个含有16个数字的week_list，将会产生16行的classes。

值得一提的是，我们之前理解的day_of_week是一个集合，因为一周可能有多个时间出现一个class。但是这个数据中day_of_week就只有一个可能。

考虑到这样会出现一些一样的class_id，于是我们将classes表的主键改成了class_id和week_num组成的数对。

但是我们发现了插入数据的时候会产生问题，因为对于同一class的不同week_num,它们的class_id此时不应该增加，换句话说，它们应该拥有相同的class_id。于是我们就要将classes对应的数据分成两块插入，第一块插入第一条数据获得一个自增id，后面都把class_id定义成这个自增id，就可以达成目的了。



# 2 各个接口功能的实现

​		由于许多接口需要实现的功能较少，SQL语句和Java语句的难度也相对较低，考虑到这篇报告的篇幅和可读性，加上我们已经提交project2相关的全部代码，这里就不将所有功能一一赘述了。我们只选择了部分难度高、代码量大的接口和其对应的功能在报告里进行阐述。一下为各个接口功能实现的介绍，包括了我们遇到的问题和解决的方案。

## 2.1 CourseService

​		CourseService是本次project代码量相对较多，难度相对较大的一个类。它承担了建立course表、prerequisite表和classes表等等功能，是本次project的核心模块之一。

### 1）先修课信息的存储

​			第一个难题便是如何将先修课的信息存储下来。先修课的结构比较复杂，有不断的“且”和“或”关系相互嵌套，无法简单直接地存储在prerequisite表里。因此，我们的首要目标便是将先修课信息的格式统一。于是，我们在MyCourseService首先实现了将先修课格式化的名为“prerequisiteToString”方法。

```java
    public String prerequisiteToString(Prerequisite prerequisite) {
        if (prerequisite == null)
            return "";
        String ans = "";
        if (prerequisite instanceof CoursePrerequisite) {
            String courseId = ((CoursePrerequisite) prerequisite).courseID;
            addCourseToPrerequisite(courseId);
            return "(" + courseId + ")";
        }
        if (prerequisite instanceof AndPrerequisite) {
            int kh = ((AndPrerequisite) prerequisite).terms.size();
            while (kh > 1) {
                ans += "(";
                kh--;
            }
            for (Prerequisite p : ((AndPrerequisite) prerequisite).terms) {
                if (ans.endsWith("(")) ans += prerequisiteToString(p);
                else ans = ans + "&" + prerequisiteToString(p) + ")";
            }
        }
        if (prerequisite instanceof OrPrerequisite) {
            int kh = ((OrPrerequisite) prerequisite).terms.size();
            while (kh > 1) {
                ans += "(";
                kh--;
            }
            for (Prerequisite p : ((OrPrerequisite) prerequisite).terms) {
                if (ans.endsWith("(")) ans += prerequisiteToString(p);
                else ans = ans + "|" + prerequisiteToString(p) + ")";
            }
        }
        return ans;
    }
```

​		这个方法的作用是将个包括CoursePrerequisite、AndPrerequisite和OrPrerequisite的Prerequisite的List转换成字符串的形式。比如，“MA102A或MA102B选择一门学习，并且须学习RD267”这一先修课关系对应的Prerequisite的List应为{OrPrerequisite，AndPrerequisite}，OrPrerequisite是一个包括两个CoursePrerequisite的List，即MA102A和MA102B，AndPrerequisite是一个只包含一个CoursePrerequisite的List，即为RD267.我们想要得到的字符串形式应为(((MA102a)|(MA102B))&(RD267))。

​		此方法的核心思想为递归，其本质其实是一个建树的过程。在上面的例子中，我们通过深度优先遍历建立了一棵CoursePrerequisite作为叶子节点，逻辑符号“&”和“|”均为非叶子节点的二叉树。需要说明的是，在其他样例中可能会出现多叉树，二叉树只是一种特殊情况。

![image-20211231031833046](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231031833046.png)

​		这样，当我们想要得到课程对应先修课的信息，只需对标准格式化后的字符串进行反向处理即可

### 2）对week_list的处理

​		前文提到过，为了满足第一范式，即不可分割性，课程对应的week_list不能简单直接地加入表格中。我们需要将list中的每一个元素取出，在表中形成对应的单独的行，这样就保证了遵循第一范式。

​		以下为相关Java代码：

``` java
@Override
public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
    try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
        List<CourseSectionClass> result = new ArrayList<>();

        PreparedStatement prepareStatement = connection.prepareStatement("select class_id, i.instructor_id as instructor_id, i.first_name||' '||i.last_name as full_name,day_of_week, class_begin, class_end, location, week_num from classes inner join instructor i on i.instructor_id = classes.instructor_id where section_id=? group by class_id, instructor_id, full_name, i.instructor_id, day_of_week, class_begin, class_end, location");
        prepareStatement.setInt(1, sectionId);
        ResultSet resultSet = prepareStatement.executeQuery();

        while (resultSet.next()) {
            CourseSectionClass courseSectionClass = new CourseSectionClass();
            Instructor instructor = new Instructor();

            courseSectionClass.id = resultSet.getInt(1);
            instructor.id = resultSet.getInt(2);
            instructor.fullName = resultSet.getString(3);
            courseSectionClass.instructor = instructor;
            courseSectionClass.dayOfWeek = DayOfWeek.of(resultSet.getInt(4));
            courseSectionClass.classBegin = (short) resultSet.getInt(5);
            courseSectionClass.classEnd = (short) resultSet.getInt(6);
            courseSectionClass.location = resultSet.getString(7);
            Array weekList = resultSet.getArray(8);

            Set<Short> week_list = new HashSet<>();
            for (Object o : (Object[]) weekList.getArray()) {
                if (o instanceof Number) {
                    try {
                        int x = (int) o;
                        short s = (short) x;
                        week_list.add(s);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            courseSectionClass.weekList = week_list;
            result.add(courseSectionClass);
        }
        connection.close();
        if (result.isEmpty()) {
            return List.of();
        } else
            return result;
    } catch (SQLException e) {
        e.printStackTrace();
        throw new IntegrityViolationException();
    }
}
```

​		以下为class表相关部分的节选：

![屏幕截图 2021-12-31 033951](C:\Users\JR\Desktop\屏幕截图 2021-12-31 033951.png)

## 2.2 StudentService

​		毫无疑问，StudentService的实现是我们整个project2中工作量最大（可以从代码里看出来），难度最高的部分。我们将挑选部分具有代表性的功能来进行阐述。

### 1）先修课关系的读取

​		在前文CourseService中，我们已经利用递归和树的思想实现了先修课关系的格式化读入和存储。在StudentService中，我们实现了名为“checkSatisfiedCondition”的方法来处理已经存储的先修课关系。

​		“checkSatisfiedCondition”的基本思想仍然是递归，将表示先修课关系的字符串进行解构。

​		代码如下：

```java
private boolean checkSatisfiedCondition(int studentId, String prereStr) {
    if (!prereStr.startsWith("("))
        return havePassedCourse(studentId, prereStr);
    int cnt = 1, i = 1;
    for (; i < prereStr.length(); i++) {
        if (prereStr.charAt(i) == '(') cnt++;
        if (prereStr.charAt(i) == ')') cnt--;
        if (cnt == 0) break;
    }
    if (cnt != 0)
        throw new IllegalStateException();

    boolean retX = checkSatisfiedCondition(studentId, prereStr.substring(1, i));
    boolean retY = checkSatisfiedCondition(studentId, prereStr.substring(i + 3, prereStr.length() - 1));

    if (prereStr.charAt(i + 1) == '&')
        return retX & retY;
    else if (prereStr.charAt(i + 1) == '|')
        return retX | retY;
    else
        throw new IllegalStateException();
}
```

### 2）SearchCourse

#### 2.目前思路

首先，根据所给参数写出基础sql，然后根据所给CourseType的不同细分了五种case，在每种case里对sql进行微小调整后得到最后的查询sql。我们将class中的weekList拆分存储，但由于在查询中使用了`distinct`字样，所以每次查询到的结果依然可以代表一个单独的class。

在搜索过程中，简单参数仅需进行等于或like条件判定即可，对于locations，使用了`Any()`方法。
另外，由于传入时参数仅仅为”一教“、“荔园”等字样，而课程的实际地点为“一教406”、“荔园207”这样的字样，
所以对原location list的每个元素后面加了%传入，然后用like匹配。

```java
String sql_basic_select =
    "select distinct " +
    "       co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,\n" +
    "       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,\n" +
    "       cl.class_id, cl.instructor_id, i.first_name, i.last_name,\n" +
    "       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end, cl.location,\n" +
    "co.course_name || '[' || cs.section_name || ']'\n" +
    "from course co\n" +
    "    join course_section cs on co.course_id = cs.course_id\n" +
    "    join classes cl on cs.section_id = cl.section_id\n" +
    "    join instructor i on cl.instructor_id = i.instructor_id\n";

String sql_basic_where =
    "and (cs.course_id like ('%'|| ? ||'%') or ? is null)\n" +
    "and (co.course_name || '[' || cs.section_name || ']' like ('%'|| ? ||'%') or ? is null)\n" +
    "and (i.first_name like (? ||'%') or i.last_name like (? ||'%')\n" +
    "    or (i.first_name || i.last_name) like (? ||'%')\n" +
    "    or (i.first_name || ' ' || i.last_name) like (? ||'%') or ? is null)\n" +
    "and (cl.day_of_week = ? or ? is null)\n" +
    "and (? between cl.class_begin and cl.class_end or ? is null)\n" +
    "and (cl.location like Any (?) or ? is null)\n";

String sql;

switch (searchCourseType) {
    case ALL:
        sql = sql_basic_select +
            "where semester_id = ?\n"
            + sql_basic_where
            +"order by co.course_id, co.course_name || '[' || cs.section_name || ']' ";
		//set PrepareStatement
        break;

    case MAJOR_COMPULSORY:
    case MAJOR_ELECTIVE:
        sql = sql_basic_select +
            "join course_majors cm on co.course_id = cm.course_id\n" +
            "    join student s on cm.major_id = s.major_id\n" +
            "where s.student_id = ? and semester_id = ?\n"
            + sql_basic_where;
        if (searchCourseType == CourseType.MAJOR_COMPULSORY)
            sql = sql + " and cm.course_type = 'MAJOR_COMPULSORY\n' ";
        if (searchCourseType == CourseType.MAJOR_ELECTIVE)
            sql = sql + " and cm.course_type = 'MAJOR_ELECTIVE'\n";
        sql = sql + "order by co.course_id, co.course_name, cs.section_name";
		//set PrepareStatement
        break;

    case CROSS_MAJOR:
        sql = sql_basic_select +
            "join course_majors cm on co.course_id = cm.course_id\n" +
            "    join student s on cm.major_id <> s.major_id\n" +
            "where s.student_id = ? and semester_id = ?\n"
            + sql_basic_where
            + "order by co.course_id, co.course_name, cs.section_name";
        //set PrepareStatement
        break;

    case PUBLIC:
        sql = sql_basic_select +
            "join course_majors cm on co.course_id <> cm.course_id\n" +
            "where semester_id = ?"
            + sql_basic_where
            + "order by co.course_id, co.course_name, cs.section_name";
		//set PrepareStatement
        break;
}
```



使用当前的secion_id找到对应的所有class存入改searchCourseEntry。

随后，关于冲突列表的寻找，使用了一个大sql语句实现。

```java
PreparedStatement st_conf = connection.prepareStatement(
    "select course_name || '[' || section_name || ']' from(\n" +
    "select distinct s2.course_name, s2.section_name\n" +
    "from\n" +
    "(select co1.course_id,\n" +
    "       cl1.week_num, cl1.day_of_week, cl1.class_begin, cl1.class_end\n" +
    "from course co1\n" +
    "    join course_section cs1 on co1.course_id = cs1.course_id\n" +
    "    join classes cl1 on cs1.section_id = cl1.section_id\n" +
    "where cs1.section_id = ?) as s1 --当前section\n" +
    "join\n" +
    "(select co.course_id, co.course_name, cs.section_name,\n" +
    "       cl.week_num, cl.day_of_week, cl.class_begin, cl.class_end\n" +
    "    from course co\n" +
    "    join course_section cs on co.course_id = cs.course_id\n" +
    "    join classes cl on cs.section_id = cl.section_id\n" +
    "    join student_selections ss on cs.section_id = ss.section_id\n" +
    "where student_id = ? and semester_id = ?) s2 --学生选的\n" +
    "on s2.course_id = s1.course_id\n" +
    "or (s2.week_num = s1.week_num and s2.day_of_week = s1.day_of_week\n" +
    "    and (  (s2.class_begin <= s1.class_begin and s2.class_end >= s1.class_end)\n" +
    "        or (s2.class_begin >= s1.class_begin and s2.class_begin <= s1.class_end)\n" +
    "        or (s2.class_end >= s1.class_begin and s2.class_end <= s1.class_end)))\n" +
    ") sub\n" +
    "order by course_name, section_name");
st_conf.setInt(1, section.id);
st_conf.setInt(2, studentId);
st_conf.setInt(3, semesterId);

ResultSet rs_conf = st_conf.executeQuery();
boolean is_conflicted = false;
while(rs_conf.next()){
    is_conflicted = true;
    conflict.add(rs_conf.getString(1));
}
```



接下来，令rs指针向前移动，直到遇到下一个新的searchCourseEntry。

```java
while (rs.next()) {
    if (!rs.getString(1).equals(course.id) || rs.getInt(6) != section.id) {
        rs.previous();
        break;
    }
}
```

关于四个ignore标签的实现，只需在其为true的时候进行对应条件的判断，若遇到需要过滤的条件，直接continue循环即可。

```java
if (ignoreFull)
    if (section.leftCapacity == 0) continue;
if (ignorePassed)
    if (havePassedCourse(studentId,course.id)) continue;
if (ignoreMissingPrerequisites)//caonima
    if (!passedPrerequisitesForCourse(studentId,course.id)) continue;
if (ignoreConflict && is_conflicted) continue;
```



#### 1.第一版思路

在最开始，我们的想法是，由于考虑到之后需要列出并过滤冲突课程列表，所以先把学生已经选了的课选出，并将已选section的course_id存入一个Hashset，
把已选class的{周、日、时间段}建立一个新的Schedule类对象并存入另一个Hashset。需要注意的是，这里重写了Schedule类的equals方法，认为只要两个schedule的周、日相同且时间段有冲突，则为相同的Schedule。之后仅需判断选中课程是否与这两个set里的元素冲突即可。

![image-20211231061449115](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231061449115.png)

```java
class Schedule {
    int week_num;
    DayOfWeek day;
    int begin_time;
    int end_time;

    public Schedule(int week_num, DayOfWeek day, int begin_time, int end_time) {
        this.week_num = week_num;
        this.day = day;
        this.begin_time = begin_time;
        this.end_time = end_time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule Schedule = (Schedule) o;

        boolean time_conflict = false;
        if (Schedule.begin_time <= this.begin_time && Schedule.end_time >= this.end_time ||
                Schedule.begin_time >= this.begin_time && Schedule.end_time <= this.end_time ||
                Schedule.end_time >= this.begin_time && Schedule.end_time <= this.end_time)
            time_conflict = true;

        return week_num == Schedule.week_num &&
                day == Schedule.day
                && time_conflict;
    }

    @Override
    public int hashCode() {
        return Objects.hash(week_num, day, begin_time, end_time);
    }
}
```

```java
List<CourseSearchEntry> result = new ArrayList<>();
//已选课程
String sql_student_selections =
        "select co.course_id,\n" +
                "       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end\n" +
                "from course co\n" +
                "    join course_section cs on co.course_id = cs.course_id\n" +
                "    join classes cl on cs.section_id = cl.section_id\n" +
                "    join student_selections ss on cs.section_id = ss.section_id\n" +
                "where ss.student_id = ?";

PreparedStatement st_select = connection.prepareStatement(sql_student_selections);
st_select.setInt(1, studentId);
ResultSet rs_select = st_select.executeQuery();

HashSet<String> course_enrolled = new HashSet<>();
HashSet<Schedule> schedule_enrolled = new HashSet<>();

while (rs_select.next()) {
    course_enrolled.add(rs_select.getString(1));

    int week_num = rs_select.getInt(3);
    DayOfWeek day = DayOfWeek.of(rs_select.getInt(2));
    int c_begin = rs_select.getInt(4);
    int c_end = rs_select.getInt(5);

    schedule_enrolled.add(new Schedule(week_num, day, c_begin, c_end));
}
```

在考虑conflict list时，在每次的while(rs.next())循环里，创建CourseSearchEntry并添加course和section，进行courseConflict判定，然后在classSet中添加当前class并进行timeConflict判定。

随后，在当前的while循环里嵌套另一个while(rs.next())循环，判断下一行中的course与section是否与当前相同，即是否还在当前CourseSearchEntry的范围内，若是，则继续添加class并进行timeConflict判定，直到出现不同的course时执行rs.previous或rs.next为null。需要注意的是，在这个嵌套的while中，如果已经有timeConflict标签，则不需再进行timeConflict判定；如果同时有timeConflict和ignoreConfilct标签，则class的添加也不需执行，只需让rs不断next到下一个不同的CourseSearchEntry即可，最后该CourseSearchEntry也不会添加到result中。

接下来，考虑搜索课程的过程，对于简单参数仅需进行等于或like条件判定即可，对于locations，使用了`Any()`方法。 另外，由于传入时参数仅仅为”一教“、“荔园”等字样，而课程的实际地点为“一教406”、“荔园207”这样的字样， 而any()又意外地会出现莫名bug，所以对原location list的每个元素后面加了%传入，然后用like匹配。 对于courseType，考虑根据传入的参数使用不同的sql语句进行查询。 ALL：不关联 MAJOR_COMPULSORY：student_major_id = course_major_id, type = comp MAJOR_ELECTIVE：=, type = elective CROSS_MAJOR：<> PUBLIC：关联，course_id <> major_course_id。

接下来，关于四个ignore标签的实现，只需在其为true的时候进行对应条件的判断，当条件符合要求时再往答案list里添加即可。

最后，考虑到这么写最终无法在数据库中使用order by 排序最后结果，放弃了在数据库中建新表并导入数据然后在排序后导出数据的做法。

## 2.3 Others

### 1）InstructorService

​		dept_name 须添加unique约束，并在addDepartment中根据该约束可能出现的错误进行IntegrityViolationException的抛出；
但对于 instructor_name，由于可能出现同名老师，故不须进行unique约束和异常抛出。

```java
@Override
public int addDepartment(String name) {

    try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
         PreparedStatement st = connection.prepareStatement(
                 "insert into department(dept_name) values (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
        st.setString(1, name);
        st.executeUpdate();

        ResultSet rs = st.getGeneratedKeys();

        if(rs.next()) {
            int ret = rs.getInt(1);
            connection.close();
            return ret;
            //EntityNotFound?
        } else {
            connection.close();
            throw new EntityNotFoundException();
        }

    } catch (SQLException e) {
        e.printStackTrace();
        throw new IntegrityViolationException();
    }
}
```

### 2）UserService

​		MyUserService 中的 removeUser 与 getUser方法，UserId应该同时面向student及instructor；但由于建表时两者分开，故分别使用两条语句在两个表中进行对应id的检索。

​		必然的，其中一条会因为找不到id而报错，此时EntityNotFound exception将会对其进行处理。

​		此外，对于User中的fullName要求，利用正则表达式构建fullName方法，写在MyUserService类中，添加public static关键字供全局调用。

```java
//构建fullName方法，public static 共全局调用
public static String getFullName(String firstName, String lastName){
    if (firstName.matches("[a-zA-Z ]+") && lastName.matches("[a-zA-Z ]+")){
        //System.out.println(firstName + " " + lastName);
        return firstName + " " + lastName;
    }
    return firstName + lastName;
}
```

## 2.4 遇到的问题

### 1）enrollCourse

​		好不容易写完了所有内容，然后一跑发现enrollCourse居然需要跑整整20分钟，1200秒。这个结果显然是不可以被接受的。

​		然后一直在思考是什么问题。

​		后来发现是建库的时候没有加index索引，直接导致了delete这个操作巨慢无比。

​		我们使用index后效率提升巨大，从原来的1200秒下降到了20秒左右，效率足足提高了足足60倍！

![image-20211231042815141](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231042815141.png)

### 2）dropCourse

​		发现dropCourse的得分是零分，并且时间奇慢无比。时间的优化应该可以通过加入index解决，但是我们左思右想也不明白为什么是0，ProjectJudge也并没有给出原因和错误信息。

![image-20211231043244084](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231043244084.png)

​		重新阅读要求后，发现dropCourse是一个所有点都需要报错的测试。原来是理解错题意了。经过修理，drop course四十多万的测试点成功通过；由于使用了index，时间也由原来的1200多秒变为四秒左右。效率提升了300多倍......

![image-20211231043549307](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231043549307.png)

### 3）enrollCourse2

​		enrollCourse2的test给我们造成了许多困扰，甚至每次可以正确通过的点数都不是一定的。

​		然后事实上我们在写判断是否AlreadyPassCourse的时候只判断了其中某一节的section是否大于六十分，实际上应该判断是否每一节section都小于六十分，不然就是算通过的。

​		另一种方法就是在写sql的时候返回值按照grade大小排序也可以。

​		于是enrollCourse2终于稳定在了787个点，开始de剩下的bug。

​		然后发现bug出现在COURSE_CONFLICT这个点上。

​		COURSE_CONFLICT其实是比较复杂的，sql语句也比较长。后来发现是sql里面判断了一个grade=null的判断句，来验证这个课程是否是这个学期选的。结果发现逻辑上是有问题的，因为原来这个学期选得课程也可能被通过

​		AddCourseGrade被手动添加进来，结果这个学期选的课也可以有grade！！（很迷惑）

​		最后把判断语句改成了semester相等就通过了所有测试点。

### 4）autocommit优化

​		一个未成功的优化方法：关闭autocommit

​		结果发现关闭autocommit后手动commit反而还更慢！

​		![image-20211231050350411](C:\Users\JR\AppData\Roaming\Typora\typora-user-images\image-20211231050350411.png)

# 3 本地测试样例完成情况



# 4 可进行的优化

## 1）减少通讯次数

​		理论上我们可以通过合并查询语句的方式，或者使用PreparedStatement的Batch功能。

​		Update大量的数据时， 先Prepare一个INSERT语句再多次的执行， 会导致很多次的网络连接.。要减少JDBC的调用次数改善性能,，你可以使用PreparedStatement的AddBatch()方法一次性发送多个查询给数据库。

​		本次project没有使用batch的原因是，不同的方法分布在不同的接口里，例化后也只能开启关闭单独的通讯，无法批量操作。

## 2）关闭autocommit

​		自动提交模式意味着除非显式地开始一个事务，否则每个查询都被当做一个单独的事务自动执行。

​		理论上关闭autocommit可以使多条数据库插入或更新得效率提升。

## 3）在多表联查时，需要考虑连接顺序问题

​		当postgresql中进行查询时，如果多表是通过逗号，而不是join连接，那么连接顺序是多表的笛卡尔积中取最优的。

​		如果有太多输入的表， PostgreSQL规划器将从穷举搜索切换为基因概率搜索，以减少可能性数目(样本空间)。基因搜索花的时间少， 但是并不一定能找到最好的规划。

   	 LEFT JOIN / RIGHT JOIN 会一定程度上指定连接顺序，但是还是会在某种程度上重新排列：
   	
   	 FULL JOIN 完全强制连接顺序。
   	
   	 如果要强制规划器遵循准确的JOIN连接顺序，我们可以把运行时参数join_collapse_limit设置为 1。

参考来源：`https://www.cnblogs.com/churao/p/8494324.html`

## 4）外键关联的删除

 如果表的有外键的话，每次操作都没去check外键整合性。因此比较慢。数据导入后再建立外键也是一种选择。（但是这个方法被ban掉了qwq）



# 5 总结与反思

## 5.1 总结

​	本次project的难度不低，既考验了我们的Java功底，又需要写出高效SQL代码的能力。

​	在边写边学的过程中，我们完成了很多需求，同时有许多方面的收获。

​		1.接口实现

​		2.样例测试+自己制造样例

​		3.debug的技巧和心态

​		4.利用index提升效率及其利弊

​		5.事务管理相关知识

​		6.小组成员之间的团结合作、互帮互助

​	不言而喻的是，经过此次磨砺，我们都受益匪浅。希望日后能不断提升自己的能力，力求将project做得更加优秀，让自己的知识水平更上一层楼。

## 5.2 反思

​		此次project我们仍有许多可以提升的空间。例如，对时间的安排和任务的分配是不是可以更加合理；一些想法只停留在计划而没有去实现；我们的效率是不是还有提升的方法......

​		当然，追求完美是极难的，甚至是不可能的。但是，世上无难事，只要肯登攀，我们应当不惧困难，善于学习新的技能和解决各种问题，这也是project教会我们最重要的内容。



以上。
