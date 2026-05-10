package com.example.quiz.controller;

import com.example.quiz.entity.*;
import com.example.quiz.repository.*;
import jakarta.servlet.http.HttpSession;
import java.util.*;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private ClassroomRepository classRepo;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {

        User user = userRepository.findByUsername(username);

        if (user == null || !user.getPassword().equals(password)) {
            model.addAttribute("error", "Sai tài khoản hoặc mật khẩu!");
            return "login";
        }

        if (user.isLocked()) {
            model.addAttribute("error", "Tài khoản đã bị khóa!");
            return "login";
        }

        session.setAttribute("user", user);

        
        if (user.getRole().equals("ROLE_ADMIN")) {
            return "redirect:/admin";
        } else if (user.getRole().equals("ROLE_TEACHER")) {
            return "redirect:/teacher";
        } else {
            return "redirect:/student";
        }
    }



    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }


    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String role,
                           Model model) {

        if (userRepository.findByUsername(username) != null) {
            model.addAttribute("error", "Username đã tồn tại!");
            return "register";
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);

        userRepository.save(user);

        if ("ROLE_STUDENT".equals(role)) {
            user.setStudentCode("HS" + user.getId());
            userRepository.save(user);
        }


        model.addAttribute("success", "Đăng ký thành công!");
        return "redirect:/login";
    }


    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


    /*@GetMapping("/teacher")
    public String teacherPage(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !user.getRole().equals("ROLE_TEACHER")) {
            return "redirect:/login";
        }

        return "teacher";
    }
        */


    @GetMapping("/student")
    public String studentDashboard(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_STUDENT".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Quiz> publicQuizzes = quizRepo.findByIsPublicTrue();

        List<Classroom> classes = classRepo.findByStudents_Id(user.getId());

        Set<Quiz> classQuizzes = new HashSet<>();
        for (Classroom c : classes) {
            classQuizzes.addAll(c.getAssignments().stream().filter(a -> a.getQuiz() != null).map(Assignment::getQuiz).collect(Collectors.toSet()));
        }

        Set<Quiz> all = new HashSet<>();
        all.addAll(publicQuizzes);
        all.addAll(classQuizzes);

        model.addAttribute("quizzes", all);

        return "student/dashboard";
    }


}