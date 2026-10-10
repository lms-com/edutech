package com.lms.finance.repository;

import com.lms.finance.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount,String> {
    Optional<BankAccount> findByInstructorIdAndIsPrimaryTrue(String instructorId);
}
