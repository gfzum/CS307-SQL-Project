package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MyInstructorService implements InstructorService{
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into instructor(instructor_id, first_name, last_name) " +
                     "values (?,?,?)")) {
            stmt.setInt(1,userId);
            stmt.setString(2,firstName);
            stmt.setString(3,lastName);
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        return null;
    }
}
