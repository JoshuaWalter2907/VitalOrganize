package com.springboot.vitalorganize.repository;

import com.springboot.vitalorganize.entity.Fund_Payments.FundEntity;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FundRepository extends JpaRepository<FundEntity, Long> {

    List<FundEntity> findByAdmin(UserEntity admin);

    @Query("SELECT f.name FROM FundEntity f WHERE f.id = :id")
    String findNameById(@Param("id") Long id);

    List<FundEntity> findByUsers_Id(Long id);
}
