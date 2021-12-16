package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyMajorService implements MajorService{
    @Override
    public int addMajor(String name, int departmentId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement st = connection.prepareStatement(
                     "insert into major(major_name, department_id) values (?,?);")) {
            st.setString(1, name);
            st.setInt(2, departmentId);
            st.executeUpdate();
            st.close();

            ResultSet rs = st.getGeneratedKeys();
            return rs.getInt(1);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeMajor(int majorId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "delete from major where major_id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1,majorId);
            st.executeUpdate();
            st.close();

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from major;";
            PreparedStatement st = connection.prepareStatement(sql);
            ResultSet rs = st.executeQuery();

            List<Major> list = new ArrayList<>();
            if (rs.next()){
                Major temp = new Major();
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

                MyDepartmentService dp = new MyDepartmentService();
                temp.department = dp.getDepartment(rs.getInt(3));

                list.add(temp);
            }
            st.close();
            rs.close();
            return list;

        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Major getMajor(int majorId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from major where major_id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, majorId);
            ResultSet rs = st.executeQuery();

            Major temp = new Major();
            if (rs.next()){
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

                MyDepartmentService dp = new MyDepartmentService();
                temp.department = dp.getDepartment(rs.getInt(3));
            }
            st.close();
            rs.close();
            return temp;

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
