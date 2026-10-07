package com.example.prep.user.config;

import com.example.prep.user.entity.Role;
import com.example.prep.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdminSeeder implements ApplicationRunner {

  private final UserService userService;
  private final String email;
  private final String password;

  public AdminSeeder(
      UserService userService,
      @Value("${app.admin.email:}") String email,
      @Value("${app.admin.password:}") String password) {
    this.userService = userService;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (email.isBlank() || password.isBlank() || userService.exists(email)) {
      return;
    }
    userService.create(email, password, Role.ADMIN);
    log.info("Seeded admin account {}", UserService.normalizeEmail(email));
  }
}
