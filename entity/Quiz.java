package com.example.quiz.entity;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "quiz")

public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long quizId;

    private String title;

    @ManyToOne
    @JoinColumn(name = "userId")
    private User user; 

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    private boolean isPublic = false;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Assignment> assignments;

    @Column(nullable = true)
    private Integer timeLimit; // đơn vị theo phút (null = không giới hạn)


    public Integer getTimeLimit() {
         return timeLimit; 
    }

    public void setTimeLimit(Integer timeLimit) { 
        this.timeLimit = timeLimit; 
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }


}