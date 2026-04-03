package server.sea_battle_server.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
class HelloWorldController {
    @GetMapping("/")
    public String hello() {
        return "Hello World";
    }
}
