package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import java.sql.*;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyCourseService implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into course (course_id, course_name, credit, class_hour, grading) values (?,?,?,?,?)")) {
            stmt.setString(1, courseId);
            stmt.setString(2, courseName);
            stmt.setInt(3, credit);
            stmt.setInt(4, classHour);
            String Grade = grading.toString();
            stmt.setString(5, Grade);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        Connection connection = null;
        try {
            connection = SQLDataSource.getInstance().getSQLConnection();
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("insert into course_section (course_id, semester_id, section_name, total_capacity) values(?, ?, ?, ?);", PreparedStatement.RETURN_GENERATED_KEYS);
            {
                int back;
                stmt.setString(1, courseId);
                stmt.setInt(2, semesterId);
                stmt.setString(3, sectionName);
                stmt.setInt(4, totalCapacity);
                stmt.executeUpdate();
                ResultSet resultSet = stmt.getGeneratedKeys();
                if (resultSet.next()) {
                    back = resultSet.getInt(3);
                    connection.commit();
                    connection.close();
                    return back;
                } else {
                    connection.commit();
                    connection.close();
                    throw new IntegrityViolationException();
                }
            }
        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement("insert into classes (section_id, instructor_id, day_of_week, week_list, class_begin, class_end, location) values (?,?,?,?,?,?,?)")) {
            stmt.setInt(1, sectionId);
            stmt.setInt(2, instructorId);
            stmt.setInt(3, dayOfWeek.getValue());
            stmt.setShort(5, classStart);
            stmt.setShort(6, classEnd);
            stmt.setString(7, location);
            //weeklist
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void removeCourse(String courseId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course where course_id = ?")) {
            stmt.setString(1, courseId);

            int result = stmt.executeUpdate();
            if (result <= 0) throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from course_section where section_id = ?")) {
            stmt.setInt(1, sectionId);

            int result = stmt.executeUpdate();
            if (result <= 0) throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "delete from classes where class_id = ?")) {
            stmt.setInt(1, classId);

            int result = stmt.executeUpdate();
            if (result <= 0) throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select * from course");
            ResultSet resultSet = prepareStatement.executeQuery();

            while (resultSet.next()) {
                String id = resultSet.getString(1);
                String name = resultSet.getString(2);
                int credit = resultSet.getInt(3);
                int hour = resultSet.getInt(4);
                String Grade = resultSet.getString(5);

                Course course = new Course();
                course.id = id;
                course.name = name;
                course.credit = credit;
                course.classHour = hour;
                course.grading = Grade.equals("PASS_OR_FAIL") ? Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;
                courses.add(course);
            }
            if (courses.isEmpty())
                throw new EntityNotFoundException();
            else
                return courses;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        List<CourseSection> courseSections = new ArrayList<>();
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select y.section_id, y.section_name, y.total_capacity, count(s.student_id) from course_section as y left outer join student_selections as s on y.section_id = s.section_id where course_id=? and semester_id=? group by s.student_id, y.section_id;");
            prepareStatement.setString(1, courseId);
            prepareStatement.setInt(2, semesterId);
            ResultSet resultSet = prepareStatement.executeQuery();

            while (resultSet.next()) {
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                int total = resultSet.getInt(3);
                int left = total - resultSet.getInt(4);

                CourseSection courseSection = new CourseSection();
                courseSection.id = id;
                courseSection.name = name;
                courseSection.totalCapacity = total;
                courseSection.leftCapacity = left;
                courseSections.add(courseSection);
            }
            if (courseSections.isEmpty())
                throw new EntityNotFoundException();
            else
                return courseSections;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select x.course_id, x.course_name, x.credit, x.class_hour, x.grading from course as x inner join course_section c on x.course_id = c.course_id where c.section_id=?;");
            prepareStatement.setInt(1, sectionId);

            ResultSet resultSet = prepareStatement.executeQuery();

            if (resultSet.next()) {
                String id = resultSet.getString(1);
                String name = resultSet.getString(2);
                int credit = resultSet.getInt(3);
                int hour = resultSet.getInt(4);
                String Grade = resultSet.getString(5);

                Course course = new Course();
                course.id = id;
                course.name = name;
                course.credit = credit;
                course.classHour = hour;
                course.grading = Grade.equals("PASS_OR_FAIL") ? Course.CourseGrading.PASS_OR_FAIL : Course.CourseGrading.HUNDRED_MARK_SCORE;
                return course;
            } else
                throw new EntityNotFoundException();
        } catch (SQLException e) {
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        return null;
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try(Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            PreparedStatement prepareStatement = connection.prepareStatement("select y.section_id, c.section_name,c.total_capacity,count(s.student_id) from classes as y inner join course_section c on c.section_id = y.section_id inner join student_selections s on c.section_id = s.section_id where y.classid=? group by y.section_id,c.section_name,c.total_capacity;");
            prepareStatement.setInt(1,classId);
            ResultSet resultSet = prepareStatement.executeQuery();

            if(resultSet.next()){
                int id = resultSet.getInt(1);
                String name = resultSet.getString(2);
                int total = resultSet.getInt(3);
                int left = total - resultSet.getInt(4);

                CourseSection courseSection = new CourseSection();
                courseSection.id = id;
                courseSection.name = name;
                courseSection.totalCapacity = total;
                courseSection.leftCapacity = left;

                return courseSection;
            }else
                throw new EntityNotFoundException();
        }catch (SQLException e){
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        return null;
    }
}
