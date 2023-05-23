package com.vam.hassan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vam.hassan.entity.UserInfo;
import com.vam.hassan.repo.UserInfoRepository;

@Service
public class UserInfoService {

	@Autowired
	UserInfoRepository userInfoRepository;

	@Autowired
	PasswordEncoder encoder;

	public String addUser(UserInfo userInfo) {

		userInfo.setPassword(encoder.encode(userInfo.getPassword()));
		userInfo = userInfoRepository.save(userInfo);
		return "User added to DB";
	}

}
