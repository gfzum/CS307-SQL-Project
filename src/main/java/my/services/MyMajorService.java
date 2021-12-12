package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
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
        //todo
        return 0;
    }

    @Override
    public void removeMajor(int majorId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "delete from major where major_id = ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1,majorId);
            stm.executeUpdate();
            stm.close();

        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Major> getAllMajors() {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from major;";
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();

            List<Major> list = new ArrayList<>();
            if (rs.next()){
                Major temp = new Major();
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

                MyDepartmentService dp = new MyDepartmentService();
                temp.department = dp.getDepartment(rs.getInt(3));

                list.add(temp);
            }
            stm.close();
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
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();

            Major temp = new Major();
            if (rs.next()){
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

                MyDepartmentService dp = new MyDepartmentService();
                temp.department = dp.getDepartment(rs.getInt(3));
            }
            stm.close();
            rs.close();
            return temp;

        } catch (SQLException e){
            e.printStackTrace();//todo exception
        }
        return null;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {

    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {

    }
}
