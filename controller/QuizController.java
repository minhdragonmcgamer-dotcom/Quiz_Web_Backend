package com.example.quiz.controller;

import com.example.quiz.entity.*;
import com.example.quiz.repository.*;

import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import java.time.LocalDateTime;
import java.util.*;

/* 
admin
giao dien
import cau hoi excel
rang buoc
bao cao
*/
@Controller
public class QuizController {

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private OptionAnswerRepository optionRepo;

    @Autowired
    private ResultRepository resultRepo;

    @Autowired
    private QuestionRepository questionRepo;

    @Autowired
    private ClassroomRepository classRepo;

    @Autowired
    private AssignmentRepository assignmentRepo;

    

    //DANH SÁCH QUIZ
    @GetMapping("/quizzes")
    public String listQuiz(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        List<Quiz> quizzes = new ArrayList<>();

        if (user != null && "ROLE_STUDENT".equals(user.getRole())) {

            List<Quiz> publicQuizzes = quizRepo.findByIsPublicTrue();

            List<Classroom> classes = classRepo.findByStudents_Id(user.getId());

            Set<Quiz> classQuizzes = new HashSet<>();
            for (Classroom c : classes) {
                classQuizzes.addAll(c.getAssignments().stream().filter(a -> a.getQuiz() != null).map(Assignment::getQuiz).collect(Collectors.toSet()));
            }

            Set<Quiz> result = new HashSet<>();
            result.addAll(publicQuizzes);
            result.addAll(classQuizzes);

            quizzes = new ArrayList<>(result);
        }

        model.addAttribute("quizzes", quizzes);

        return "student/dashboard";
    }

    //LÀM QUIZ
    @GetMapping("/quiz/{id}")
    public String takeQuiz(@PathVariable Long id,
                        HttpSession session,
                        Model model) {

        User user = (User) session.getAttribute("user");
        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null) {
            return "redirect:/quizzes";
        }        


        boolean isPublic = quiz.getIsPublic();
        boolean inClass = false;

        if (user != null && "ROLE_STUDENT".equals(user.getRole())) {

            List<Classroom> classes = classRepo.findByStudents_Id(user.getId());

            for (Classroom c : classes) {
                if (c.getAssignments().stream().anyMatch(a -> a.getQuiz() != null && a.getQuiz().equals(quiz))) {
                    inClass = true;
                    break;
                }
            }

            if (!isPublic && !inClass) {
                return "student/classes";
            }
        }

        if (quiz.getTimeLimit() != null) {
            String sessionKey = "quiz_start_" + id;
            long currentTime = System.currentTimeMillis();
            
            if (session.getAttribute(sessionKey) == null) {
                session.setAttribute(sessionKey, currentTime);
            }

            long startTime = (long) session.getAttribute(sessionKey);
            long durationInSeconds = quiz.getTimeLimit() * 60;
            long elapsedSeconds = (currentTime - startTime) / 1000;
            long remainingSeconds = durationInSeconds - elapsedSeconds;

            if (remainingSeconds <= 0) {
                model.addAttribute("remainingSeconds", 0);
            } else {
                model.addAttribute("remainingSeconds", remainingSeconds);
            }

        }

