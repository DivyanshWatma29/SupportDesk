package com.supportdesk.config;

import com.supportdesk.model.Department;
import com.supportdesk.model.User;
import com.supportdesk.model.UserRole;
import com.supportdesk.repository.DepartmentRepository;
import com.supportdesk.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DataInitializer(DepartmentRepository departmentRepository, UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() == 0) {
            Department it = departmentRepository.save(new Department("IT Support", "IT"));
            departmentRepository.save(new Department("Staff Support", "HR"));
            departmentRepository.save(new Department("Fees & Finance", "FIN"));
            departmentRepository.save(new Department("Campus Facilities", "FAC"));
            departmentRepository.save(new Department("ID Cards & Access", "SEC"));

            if (userRepository.count() == 0) {
                userRepository.save(new User("System Admin", "admin@supportdesk.local", UserRole.ADMIN, it));
                userRepository.save(new User("Vikram Malhotra", "agent@supportdesk.local", UserRole.AGENT, it));
            }
        }
    }
}
