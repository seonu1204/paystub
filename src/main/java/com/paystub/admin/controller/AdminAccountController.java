package com.paystub.admin.controller;

import com.paystub.admin.service.ManagementUserService;
import com.paystub.user.dto.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminAccountController {

    private final ManagementUserService managementUserService;

    // 관리자 페이지에서 UserForm을 선택했을 시 페이지 로드하는 메서드
    @GetMapping("/adminUserManagement")
    public String getAdminUserForm(Model model) {

        // 관리자용 사용자 목록을 가져옴
        List<UserDao> userList = managementUserService.getAdminUserForm();

        for (UserDao userDao : userList) {
            log.info("-------------------------- userDao.getName() = " + userDao.getName());
            log.info("-------------------------- userDao.getEmployeeID() = " + userDao.getEmployeeID());
        }
        model.addAttribute("adminUserForm", userList);

        // 관리자용 사용자 관리 페이지 반환
        return "admin/adminUserManagement";
    }

    @PostMapping("/adminUserManagement")
    public String deleteUsers(@RequestParam List<Long> employeeIds) {

        // 사용자 ID 목록을 기반으로 사용자 삭제
        managementUserService.deleteUsersByIds(employeeIds);

        // 삭제 후 관리자 페이지로 리다이렉트
        return "redirect:/adminUserManagement";
    }

}
