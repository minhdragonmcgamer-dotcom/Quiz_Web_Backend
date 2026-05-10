package com.example.quiz.repository;

import com.example.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.quiz.entity.Quiz;


public interface QuestionRepository extends JpaRepository<Question, Long> {
    void deleteByQuestionId(Long questionId);

    
    @Modifying
    @Query("DELETE FROM Question q WHERE q.quiz = :quiz")
    void deleteByQuiz(@Param("quiz") Quiz quiz);
}