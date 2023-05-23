package com.vam.hassan.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vam.hassan.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

}
