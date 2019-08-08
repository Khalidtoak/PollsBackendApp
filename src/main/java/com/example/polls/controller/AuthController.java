package com.example.polls.controller;

import java.net.URI;
import java.util.Collections;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.polls.exception.AppException;
import com.example.polls.models.Role;
import com.example.polls.models.RoleName;
import com.example.polls.models.User;
import com.example.polls.payloads.ApiResponse;
import com.example.polls.payloads.JwtAuthenticationResponse;
import com.example.polls.payloads.LoginRequest;
import com.example.polls.payloads.SignUpRequest;
import com.example.polls.repositories.RoleRepository;
import com.example.polls.repositories.UserRepository;
import com.example.polls.security.JwtTokenProvider;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;
	@Autowired
	UserRepository userRepository;

	 @Autowired
      RoleRepository roleRepository;

	    @Autowired
	    PasswordEncoder passwordEncoder;

	    @Autowired
	    JwtTokenProvider tokenProvider;
	    @PostMapping("/signin")
	    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest){
	    	Authentication authentication = authenticationManager.
	    			authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserNameOrEmail(), 
	    					loginRequest.getPassword()));
	    	SecurityContextHolder.getContext().setAuthentication(authentication);
	    	String jwtToken = tokenProvider.generateToken(authentication);
	    	return ResponseEntity.ok(new JwtAuthenticationResponse(jwtToken));

	    }
	    @SuppressWarnings({ "unchecked", "rawtypes" })
		@PostMapping("/signup")
	    public ResponseEntity<?> RegisterUser(@Valid @RequestBody SignUpRequest signUpRequest){
	    	if(userRepository.existsByUsername(signUpRequest.getUsername())) {
	    		return new ResponseEntity(new ApiResponse(false, "User name already exists"),
	    				HttpStatus.BAD_REQUEST);
	    	}
	    	if(userRepository.existsByEmail(signUpRequest.getEmail())) {
	    		return new ResponseEntity(new ApiResponse(false, 
	    				"This email is registered on this plaform alredy"), HttpStatus.BAD_REQUEST);
	    	}
	    	// Create user
	    	User user = new User(signUpRequest.getName(), signUpRequest.getUsername(), signUpRequest.getEmail(), 
	    			signUpRequest.getPassword());
	    	user.setPassword(passwordEncoder.encode(user.getPassword()));
	    	Role roles = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow(()-> new
	    			AppException("User role not set"));
	    	user.setRoles(Collections.singleton(roles));
	    	User result = userRepository.save(user);
	    	URI location = ServletUriComponentsBuilder
            .fromCurrentContextPath().path("/api/users/{username}")
            .buildAndExpand(result.getUsername()).toUri();
	    	 ResponseEntity.created(location).body(new ApiResponse(true,
	    			"User registered successfully" ));
	    	 Authentication authentication = authenticationManager.
		    			authenticate(new UsernamePasswordAuthenticationToken(signUpRequest.getUsername(), 
		    					signUpRequest.getPassword()));
		    	SecurityContextHolder.getContext().setAuthentication(authentication);
		    	String jwtToken = tokenProvider.generateToken(authentication);
		    	return ResponseEntity.ok(new JwtAuthenticationResponse(jwtToken));
	    }

}
