package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyDepartmentService implements DepartmentService{
    @Override
    public int addDepartment(String name) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement st = connection.prepareStatement(
                     "insert into department(dept_name) values (?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
            st.setString(1, name);
            st.executeUpdate();

            ResultSet rs = st.getGeneratedKeys();

            if(rs.next()) {
                int ret = rs.getInt(1);
                connection.close();
                return ret;
                //EntityNotFound?
            } else {
                connection.close();
                throw new EntityNotFoundException();
            }

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
            //stm.close();
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Department> getAllDepartments() {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            List<Department> list = new ArrayList<>();

            String sql = "select * from department";
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();

            while (rs.next()){
                Department temp = new Department();
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);

                list.add(temp);
            }
            //stm.close();
            //rs.close();
            connection.close();
            return list;

        } catch (SQLException e){
            e.printStackTrace();
        }

       return List.of();
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
            //stm.close();
            //rs.close();
            connection.close();
            return temp;

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }
}
