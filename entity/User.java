package com.example.quiz.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;
    private String password;
    private String role;
    private boolean locked = false; 

    // Mã sinh viên, chỉ dành cho role "student"
    @Column(unique = true)
    private String studentCode;


    public Long getId() { return userId; }
    public void setId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isLocked() { return locked; }
    public void setLocked() { this.locked = true; }
    public void setUnlocked() { this.locked = false; }

}

