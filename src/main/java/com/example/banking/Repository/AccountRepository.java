package com.example.banking.Repository;

import com.example.banking.Entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.converter.json.GsonBuilderUtils;

public interface AccountRepository extends JpaRepository<Account, Long> {


}
