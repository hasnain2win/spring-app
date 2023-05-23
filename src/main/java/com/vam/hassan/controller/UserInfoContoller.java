package com.vam.hassan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vam.hassan.entity.UserInfo;
import com.vam.hassan.service.UserInfoService;

@RestController
@RequestMapping("/user/")
public class UserInfoContoller {

	@Autowired
	UserInfoService userInfoService;

	@PostMapping("add")
	public ResponseEntity<Object> addUserDetails(@RequestBody UserInfo userInfo) {

		return new ResponseEntity<Object>(userInfoService.addUser(userInfo), HttpStatus.OK);

	}

}
