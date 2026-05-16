package com.example.quiz.controller;

import com.example.quiz.entity.*;
import com.example.quiz.repository.*;
import jakarta.servlet.http.HttpSession;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.CellType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private AssignmentRepository assignmentRepo;

    @Autowired
    private QuizRepository quizRepo;

    @Autowired
    private ResultRepository resultRepo;

    @Autowired
    private ClassroomRepository classRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuestionRepository questionRepo;

    @Autowired
    private OptionAnswerRepository optionRepo;

    @GetMapping
    public String dashboard(HttpSession session, Model model) {

        User teacher = (User) session.getAttribute("user");

        if (teacher == null || !teacher.getRole().equals("ROLE_TEACHER")) {
            return "redirect:/login";
        }

        List<Quiz> quizzes = quizRepo.findByUser_UserId(teacher.getId());
        List<Classroom> classes = classRepo.findByTeacher_UserId(teacher.getId());

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("classes", classes);

        return "teacher/dashboard";
    }

    //HÀM CHUNG CHO TẠO VÀ SỬA QUIZ
    private void buildQuizFromParams(Quiz quiz, Map<String, String> params) {

        String title = params.get("title");
        if (title == null || title.trim().isEmpty()) {
            throw new RuntimeException("Quiz title is required!");
        }
        quiz.setTitle(title.trim());

        String idxStr = params.get("questionIndexes");
        if (idxStr == null || idxStr.isBlank()) return;

        List<Integer> questionIndexes = Arrays.stream(idxStr.split(","))
                .map(Integer::parseInt)
                .toList();

        List<Question> questions = new ArrayList<>();

        for (Integer i : questionIndexes) {
            String content = params.get("q_" + i);
            if (content == null || content.isBlank()) continue;

            Question q = new Question();
            q.setContent(content.trim());
            q.setQuiz(quiz);

            String correctStr = params.get("q_" + i + "_correct");
            if (correctStr == null) throw new RuntimeException("Question " + i + " missing correct answer!");

            int correct;
            try {
                correct = Integer.parseInt(correctStr);
            } catch (Exception e) {
                throw new RuntimeException("Invalid correct answer at question " + i);
            }

            List<OptionAnswer> options = new ArrayList<>();
            int optionCount = 0;

            for (int j = 1; j <= 4; j++) {
                String optContent = params.get("q_" + i + "_opt" + j);
                if (optContent != null && !optContent.trim().isEmpty()) {
                    optionCount++;
                    OptionAnswer opt = new OptionAnswer();
                    opt.setContent(optContent.trim());
                    opt.setCorrect(j == correct);
                    opt.setQuestion(q);
                    options.add(opt);
                }
            }

            if (optionCount < 2) throw new RuntimeException("Question " + i + " must have at least 2 options!");
            if (correct < 1 || correct > 4) throw new RuntimeException("Invalid correct answer at question " + i);

            q.setOptions(options);
            questions.add(q);
        }

        if (questions.isEmpty()) throw new RuntimeException("Quiz must have at least one question!");

        if (quiz.getQuizId() != null) {
            optionRepo.deleteByQuiz(quiz);
            questionRepo.deleteByQuiz(quiz);
        }

        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(questions);
    }


    //TẠO QUIZ
    @GetMapping("/create")
    public String createPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("quiz", new Quiz());
        model.addAttribute("action", "/teacher/create");

        return "teacher/quiz_form";
    }

    @Transactional
    @PostMapping("/create")
    public String createQuiz(@RequestParam Map<String, String> params,
                            @RequestParam(required=false) Integer timeLimit,
                            @RequestParam(defaultValue = "false") boolean isPublic,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = new Quiz();
        quiz.setUser(user);
        quiz.setIsPublic(isPublic);

        if (timeLimit != null && timeLimit > 0) {
            quiz.setTimeLimit(timeLimit);
        }

        buildQuizFromParams(quiz, params);
        
        quizRepo.save(quiz);

        return "redirect:/teacher";
    }
    

    //SỬA QUIZ 
    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id,
                        HttpSession session,
                        Model model) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("action", "/teacher/edit/" + id);
            
        quiz.getQuestions().forEach(q ->
            q.getOptions().sort(Comparator.comparing(
                OptionAnswer::getOptionId,
                Comparator.nullsLast(Comparator.naturalOrder())
            ))
        );
        return "teacher/quiz_form";
    }

    @Transactional
    @PostMapping("/edit/{id}")
    public String updateQuiz(@PathVariable Long id,
                            @RequestParam Map<String, String> params,
                            @RequestParam boolean isPublic,
                            @RequestParam(required=false) Integer timeLimit,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }

        quiz.setIsPublic(isPublic);

        if (timeLimit != null && timeLimit > 0) {
            quiz.setTimeLimit(timeLimit);
        } else {
            quiz.setTimeLimit(null);
        }

        buildQuizFromParams(quiz, params);
        quizRepo.save(quiz);

        return "redirect:/teacher";
    }


    //XÓA CÂU HỎI
    @PostMapping("/question/delete/{id}")
    public String deleteQuestion(@PathVariable Long id,
                                @RequestParam Long quizId) {
        
        questionRepo.deleteById(id);

        return "redirect:/teacher/edit/" + quizId;
    }

    //XÓA QUIZ
    @Transactional 
    @GetMapping("/delete/{id}")
    public String deleteQuiz(@PathVariable Long id, HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(id).orElse(null);

        if (quiz == null || user == null || !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/quizzes";
        }


        List<Classroom> classes = classRepo.findAll();

        for (Classroom c : classes) {
            c.getAssignments().removeIf(a -> a.getQuiz() != null && a.getQuiz().getQuizId().equals(id));
        }

        classRepo.saveAll(classes);

        resultRepo.deleteByQuiz_QuizId(id);
        quizRepo.delete(quiz);

        return "redirect:/teacher";
    }




    @GetMapping("/quiz/{id}/results")
    public String quizResults(@PathVariable Long id, Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");
        
        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        model.addAttribute("results", resultRepo.findById(id));
        return "teacher/results";
    }


    @GetMapping("/classes")
    public String classPage(Model model, HttpSession session) {

        User teacher = (User) session.getAttribute("user");

        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        List<Classroom> classes = classRepo.findByTeacher_UserId(teacher.getId());
        List<Quiz> quizzes = quizRepo.findByUser_UserId(teacher.getId());

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("classes", classes);

        return "teacher/classes";
    }

    @PostMapping("/class/create")
    public String createClass(@RequestParam String name,
                            HttpSession session) {

        User teacher = (User) session.getAttribute("user");

        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Classroom c = new Classroom();
        c.setName(name);
        c.setTeacher(teacher);

        classRepo.save(c);

        return "redirect:/teacher/classes";
    }

    @PostMapping("/class/add-student")
    public String addStudent(@RequestParam Long classId,
                            @RequestParam String studentCodes,
                            HttpSession session) {

        User teacher = (User) session.getAttribute("user");
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Classroom c = classRepo.findById(classId).orElse(null);

        if (c == null) return "redirect:/teacher/classes";

        String[] codes = studentCodes.split("[,\\s]+"); 

        for (String code : codes) {
            User u = userRepo.findByStudentCode(code.trim());

            if (u != null && !c.getStudents().contains(u)) {
                c.getStudents().add(u);
            }
        }

        classRepo.save(c);

        return "redirect:/teacher/classes";
    }

    @PostMapping("/class/remove-student")
    public String removeStudent(@RequestParam Long classId,
                                @RequestParam Long userId,
                                HttpSession session) {

        User teacher = (User) session.getAttribute("user");
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Classroom c = classRepo.findById(classId).orElse(null);
        User u = userRepo.findById(userId).orElse(null);

        if (c != null && u != null) {
            c.getStudents().remove(u);
            classRepo.save(c);
        }

        return "redirect:/teacher/classes";
    }



    @PostMapping("/{id}/add-quiz")
    public String addQuizToClass(@PathVariable Long id,
                                @RequestParam Long quizId,
                                @RequestParam String startTime,
                                @RequestParam String endTime,
                                @RequestParam(required=false) Integer timeLimit,
                                HttpSession session) {

        User teacher = (User) session.getAttribute("user");
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Classroom c = classRepo.findById(id).orElse(null);
        Quiz q = quizRepo.findById(quizId).orElse(null);

        if (c == null || q == null) return "redirect:/teacher/classes";

        boolean exists = c.getAssignments().stream()
                        .anyMatch(a -> a.getQuiz() != null && a.getQuiz().getQuizId().equals(quizId));

        if (!exists) {
            Assignment a = new Assignment();
            a.setClassroom(c);
            a.setQuiz(q);

            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            a.setStartTime(LocalDateTime.parse(startTime, f));
            a.setEndTime(LocalDateTime.parse(endTime, f));
            if(timeLimit != null) a.setTimeLimit(timeLimit);

            assignmentRepo.save(a);
        }

        return "redirect:/teacher/classes";
}

    @PostMapping("/class/remove-quiz")
    @Transactional
    public String removeQuiz(@RequestParam Long classId,
                            @RequestParam Long quizId,
                            HttpSession session) {

        User teacher = (User) session.getAttribute("user");
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        assignmentRepo.deleteByClassroom_IdAndQuiz_QuizId(classId, quizId);

        return "redirect:/teacher/classes";
    }

    @Transactional
    @PostMapping("/class/delete")
    public String deleteClassroom(@RequestParam Long classId,
                                HttpSession session) {
                                
        User teacher = (User) session.getAttribute("user");
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Classroom classroom = classRepo
                .findById(classId)
                .orElseThrow();

        classroom.getStudents().clear();

        for (Assignment a : classroom.getAssignments()) {
            resultRepo.deleteByAssignment(a);
        }

        assignmentRepo.deleteAll(classroom.getAssignments());

        classRepo.flush();
        classRepo.delete(classroom);

        return "redirect:/teacher/classes";
    }


    @GetMapping("/result/{quizId}")
    public String viewQuizResults(@PathVariable Long quizId,
                                HttpSession session,
                                Model model) {

        User teacher = (User) session.getAttribute("user");

        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(quizId).orElse(null);

        if (quiz == null || !quiz.getUser().getId().equals(teacher.getId())) {
            return "redirect:/teacher";
        }

        List<Result> results = resultRepo.findByQuiz_QuizId(quizId);

        model.addAttribute("quiz", quiz);
        model.addAttribute("results", results);

        return "teacher/quiz_results";
    }


    @GetMapping("/import")
    public String importNewQuiz(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        return "teacher/import";
    }

    @PostMapping("/import")
    public String importNewQuiz(@RequestParam("title") String title,
                            @RequestParam("file") MultipartFile file,
                            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setUser(user);
        quiz.setQuestions(new ArrayList<>());

        List<Question> questions = new ArrayList<>();

        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 0; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String content = row.getCell(0).toString().trim();
                if (content.isEmpty()) continue;

                Question q = new Question();
                q.setContent(content);
                q.setQuiz(quiz);

                List<OptionAnswer> options = new ArrayList<>();

                Cell correctCell = row.getCell(5);
                if (correctCell == null) continue;

                int correct;
                try {
                    correct = (int) correctCell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        correct = Integer.parseInt(correctCell.toString().replace(".0", ""));
                    } catch (Exception ex) {
                        continue;
                    }
                }

                for (int j = 1; j <= 4; j++) {
                    Cell optCell = row.getCell(j);

                    String optContent =
                            optCell == null ? "" : optCell.toString().trim();

                    OptionAnswer opt = new OptionAnswer();
                    opt.setContent(optContent);
                    opt.setCorrect(j == correct);
                    opt.setQuestion(q);

                    options.add(opt);
                }

                q.setOptions(options);
                questions.add(q);
            }

            quiz.setQuestions(questions);

            System.out.println("Imported questions: " + questions.size());

            quizRepo.save(quiz);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "redirect:/teacher";
    }   


    @GetMapping("import/{quizId}")
    public String importToQuizPage(@PathVariable Long quizId,
                                HttpSession session,
                                Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(quizId).orElse(null);

        if (quiz == null || user == null ||
            !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/teacher";
        }

        model.addAttribute("quiz", quiz);
        return "teacher/import_to_quiz";
    }


    @PostMapping("import/{quizId}")
    @Transactional
    public String importToQuiz(@PathVariable Long quizId,
                               @RequestParam("file") MultipartFile file,
                               HttpSession session,
                               RedirectAttributes redirect) {
    
        User user = (User) session.getAttribute("user");
        if (user == null || !"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/login";
        }

        Quiz quiz = quizRepo.findById(quizId).orElse(null);
    
        if (quiz == null || user == null ||
            !quiz.getUser().getId().equals(user.getId())) {
            return "redirect:/teacher";
        }
    
        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
    
            Set<String> existingQuestions = new HashSet<>();
            for (Question q : quiz.getQuestions()) {
                existingQuestions.add(q.getContent().trim().toLowerCase());
            }
    
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
    
                Row row = sheet.getRow(i);
                if (row == null) continue;
    
                String content = row.getCell(0).toString().trim();
                if (content.isEmpty()) continue;
    
                String key = content.toLowerCase();
    
                if (existingQuestions.contains(key)) {
                    continue;
                }
    
                Question q = new Question();
                q.setContent(content);
                q.setQuiz(quiz);
    
                List<OptionAnswer> options = new ArrayList<>();
    
                Cell correctCell = row.getCell(5);
                if (correctCell == null) continue;
    
                int correct;
                try {
                    correct = (int) correctCell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        correct = Integer.parseInt(correctCell.toString().replace(".0", ""));
                    } catch (Exception ex) {
                        continue;
                    }
                }
    
                for (int j = 1; j <= 4; j++) {
                    Cell optCell = row.getCell(j);
                    String optContent;
                    if (optCell.getCellType() == CellType.NUMERIC) {
                        double val = optCell.getNumericCellValue();
                        optContent = (val == Math.floor(val)) 
                            ? String.valueOf((long) val) 
                            : String.valueOf(val);
                    } else {
                        optContent = optCell.toString().trim();
                    }    
                    OptionAnswer opt = new OptionAnswer();
                    opt.setContent(optContent);
                    opt.setCorrect(j == correct);
                    opt.setQuestion(q);
    
                    options.add(opt);
                }
    
                q.setOptions(options);
    
                quiz.getQuestions().add(q);
                existingQuestions.add(key);
            }
    
            quizRepo.save(quiz);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    
        return "redirect:/teacher";
    }

}
