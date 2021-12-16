package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyDepartmentService implements DepartmentService{
    @Override
    public int addDepartment(String name) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement st = connection.prepareStatement(
                     "insert into department(dept_name) values (?)")) {
            st.setString(1, name);
            st.executeUpdate();

            ResultSet rs = st.getGeneratedKeys();
            return rs.getInt(1);
            //EntityNotFound?

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeDepartment(int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "delete from department where dept_id = ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1,departmentId);
            stm.executeUpdate();
            stm.close();

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from department";
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();

            List<Department> list = new ArrayList<>();
            if (rs.next()){
                Department temp = new Department();
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

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
    public Department getDepartment(int departmentId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from department where dept_id = ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1,departmentId);
            ResultSet rs = stm.executeQuery();

            Department temp = new Department();
            if (rs.next()){
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);
            }
            stm.close();
            rs.close();
            return temp;

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
