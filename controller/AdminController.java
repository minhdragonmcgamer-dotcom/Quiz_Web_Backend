package com.example.quiz.controller;

import com.example.quiz.entity.*;
import com.example.quiz.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;


//sua import cau hoi tu excel, them nhung cau hoi chua trung
    //paste student vao o add student trong class
    //tao ma sinh vien 


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private ResultRepository resultRepo;

    @Autowired
    private ClassroomRepository classRepo;


    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && user.getRole().equals("ROLE_ADMIN");
    }

    @GetMapping("")
    public String adminHome(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        model.addAttribute("quizCount", quizRepo.count());
        model.addAttribute("userCount", userRepo.count());
        model.addAttribute("resultCount", resultRepo.count());
        return "admin/index";
    }


    @GetMapping("/users")
    public String listUsers(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        model.addAttribute("users", userRepo.findAll());
        return "admin/admin_users";
    }

    @Transactional
    @GetMapping("/user/delete/{id}")
    public String deleteUser(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes redirectAttribute) {

        if (!isAdmin(session)) return "redirect:/login";

        User admin = (User) session.getAttribute("user");

        if (admin.getId().equals(id)) {
            redirectAttribute.addFlashAttribute("error", "Bạn không thể xóa chính mình!");
            return "redirect:/admin/users";
        }

        User user = userRepo.findById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";

        List<Classroom> classes = classRepo.findAll();

        for (Classroom c : classes) {
            c.getStudents().removeIf(s -> s.getId().equals(id));
        }
        classRepo.saveAll(classes);

        for (Classroom c : classes) {
            if (c.getTeacher() != null && c.getTeacher().getId().equals(id)) {
                c.setTeacher(null);
            }
        }
        classRepo.saveAll(classes);

        resultRepo.deleteByUser_UserId(id);

        List<Quiz> quizzes = quizRepo.findByUser_UserId(id);

        for (Quiz q : quizzes) {
            for (Classroom c : classes) {
                c.getAssignments().removeIf(a -> a.getQuiz() != null && a.getQuiz().getQuizId().equals(q.getQuizId()));
            }
            resultRepo.deleteByQuiz_QuizId(q.getQuizId());
        }

        quizRepo.deleteAll(quizzes);

        userRepo.delete(user);

        return "redirect:/admin/users";
    }


    @GetMapping("/user/lock/{id}")
    public String lockUser(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) return "redirect:/login";

        User user = userRepo.findById(id).orElse(null);

        User admin = (User) session.getAttribute("user");

        if (user.getId().equals(admin.getId())) {
           redirectAttributes.addFlashAttribute("error", "Bạn không thể khóa chính mình!");
           return "redirect:/admin/users";
        }

        if (user != null) {
            user.setLocked();
            userRepo.save(user);
        }

        return "redirect:/admin/users";
    }


    @GetMapping("/user/unlock/{id}")
    public String unlockUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        User user = userRepo.findById(id).orElse(null);
        if (user != null) {
            user.setUnlocked();
            userRepo.save(user);
        }

        return "redirect:/admin/users";
    }


    @GetMapping("/user/role")
    public String changeRole(@RequestParam Long id,
                            @RequestParam String role,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) return "redirect:/login";

        User user = userRepo.findById(id).orElse(null);

        User admin = (User) session.getAttribute("user");
        if (user.getId().equals(admin.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể thay đổi vai trò của chính mình!");
            return "redirect:/admin/users";
        }
        
        if (user != null) {
            user.setRole(role);
            userRepo.save(user);
        }

        return "redirect:/admin/users";
    }

    
    
    @GetMapping("/quizzes")
    public String listQuiz(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        model.addAttribute("quizzes", quizRepo.findAll());
        return "admin/admin_quizzes";
    }

    @GetMapping("/quiz/{id}")
    public String viewQuiz(@PathVariable Long id,
                        HttpSession session,
                        Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null) {
            return "redirect:/admin/quizzes";
        }

        model.addAttribute("quiz", quiz);

        return "admin/quiz_view";
    }


   @Transactional
    @GetMapping("/quiz/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        Quiz quiz = quizRepo.findById(id).orElse(null);
        if (quiz == null) return "redirect:/admin/quizzes";

        List<Classroom> classes = classRepo.findAll();
        for (Classroom c : classes) {
            c.getAssignments().removeIf(a -> a.getQuiz() != null && a.getQuiz().getQuizId().equals(id));
        }
        classRepo.saveAll(classes);


        resultRepo.deleteByQuiz_QuizId(id);

        quizRepo.delete(quiz);

        return "redirect:/admin/quizzes";
    }


    @GetMapping("/results")
    public String results(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        model.addAttribute("results", resultRepo.findAll());
        return "admin/admin_results";
    }
}