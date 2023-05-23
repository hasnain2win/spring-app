package com.vam.hassan.repo;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.vam.hassan.model.Employee;

@Repository
public class EmployeeDao {

	private final JdbcTemplate jdbcTemplate;

	public EmployeeDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Employee saveEmp(Employee emp) {

		var sql = "INSERT INTO employee (name, designation) VALUES (?, ?);";
		int result = jdbcTemplate.update(sql, emp.getName(), emp.getDesignation());
		if (result != 0) {
			return emp;
		}
		return new Employee();

	}

	public List<Employee> fetchAllEmp() {
		String sql = "SELECT * FROM employee";
		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			Employee employee = new Employee();
			employee.setId(rs.getLong("id"));
			employee.setName(rs.getString("name"));
			employee.setDesignation(rs.getString("designation"));
			return employee;
		});
	}

}
