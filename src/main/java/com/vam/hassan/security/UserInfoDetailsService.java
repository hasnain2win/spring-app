package com.vam.hassan.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.vam.hassan.entity.UserInfo;
import com.vam.hassan.repo.UserInfoRepository;

@Component
public class UserInfoDetailsService implements UserDetailsService {

	private final UserInfoRepository infoRepository;

	@Autowired
	public UserInfoDetailsService(UserInfoRepository infoRepository) {
		this.infoRepository = infoRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		UserInfo userInfo = infoRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		return new UserInfoUserDetails(userInfo);
	}
}
