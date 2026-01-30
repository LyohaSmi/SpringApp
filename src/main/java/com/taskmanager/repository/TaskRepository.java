package com.taskmanager.repository;

import com.taskmanager.model.Task;
import com.taskmanager.model.TaskStatus;
import com.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.id = :userId")
    List<Task> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<Task> findAllWithUser();

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByStatusWithUser(@Param("status") TaskStatus status);

    @Query("SELECT t FROM Task t JOIN FETCH t.user WHERE t.user.id = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByUserIdAndStatusWithUser(@Param("userId") Long userId, @Param("status") TaskStatus status);
}