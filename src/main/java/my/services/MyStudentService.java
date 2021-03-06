package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.exception.UnsupportedOperationException;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.service.StudentService;
import com.impossibl.jdbc.spy.CallableStatementRelay;

import javax.annotation.Nullable;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

public class MyStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into student(student_id, first_name, last_name, " +
                     "enrolled_date, major_id) values (?,?,?,?,?)")) {
            stmt.setInt(1, userId);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setDate(4, enrolledDate);
            stmt.setInt(5, majorId);
            stmt.execute();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<CourseSearchEntry> searchCourse
            (int studentId, int semesterId, @Nullable String searchCid,
             @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek,
             @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
             CourseType searchCourseType,
             boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites,
             int pageSize, int pageIndex) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {

            List<CourseSearchEntry> result = new ArrayList<>();
            //????????????
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

            //????????????
            String sql_basic_select =
                    "select distinct " +
                            "       co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,\n" +
                            "       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,\n" +
                            "       cl.class_id, cl.instructor_id, i.first_name, i.last_name,\n" +
                            "       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end, cl.location\n" +
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
                            "and (cl.location = Any (?) or ? is null)\n" +
                            "order by co.course_id, co.course_name, cs.section_name";

            String sql;

            switch (searchCourseType){
                case ALL:
                    sql = sql_basic_select +
                            "where semester_id = ?\n"
                            + sql_basic_where;

                    PreparedStatement st = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
                    st.setInt(1,semesterId);
                    st.setString(2,searchCid);
                    st.setString(3,searchCid);
                    st.setString(4,searchName);
                    st.setString(5,searchName);
                    st.setString(6,searchInstructor);
                    st.setString(7,searchInstructor);
                    st.setString(8,searchInstructor);
                    st.setString(9,searchInstructor);
                    st.setString(10,searchInstructor);
                    if (searchDayOfWeek == null){
                        st.setNull(11, Types.INTEGER);
                        st.setNull(12, Types.INTEGER);
                    }
                    else{
                        st.setInt(11,searchDayOfWeek.getValue());
                        st.setInt(12,searchDayOfWeek.getValue());
                    }
                    if (searchClassTime == null){
                        st.setNull(13, Types.INTEGER);
                        st.setNull(14, Types.INTEGER);
                    }
                    else{
                        st.setInt(13,searchClassTime);
                        st.setInt(14,searchClassTime);
                    }
                    if (searchClassLocations == null){
                        st.setNull(15, Types.ARRAY);
                        st.setNull(16, Types.VARCHAR);
                    }
                    else{
                        st.setArray(15,connection.createArrayOf("varchar",searchClassLocations.toArray()));
                        st.setString(16,"notNull"); //???????????????
                    }

                    ResultSet rs = st.executeQuery();

                    //???????????????????????????courseEntry
                    while(rs.next()){
                        CourseSearchEntry cse = new CourseSearchEntry();
                        Course course = new Course();
                        CourseSection section = new CourseSection();
                        Set<CourseSectionClass> classes = new HashSet<>();
                        List<String> conflict = new ArrayList<>();

                        course.id = rs.getString(1);
                        course.name = rs.getString(2);
                        course.credit = rs.getInt(3);
                        course.classHour = rs.getInt(4);

                        String grading = rs.getString(5);
                        if (grading.equals("PASS_OR_FAIL"))
                            course.grading = Course.CourseGrading.PASS_OR_FAIL;
                        else course.grading = Course.CourseGrading.HUNDRED_MARK_SCORE;

                        section.id = rs.getInt(6);
                        section.name = rs.getString(7);
                        section.totalCapacity = rs.getInt(8);
                        section.leftCapacity = rs.getInt(9);

                        //CourseConflict?????????ignore
                        if (course_enrolled.contains(course.id)){
                            PreparedStatement findConfSec = connection.prepareStatement("" +
                                    "select section_name from course_section join student_selections ss \n" +
                                    "on course_section.section_id = ss.section_id\n" +
                                    "where course_id = ? and student_id = ?");
                            findConfSec.setString(1,course.id);
                            findConfSec.setInt(2,studentId);
                            ResultSet confSection = findConfSec.executeQuery();
                            if (confSection.next())
                                conflict.add(String.format("%s[%s]", course.name, confSection.getString(1)));
                            if (ignoreConflict) continue;
                        }
                        if (ignoreFull)
                            if (section.leftCapacity == 0) continue;

                        //todo ?????????????????????????????????????????????sql
                        if (ignorePassed)
                            if (havePassedCourse(studentId,course.id)) continue;
                        if (ignoreMissingPrerequisites)
                            if (passedPrerequisitesForCourse(studentId,course.id)) continue;

                        //class??????
                        CourseSectionClass lesson = new CourseSectionClass();
                        lesson.id = rs.getInt(10);
                        Instructor ins = new Instructor();
                        ins.id = rs.getInt(11);
                        ins.fullName = MyUserService.getFullName(rs.getString(12),rs.getString(13));
                        lesson.instructor = ins;
                        lesson.dayOfWeek = DayOfWeek.of(rs.getInt(14));
                        lesson.classBegin = rs.getShort(16);
                        lesson.classEnd = rs.getShort(17);
                        lesson.location = rs.getString(18);

                        //time conflict
                        boolean timeConflict = false; //????????????conflict??????????????????
                        Schedule lesson_time = new Schedule(
                                rs.getInt(15), lesson.dayOfWeek, lesson.classBegin, lesson.classEnd);
                        if (schedule_enrolled.contains(lesson_time)) {
                            //?????????????????????section
                            PreparedStatement findConfTime = connection.prepareStatement("" +
                                    "select course_name, section_name from course_section\n" +
                                    "    join student_selections ss on course_section.section_id = ss.section_id\n" +
                                    "    join classes cl on course_section.section_id = cl.section_id\n" +
                                    "    join course co on course_section.course_id = co.course_id\n" +
                                    "where student_id = ? and cl.week_num = ? and cl.day_of_week = ?\n" +
                                    "    and ((cl.class_begin <= ? and cl.class_end >= ?) " +
                                    "       or (cl.class_begin >= ? and cl.class_begin <= ?)\n" +
                                    "       or(cl.class_end >=? and cl.class_end <= ?))");
                            findConfTime.setInt(1, studentId);
                            findConfTime.setInt(2, rs.getInt(15));
                            findConfTime.setInt(3, rs.getInt(14));
                            findConfTime.setInt(4, lesson.classBegin);
                            findConfTime.setInt(5, lesson.classEnd);
                            findConfTime.setInt(6, lesson.classBegin);
                            findConfTime.setInt(7, lesson.classEnd);
                            findConfTime.setInt(8, lesson.classBegin);
                            findConfTime.setInt(9, lesson.classEnd);
                            ResultSet confTime = findConfTime.executeQuery();
                            if (confTime.next())
                                conflict.add(String.format("%s[%s]", confTime.getString(1), confTime.getString(2)));

                            //?????????section????????????class?????????time_conflict?????????????????????conf???result??????????????????
                            timeConflict = true;
                        }

                        PreparedStatement week_st = connection.prepareStatement("" +
                                "select week_num from classes where class_id = ?");
                        week_st.setInt(1, rs.getInt(10));
                        ResultSet week_rs = week_st.executeQuery();
                        lesson.weekList = new HashSet<>();
                        while (week_rs.next())
                            lesson.weekList.add(week_rs.getShort(1));

                        classes.add(lesson);

                        //todo ?????????class???????????????sql???????????????group by???????????????????????????sql??????????????????????????????
                        //????????????rs next?????????????????????course
                        while (true) {
                            if (rs.next()) {
                                if (rs.getString(1).equals(course.id) && rs.getInt(6) == section.id) {
                                    //???????????????????????????????????????????????????
                                    if (!(ignoreConflict && timeConflict)) {
                                        CourseSectionClass new_temp_class = new CourseSectionClass();
                                        new_temp_class.id = rs.getInt(10);
                                        Instructor ins_temp = new Instructor();
                                        ins_temp.id = rs.getInt(11);
                                        ins_temp.fullName = MyUserService.getFullName(rs.getString(12), rs.getString(13));
                                        new_temp_class.instructor = ins_temp;
                                        new_temp_class.dayOfWeek = DayOfWeek.of(rs.getInt(14));

                                        new_temp_class.classBegin = rs.getShort(16);
                                        new_temp_class.classEnd = rs.getShort(17);
                                        new_temp_class.location = rs.getString(18);

                                        //time conflict??????????????????????????????
                                        if (!timeConflict){
                                            Schedule temp_time = new Schedule(
                                                    rs.getInt(15), new_temp_class.dayOfWeek, new_temp_class.classBegin, new_temp_class.classEnd);
                                            if (schedule_enrolled.contains(temp_time)) {
                                                PreparedStatement findConfTime = connection.prepareStatement("" +
                                                        "select course_name, section_name from course_section\n" +
                                                        "    join student_selections ss on course_section.section_id = ss.section_id\n" +
                                                        "    join classes cl on course_section.section_id = cl.section_id\n" +
                                                        "    join course co on course_section.course_id = co.course_id\n" +
                                                        "where student_id = ? and cl.week_num = ? and cl.day_of_week = ?\n" +
                                                        "    and ((cl.class_begin <= ? and cl.class_end >= ?) " +
                                                        "       or (cl.class_begin >= ? and cl.class_begin <= ?)\n" +
                                                        "       or(cl.class_end >=? and cl.class_end <= ?))");
                                                findConfTime.setInt(1, studentId);
                                                findConfTime.setInt(2, rs.getInt(15));
                                                findConfTime.setInt(3, rs.getInt(14));
                                                findConfTime.setInt(4, lesson.classBegin);
                                                findConfTime.setInt(5, lesson.classEnd);
                                                findConfTime.setInt(6, lesson.classBegin);
                                                findConfTime.setInt(7, lesson.classEnd);
                                                findConfTime.setInt(8, lesson.classBegin);
                                                findConfTime.setInt(9, lesson.classEnd);
                                                ResultSet confTime = findConfTime.executeQuery();
                                                if (confTime.next())
                                                    conflict.add(String.format("%s[%s]", confTime.getString(1), confTime.getString(2)));

                                                timeConflict = true;
                                                if (ignoreConflict)  continue;

                                            }
                                        }

                                        PreparedStatement temp_st = connection.prepareStatement("" +
                                                "select week_num from classes where class_id = ?");
                                        temp_st.setInt(1, rs.getInt(10));
                                        ResultSet temp_rs = temp_st.executeQuery();
                                        new_temp_class.weekList = new HashSet<>();
                                        while (temp_rs.next())
                                            new_temp_class.weekList.add(temp_rs.getShort(1));

                                        classes.add(new_temp_class);
                                    }
                                }
                                //??????class???
                                else {
                                    rs.previous();
                                    break;
                                }
                            } else break;
                        }

                        if (ignoreConflict && timeConflict) continue;
                        cse.course = course;
                        cse.section = section;
                        cse.sectionClasses = classes;
                        cse.conflictCourseNames = conflict;

                        result.add(cse);
                    }
                    break;

                case MAJOR_COMPULSORY:
                case MAJOR_ELECTIVE:

                    break;

                case CROSS_MAJOR:
                    throw new UnsupportedOperationException();
                    //break;

                case PUBLIC:
                    throw new UnsupportedOperationException();
                    //break;
            }
            connection.close();
            return result;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
        //return List.of();
    }

    private boolean isEnrolledSection(int studentId, int sectionId, Connection connection) {
        try (//Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select student_id from student_selections\n" +
                             "where student_id = ? and section_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);

            ResultSet rsst = stmt.executeQuery();
            if (rsst.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    private boolean havePassedCourse(int studentId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select grade from student_selections\n" +
                             "join course_section cs on cs.section_id = student_selections.section_id\n" +
                             "where student_id = ? and cs.course_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.setString(2, courseId);

            ResultSet rsst = stmt.executeQuery();

            while(rsst.next()){
                if(rsst.getInt(1) >= 60 ) {
                    connection.close();
                    return true;
                }
            }
            connection.close();
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }


    private boolean courseConflictFound(int studentId, int sectionId, Connection connection) {

        try (//Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select conf.course_id, conf.section_id from\n" +
                             "\n" +
                             "(select * from course_section\n" +
                             "join classes c on course_section.section_id = c.section_id\n" +
                             "where c.section_id = ?) as now\n" +
                             "\n" +
                             "\n" +
                             "join\n" +
                             "        (select cls.*, cs.semester_id, cs.course_id from classes cls\n" +
                             "        join course_section cs on cls.section_id = cs.section_id) as conf\n" +
                             "\n" +
                             "on ((conf.class_begin<=now.class_begin and conf.class_end>=now.class_end)\n" +
                             "            or(conf.class_begin>=now.class_begin and conf.class_end<=now.class_end)\n" +
                             "            or(conf.class_begin>=now.class_begin and conf.class_begin<=now.class_end)\n" +
                             "            or(conf.class_end>=now.class_begin and conf.class_end<=now.class_end))\n" +
                             "        and conf.day_of_week = now.day_of_week\n" +
                             "        and conf.week_num = now.week_num\n" +
                             "        and conf.semester_id = now.semester_id\n" +
                             "\n" +
                             "join\n" +
                             "\n" +
                             "(select cs.section_id, semester_id from student_selections ss\n" +
                             "join course_section cs on ss.section_id = cs.section_id\n" +
                             "where ss.student_id = ? ) as stu\n" +
                             "on stu.section_id = conf.section_id and now.semester_id = conf.semester_id\n" +
                             "\n" +
                             "union\n" +
                             "\n" +
                             "select cs.course_id, section_id from (\n" +
                             "                  select semester_id, c.course_id from student_selections ss\n" +
                             "                           join course_section cs on ss.section_id = cs.section_id\n" +
                             "                           join course c on c.course_id = cs.course_id\n" +
                             "                  where student_id = ?\n" +
                             "              ) as now\n" +
                             "join course cou on cou.course_id = now.course_id\n" +
                             "join course_section cs on cou.course_id = cs.course_id\n" +
                             "where cs.semester_id = now.semester_id and cs.section_id = ?")) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, studentId);
            stmt.setInt(3, studentId);
            stmt.setInt(4, sectionId);

            ResultSet rsst = stmt.executeQuery();

            if (rsst.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }


    private boolean checkHaveLeftCapacity(int sectionId, Connection connection) {
        try (//Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select left_capacity from course_section\n" +
                             "where section_id = ?")) {
            stmt.setInt(1, sectionId);

            ResultSet rsst = stmt.executeQuery();
            if (rsst.next()) {
                if (rsst.getInt(1) >= 1)
                    return true;
                else return false;
            } else
                throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }


    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {

            MyCourseService mcs = new MyCourseService();
            Course course;
            try {
                course = mcs.getCourseBySection(sectionId);
            } catch (EntityNotFoundException e) {
                //System.out.println("COURSE_NOT_FOUND");
                connection.close();
                return EnrollResult.COURSE_NOT_FOUND;
            }
            if (isEnrolledSection(studentId, sectionId, connection)) {
                //System.out.println("ALREADY_ENROLLED");
                connection.close();
                return EnrollResult.ALREADY_ENROLLED;
            }
            if (havePassedCourse(studentId, course.id)) {
                //System.out.println("ALREADY_PASSED");
                connection.close();
                return EnrollResult.ALREADY_PASSED;
            }
            if (!passedPrerequisitesForCourse(studentId, course.id)) {
                //System.out.println("PREREQUISITES_NOT_FULFILLED");
                connection.close();
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            if (courseConflictFound(studentId, sectionId, connection)) {
                //System.out.println("COURSE_CONFLICT_FOUND");
                connection.close();
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }
            if (!checkHaveLeftCapacity(sectionId, connection)) {
                //System.out.println("COURSE_IS_FULL");
                connection.close();
                return EnrollResult.COURSE_IS_FULL;
            }

            PreparedStatement stmt = connection.prepareStatement(
                    "insert into student_selections(student_id, section_id) values(?,?);\n"); //TODO??? ??????????????????????????????????????????????????????leftcapacity??????
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);

            stmt.executeUpdate();

            stmt = connection.prepareStatement("update course_section set left_capacity = left_capacity - 1\n " +
                    "where section_id = ?;");

            stmt.setInt(1, sectionId);
            stmt.executeUpdate();
            //System.out.println("SUCCESS");

            connection.close();
            return EnrollResult.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }

    }

    @Override
    public void dropCourse(int studentId, int sectionId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {

            PreparedStatement stmt = connection.prepareStatement(
                    "select grade from student_selections\n" +
                            "where student_id = ? and section_id = ?");

            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);

            ResultSet rsst = stmt.executeQuery();

            if (rsst.next()) {
                if (rsst.getString(1) != null) {
                    connection.close();
                    throw new IllegalStateException();
                }
            } else {
                connection.close();
                throw new IllegalStateException();
            }

            stmt = connection.prepareStatement(
                    "delete from student_selections where student_id = ? and section_id = ?");

            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);

            if (stmt.executeUpdate() == 0) {
                connection.close();
                throw new IllegalStateException();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    private int getSectionGradeType(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select grading from course " +
                             "join course_section cs on course.course_id = cs.course_id " +
                             "where cs.section_id = ?")) {//
            stmt.setInt(1, sectionId);

            ResultSet rsst = stmt.executeQuery();
            //connection.close();
            if (rsst.next()) {
                String ret = rsst.getString(1);
                connection.close();
                return (ret.equals("PASS_OR_FAIL")) ? 0 : 1;
            } else {
                connection.close();
                throw new IntegrityViolationException();
            }
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

        int sectionGradeType;
        sectionGradeType = getSectionGradeType(sectionId);
        if (grade != null) {
            if (grade instanceof PassOrFailGrade)
                if (sectionGradeType != 0) throw new IntegrityViolationException();
            if (grade instanceof HundredMarkGrade)
                if (sectionGradeType != 1) throw new IntegrityViolationException();
        }
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();


             PreparedStatement stmt = connection.prepareStatement(
                     "insert into student_selections (grade, student_id, section_id) values (?,?,?)")) {

            if (grade == null) {
                stmt.setObject(1, null);
            } else if (sectionGradeType == 1)
                stmt.setInt(1, ((HundredMarkGrade) grade).mark);
            else stmt.setInt(1, (((PassOrFailGrade) grade) == PassOrFailGrade.PASS) ? 60 : 0);
            stmt.setInt(2, studentId);
            stmt.setInt(3, sectionId);

            int ret = stmt.executeUpdate();
            if (ret <= 0){
                connection.close();
                throw new IntegrityViolationException();
            }
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

        int sectionGradeType;
        sectionGradeType = getSectionGradeType(sectionId);
        if (grade instanceof PassOrFailGrade)
            if (sectionGradeType != 0) throw new IntegrityViolationException();
        if (grade instanceof HundredMarkGrade)
            if (sectionGradeType != 1) throw new IntegrityViolationException();

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "update student_selections set grade = ? where student_id = ? and section_id = ?")) {

            if (sectionGradeType == 1)
                stmt.setInt(1, ((HundredMarkGrade) grade).mark);
            else stmt.setInt(1, (((PassOrFailGrade) grade) == PassOrFailGrade.PASS) ? 60 : 0);
            stmt.setInt(2, studentId);
            stmt.setInt(3, sectionId);

            int ret = stmt.executeUpdate();
            connection.close();
            if (ret <= 0) throw new IntegrityViolationException();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            Map<Course, Grade> ret = new HashMap<>();

            String SQLStatement = "select c.course_id, c.course_name, c.class_hour, c.credit, c.grading, ss.grade\n" +
                    "from student_selections ss\n" +
                    "join course_section cs on cs.section_id = ss.section_id\n" +
                    "join course c on cs.course_id = c.course_id\n" +
                    "where ss.student_id = ?";
            if (semesterId != null) SQLStatement += " and cs.semester_id = ?";

            PreparedStatement stmt = connection.prepareStatement(SQLStatement);

            stmt.setInt(1, studentId);
            if (semesterId != null) stmt.setInt(2, semesterId);

            ResultSet rsst = stmt.executeQuery();

            while (rsst.next()) {
                Course course = new Course();
                course.id = rsst.getString(1);
                course.name = rsst.getString(2);
                course.classHour = rsst.getInt(3);
                course.credit = rsst.getInt(4);
                String grading = rsst.getString(5);
                course.grading = (grading.equals("PASS_OR_FAIL")) ? Course.CourseGrading.PASS_OR_FAIL
                        : Course.CourseGrading.HUNDRED_MARK_SCORE;

                Grade grade;
                if (rsst.getString(6) == null) grade = null;
                else if (grading.equals("PASS_OR_FAIL"))
                    grade = (rsst.getShort(6) == 60) ? PassOrFailGrade.PASS
                            : PassOrFailGrade.FAIL;
                else grade = new HundredMarkGrade(rsst.getShort(6));
                ret.put(course, grade);
            }

            if (ret.isEmpty()) return Map.of(); //????????????
            connection.close();
            return ret;

        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }

    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select cs.section_name, c.course_name,\n" +
                             "        i.instructor_id, i.first_name, i.last_name,\n" +
                             "        cls.class_begin, cls.class_end, cls.location, cls.day_of_week\n" +
                             "from semester sem\n" +
                             "join course_section cs\n" +
                             "    on cs.semester_id=sem.semester_id\n" +
                             "join classes cls\n" +
                             "    on cs.section_id = cls.section_id\n" +
                             "        and cls.week_num = (floor((? - sem.sem_begin) / 7.0)::integer + 1)\n" +
                             "join course c\n" +
                             "    on c.course_id = cs.course_id\n" +
                             "join instructor i\n" +
                             "    on i.instructor_id = cls.instructor_id\n" +
                             "join\n" +
                             "(select section_id from student_selections where student_id = ?) as sec\n" +
                             "    on sec.section_id = cs.section_id\n" +
                             "where ? between sem.sem_begin and sem.sem_end;")) {

            stmt.setDate(1, date);
            stmt.setInt(2, studentId);
            stmt.setDate(3, date);

            ResultSet rsst = stmt.executeQuery();

            CourseTable ret = new CourseTable();
            ret.table = new HashMap<>();
            for (DayOfWeek day : DayOfWeek.values())
                ret.table.put(day, new HashSet<>());

            while (rsst.next()) {

                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();

                entry.courseFullName =
                        String.format("%s[%s]", rsst.getString(2), rsst.getString(1));

                Instructor ins = new Instructor();
                ins.id = rsst.getInt(3);
                ins.fullName = MyUserService.getFullName(rsst.getString(4), rsst.getString(5));
                entry.instructor = ins;

                entry.classBegin = (short) rsst.getInt(6);
                entry.classEnd = (short) rsst.getInt(7);
                entry.location = rsst.getString(8);

                DayOfWeek day = DayOfWeek.of(rsst.getInt(9));
                ret.table.get(day).add(entry);
            }
            connection.close();
            return ret;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    private String getPrerequisiteStringByCourseId(String courseId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select prerequisite from course\n" +
                             "where course_id = ?")) {

            stmt.setString(1, courseId);
            ResultSet rsst = stmt.executeQuery();
            if (rsst.next()) {
                return rsst.getString(1);
            } else {
                connection.close();
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        String prereStr = getPrerequisiteStringByCourseId(courseId);
        if (prereStr.equals(""))
            return true;
        prereStr = prereStr.substring(1, prereStr.length() - 1);
        if (checkSatisfiedCondition(studentId, prereStr))
            return true;
        else return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select major_id, major_name, dept_id, dept_name\n" +
                             "from major m\n" +
                             "join department d on m.department_id = d.dept_id\n" +
                             "join student s on m.major_id = s.major_id and s.student_id = ?")) {

            stmt.setInt(1, studentId);
            ResultSet rsst = stmt.executeQuery();
            if (rsst.next()) {
                Major ret = new Major();
                ret.id = rsst.getInt(1);
                ret.name = rsst.getString(2);
                ret.department = new Department();
                ret.department.id = rsst.getInt(3);
                ret.department.name = rsst.getString(4);

                connection.close();
                return ret;
            } else {
                connection.close();
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

//
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