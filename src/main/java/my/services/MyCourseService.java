package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.sql.rowset.serial.SerialArray;
import java.sql.*;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;

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

    public String prerequisiteToString(Prerequisite prerequisite) {
        String ans = "";
        if (prerequisite instanceof CoursePrerequisite) {
            ans += prerequisite.toString();
        } else if (prerequisite instanceof AndPrerequisite) {
            int kh = ((AndPrerequisite) prerequisite).terms.size();
            while (kh > 0) {
                ans += "(";
                kh--;
            }
            for (Prerequisite p : ((AndPrerequisite) prerequisite).terms)
                ans = ans + "&" + prerequisiteToString(p) + ")";
        } else if (prerequisite instanceof OrPrerequisite) {
            int kh = ((OrPrerequisite) prerequisite).terms.size();
            while (kh > 0) {
                ans += "(";
                kh--;
            }
            for (Prerequisite p : ((OrPrerequisite) prerequisite).terms)
                ans = ans + "|" + prerequisiteToString(p) + ")";
        }
        return ans;
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        Connection connection = null;
        try {
            connection = SQLDataSource.getInstance().getSQLConnection();
            connection.setAutoCommit(false);
            PreparedStatement stmt = connection.prepareStatement("insert into course_section (course_id, semester_id, section_name, total_capacity, left_capacity) values(?, ?, ?, ?, ?);",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            {
                int back;
                stmt.setString(1, courseId);
                stmt.setInt(2, semesterId);
                stmt.setString(3, sectionName);
                stmt.setInt(4, totalCapacity);
                stmt.setInt(5, totalCapacity);
                stmt.executeUpdate();
                ResultSet resultSet = stmt.getGeneratedKeys();
                if (resultSet.next()) {
                    back = resultSet.getInt(1);
                    connection.commit();
                    connection.close();


                    // TODO ：这里为啥要close connection???? commit 有啥用


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
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {

            PreparedStatement stmt = connection.prepareStatement(
                    "insert into classes (section_id, instructor_id, day_of_week, week_num, class_begin, class_end, location) values (?,?,?,?,?,?,?)",
                    PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, sectionId);
            stmt.setInt(2, instructorId);
            stmt.setInt(3, dayOfWeek.getValue());
            stmt.setShort(5, classStart);
            stmt.setShort(6, classEnd);
            stmt.setString(7, location);

            Iterator it = weekList.iterator();
            stmt.setShort(4, (short) it.next());

            stmt.executeUpdate();
            ResultSet rsst = stmt.getGeneratedKeys();
            int classId;
            if (rsst.next()) {
                classId = rsst.getInt(1);
            } else throw new EntityNotFoundException();

            stmt = connection.prepareStatement(
                    "insert into classes (class_id, section_id, instructor_id, day_of_week, week_num, class_begin, class_end, location) values (?,?,?,?,?,?,?,?)");
            stmt.setInt(1, classId);
            stmt.setInt(2, sectionId);
            stmt.setInt(3, instructorId);
            stmt.setInt(4, dayOfWeek.getValue());
            stmt.setShort(6, classStart);
            stmt.setShort(7, classEnd);
            stmt.setString(8, location);

            for (; it.hasNext(); ) {
                stmt.setInt(5, (short) it.next());
                stmt.executeUpdate();
            }

            return classId;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
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
                return List.of(); //题目要求
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
                return List.of();
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
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            List<CourseSectionClass> result = new ArrayList<>();

            PreparedStatement prepareStatement = connection.prepareStatement("select classid, i.student_id as instructor_id, i.first_name||' '||i.last_name as full_name,day_of_week, class_begin, class_end, location, week_list from classes inner join instructor i on i.userid = classes.instructor_id where section_id=? group by class_id, instructor_id, full_name, i.instructor_id, day_of_week, class_begin, class_end, location");
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

            if (result.isEmpty()) {
                return List.of();
            } else
                return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement prepareStatement = connection.prepareStatement("select y.section_id, c.section_name,c.total_capacity,count(s.student_id) from classes as y inner join course_section c on c.section_id = y.section_id inner join student_selections s on c.section_id = s.section_id where y.classid=? group by y.section_id,c.section_name,c.total_capacity;");
            prepareStatement.setInt(1, classId);
            ResultSet resultSet = prepareStatement.executeQuery();

            if (resultSet.next()) {
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
            } else
                throw new EntityNotFoundException();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        Connection connection = null;
        try {
            connection = SQLDataSource.getInstance().getSQLConnection();
            connection.setAutoCommit(false);

            List<Student> result = new ArrayList<>();
            PreparedStatement prepareStatement = connection.prepareStatement("select s.student_id as id, s.first_name||' '||s.last_name as fullname, s.enrolled_date, m.major_id as major_id, m.name as major_name, d.department_id as department_id, d.name as department_name from course_section inner join student_selections ss on course_section.section_id = ss.section_id inner join student as s on s.student_id = ss.student_id inner join major m on m.major_id = s.major_id inner join department d on d.department_id = m.department_id where course_id=? and semester_id=? ;");

            prepareStatement.setString(1, courseId);
            prepareStatement.setInt(2, semesterId);

            ResultSet resultSet = prepareStatement.executeQuery();
            connection.commit();

            while (resultSet.next()) {
                int sid = resultSet.getInt(1);
                String fullname = resultSet.getString(2);
                Date date = resultSet.getDate(3);

                Student student = new Student();
                student.id = sid;
                student.fullName = fullname;
                student.enrolledDate = date;

                int mid = resultSet.getInt(4);
                String mname = resultSet.getString(5);

                Major major = new Major();
                major.id = mid;
                major.name = mname;

                student.major = major;

                int did = resultSet.getInt(6);
                String dname = resultSet.getString(7);

                Department department = new Department();
                department.id = did;
                department.name = dname;
                major.department = department;

                result.add(student);
            }

            connection.close();

            if (result.isEmpty())
                return List.of();
            else
                return result;
        } catch (SQLException e) {
            try {
                if (connection != null) {
                    connection.rollback();
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new IntegrityViolationException();
        }
    }
}
