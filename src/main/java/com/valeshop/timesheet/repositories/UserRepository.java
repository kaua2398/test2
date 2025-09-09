package com.valeshop.timesheet.repositories;

import com.valeshop.timesheet.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
