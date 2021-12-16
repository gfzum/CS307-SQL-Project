package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.HashMap;
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

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        return null;
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

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            Map<Course, Grade> ret = new HashMap<>();

            String SQLStatement = "select c.course_id, c.course_name, c.class_hour, c.credit, c.grading, ss.grade\n" +
                    "from student_selections ss\n" +
                    "join course_section cs on cs.section_id = ss.section_id\n" +
                    "join course c on cs.course_id = c.course_id\n" +
                    "where ss.student_id = ?";
            if(semesterId != null) SQLStatement += " and cs.semester_id = ?";

            PreparedStatement stmt = connection.prepareStatement(SQLStatement);

            stmt.setInt(1,studentId);
            if(semesterId != null) stmt.setInt(2,semesterId);

            ResultSet rsst = stmt.executeQuery();

            while(rsst.next()) {
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

            if(ret.isEmpty()) throw new EntityNotFoundException();

            return ret;

        } catch (SQLException e){
            throw new IntegrityViolationException();
        }

    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        return null;
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
