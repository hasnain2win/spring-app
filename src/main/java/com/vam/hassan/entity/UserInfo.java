package com.vam.hassan.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserInfo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String username;
	private String password;
	private String email;
	private String roles;
	// Add additional fields as per your requirements

	public UserInfo() {
	}

	public UserInfo(String username, String password, String email, String roles) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.roles = roles;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	
	
	public String getRoles() {
		return roles;
	}

	
	public void setRoles(String roles) {
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "UserInfo [id=" + id + ", username=" + username + ", password=" + password + ", email=" + email
				+ ", roles=" + roles + "]";
	}
	
	
}
