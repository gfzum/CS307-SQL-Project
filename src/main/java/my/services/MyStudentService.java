package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MyStudentService implements StudentService {
    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into student(student_id, first_name, last_name, " +
                     "enrolled_date, major_id) values (?,?,?,?,?)")) {
             stmt.setInt(1,userId);
             stmt.setString(2,firstName);
             stmt.setString(3,lastName);
             stmt.setDate(4,enrolledDate);
             stmt.setInt(5,majorId);
             stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        return null;
    }

    private boolean isEnrolledSection (int studentId, int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select student_id from student_selections\n" +
                             "where student_id = ? and section_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.setInt(2,sectionId);

            ResultSet rsst = stmt.executeQuery();
            if(rsst.next()){
                return true;
            }else
                return false;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    private boolean havePassedCourse (int studentId, String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select grade from student_selections\n" +
                             "join course_section cs on cs.section_id = student_selections.section_id\n" +
                             "where student_id = ? and cs.course_id = ?")) {
            stmt.setInt(1, studentId);
            stmt.setString(2, courseId);

            ResultSet rsst = stmt.executeQuery();

            if(rsst.next()){
                if( rsst.getInt(1) >= 60 )
                    return true;
                else return false;
            }else
                return false;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    private boolean courseConflictFound (int studentId, int sectionId){
        return false;
    }

    private boolean checkHaveLeftCapacity (int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select left_capacity from course_section\n" +
                             "where section_id = ?")) {
            stmt.setInt(1, sectionId);

            ResultSet rsst = stmt.executeQuery();

            if(rsst.next()){
                if( rsst.getInt(1) >= 1 )
                    return true;
                else return false;
            }else
                return false;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }


    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){

            MyCourseService mcs = new MyCourseService();
            Course course;
            try {
                course = mcs.getCourseBySection(sectionId);
            } catch (EntityNotFoundException e){
                return EnrollResult.COURSE_NOT_FOUND;
            }
            if(isEnrolledSection( studentId, sectionId)){
                return EnrollResult.ALREADY_ENROLLED;
            }
            if( havePassedCourse( studentId, course.id) ){
                return EnrollResult.ALREADY_PASSED;
            }
            if( !passedPrerequisitesForCourse( studentId, course.id)){
                return EnrollResult.PREREQUISITES_NOT_FULFILLED;
            }
            if( courseConflictFound( studentId, sectionId)){
                return EnrollResult.COURSE_CONFLICT_FOUND;
            }
            if( !checkHaveLeftCapacity( sectionId ) ){
                return EnrollResult.COURSE_IS_FULL;
            }

            PreparedStatement stmt = connection.prepareStatement(
                    "insert into student_selections(student_id, section_id) values(?,?);\n" +
                    "update course_section set left_capacity = left_capacity - 1;");
            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            stmt.executeUpdate();

            return EnrollResult.SUCCESS;

        } catch (SQLException e) {
            e.printStackTrace();
            return EnrollResult.UNKNOWN_ERROR;
        }

    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from student_selections where student_id = ? and section_id = ?")) {
            stmt.setInt(1,studentId);
            stmt.setInt(2,sectionId);

            int ret =stmt.executeUpdate();
            if( ret <= 0 ) throw new IllegalStateException();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getSectionGradeType(int sectionId){
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = connection.prepareStatement(
                    "select grading from course " +
                            "join course_section cs on course.course_id = cs.course_id " +
                            "where cs.section_id = ?")) {
            stmt.setInt(1, sectionId);

            ResultSet rsst = stmt.executeQuery();
            if(rsst.next()){
                String ret = rsst.getString(1);
                return (ret == "PASS_OR_FAIL")?1:0;
            }else
                throw new IntegrityViolationException();

        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {

        int sectionGradeType;
        sectionGradeType = getSectionGradeType(sectionId);
        if(grade != null) {
            if (grade instanceof PassOrFailGrade)
                if (sectionGradeType != 0) throw new IntegrityViolationException();
            if (grade instanceof HundredMarkGrade)
                if (sectionGradeType != 1) throw new IntegrityViolationException();
        }
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into student_selections (grade, student_id, section_id) values (?,?,?)")){

            if(grade == null){
                stmt.setObject(1,null);
            }else if(sectionGradeType == 1)
                stmt.setInt(1,((HundredMarkGrade)grade).mark);
            else stmt.setInt(1,(((PassOrFailGrade)grade) == PassOrFailGrade.PASS)?60:0);
            stmt.setInt(2,studentId);
            stmt.setInt(3,sectionId);

            int ret = stmt.executeUpdate();
            if( ret <= 0 )throw new IntegrityViolationException();

        } catch (SQLException e){
            e.printStackTrace();
        }

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {

        int sectionGradeType;
        sectionGradeType = getSectionGradeType(sectionId);
        if(grade instanceof PassOrFailGrade)
            if(sectionGradeType != 0) throw new IntegrityViolationException();
        if(grade instanceof HundredMarkGrade)
            if(sectionGradeType != 1) throw new IntegrityViolationException();

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "update student_selections set grade = ? where student_id = ? and section_id = ?")){

            if(sectionGradeType == 1)
                stmt.setInt(1,((HundredMarkGrade)grade).mark);
            else stmt.setInt(1,(((PassOrFailGrade)grade) == PassOrFailGrade.PASS)?60:0);
            stmt.setInt(2,studentId);
            stmt.setInt(3,sectionId);

            int ret = stmt.executeUpdate();
            if( ret <= 0 )throw new IntegrityViolationException();

        } catch (SQLException e){
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
                course.grading = (grading == "PASS_OR_FAIL") ? Course.CourseGrading.PASS_OR_FAIL
                        : Course.CourseGrading.HUNDRED_MARK_SCORE;

                Grade grade;
                if (rsst.getString(6) == null) grade = null;
                else if (grading == "PASS_OR_FAIL")
                    grade = (rsst.getShort(6) == 60) ? PassOrFailGrade.PASS
                            : PassOrFailGrade.FAIL;
                else grade = new HundredMarkGrade(rsst.getShort(6));
                ret.put(course, grade);
            }

            if (ret.isEmpty()) throw new EntityNotFoundException();

            return ret;

        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }

    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {

        try (Connection connection=SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt=connection.prepareStatement(
                    "select cs.section_name, c.course_name,\n" +
                            "        i.instructor_id, i.first_name, i.last_name,\n" +
                            "        cls.class_begin, cls.class_end, cls.location, cls.days\n" +
                            "from semester sem\n" +
                            "inner join course_section cs\n" +
                            "    on cs.semester_id=sem.semester_id\n" +
                            "inner join classes cls\n" +
                            "    on cs.section_id = cls.section_id\n" +
                            "        and cls.week_list=(floor((? - sem.sem_begin) / 7.0)::integer + 1)\n" +
                            "inner join course c\n" +
                            "    on c.course_id = cs.course_id\n" +
                            "inner join instructor i\n" +
                            "    on i.instructor_id=cls.instructor_id\n" +
                            "inner join\n" +
                            "(select section_id from student_selections where student_id = ?) as sec\n" +
                            "    on sec.section_id = cs.section_id\n" +
                            "where ? between sem.sem_begin and sem.sem_end")){

            stmt.setDate(1,date);
            stmt.setInt(2,studentId);
            stmt.setDate(3,date);

            ResultSet rsst = stmt.executeQuery();

            CourseTable ret = new CourseTable();
            ret.table = new HashMap<>();
            for (DayOfWeek day: DayOfWeek.values())
                ret.table.put(day, new HashSet<>());

            while (rsst.next()){

                CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();

                entry.courseFullName =
                        String.format("%s[%s]",rsst.getString(2),rsst.getString(1));

                Instructor ins = new Instructor();
                ins.id = rsst.getInt(3);
                ins.fullName = MyUserService.getFullName( rsst.getString(4), rsst.getString(5) );
                entry.instructor = ins;

                entry.classBegin = (short)rsst.getInt(6);
                entry.classEnd = (short)rsst.getInt(7);
                entry.location = rsst.getString(8);

                DayOfWeek day = DayOfWeek.of( rsst.getInt(9) );
                ret.table.get(day).add(entry);
            }

            return ret;

        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }

    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        return false;
    }

    @Override
    public Major getStudentMajor(int studentId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "select major_id, major_name, dept_id, dept_name\n" +
                             "from major m\n" +
                             "join department d on m.department_id = d.dept_id\n" +
                             "join student s on m.major_id = s.major_id and s.student_id = ?")) {
            stmt.setInt(1,studentId);
            ResultSet rsst =stmt.executeQuery();
            if(rsst.next()){
                Major ret = new Major();
                ret.id = rsst.getInt(1);
                ret.name = rsst.getString(2);
                ret.department = new Department();
                ret.department.id = rsst.getInt(3);
                ret.department.name = rsst.getString(4);
                return ret;
            }else{
                throw new EntityNotFoundException();
            }
        } catch (SQLException e) {
             e.printStackTrace();
        }
        return null;
    }
}
