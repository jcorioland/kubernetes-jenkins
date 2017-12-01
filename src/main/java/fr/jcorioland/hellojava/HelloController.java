package fr.jcorioland.hellojava;

import java.net.InetAddress;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    @RequestMapping("/hello")
    public String hello() {
        String hostname = "Unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            // nothing.
        }
        return "Hello Kubernetes !! I am running on " + hostname;
    }

    @RequestMapping("/echo")
    public String test() {
        return "echo";
    }
}