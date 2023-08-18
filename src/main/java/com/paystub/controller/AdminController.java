package com.paystub.controller;

import com.paystub.dto.response.AdminSalaryResponse;
import com.paystub.dto.request.FileUploadRequest;
import com.paystub.dto.request.AdminDeleteSalaryRequest;
import com.paystub.dto.AdminUserListResponseAndUserSaveDao;
import com.paystub.dto.request.AdminSearchRequest;
import com.paystub.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/adminSelect")
    public String getAdminSelect() {
        return "adminSelect";
    }

    @GetMapping("/admin")
    public String getUploadPage(@Validated @ModelAttribute AdminSearchRequest request, BindingResult bindingResult,
                                Model model, RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            Map<String, String> errorMap = new HashMap<>();
            for (FieldError error : fieldErrors) {
                errorMap.put(error.getField(), error.getDefaultMessage());
            }
            redirectAttributes.addFlashAttribute("searchErrors", errorMap);
            return "redirect:/admin";
        }


        List<AdminSalaryResponse> adminSalaryRespons = adminService.findResponseByYearAndMonth(
                request.getYear(),
                request.getMonth(),
                request.getName(),
                request.getEmployeeID()
        );

        
        model.addAttribute("request", request);
        model.addAttribute("adminSalaryRespons", adminSalaryRespons);
        return "admin";
    }

    @PostMapping("/admin")
    public String handleFileUpload(@ModelAttribute @Valid FileUploadRequest form, BindingResult bindingResult,
                                   @RequestParam(required = false, defaultValue = "9999") Long year,
                                   @RequestParam(required = false, defaultValue = "0") Long month,
                                   Model model) {

        MultipartFile file = null;

        if (form != null && form.getFile() != null) {
            file = form.getFile();
        }

        if (file == null || file.isEmpty()) {
            // 파일이 비어 있을 경우 오류 메시지 설정
            bindingResult.rejectValue("file", "error.file", "파일을 선택해주세요.");
        } else {
            String contentType = file.getContentType();
            if (contentType == null ||
                    !(contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                            contentType.equals("application/vnd.ms-excel") ||
                            contentType.equals("application/vnd.ms-excel.sheet.macroEnabled.12") || // for .xlsm
                            contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))) { // for .xlsx
                bindingResult.rejectValue("file", "error.file", "엑셀 파일만 업로드 가능합니다.");
            }
        }

        List<AdminSalaryResponse> adminSalaryRespons = null;
        if (!bindingResult.hasErrors()) {
            // 엑셀 파일 처리 로직 작성
            adminSalaryRespons = adminService.processExcelFile(file, bindingResult, year, month); // 결과를 받아옴
        }

        if (bindingResult.hasErrors()) {
            // 오류 메시지를 모델에 추가
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());
            model.addAttribute("errors", errors);

            // 현재 페이지 데이터를 다시 로드할 필요가 없음
            if (adminSalaryRespons == null) {
                adminSalaryRespons = adminService.processExcelFile(file, bindingResult, year, month);
            }
            model.addAttribute("responseDtos", adminSalaryRespons);

            // 같은 뷰를 반환하여 현재 페이지에 오류를 표시
            return "admin";
        }
        AdminSearchRequest request = new AdminSearchRequest();
        request.setYear(year);
        request.setMonth(month);

        model.addAttribute("request", request); // 모델에 "req

        return "redirect:/admin"; // 성공적인 업로드 후 리다이렉션
    }

    // 관리자 페이지에서 UserForm을 선택했을 시 페이지 로드하는 메서드
    @GetMapping("/adminUserForm")
    public String getAdminUserForm(Model model) {

        List<AdminUserListResponseAndUserSaveDao> userList = adminService.getAdminUserForm();

        model.addAttribute("adminUserForm", userList);
        return "adminUserForm";
    }

    @PostMapping("/adminUserForm")
    public String deleteUsers(@RequestParam List<Long> employeeIds) {
        adminService.deleteUsersByIds(employeeIds);
        return "redirect:/adminUserForm"; // 삭제 후 관리자 페이지로 리다이렉트
    }

    @PostMapping("/adminDeleteSalary")
    public String deleteSalaries(@RequestParam List<String> salaryKeys) {
        List<AdminDeleteSalaryRequest> salaryIds = salaryKeys.stream()
                .map(key -> {
                    String[] parts = key.split("_");
                    return new AdminDeleteSalaryRequest(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
                })
                .collect(Collectors.toList());
        adminService.deleteSalariesByIds(salaryIds);
        return "redirect:/admin";
    }


}