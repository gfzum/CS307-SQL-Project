package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyUserService implements UserService{
    @Override
    public void removeUser(int userId) {
        try (Connection con = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement ps1 = con.prepareStatement("delete from student where student_id = ?");
            ps1.setInt(1, userId);
            ps1.executeUpdate();

            PreparedStatement ps2 = con.prepareStatement("delete from instructor where instructor_id = ?");
            ps2.setInt(1, userId);
            ps2.executeUpdate();

            ps1.close();
            ps2.close();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public List<User> getAllUsers() {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            PreparedStatement st1 = connection.prepareStatement(
                    "select student_id, first_name, last_name from student");
            ResultSet rs1 = st1.executeQuery();

            PreparedStatement st2 = connection.prepareStatement(
                    "select instructor_id, first_name, last_name from instructor");
            ResultSet rs2 = st2.executeQuery();

            List<User> list = new ArrayList<>();
            while (rs1.next()){
                User temp = new Student();
                temp.id = rs1.getInt(1);
                temp.fullName = getFullName(rs1.getString(2),rs1.getString(3));
                list.add(temp);
            }
            st1.close();
            rs1.close();

            while (rs2.next()){
                User temp = new Instructor();
                temp.id = rs2.getInt(1);
                temp.fullName = getFullName(rs2.getString(2),rs2.getString(3));
                list.add(temp);
            }
            st2.close();
            rs2.close();

            return list;

        } catch (SQLException e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public User getUser(int userId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select student_id, first_name, last_name from student where student_id = ?";
            PreparedStatement st1 = connection.prepareStatement(sql);
            st1.setInt(1, userId);
            ResultSet rs1 = st1.executeQuery();

            String sql2 = "select instructor_id, first_name, last_name from instructor where instructor_id = ?";
            PreparedStatement st2 = connection.prepareStatement(sql2);
            st2.setInt(1, userId);
            ResultSet rs2 = st2.executeQuery();

            User stu = new Student();
            User inst = new Instructor();
            if (rs1.next()){
                stu.id = rs1.getInt(1);
                stu.fullName = getFullName(rs1.getString(2), rs1.getString(3));
                st1.close();
                rs1.close();
                
                return stu;
            }
            else{
                inst.id = rs2.getInt(1);
                inst.fullName = getFullName(rs2.getString(2), rs2.getString(3));
                st2.close();
                rs2.close();

                return inst;
            }

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    //构建fullName方法，public static 共全局调用
    public static String getFullName(String firstName, String lastName){
        if (firstName.matches("[a-zA-Z ]+") && lastName.matches("[a-zA-Z ]+")){
            //System.out.println(firstName + " " + lastName);
            return firstName + " " + lastName;
        }
        return firstName + lastName;
    }
}