    model.addAttribute("quiz", quiz);
    return "quiz_take";
    }

    //LÀM BÀI THEO ASSIGNMENT
    @GetMapping("/assignment/{id}")
    public String takeAssignment(@PathVariable Long id, Model model, HttpSession session) {

        Assignment a = assignmentRepo.findById(id).orElse(null);
        if (a == null || a.getQuiz() == null) return "redirect:/quizzes";

        User user = (User) session.getAttribute("user");
        if (user == null || !"ROLE_STUDENT".equals(user.getRole())) {
            return "redirect:/login";
        }

        Classroom c = a.getClassroom();
        if (c == null || c.getStudents().stream().noneMatch(s -> s.getId().equals(user.getId()))) {
            return "redirect:/quizzes";
        }

        if (a.getStartTime() != null && a.getEndTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(a.getStartTime())) return "redirect:/quizzes?not_started";
            if (now.isAfter(a.getEndTime())) return "redirect:/quizzes?expired";
        }

        model.addAttribute("assignment", a);
        model.addAttribute("quiz", a.getQuiz());

        if (a.getTimeLimit() != null) {
            model.addAttribute("remainingSeconds", a.getTimeLimit() * 60L);
        }

        return "quiz_take";
    }

    

    //NỘP BÀI
    @PostMapping("/submit")
    public String submit(@RequestParam Map<String, String> answers,
                        HttpSession session,
                        Model model) {
        
        String quizIdStr = answers.get("quizId");
        if (quizIdStr == null) return "redirect:/quizzes";
        
        Long quizId = Long.parseLong(quizIdStr);
        Quiz quiz = quizRepo.findById(quizId).orElse(null);
        if (quiz == null) return "redirect:/quizzes";

        int score = 0;
        List<Question> questions = quiz.getQuestions();
        int total = questions.size();

        for (Question q : questions) {
            String answerValue = answers.get("q_" + q.getQuestionId());
            if (answerValue != null) {
                Long optionId = Long.parseLong(answerValue);
                OptionAnswer opt = optionRepo.findById(optionId).orElse(null);
                if (opt != null && opt.isCorrect()) {
                    score++;
                }
            }
        }

        User user = (User) session.getAttribute("user");
        Result result = new Result();
        result.setUser(user);
        result.setScore(score);
        result.setTotalQuestion(total);
        result.setQuiz(quiz);
        result.setAssignment(null); 
        resultRepo.save(result);

        // Xóa session timer sau khi đã nộp bài thành công
        session.removeAttribute("quiz_start_" + quizId);

        model.addAttribute("score", score);
        model.addAttribute("total", total);
        model.addAttribute("quiz", quiz);

        return "student/result"; 
    }

    @PostMapping("/assignment/submit")
    public String submitAssignment(@RequestParam Map<String, String> answers,
                                HttpSession session,
                                Model model) {

        String quizIdStr = answers.get("quizId");
        if (quizIdStr == null) return "redirect:/quizzes";

        Long quizId = Long.parseLong(quizIdStr);

        Quiz quiz = quizRepo.findById(quizId).orElse(null);
        if (quiz == null) return "redirect:/quizzes";

        // Lấy assignmentId từ form
        String assignmentIdStr = answers.get("assignmentId");
        Assignment assignment = null;

        if (assignmentIdStr != null) {
            assignment = assignmentRepo.findById(Long.parseLong(assignmentIdStr)).orElse(null);
        }

        int score = 0;

        for (Question q : quiz.getQuestions()) {
            String answerValue = answers.get("q_" + q.getQuestionId());

            if (answerValue != null) {
                Long optionId = Long.parseLong(answerValue);
                OptionAnswer opt = optionRepo.findById(optionId).orElse(null);

                if (opt != null && opt.isCorrect()) {
                    score++;
                }
            }
        }

        User user = (User) session.getAttribute("user");

        Result result = new Result();
        result.setUser(user);
        result.setScore(score);
        result.setTotalQuestion(quiz.getQuestions().size());
        result.setQuiz(quiz);

        // SAFE
        if (assignment != null) {
            result.setAssignment(assignment);
        }

        resultRepo.save(result);

        model.addAttribute("score", score);
        model.addAttribute("total", quiz.getQuestions().size());
        model.addAttribute("quiz", quiz);

        return "student/result";
    }


    @GetMapping("/classes")
    public String viewClasses(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_STUDENT".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Classroom> classes = classRepo.findByStudents_Id(user.getId());

        model.addAttribute("classes", classes);

        return "student/classes";
    }

    @GetMapping("/results")
    public String viewResults(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_STUDENT".equals(user.getRole())) {
            return "redirect:/login";
        }

        List<Result> results = resultRepo.findByUser_UserId(user.getId());

        model.addAttribute("results", results);

        return "student/results";
    }

}