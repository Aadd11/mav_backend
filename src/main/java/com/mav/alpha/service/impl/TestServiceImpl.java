package com.mav.alpha.service.impl;

import com.mav.alpha.entity.TestTable;
import com.mav.alpha.repository.TestTableRepository;
import com.mav.alpha.service.TestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestServiceImpl implements TestService {
    private final TestTableRepository testTableRepository;
    public TestTable putTable(String name) {
        TestTable testTable = new TestTable(name);
        testTableRepository.saveAndFlush(testTable);
        return testTable;
    }
    public TestTable getTable(String name){
        return testTableRepository.findFirstByName(name);
    }
}
