package mz.mobile.bussiness.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/greet")
public class GreetingsController {

    @GetMapping
    public ResponseEntity<String> greetHello(@RequestParam String name){
        return ResponseEntity.ok().body("Hello " + name + "!");
    }
}
