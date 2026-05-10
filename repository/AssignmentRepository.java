package com.example.quiz.repository;

import com.example.quiz.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.stereotype.Repository;


public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByClassroom_Id(Long classId);
    void deleteByClassroom_IdAndQuiz_QuizId(Long classId, Long quizId);
}
