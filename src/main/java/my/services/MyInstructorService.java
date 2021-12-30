package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyInstructorService implements InstructorService{
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement st = connection.prepareStatement(
                     "insert into instructor(instructor_id, first_name, last_name) " +
                     "values (?,?,?)")) {
            st.setInt(1,userId);
            st.setString(2,firstName);
            st.setString(3,lastName);
            st.executeUpdate();

            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            //throw new IntegrityViolationException(); 没有unique约束，不需要
        }
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {

        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            //魔改了一波
            List<CourseSection> list = new ArrayList<>();

            //查找该老师是否有教班
            PreparedStatement ps1 = con.prepareStatement(
                    "select * from classes where instructorId = ?");
            ps1.setInt(1, instructorId);
            ResultSet rs1 = ps1.executeQuery();
            int sec_id = rs1.getInt(8);
            ps1.close();
            rs1.close();

            //查找该班级在该学期是否有对应课程
            PreparedStatement ps2 = con.prepareStatement(
                    "select section_id, section_name, total_capacity, left_capacity " +
                            "from course_section where semester_id = ? and section_id = ?");
            ps2.setInt(1, semesterId);
            ps2.setInt(2, sec_id);
            ResultSet rs2 = ps2.executeQuery();

            CourseSection selected = new CourseSection();

            while (rs2.next()) {
                selected.id = rs2.getInt(1);
                selected.name = rs2.getString(2);
                selected.totalCapacity = rs2.getInt(3);
                selected.leftCapacity = rs2.getInt(4);

                list.add(selected);
            }
            ps2.close();
            rs2.close();

            con.close();
            return list;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }
}
