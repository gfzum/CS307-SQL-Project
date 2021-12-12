package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public class MyCourseService implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into course values (?,?,?,?,?)")) {
            stmt.setString(1, courseId);
            stmt.setString(2, courseName);
            stmt.setInt(3, credit);
            stmt.setInt(4, classHour);
            //
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        String sql = "insert into coursesection" + "(courseId, semesterId, sectionName, totalCapacity)" + "values(?, ?, ?, ?);";
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(sql,PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, courseId);
            stmt.setInt(2, semesterId);
            stmt.setString(3, sectionName);
            stmt.setInt(4, totalCapacity);
            stmt.executeUpdate();
            ResultSet resultSet = stmt.getGeneratedKeys();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        return 0;
    }

    @Override
    public void removeCourse(String courseId) {

    }

    @Override
    public void removeCourseSection(int sectionId) {

    }

    @Override
    public void removeCourseSectionClass(int classId) {

    }

    @Override
    public List<Course> getAllCourses() {
        return null;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        return null;
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        return null;
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        return null;
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
