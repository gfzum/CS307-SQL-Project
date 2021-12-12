package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class MyDepartmentService implements DepartmentService{
    @Override
    public int addDepartment(String name) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "insert into department(dept_name) values (?)")) {
//           todo
//            stmt.setInt(23, 1);
//            stmt.setString(, name);
//            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    @Override
    public void removeDepartment(int departmentId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "delete from department where dept_id = ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1,departmentId);
            stm.executeUpdate();

        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public List<Department> getAllDepartments() {
        return null;
    }

    @Override
    public Department getDepartment(int departmentId) {
        return null;
    }
}
