package com.example.banking.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="users")
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=254) private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(name="full_name", nullable=false, length=120) private String fullName;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="user_roles", joinColumns=@JoinColumn(name="user_id")) @Enumerated(EnumType.STRING) @Column(name="role", nullable=false) private Set<Role> roles=new HashSet<>();
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected AppUser() {}
    public AppUser(String email,String passwordHash,String fullName,Role role){this.email=email;this.passwordHash=passwordHash;this.fullName=fullName;roles.add(role);}
    public Long getId(){return id;} public String getEmail(){return email;} public String getPasswordHash(){return passwordHash;} public String getFullName(){return fullName;} public Set<Role> getRoles(){return Collections.unmodifiableSet(roles);}
}
