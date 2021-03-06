package com.blibli.future.controller;

import com.blibli.future.model.Catering;
import com.blibli.future.model.Customer;
import com.blibli.future.model.Product;
import com.blibli.future.model.User;
import com.blibli.future.repository.CateringRepository;
import com.blibli.future.repository.CustomerRepository;
import com.blibli.future.repository.ProductRepository;
import com.blibli.future.utility.Helper;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class PublicController {

	@Autowired
	CateringRepository cateringRepository;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	ProductRepository productRepository;

	@Autowired
	Helper helper;

	@ModelAttribute("helper")
	public Helper getHelper() {
		return helper;
	}

	@ModelAttribute("activeUser")
	public User getActiveUser() {
		return helper.getCurrentUser();
	}

	@RequestMapping("/")
	public String landing(
			Model model) {
		List<Product> productList = productRepository.findAll().subList(0,12);
		List<Catering> firstSixCatering = cateringRepository.findAll().subList(0, 6);
		model.addAttribute("caterings", firstSixCatering);
		model.addAttribute(productList);
        return "public/landing";
	}

	@RequestMapping(value="/login", method= RequestMethod.GET)
	public String authenticateUser(
			@RequestParam(value = "error" , required = false) String error,
			@RequestParam(value = "logout" , required = false) String logout,
			@ModelAttribute User newUser,
			HttpServletRequest request,
			Model model)
	{
		if (error != null) {
			model.addAttribute("error", "Invalid username and password!");

			System.out.print("Haha");
			//login form for update page
			//if login error, get the targetUrl from session again.
			String targetUrl = getRememberMeTargetUrlFromSession(request);
			System.out.println(targetUrl);
			if(StringUtils.hasText(targetUrl)){
				model.addAttribute("targetUrl", targetUrl);
			}

		}

		if (logout != null) {
			model.addAttribute("msg", "You've been logged out successfully.");
		}

		String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
		model.addAttribute("_csrf", _csrf);
		return "public/login";
	}

	@RequestMapping(value="/login/process")
	public String decideUser(Model model) {
		User user = helper.getCurrentUser();
		if(user instanceof Customer){
			return "redirect:/my-customer/profile";
		}else if(user instanceof Catering){
			return "redirect:/my-catering/profile";
		}
		return "redirect:/";
	}

	//catering

	@RequestMapping(value="/catering", method=RequestMethod.GET)
	public String showAllCateringList(Model model, HttpServletRequest request){
	    final String PAGE_PARAMETER = "page";
	    final String SIZE_PARAMETER = "count";
	    final String ORDER_BY_PARAMETER = "sort";
		int currentPage;
		int cateringPerPage;
		String sortBy = "id";

		// Try to get current page from URL, if parameter is empty, open first page
		try {
			currentPage = Integer.parseInt(request.getParameter(PAGE_PARAMETER));
		}
		catch (NumberFormatException e) {
			currentPage = 1;
		}
		try {
			cateringPerPage = Integer.parseInt(request.getParameter(SIZE_PARAMETER));
		}
		catch (NumberFormatException e) {
			cateringPerPage = 8;
		}
		if (request.getParameter(ORDER_BY_PARAMETER) != null) {
            sortBy = request.getParameter(ORDER_BY_PARAMETER);
        }

        // variables related to pagination calculation
		int startIndex = (currentPage-1) * cateringPerPage;
		int endIndex = startIndex + cateringPerPage;
        int cateringLastIndex;
        List<Catering> allCatering;

        // get catering in order of what our request ask.
        System.out.println(sortBy);
        if (sortBy.equals("name")) {
            allCatering = cateringRepository.findAllByOrderByCateringName();
        } else {
            allCatering = cateringRepository.findAll();
        }
        cateringLastIndex = allCatering.size() - 1;

		// Please don't try to process negative page :)
		if (currentPage < 1) {
			return "redirect:/catering";
		}

		// Avoid accessing out of bound page
		if (startIndex > cateringLastIndex) {
			return "redirect:/catering?" + PAGE_PARAMETER + "=" + (currentPage-1) + "&"+ SIZE_PARAMETER + "=" + cateringPerPage;
		}

		// End of list cutting  must match Catering number
		if (endIndex > cateringLastIndex) {
			endIndex = cateringLastIndex+1;
			model.addAttribute("isLastPage", true);
		}

		if (currentPage == 1) {
			model.addAttribute("isFirstPage", true);
		}
		List<Catering> subset = allCatering.subList(startIndex, endIndex);

		model.addAttribute("caterings", subset);
		model.addAttribute("cateringPerPage", cateringPerPage);
		model.addAttribute("currentPage", currentPage);
		model.addAttribute("prevPage", currentPage-1);
		model.addAttribute("nextPage", currentPage+1);
		model.addAttribute("start", startIndex+1);
		model.addAttribute("end", endIndex);
		model.addAttribute("total", cateringLastIndex+1);

		return "public/catering-index";
	}

	@RequestMapping("/catering/{username}")
	public String cateringPublicProfile(
			@PathVariable String username,
			HttpServletRequest request,
			Model model){
		String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
		model.addAttribute("_csrf", _csrf);

		Catering catering = cateringRepository.findByUsername(username);
		model.addAttribute("catering", catering);
		model.addAttribute("products", catering.getProducts());

		return "public/catering-profile";
	}

	//Customer
	@RequestMapping("/customer/{username}")
	public String showPublicUserProfile(
			@PathVariable String username,
			Model model)
	{
		model.addAttribute("customer", customerRepository.findByUsername(username));
		return "/customer/dashboard";
	}

	/**
	 * Check if user is login by remember me cookie, refer
	 * org.springframework.security.authentication.AuthenticationTrustResolverImpl
	 */
	private boolean isRememberMeAuthenticated() {

		Authentication authentication =
				SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null) {
			return false;
		}

		return RememberMeAuthenticationToken.class.isAssignableFrom(authentication.getClass());
	}

	/**
	 * save targetURL in session
	 */
	private void setRememberMeTargetUrlToSession(HttpServletRequest request){
		HttpSession session = request.getSession(false);
		if(session!=null){
			session.setAttribute("targetUrl", "/admin/update");
		}
	}

	/**
	 * get targetURL from session
	 */
	private String getRememberMeTargetUrlFromSession(HttpServletRequest request){
		String targetUrl = "";
		HttpSession session = request.getSession(false);
		if(session!=null){
			targetUrl = session.getAttribute("targetUrl")==null?""
					:session.getAttribute("targetUrl").toString();
		}
		return targetUrl;
	}

	/**
	 * This update page is for user login with password only.
	 * If user is login via remember me cookie, send login to ask for password again.
	 * To avoid stolen remember me cookie to update info
	 */
	@RequestMapping(value = "/admin/update**", method = RequestMethod.GET)
	public String updatePage(
			HttpServletRequest request,
			Model model) {

		if (isRememberMeAuthenticated()) {
			//send login for update
			System.out.println("Masuk");
			setRememberMeTargetUrlToSession(request);
			model.addAttribute("loginUpdate", true);
			return "redirect::/login";

		} else {
			return "login";
		}

	}
}
