package com.example.banking.service;

import com.example.banking.entity.*;
import com.example.banking.repository.UserRepository;
import com.example.banking.repository.RefreshTokenRepository;
import com.example.banking.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
 @Test void registrationHashesPasswordAndReturnsCustomerToken(){
  UserRepository users=mock(UserRepository.class);RefreshTokenRepository refresh=mock(RefreshTokenRepository.class);BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);JwtService jwt=new JwtService("test-secret-must-have-at-least-thirty-two-bytes",900);
  when(users.existsByEmail("person@example.com")).thenReturn(false);when(users.save(any())).thenAnswer(i->i.getArgument(0));
  var result=new AuthService(users,refresh,encoder,jwt,3600).register(new com.example.banking.dto.ApiDtos.RegisterRequest("person@example.com","long-password","Test Person"));
  assertNotNull(result.accessToken());assertEquals("Bearer",result.tokenType());assertEquals(Role.CUSTOMER,result.user().roles().iterator().next());
  var captor=org.mockito.ArgumentCaptor.forClass(AppUser.class);verify(users).save(captor.capture());assertNotEquals("long-password",captor.getValue().getPasswordHash());assertTrue(encoder.matches("long-password",captor.getValue().getPasswordHash()));
 }
 @Test void loginRejectsWrongPassword(){UserRepository users=mock(UserRepository.class);var encoder=new BCryptPasswordEncoder(4);var u=new AppUser("person@example.com",encoder.encode("correct-password"),"Person",Role.CUSTOMER);when(users.findByEmail("person@example.com")).thenReturn(Optional.of(u));var svc=new AuthService(users,mock(RefreshTokenRepository.class),encoder,new JwtService("test-secret-must-have-at-least-thirty-two-bytes",900),3600);assertThrows(com.example.banking.exception.ApiException.class,()->svc.login(new com.example.banking.dto.ApiDtos.LoginRequest("person@example.com","wrong-password")));}
}
