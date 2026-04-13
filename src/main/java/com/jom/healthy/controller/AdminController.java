package com.jom.healthy.controller;

import com.jom.healthy.entity.FoodNutrition;
import com.jom.healthy.service.AppDownLoadService;
import com.jom.healthy.service.FoodNutritionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class AdminController {

    @Autowired
    private FoodNutritionService foodNutritionService;

    @Resource
    private AppDownLoadService appDownLoadService;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    // 首页
    @GetMapping("/")
    public String indexPage() {
        return "index";
    }

    @GetMapping("/getDownLoadUrl")
    @ResponseBody
    public String getDownLoadUrl() {
        return appDownLoadService.getAppNewVersionUrl();
    }

    // 1. 登录跳转
    @GetMapping("/admin")
    public String loginPage() {
        return "login";
    }

    // 2. 登录验证
    @PostMapping("/login")
    public String doLogin(String username, String password, HttpSession session) {
        if ("admin".equals(username) && adminPassword.equals(password)) {
            session.setAttribute("user", "admin");
            return "redirect:/manage";
        }
        return "redirect:/?error=true";
    }

    // 3. 管理主页 (本地查询)
    @GetMapping("/manage")
    public String managePage(@RequestParam(required = false) String name, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) return "redirect:/";
        List<FoodNutrition> foods = foodNutritionService.queryFood(name);
        model.addAttribute("foods", foods);
        return "manage";
    }

    @PostMapping("/modify")
    @ResponseBody
    public String modify(@RequestParam Integer id, @RequestParam String picUrl, HttpSession session) {
        if (session.getAttribute("user") == null) return "Unauthorized";

        FoodNutrition food = new FoodNutrition();
        food.setId(id);
        food.setPicUrl(picUrl); // 只设置图片URL字段

        // updateById 内部逻辑：UPDATE food SET pic_url = ? WHERE id = ?
        boolean success = foodNutritionService.updateById(food);
        return success ? "success" : "fail";
    }
}
