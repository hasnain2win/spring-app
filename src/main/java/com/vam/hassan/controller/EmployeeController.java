package com.vam.hassan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.vam.hassan.exception.ProductNotfoundException;
import com.vam.hassan.model.Employee;
import com.vam.hassan.repo.EmployeeRepository;
import com.vam.hassan.service.EmployeeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class EmployeeController {

	@Autowired
	EmployeeService employeeService;

	@Autowired
	EmployeeRepository employeeRepository;

	@GetMapping("/employee-details")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public String getEmployeeDetails(Model model) {
		List<String> columnNames = Arrays.asList("ID #", "Name", "Designation#");
		List<Employee> employees = new ArrayList<>();

		employees = employeeService.getAllEmploye();

		model.addAttribute("columnNames", columnNames);
		model.addAttribute("employees", employees);

		return "employees";
	}

	@GetMapping("/employee")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<Object> getAllEmployee() {
		return new ResponseEntity<Object>(employeeService.getAllEmploye(), HttpStatus.OK);
	}

	@PostMapping("/save")
	public ResponseEntity<Employee> saveEmpDetails(@RequestBody Employee employee) {

		Employee result = employeeService.saveDetails(employee);
		return new ResponseEntity<Employee>(result, HttpStatus.OK);
	}

	@GetMapping("/findBy/{id}")
	@PreAuthorize("hasAuthority('ROLE_USER')")
	public ResponseEntity<Object> findById(@PathVariable Long id) throws ProductNotfoundException {

		Optional<Employee> emp = employeeRepository.findById(id);
		if (!emp.isPresent()) {
			throw new ProductNotfoundException();
		} else {
			return new ResponseEntity<Object>(emp, HttpStatus.OK);
		}

	}
}
