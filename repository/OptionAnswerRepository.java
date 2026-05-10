package com.example.quiz.repository;

import com.example.quiz.entity.OptionAnswer;
import com.example.quiz.entity.Quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OptionAnswerRepository extends JpaRepository<OptionAnswer, Long> {

    @Modifying  
    @Query("DELETE FROM OptionAnswer o WHERE o.question IN (SELECT q FROM Question q WHERE q.quiz = :quiz)")
    void deleteByQuiz(@Param("quiz") Quiz quiz);

}