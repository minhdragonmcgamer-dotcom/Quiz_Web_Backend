package com.example.quiz.repository;

import com.example.quiz.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByUser_UserId(Long user_id);
    void deleteByQuiz_QuizId(Long quizId);
    void deleteByUser_UserId(Long userId);
    List<Result> findByQuiz_QuizId(Long quizId);

    boolean existsByUser_UserIdAndQuiz_QuizId(Long userId, Long quizId);
}