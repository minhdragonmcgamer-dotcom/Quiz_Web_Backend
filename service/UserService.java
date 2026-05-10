/* package com.example.quiz.service;

import com.example.quiz.entity.User;
import com.example.quiz.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    // đăng ký
    public String register(User user) {
        if (repo.findByUsername(user.getUsername()) != null) {
            return "Username đã tồn tại!";
        }

        repo.save(user);
        return "success";
    }

    // đăng nhập
    public User login(String username, String password) {
        User user = repo.findByUsername(username);

        if (user == null) return null;

        if (!user.getPassword().equals(password)) return null;

        return user;
    }
}
*/