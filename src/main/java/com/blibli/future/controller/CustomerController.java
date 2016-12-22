package com.blibli.future.controller;

import com.blibli.future.model.*;
import com.blibli.future.model.UserRole;
import com.blibli.future.repository.*;
import com.blibli.future.security.SecurityService;
import com.blibli.future.security.SecurityService;
import com.blibli.future.repository.UserRoleRepository;
import com.blibli.future.utility.Helper;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;


import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CustomerController{
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CateringRepository cateringRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderDetailRepository orderDetailRepository;
    @Autowired
    private Helper helper;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private SecurityService securityService;

    @RequestMapping(value="/my-customer/profile" , method = RequestMethod.GET)
    public String showMyCustomerProfile(ModelMap model)
    {
        Customer customer = (Customer) helper.getCurrentUser();
        model.addAttribute("customer", customer);
        return "customer/profile";
    }

    @RequestMapping(value="/register", method= RequestMethod.GET)
    public String addUserForm(
            HttpServletRequest request,
            Model model)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);
        return "customer/register";
    }

    @RequestMapping(value="/register", method= RequestMethod.POST)
    public String addUser(
            @ModelAttribute Customer newCustomer,
            Model model)
    {
        customerRepository.save(newCustomer);
        model.addAttribute("customer", newCustomer);
        UserRole r = new UserRole();
        r.setUsername(newCustomer.getUsername());
        r.setRole("ROLE_CUSTOMER");
        userRoleRepository.save(r);
        securityService.autologin(newCustomer.getUsername(), newCustomer.getPassword());
        System.out.println("Success");
        return "redirect:/my-customer/profile";
    }

    @RequestMapping(value="/customer/{username}/order",method = RequestMethod.GET)
    public String showOrder(
            @PathVariable String username,
            Model model,
            HttpServletRequest request)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        model.addAttribute("customer", customerRepository.findByUsername(username));

        return "customer/order";
    }


    @RequestMapping(value="/my-customer/edit", method= RequestMethod.GET)
    public String editCustomerForm(
            HttpServletRequest request,
            Model model)
    {
        Customer customer = (Customer) helper.getCurrentUser();
        model.addAttribute("customer", customer);
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);
        return "customer/edit";
    }

    @RequestMapping(value="/my-customer/edit", method= RequestMethod.POST)
    public String editCustomer(
            HttpServletRequest request)
    {
        Customer customer = (Customer) helper.getCurrentUser();
        customer.setUsername(request.getParameter("username"));
        customer.setPassword(request.getParameter("password"));
        customer.setEmail(request.getParameter("email"));
        customer.setFullName(request.getParameter("fullName"));
        customer.setNickName(request.getParameter("nickName"));
        customerRepository.save(customer);
        return "redirect:/my-customer/profile";
    }

    @RequestMapping(value = "my-customer/order")
    public String orderIndex(Model model) {
        Customer customer = helper.getCurrentCustomer();
        List<Order> orderList = customer.getOrders();
        orderList.size(); // lazy load the order
        model.addAttribute("orderList", orderList);
        model.addAttribute("customer", customer);
        return "customer/order";
    }

    @RequestMapping(value = "/my-customer/order/new", method = RequestMethod.POST)
    public String createNewOrder(HttpServletRequest request)
    {
        Order order = new Order();
        int orderQuantity = Integer.parseInt(request.getParameter("quantity"));
        Catering catering = cateringRepository.findOne(Long.parseLong(request.getParameter("catering-id")));
        order.setCustomer(helper.getCurrentCustomer());
        order.setCatering(catering);
        order.setQuantities(orderQuantity);
        order.setCreateDate(new Date());
        try {
            order.setDeliveryDate((new SimpleDateFormat("dd/MM/yyyy").parse(request.getParameter("deliveryDate"))));
        }
        catch (java.text.ParseException e) {
            // TODO this should be throwing error to user and prompt them to
            // re-input delivery date correctly.
            order.setDeliveryDate(new Date());
        }
        order.setNote(request.getParameter("note"));
        orderRepository.save(order);

        for(String productId: request.getParameterValues("choosen-product")) {
            Product orderedProduct = productRepository.findOne(Long.parseLong(productId));
            OrderDetail od = new OrderDetail(order, orderedProduct);
            orderDetailRepository.save(od);
        }
        order.updateTotalPrices();
        orderRepository.save(order);

        return "redirect:/my-customer/order/cart";
    }

    @RequestMapping(value = "/my-customer/order/cart", method = RequestMethod.GET)
    public String orderCart(
            Model model,
            HttpServletRequest request)
    {
        String email = helper.getCurrentCustomer().getEmail();
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        model.addAttribute("customer", helper.getCurrentCustomer());
        model.addAttribute("order", orderRepository.findByCustomerEmailAndStatus(email, Order.ORDER_STATUS_CART).get(0));

        return "/customer/cart";
    }

    @RequestMapping(value = "/my-costumer/order/{id}/confirmation", method = RequestMethod.POST)
    public String checkoutOrder(
            Model model,
            HttpServletRequest request,
            @PathVariable int id)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        Order o = orderRepository.findOne((long) id);
        if(o.getStatus() == Order.ORDER_STATUS_CART) {
            o.setStatus(Order.ORDER_STATUS_PENDING);
        }
        orderRepository.save(o);
        return "redirect:/my-customer/order";
    }
}
