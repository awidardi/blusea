package com.blibli.future.controller;

import com.blibli.future.model.*;
import com.blibli.future.repository.OrderRepository;
import com.blibli.future.repository.UserRoleRepository;
import com.blibli.future.security.SecurityService;
import com.blibli.future.utility.Helper;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.data.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.blibli.future.repository.CateringRepository;
import com.blibli.future.repository.ProductRepository;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class CateringController {

    @Autowired
    private Environment env;
    @Autowired
    private CateringRepository cateringRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private Helper helper;
    @Autowired
    private SecurityService securityService;

    private static final Logger logger = LoggerFactory
            .getLogger(CateringController.class);

    @ModelAttribute("helper")
    public Helper getHelper() {
        return helper;
    }

    @ModelAttribute("activeUser")
    public User getActiveUser() {
        return helper.getCurrentUser();
    }

    @RequestMapping(value="/my-catering/register",method=RequestMethod.GET)
    public String cateringRegisterForm(
            Model model,
            HttpServletRequest request)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        return "catering/register";
    }

    @RequestMapping(value="/my-catering/register",method=RequestMethod.POST)
    public String cateringAdd(
            @ModelAttribute Catering newCatering,
            Model model,
            HttpServletRequest request){
        cateringRepository.save(newCatering);
        UserRole r = new UserRole();
        r.setUsername(newCatering.getUsername());
        r.setRole("ROLE_CATERING");
        userRoleRepository.save(r);
        helper.authenticateUserAndSetSession(newCatering , request);

        return "redirect:/my-catering/profile";
    }

    @RequestMapping(value="/my-catering/addproducts",method=RequestMethod.GET)
    public String cateringGetAddProduct(
            Model model,
            HttpServletRequest request)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        model.addAttribute("catering", helper.getCurrentCatering());

        return "catering/addproducts";
    }

    @RequestMapping(value="/my-catering/addproducts", method=RequestMethod.POST)
    public String cateringPostAddProduct(
            @ModelAttribute Product newProduct,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request){
        Catering catering = helper.getCurrentCatering();

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = format1.format(cal.getTime());

        if(catering != null){
            if(!catering.hasProduct(newProduct)){
                newProduct.setCatering(catering);
            }
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();

                    String fileName = UUID.randomUUID().toString().replaceAll("-","");

                    // Creating the directory to store file
                    //String rootPath = System.getProperty("catalina.home");
                    File dir = new File(env.getProperty("blusea.PhotoDir.path") + "/Product/" + formatted);
                    if (!dir.exists())
                        dir.mkdirs();

                    // Create the file on server
                    File serverFile = new File(dir.getAbsolutePath()
                            + File.separator + fileName + ".jpg");
                    newProduct.setPhoto("http://localhost/gambar/Product"
                            + File.separator + formatted + File.separator + fileName + ".jpg");
                    BufferedOutputStream stream = new BufferedOutputStream(
                            new FileOutputStream(serverFile));
                    stream.write(bytes);
                    stream.close();

                    logger.info("Server File Location="
                            + serverFile.getAbsolutePath());

                } catch (Exception e) {
                    return "You failed to upload " + newProduct.getName() + " => " + e.getMessage();
                }
            } else {
                return "You failed to upload " + newProduct.getName()
                        + " because the file was empty.";
            }

            String[] quantity = request.getParameterValues("quantity");
            String[] price = request.getParameterValues("price");
            ArrayList<Pair<Integer, Integer>> pricePair = new ArrayList<>();
            for(int i=0;i<quantity.length;i++){
                pricePair.add(Pair.of(Integer.parseInt(quantity[i]) , Integer.parseInt(price[i])));
            }

            Collections.sort(pricePair, new Comparator<Pair<Integer,Integer>>() {
                @Override public int compare(Pair<Integer,Integer> x, Pair<Integer,Integer> y) {
                    return x.getFirst() - y.getFirst();
                }
            });

            for(int i=0;i<pricePair.size();i++){
                quantity[i] = String.valueOf(pricePair.get(i).getFirst());
                price[i] = String.valueOf(pricePair.get(i).getSecond());
            }

            String newProductPrice = helper.setProductPrice(quantity,price);
            newProduct.setPrice(newProductPrice);

            productRepository.save(newProduct);
            cateringRepository.save(catering);
            return "redirect:/my-catering/profile";
        }
        return "redirect:/my-catering/profile";
    }

    @RequestMapping(value="/my-catering/profile" , method = RequestMethod.GET)
    public String showMyCateringProfile(
            Model model,
            HttpServletRequest request)
    {
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);

        Catering catering = (Catering) helper.getCurrentUser();
        model.addAttribute("catering", catering);
        return "catering/profile";
    }

    //editing
    @RequestMapping(value="/my-catering/edit", method= RequestMethod.GET)
    public String editCateringForm(
            HttpServletRequest request,
            Model model)
    {
        Catering catering = (Catering) helper.getCurrentUser();
        model.addAttribute("catering", catering);

        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);
        return "catering/edit";
    }

    @RequestMapping(value="/my-catering/edit", method= RequestMethod.POST)
    public String editCatering(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request)
    {
        Catering catering = helper.getCurrentCatering();

        //photo
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = format1.format(cal.getTime());

        if(catering != null){
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();

                    String fileName = UUID.randomUUID().toString().replaceAll("-","");

                    // Creating the directory to store file
                    File dir = new File(env.getProperty("blusea.PhotoDir.path") + "/Catering/" + formatted);
                    if (!dir.exists())
                        dir.mkdirs();

                    // Create the file on server
                    File serverFile = new File(dir.getAbsolutePath()
                            + File.separator + fileName + ".jpg");
                    catering.setPhoto("http://localhost/gambar/Catering"
                            + File.separator + formatted + File.separator + fileName + ".jpg");
                    BufferedOutputStream stream = new BufferedOutputStream(
                            new FileOutputStream(serverFile));
                    stream.write(bytes);
                    stream.close();

                    logger.info("Server File Location="
                            + serverFile.getAbsolutePath());

                } catch (Exception e) {
                    return "You failed to upload " + catering.getCateringName() + " => " + e.getMessage();
                }
            }
        }
        catering.setPassword(request.getParameter("password"));
        catering.setEmail(request.getParameter("email"));
        catering.setCateringName(request.getParameter("cateringName"));
        catering.setAddress(request.getParameter("address"));
        catering.setDescription(request.getParameter("description"));
        catering.setPhoneNumber(request.getParameter("phoneNumber"));
        catering.setDp(request.getParameter("dp"));

        cateringRepository.save(catering);
        return "redirect:/my-catering/profile";
    }

    @RequestMapping(value="/my-catering/product/{id}/delete" , method = RequestMethod.POST)
    public String deleteProduct(
            @PathVariable long id,
            Model model)
    {
        Catering catering = (Catering) helper.getCurrentUser();
        //model.addAttribute("catering", catering);

        Product product = productRepository.findOne(id);
        productRepository.delete(product);

        return "redirect:/my-catering/profile";
    }

    @RequestMapping(value="/my-catering/product/{id}/edit", method= RequestMethod.GET)
    public String editProductForm(
            @PathVariable long id,
            HttpServletRequest request,
            Model model)
    {
        Product product = productRepository.findOne(id);
        model.addAttribute("product", product);
        ArrayList<Pair<Integer , Integer >> productPriceList = product.getPricePair();
        model.addAttribute("productPriceList",productPriceList);
        String _csrf = ((CsrfToken) request.getAttribute("_csrf")).getToken();
        model.addAttribute("_csrf", _csrf);
        return "catering/editProduct";
    }

    @RequestMapping(value = "/my-catering/product/{id}/edit" , method = RequestMethod.POST)
    public String editProduct(
            @RequestParam("file") MultipartFile file,
            @PathVariable long id,
            HttpServletRequest request)
    {
        Catering catering = (Catering) helper.getCurrentUser();

        Product product = productRepository.findOne(id);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = format1.format(cal.getTime());

        if(catering != null){
            if (!file.isEmpty()) {
                try {
                    byte[] bytes = file.getBytes();

                    String fileName = UUID.randomUUID().toString().replaceAll("-","");

                    // Creating the directory to store file
                    //String rootPath = System.getProperty("catalina.home");
                    File dir = new File(env.getProperty("blusea.PhotoDir.path") + "/Product/" + formatted);
                    if (!dir.exists())
                        dir.mkdirs();

                    // Create the file on server
                    File serverFile = new File(dir.getAbsolutePath()
                            + File.separator + fileName + ".jpg");
                    BufferedOutputStream stream = new BufferedOutputStream(
                            new FileOutputStream(serverFile));
                    stream.write(bytes);
                    stream.close();
                    product.setPhoto("http://localhost/gambar/Product"
                        + File.separator + formatted + File.separator + fileName + ".jpg");

                    logger.info("Server File Location="
                            + serverFile.getAbsolutePath());

                } catch (Exception e) {
                    return "You failed to upload " + product.getName() + " => " + e.getMessage();
                }
            }
            product.setDescription(request.getParameter("description"));
            product.setName(request.getParameter("name"));
            String[] quantity = request.getParameterValues("quantity");
            String[] price = request.getParameterValues("price");
            ArrayList<Pair<Integer, Integer>> pricePair = new ArrayList<>();
            for(int i=0;i<quantity.length;i++){
                pricePair.add(Pair.of(Integer.parseInt(quantity[i]) , Integer.parseInt(price[i])));
            }

            Collections.sort(pricePair, new Comparator<Pair<Integer,Integer>>() {
                @Override public int compare(Pair<Integer,Integer> x, Pair<Integer,Integer> y) {
                    return x.getFirst() - y.getFirst();
                }
            });

            for(int i=0;i<pricePair.size();i++){
                quantity[i] = String.valueOf(pricePair.get(i).getFirst());
                price[i] = String.valueOf(pricePair.get(i).getSecond());
            }

            String newProductPrice = helper.setProductPrice(quantity,price);
            product.setPrice(newProductPrice);
            productRepository.save(product);
            cateringRepository.save(catering);
            return "redirect:/my-catering/profile";
        }
        return "redirect:/my-catering/profile";

    }

    @RequestMapping(value = "/my-catering/order")
    public String orderIndex(Model model) {
        // Get ALL order except the one that still have CART status
        // Catering should not see CART
        List<Order> orderList = orderRepository.findByCateringEmailAndStatus(helper.getEmail(), Order.ORDER_STATUS_PENDING);
        orderList.addAll(orderRepository.findByCateringEmailAndStatus(helper.getEmail(), Order.ORDER_STATUS_WAITING));
        orderList.addAll(orderRepository.findByCateringEmailAndStatus(helper.getEmail(), Order.ORDER_STATUS_COMPLETE));

        model.addAttribute("orderList", orderList);
        return "catering/order";
    }

    @RequestMapping(value = "/my-catering/order/{id}/confirm")
    public String processOrder(@PathVariable long id) {
        Order order = orderRepository.findOne(id);
        if (order.isPending()) {
            order.setStatus(Order.ORDER_STATUS_WAITING);
        } else if (order.isWaiting()) {
            order.setStatus(Order.ORDER_STATUS_COMPLETE);
        }
        orderRepository.save(order);
        return "redirect:/my-catering/order";
    }

    @RequestMapping(value = "/my-catering/order/{id}/delete", method = RequestMethod.GET)
    public String deleteOrder(
            @PathVariable int id) {
        // TODO remove its orderDetail data in database.
        orderRepository.delete((long) id);
        return "redirect:/my-catering/order";
    }
}
