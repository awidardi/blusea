package com.blibli.future.controller;

import com.blibli.future.model.User;
import com.blibli.future.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by dhika on 29/08/2016.
 */
@Controller
public class UserController {
    @Autowired
    private UserRepository repo;

    @RequestMapping("/user/{userId}")
    public @ResponseBody String show(
            @PathVariable(value="userId") String userId,
            Model model)
    {
        long id = Integer.getInteger(userId);
        model.addAttribute("user", (User) repo.findOne(id));
        return "user/dashboard";
    }

}