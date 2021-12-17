package my.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Semester;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.SemesterService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySemesterService implements SemesterService {
    @Override
    public int addSemester(String name, Date begin, Date end) {
        //id需要自增
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection();
             PreparedStatement st = connection.prepareStatement(
                     "insert into semester (sem_name, sem_begin, sem_end) values (?,?,?);", PreparedStatement.RETURN_GENERATED_KEYS)) {
            st.setString(1, name);
            st.setDate(2, begin);
            st.setDate(3, begin);
            st.executeUpdate();

            ResultSet rs = st.getGeneratedKeys();

            if(rs.next())
                return rs.getInt(1);
            else throw new EntityNotFoundException();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new IntegrityViolationException();
        }
    }

    @Override
    public void removeSemester(int semesterId) {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "delete from semester where semester_id = ?";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1,semesterId);
            stm.executeUpdate();
            stm.close();

        } catch (SQLException e){
            e.printStackTrace();
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Semester> getAllSemesters() {
        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from semester;";
            PreparedStatement stm = connection.prepareStatement(sql);
            ResultSet rs = stm.executeQuery();

            List<Semester> list = new ArrayList<>();
            while (rs.next()){
                Semester temp = new Semester();
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);
                temp.begin = rs.getDate(3);
                temp.end = rs.getDate(4);

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
    public Semester getSemester(int semesterId) {

        try (Connection connection = SQLDataSource.getInstance().getSQLConnection()){
            String sql = "select * from semester where semester_id = ?;";
            PreparedStatement stm = connection.prepareStatement(sql);
            stm.setInt(1, semesterId);
            ResultSet rs = stm.executeQuery();

            Semester temp = new Semester();
            if (rs.next()){
                temp.id = rs.getInt(1);
                temp.name = rs.getString(2);
                temp.begin = rs.getDate(3);
                temp.end = rs.getDate(4);
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
