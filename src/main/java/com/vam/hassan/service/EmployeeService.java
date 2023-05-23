package com.vam.hassan.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.vam.hassan.model.Employee;
import com.vam.hassan.repo.EmployeeDao;

@Service
public class EmployeeService {

	@Autowired
	EmployeeDao employeeDao;

	public Employee saveDetails(Employee emp) {
		return employeeDao.saveEmp(emp);
	}

	public List<Employee> getAllEmploye() {
		return employeeDao.fetchAllEmp();
	}

}
