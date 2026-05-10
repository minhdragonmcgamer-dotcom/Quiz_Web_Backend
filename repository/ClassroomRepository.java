package com.example.quiz.repository;

import com.example.quiz.entity.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    
    List<Classroom> findByTeacher_UserId(Long id);
    List<Classroom> findByStudents_Id(Long id);
}
