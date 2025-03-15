package com.mav.alpha.service;

import com.mav.alpha.entity.TestTable;

public interface TestService {
    TestTable putTable(String name);
    TestTable getTable(String name);
}
