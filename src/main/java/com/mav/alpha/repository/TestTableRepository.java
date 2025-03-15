package com.mav.alpha.repository;

import com.mav.alpha.entity.TestTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestTableRepository extends JpaRepository<TestTable, Long> {
    TestTable findFirstByName(String name);
}
