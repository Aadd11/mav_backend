package com.mav.alpha.controller;

import com.mav.alpha.entity.TestTable;
import com.mav.alpha.service.TestService;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class test {
    private final TestService testService;
    @GetMapping
    public ResponseEntity<String> getTest(){
        String testStr = "MYAAAAV!";
        return ResponseEntity.ok(testStr);
    }
    @GetMapping("/getTable")
    public ResponseEntity<TestTable> getTable(@RequestParam("name") String name){
        TestTable testTable = testService.getTable(name);
        return ResponseEntity.ok(testTable);
    }

    @PostMapping("/putTable")
    public ResponseEntity<String> putTable(@RequestParam("name") String name){
        testService.putTable(name);
        return ResponseEntity.status(HttpStatus.CREATED).body("table with name " + name + " created.");
    }

}
